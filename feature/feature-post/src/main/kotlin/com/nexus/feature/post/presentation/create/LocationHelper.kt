package com.nexus.feature.post.presentation.create

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

private const val LOCATION_TIMEOUT_MS = 10_000L

class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentCityName(): String? = try {
        val cts = CancellationTokenSource()
        val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .await()
        } ?: run {
            cts.cancel()
            return null
        }

        val geocoder = Geocoder(context, Locale.getDefault())

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    val name = addresses.firstOrNull()?.let { addr ->
                        addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.countryName
                    }
                    if (cont.isActive) cont.resume(name)
                }
            }
        } else {
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
                ?.let { addr ->
                    addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.countryName
                }
        }
    } catch (_: Exception) {
        null
    }
}
