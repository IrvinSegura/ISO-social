package com.nexus.feature.post.presentation.create

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.SystemClock
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

private const val FRESH_FIX_TIMEOUT_MS = 6_000L
private const val LOCATION_TIMEOUT_MS = 10_000L
private const val MAX_LAST_LOCATION_AGE_MS = 30 * 60 * 1000L

class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    suspend fun getCurrentCityName(): String? {
        val location = currentOrLastLocation() ?: run {
            Timber.w("Location: no current or last known fix")
            return null
        }
        return cityName(location)
    }

    // Only the city is shown, so a recent last known fix answers at once. A fresh fix can
    // take a while (or never come indoors), and an old last known one still beats nothing.
    @SuppressLint("MissingPermission")
    private suspend fun currentOrLastLocation(): Location? {
        val last = lastKnownLocation()
        if (last != null && last.ageMs() <= MAX_LAST_LOCATION_AGE_MS) return last

        val cts = CancellationTokenSource()
        val current = try {
            withTimeoutOrNull(FRESH_FIX_TIMEOUT_MS) {
                fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Location: getCurrentLocation failed")
            null
        }
        if (current != null) return current
        cts.cancel()
        Timber.i("Location: no fresh fix, using the last known location")
        return last
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastKnownLocation(): Location? = try {
        fusedLocationClient.lastLocation.await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "Location: lastLocation failed")
        null
    }

    private fun Location.ageMs(): Long =
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000

    private suspend fun cityName(location: Location): String? {
        if (!Geocoder.isPresent()) {
            Timber.w("Location: no Geocoder backend on this device")
            return null
        }
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // onError must resume too, or a geocoder failure leaves the spinner on forever
                withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (cont.isActive) cont.resume(addresses.firstOrNull()?.placeName())
                                }

                                override fun onError(errorMessage: String?) {
                                    Timber.w("Location: geocoder error: %s", errorMessage)
                                    if (cont.isActive) cont.resume(null)
                                }
                            },
                        )
                    }
                }
            } else {
                // The pre-33 API does a blocking network call: keep it off the main thread
                withContext(Dispatchers.IO) {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        ?.firstOrNull()
                        ?.placeName()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Location: geocoder failed")
            null
        }.also { if (it == null) Timber.w("Location: geocoder gave no place name") }
    }

    private fun Address.placeName(): String? = locality ?: subAdminArea ?: adminArea ?: countryName
}
