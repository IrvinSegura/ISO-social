# Informe de revisión: Nexus

Revisión hecha el 20 de noviembre de 2026 sobre el árbol de trabajo de la rama `fix/code-review-improvements`, con cambios sin commitear incluidos. La hicieron cinco revisores en paralelo: correctitud, seguridad, rendimiento, mantenibilidad y producto. Comprobé a mano en el código los hallazgos de prioridad alta. Los que no pude confirmar del todo están marcados como **(probable)**.

## Estado de las correcciones (20 de noviembre de 2026)

Todo se aplicó en la misma rama y todavía no hay nada commiteado. `./gradlew assembleDebug testDebugUnitTest` pasa en todo el proyecto. Las reglas pasan 31 pruebas en el emulador de Firestore. Los archivos se movieron de Firebase Storage a Supabase Storage, porque Storage ahora exige el plan Blaze.

| Estado | Hallazgos |
|---|---|
| **Corregido** | Bugs: B1-B21 y B23. Seguridad: S2-S5, S7 y S8, más S1 en reglas y cliente. Rendimiento: P1-P7, P9 y P11 (salvo `SKIP_INITIAL_REFRESH`). Además P8 (caché en disco) y P10 (últimos 50 ecos, cargados en paralelo). Mantenibilidad: M2 (wrapper y GitHub Actions), M5, M6 (Room v5 con migración y esquema exportado), M12 (`versionCode` desde CI) y en parte M3 y M11. |
| **Falta en la consola de Firebase o GCP** | S1: activar el TTL en `stories.expireAt` y una regla de ciclo de vida en el bucket para `stories/`, de unos 2 días. S3: App Check en modo enforce y alertas de presupuesto. S9: restringir la API key. |
| **Decisión pendiente** | S6, exigir email verificado, bloquearía a las cuentas de correo sin verificar, y la app no tiene pantalla para reenviar la verificación. |
| **Pospuesto a propósito** | B22: un post offline se queda en "Publicando…". Un timeout borraría archivos que Firestore todavía tiene en cola; el arreglo de verdad es el outbox con WorkManager. P8: queda pendiente un solo player con playlist. P10: falta un botón para ver los ecos anteriores. P11: se mantiene el refresh inicial porque el feed debe abrir con datos frescos. M1, M4 y M7-M10 se quedan como trabajo de refactor. |

### Funciones nuevas (20 de noviembre de 2026)

Salen de la sección 5 de este informe y de comparar Nexus con IRSO BLOG.

| Función | Dónde | Notas |
|---|---|---|
| Perfil con totales, pestañas Publicaciones y Guardados | `feature-profile` (módulo nuevo) | Sustituye al placeholder. Los guardados se cargan post por post (antes en lotes de 10 ids) y se limpian si el post ya no existe. |
| Guardar y reportar posts | Feed (menú ⋮) y detalle | Lógica compartida en `core-firebase/post/PostActions.kt`. Reglas: `users/{uid}/saved` y `reports/{postId}_{uid}`, un reporte por persona. |
| Editar y eliminar el propio post | Detalle | Solo el texto y el estado de ánimo, que es lo que permiten las reglas. Al borrar se eliminan los archivos de la carpeta del post. El feed y el perfil se enteran por `PostChange`. |
| Tema Sistema / Claro / Oscuro | Ajustes del perfil | `AppPreferencesDataStore`. Los componentes usan `isNexusDarkTheme()` en lugar del tema del sistema. |
| Compresión de fotos antes de subir | `core-common/media/ImageCompressor.kt` | Máximo 1600 px y JPEG 85. La misma idea se llevó a las portadas de IRSO BLOG. |

### Círculos, mensajes y diseño de IRSO BLOG (21 de noviembre de 2026)

| Función | Dónde | Notas |
|---|---|---|
| Círculos privados | `feature-circles` (módulo nuevo) | Crear con nombre, emoji y color; unirse con código de 8 caracteres; salir, quitar miembros (el código cambia), cambiar el código y eliminar. Máximo 30 personas. |
| Publicar en un círculo | `feature-post` (selector de audiencia) | "Público" o uno de tus círculos. El feed de Inicio consulta solo `circleId == null` y no guarda en Room los posts de círculos. Los guardados ahora se leen post por post, porque una consulta `whereIn` no puede probar que no incluye posts privados. |
| Mensajes directos | `feature-messages` (módulo nuevo) | Solo entre personas que comparten un círculo. Tiempo real, no leídos en la pestaña, "Visto". Solo texto, porque el bucket de Supabase es público. |
| Reglas e índices | `firestore.rules`, `firestore.indexes.json` | Círculos, invitaciones, prueba de membresía, visibilidad de posts y ecos de círculos, conversaciones y mensajes. 93 pruebas en el emulador (98 tras los GIF y 115 con los hilos de ecos). |
| Diseño de IRSO BLOG | `core-ui` y todas las pantallas | Paleta gris y ámbar, botones negros, tarjetas con borde fino, avatares con iniciales, barra inferior plana con marca ámbar. Sustituye al morado y turquesa con degradados. |

Esto cierra el punto 2 del resumen (Círculos y Mensajes eran "próximamente") y la propuesta de la sección 6 de mensajes solo dentro de los círculos.

### Tarjetas del blog y Giphy (21 de noviembre de 2026)

| Cambio | Dónde | Notas |
|---|---|---|
| Tarjeta de publicación estilo IRSO BLOG | `core-ui/NexusPostCard.kt`, feed y círculos | Foto o GIF de fondo con degradado, pastilla ámbar, titular, autor, fecha, reacciones y ecos. El titular sale de la primera línea o de la primera frase (`splitPostText`, con pruebas). Las 5 reacciones pasan a un solo contador: toque para el corazón, toque largo para elegir. El perfil mantiene su lista compacta. |
| GIFs y emojis animados de Giphy | `core-giphy` (módulo nuevo) | Un selector con pestañas GIF y Emoji (`v1/gifs`, `v2/emoji` y `v1/stickers/search`) que usan las publicaciones, los ecos y los mensajes. Sustituye al cliente que vivía en `feature-post` (cierra la parte de `TenorModels.kt` de M11). |
| `gifUrl` en posts y mensajes | `firestore.rules`, Room v6 | Función `isGiphyUrl` compartida: solo `https://*.giphy.com`. Un mensaje puede ser solo un GIF; la vista previa dice "GIF". Migración 5→6 añade la columna al caché del feed. |
| Sin etiquetas de color arriba | Todas las pantallas | Se quitó `NexusEyebrow` ("Tu comunidad", "Tus grupos", "Privado", etc.). |
| Tipografía Figtree | `core-ui/theme/NexusFont.kt`, `res/font/figtree.ttf` | La fuente del blog, en un solo archivo variable (62 KB) con cada peso de Light a Black apuntado a su eje `wght`, así las negritas son las reales y no sintetizadas. Licencia SIL OFL 1.1 en `docs/licenses/figtree-OFL.txt`. |

Del blog a Nexus vinieron la idea de los guardados, los reportes con motivo y la vista de perfil con totales. De Nexus al blog fueron las reacciones con emoji (una por persona), la compresión de imágenes, la animación en cascada de las tarjetas y los mismos motivos de reporte.

## Resumen

| Área | Crítico | Alto | Medio | Bajo |
|---|:-:|:-:|:-:|:-:|
| Bugs y correctitud | 0 | 5 | 12 | 6 |
| Seguridad | 0 | 0 | 3 | 6 |
| Rendimiento | n/a | 3 | 7 | 5 |
| Mantenibilidad | n/a | 3 | 6 | 2 |

**Lo que ya está bien.** Los revisores lo confirmaron explícitamente:
- La paginación con cursor compuesto y claves remotas.
- El rollback de reacciones con `Mutex` por post.
- La propagación de `CancellationException`.
- Las reglas de Firestore impiden falsificar `authorId` u `ownerId`, inflar contadores y editar contenido ajeno.
- Tienes `allowBackup=false`, cleartext deshabilitado, R8 activo y Timber solo en debug.
- Ningún secreto se subió nunca al historial de git.

**Lo que más pesa hoy**:
1. **Varias funciones visibles no funcionan**: el segundo video seguido en historias, la música de historias, la ubicación en Android 12 o superior y la respuesta a historias.
2. **Faltan cosas básicas**: no se puede cerrar sesión y Perfil, Círculos y Mensajes son pantallas de "próximamente".
3. **No hay CI** y la caché de Room se borra en cada cambio de esquema.

---

## 1. Bugs

### Alto: rompen funciones visibles

| # | Bug | Dónde | Corrección |
|---|---|---|---|
| B1 | **El segundo video seguido en historias sale negro o congelado.** `AndroidView` solo tiene `factory` y `StoryContent` no va envuelto en `key(story.id)`. Compose reutiliza el `PlayerView`, que sigue apuntando al ExoPlayer ya liberado. | `feature-feed/.../story/StoryViewerScreen.kt:332-364` | `update = { it.player = exoPlayer }` o `key(currentStory.id) { ... }` |
| B2 | **La música de historias no suena** (probable). El `LaunchedEffect(isPaused, resolvedUrl)` llama a `seekTo()` y `start()` mientras el `MediaPlayer` sigue en `prepareAsync`. Eso dispara el error -38, `onError` pone `resolvedUrl = null` y el player se destruye. | `StoryViewerScreen.kt:427-466` | Llevar un estado `isPrepared` y no reproducir hasta que valga true. Confírmalo en logcat con "Error (-38,0)". |
| B3 | **Las historias se guardan con el tipo equivocado.** Un video elegido en modo Historia se guarda como IMAGE (`.jpg`), y una imagen en modo REEL se guarda como `.mp4`. | `StoryCreatorSheet.kt:447-451`, `StoryRemoteDataSourceImpl.kt:81` | Sacar el tipo del MIME (`contentResolver.getType(uri)`) y validar que un REEL lleve video. |
| B4 | **La ubicación no funciona en Android 12 o superior.** Se pide solo `ACCESS_FINE_LOCATION`, y el sistema ignora esa petición si no va junto con COARSE. | `feature-post/.../create/ui/CreatePostScreen.kt:131-146` | `RequestMultiplePermissions(FINE, COARSE)`, aceptar cualquiera de las dos y avisar si se niegan. |
| B5 | **El detalle del post queda en blanco para siempre si falla la carga.** El snackbar descarta el error, así que `error == null && post == null` y desaparece "Reintentar". | `feature-post/.../detail/ui/PostDetailScreen.kt:102-107,141` | Separar `loadError` (persistente) del error de envío de eco (snackbar). |

### Medio

| # | Bug | Dónde |
|---|---|---|
| B6 | Si el geocoder falla, el indicador de ubicación se queda cargando para siempre: en API 33+ no se implementa `onError`. | `LocationHelper.kt:44-51` |
| B7 | Un doble tap en "Publicar" crea posts duplicados: `publish()` no tiene guarda y vuelve a `Idle` antes de navegar. | `CreatePostViewModel.kt:179-235` |
| B8 | Si falla `cachePost`, el post ya confirmado desaparece del feed hasta el siguiente refresh. | `FeedViewModel.kt:85-90` |
| B9 | Pausar una historia la reinicia desde cero, porque `isPaused` es key del effect que hace `progress = 0f`. | `StoryViewerScreen.kt:104-117` |
| B10 | **"Enviar respuesta" en una historia no envía nada:** solo borra el texto. Lo comprobé. | `StoryViewerScreen.kt:298-304` |
| B11 | El audio y el video de historias siguen sonando con la app en segundo plano, el video no se pausa al mantener pulsado y no hay `BackHandler` en todo el proyecto, así que "atrás" cierra la app en vez del visor. | `StoryViewerScreen.kt`, `FeedScreen.kt:283-296` |
| B12 | Los errores de los listeners de Firestore no se propagan: el `callbackFlow` no hace `close(error)` y las historias se quedan viejas sin avisar. | `StoryRemoteDataSourceImpl.kt:34-37,56-59` |
| B13 | La foto de la cámara y la media elegida se pierden si la Activity se recrea, porque se guardan con `remember` y no con `rememberSaveable`. | `StoryCreatorSheet.kt:122-141` |
| B14 | En Android 14 con acceso parcial a fotos, la galería de historias no abre aunque `GetContent` no necesita ningún permiso. | `StoryCreatorSheet.kt:145-161` |
| B15 | El nombre del archivo adjunto se guarda mal: `lastPathSegment` devuelve cosas como `document:1234`. Hay que leer `OpenableColumns.DISPLAY_NAME`. | `CreatePostScreen.kt:119` |
| B16 | Fugas de `MediaRecorder`: si `prepare()` falla no se libera, y un doble permiso concedido crea dos grabadores. | `AudioRecorderManager.kt:16-30`, `CreatePostViewModel.kt:120-134` |
| B17 | Los temporizadores del recorte de música se pisan entre sí, y si falla la URL el spinner gira sin fin. | `StoryCreatorSheet.kt:775-803` |

### Bajo

- **B18:** `removeAt(index)` no comprueba el rango, así que un doble tap al quitar una imagen puede lanzar una excepción. `CreatePostViewModel.kt:67-74`
- **B19:** el cruce con Spotify se hace solo por título, así que canciones distintas con el mismo nombre reciben la portada de otra. `MusicService.kt:101-122`
- **B20:** el contador de ecos se desincroniza:
  - Un refresh entre el +1 optimista y un fallo deja el contador uno por debajo.
  - Un eco enviado desde el detalle no actualiza Room.

  `FeedRepositoryImpl.kt:58-70`, `PostDetailViewModel.kt:85-89`
- **B21:** la caché de Room no se borra al cerrar sesión. Si entra otro usuario, ve las reacciones del anterior. Hoy no pasa porque no hay forma de cerrar sesión, pero pasará en cuanto la agregues. `AuthRepositoryImpl.kt:41-44`
- **B22** (posible): sin red, un post puede quedarse en "Publicando…" para siempre, porque `set().await()` no tiene timeout. `PostRemoteDataSourceImpl.kt:113`
- **B23:** si se niega el permiso de micrófono, no se avisa al usuario. `CreatePostScreen.kt:128`

---

## 2. Seguridad

No hay hallazgos críticos ni altos. Lo que queda son huecos de privacidad, de suplantación y de costo.

| # | Prioridad | Hallazgo | Corrección |
|---|---|---|---|
| S1 | Medio | **Las historias caducadas se pueden leer para siempre.** `allow read: if isAuth()` no mira `expiresAt`, y un cliente modificado puede descargar todas las historias con su `mediaUrl`. Lo comprobé (`firestore.rules:271`, `storage.rules:79`). | Guardar `expireAt` como Timestamp, activar el TTL de Firestore y una regla de ciclo de vida en el bucket para `stories/`. |
| S2 | Medio | **Se puede suplantar a otra persona:** `authorName` y `authorPhotoUrl` solo se validan por longitud. Alguien puede publicar como "Equipo Nexus" con la foto de otro usuario. | En las reglas, exigir que coincidan con `get(/users/$(uid)).data`. |
| S3 | Medio | **No hay límite de archivos en Storage.** Se pueden subir archivos de 20-50 MB en bucle sin crear el post, lo que da hosting gratis a costa de tu cuota. | App Check en modo enforce, nombres de archivo fijos, limpieza de archivos huérfanos y alertas de presupuesto. |
| S4 | Bajo (posible) | En Storage, `allow read` también permite `list`. | Usar `allow get`. |
| S5 | Bajo | El campo `id` de las historias no se valida y admite unos 1 MB de basura. | Exigir `id == ''` o quitarlo de `hasOnly`. |
| S6 | Bajo | No se exige email verificado para publicar, lo que facilita el spam con cuentas falsas. | `request.auth.token.email_verified == true` |
| S7 | Bajo | Los `android.util.Log` siguen en release. | Pasarlos a Timber o usar `-assumenosideeffects` en ProGuard. |
| S8 | Bajo | Hay permisos que sobran: `READ_MEDIA_*` y `READ_EXTERNAL_STORAGE` (con `GetContent` no hacen falta) y `FINE_LOCATION`, cuando basta con COARSE. | Quitarlos. |
| S9 | Bajo | `google-services.json` está versionado. No es un secreto. La clave de Giphy va dentro del APK. | Restringir la API key en GCP al paquete y su SHA-1, y activar App Check. |
| S10 | Info | El client secret de Spotify se compila en el APK de debug. | Para producción, pasarlo a un backend o una Cloud Function. |

---

## 3. Rendimiento

| # | Prioridad | Hallazgo | Corrección |
|---|---|---|---|
| P1 | Alto | **La transacción de reacciones lee el documento entero del post** (`tx.get(postRef)`, lo comprobé). Cualquier reacción o eco simultáneo la obliga a reintentar, y puede terminar en error con rollback visible. `FeedRemoteDataSourceImpl.kt:89-117` | Leer solo `userReactionRef`. `FieldValue.increment` ya es atómico. |
| P2 | Alto | **Imágenes y videos se suben sin comprimir** (`putFile` directo), y cada lector los descarga enteros. | Redimensionar a 1080-1440 px y usar WebP/JPEG con calidad 80. Para video, Media3 Transformer. |
| P3 | Alto | **Las llamadas HTTP de música no tienen timeout** (`URL.readText()`), así que retienen hilos de IO y no se cancelan. | Usar el OkHttp de `core-network` con timeouts. |
| P4 | Medio | `AppearOnce` vuelve a animar cada tarjeta al hacer scroll y recompone en cada frame. | Leer el valor dentro de `graphicsLayer {}` y animar solo la primera carga. |
| P5 | Medio | Con Kotlin 2.0.0 no hay strong skipping y `Post` tiene `List`/`Map` inestables, así que se recomponen todas las tarjetas en cada reacción. | Kotlin 2.0.20 o superior, o `@Immutable`. |
| P6 | Medio | `SubcomposeAsyncImage` dentro de listas es lento. | `AsyncImage` con placeholder. |
| P7 | Medio | El progreso de la historia recompone toda la pantalla 20 veces por segundo. | Leer el progreso dentro de la lambda y usar `Animatable`. |
| P8 | Medio | Se crea un ExoPlayer por historia, sin caché ni precarga. | Un solo player con playlist y `SimpleCache` de unos 100 MB. |
| P9 | Medio | Los listeners de historias siguen activos en segundo plano y cobran lecturas. | `stateIn(WhileSubscribed(5_000))` |
| P10 | Medio | **Los ecos se cargan todos de golpe** (lo comprobé): 2.000 ecos son 2.000 lecturas. Además, `getPost` y `getEcos` van en secuencia. | `limit(30)` más paginación, y `async` en paralelo. |
| P11 | Bajo | Faltan varias cosas menores: índice Room `(createdAt, id)`, `remember` en `StoriesRow`, `SKIP_INITIAL_REFRESH` con caché reciente y un Baseline Profile. | n/a |

**Costo en Firestore por acción:**
- **Página del feed:** 21-40 lecturas, sin N+1, porque el autor va desnormalizado en el post.
- **Reacción:** unas 4 lecturas y 3 escrituras.
- **Abrir una historia no vista:** 2 escrituras.

---

## 4. Mantenibilidad y arquitectura

| # | Prioridad | Hallazgo | Acción |
|---|---|---|---|
| M1 | Alto | **`PostDto`, `PostMapper` y `SendEcoUseCase` están duplicados** entre feature-feed y feature-post. | Crear `core-model` con lo compartido. |
| M2 | Alto | **No hay CI, detekt, ktlint ni lint baseline.** | `.github/workflows/ci.yml` con `testDebugUnitTest lintDebug detekt assembleDebug`, y un badge en el README. |
| M3 | Alto | **Hay lógica de datos en presentación:** `MusicService` (HTTP a mano en un `object` global), `LocationHelper`, `AudioRecorderManager` y `PostUploadManager` están en `presentation/`. Además, `CreatePostViewModel` inyecta `FirebaseAuth` directamente. | Crear `MusicRepository` con Retrofit y mover los helpers a `data/`. |
| M4 | Medio | El dominio importa `android.net.Uri` y `PagingData`. | Usar `String` o un value class para la URI. Documentar `PagingData` en un ADR. |
| M5 | Medio | **`core-network` y `core-database` no los usa ningún módulo** (lo comprobé). | Usarlos o borrarlos. |
| M6 | Medio | **Room usa `fallbackToDestructiveMigration()` con `exportSchema = false`** en la versión 4 (lo comprobé). | Exportar el esquema, escribir migraciones y tests de migración. |
| M7 | Medio | Hay archivos demasiado grandes: `StoryCreatorSheet.kt` tiene 1352 líneas, `PostCard.kt` 665, `StoryViewerScreen.kt` 533 y `PostDetailScreen.kt` 526. | Dividirlos y darle a StoryCreator su propio ViewModel. |
| M8 | Medio | La configuración se repite en cada `build.gradle.kts` y las versiones son viejas: Kotlin 2.0.0, Compose BOM 2024.09 y Firebase BoM 33 con `-ktx`. | `build-logic/` con convention plugins y actualizar en un PR aparte. |
| M9 | Medio | Hay solo 8 clases de test. Ningún ViewModel salvo el del feed tiene tests, y no hay tests de UI ni de reglas de Firestore. | Tests de `LoginViewModel`, `CreatePostViewModel` y las reglas con el emulador, más Kover para medir cobertura. |
| M10 | Medio | **Unos 60 textos están escritos directamente en el código:** `strings.xml` tiene 1 string. Además hay 16 `contentDescription = null`. | Pasar los textos a `strings.xml` por módulo (es/en) y activar la regla de lint `HardcodedText`. |
| M11 | Bajo | Hay código muerto o mal nombrado: `circleId` siempre llega `null`, `TenorModels.kt` contiene los modelos de Giphy, `firebase-messaging` no se usa y los nombres de colecciones están repetidos como literales. | Limpiarlo. |
| M12 | Bajo | `versionCode` está fijo en 1 y `.idea/` tiene cambios locales que no se ignoran. | Derivar el versionCode en CI y ajustar `.gitignore`. |

---

## 5. Huecos funcionales

Todos comprobados en el código:

1. **No se puede cerrar sesión:** `SignOutUseCase` existe, pero ninguna pantalla lo usa. Tampoco hay ajustes ni forma de borrar la cuenta, y **Google Play exige poder borrar la cuenta**.
2. **No hay perfil:** es un placeholder (`MainScreen.kt:131`) y `onAuthorClick` no hace nada.
3. **Los círculos no funcionan:** la pestaña es un placeholder y `circleId` siempre llega `null`.
4. **No hay mensajes directos,** y responder a una historia descarta el texto (B10).
5. **No hay seguimiento:** el feed y las historias son globales.
6. **No hay notificaciones:** FCM está declarado pero no se usa.
7. **No se pueden editar ni borrar** posts ni historias, aunque las reglas ya lo permiten.
8. **No se puede reportar ni bloquear,** y Google Play lo exige en apps con contenido de usuarios.
9. **No hay búsqueda, onboarding ni recuperación de contraseña.**
10. **El tema solo sigue al sistema:** no hay selector ni Material You.
11. **El contador de vistas de una historia nunca sube:** las reglas fijan `views == 0` y prohíben cualquier update.

---

## 6. Propuestas para diferenciar Nexus

El estado vacío del feed ya resume la idea: *"sin métricas, sin ruido"*. Nexus funciona mejor como una **red social tranquila, de diálogo y centrada en grupos pequeños**, construida sobre ecos, moods y círculos. Cada propuesta demuestra algo que tu red social en Laravel no puede: trabajo en segundo plano, widgets, notificaciones con respuesta directa, cámara, IA en el teléfono y pantallas grandes.

| # | Propuesta | Qué hace | Qué demuestra de Android | Módulo | Esfuerzo | Impacto |
|---|---|---|---|---|:-:|:-:|
| 1 | **Círculos privados** | Grupos de hasta unas 30 personas con invitación por enlace o QR, un selector de círculo al publicar y el feed filtrado por círculo | App Links, deep links tipados, CameraX y ML Kit Barcode, reglas de membresía | `feature-circles` | L | Alto |
| 2 | **Notificaciones tranquilas** | Un canal por círculo, resumen diario, horas de silencio y responder un eco desde la notificación | FCM, `MessagingStyle`, `RemoteInput`, WorkManager, permiso de notificaciones de Android 13 | `core-notifications` | L | Alto |
| 3 | **Diario de ánimo y "clima" del círculo** | Línea de tiempo de tus moods, gráficos semanales y un clima anónimo de cada círculo | Room con agregaciones, gráficos con Canvas en Compose, accesibilidad en los gráficos | `feature-insights` | M | Alto |
| 4 | **Tema que cambia con el mood** | Material You más un tinte según el mood activo, y selector de tema | `dynamicColorScheme`, colores animados, DataStore | `core-ui`, `feature-settings` | S | Medio |
| 5 | **Widget de check-in** | Registrar el mood del día y ver los ecos sin leer desde la pantalla de inicio | Jetpack Glance, `ActionCallback`, WorkManager | `feature-widget` | M | Alto |
| 6 | **Modo escucha** | Escuchar en cola las notas de voz y los ecos de audio, con controles en la pantalla de bloqueo | `MediaSessionService` de Media3, servicio en primer plano, forma de onda | `core-media` | M | Alto |
| 7 | **Ecos asistidos con IA en el teléfono** | Respuestas rápidas sugeridas, resumen de hilos, transcripción de notas de voz y mood sugerido a partir del texto | ML Kit Smart Reply, Gemini Nano (donde haya soporte), reconocimiento de voz en el dispositivo | `core-ml` | M | Alto |
| 8 | **Cámara propia para historias** | Desenfoque de fondo, stickers que siguen la cara y filtros según el mood | CameraX, ML Kit Selfie Segmentation y Face Detection | `feature-camera` | L | Alto |
| 9 | **Compartir hacia Nexus y atajos** | Nexus aparece al compartir desde otras apps y hay atajos del ícono ("Nuevo eco de voz") | `ACTION_SEND`, Sharing Shortcuts, predictive back | `app` | S | Medio |
| 10 | **Outbox offline con WorkManager** | Posts, ecos y reacciones se guardan primero y se suben solos, incluso si el sistema cierra la app | `HiltWorkerFactory`, constraints, trabajo encadenado, migraciones de Room | `core-sync` | M | Alto |
| 11 | **Diseño adaptativo** | Rail o drawer en tablet, lista y detalle lado a lado, y postura de mesa en plegables | `NavigationSuiteScaffold`, `ListDetailPaneScaffold`, `WindowSizeClass`, shared elements | `core-ui` | M | Alto |
| 12 | **Rendimiento medido y CI** | Baseline Profile, Macrobenchmark y CI con tests en emulador, con las cifras publicadas en el README | `baselineprofile`, `macrobenchmark`, JankStats, Gradle Managed Devices | `:benchmark`, `build-logic` | M | Alto |

**Extras:**
- **Búsqueda offline** con Room FTS4 sobre posts, ecos y transcripciones de voz (`feature-search`, esfuerzo S/M).
- **Wear OS** para responder un eco con emoji o voz desde el reloj.
- **Mensajes directos como "ecos privados" dentro de un círculo,** en lugar de copiar un chat genérico.

### Módulos objetivo

```
Nexus/
├── build-logic/convention/   # NUEVO: convention plugins
├── app/  baselineprofile/  benchmark/
├── core/
│   ├── core-common  core-ui  core-datastore  core-network  core-database  core-firebase   (existen)
│   ├── core-model           # NUEVO: Post, Eco, Mood, Circle, UserProfile (quita la duplicación)
│   ├── core-work  core-sync # NUEVO: WorkManager y outbox
│   ├── core-notifications   # NUEVO: FCM y canales
│   ├── core-media           # NUEVO: Media3 session, grabador; absorbe MusicService y AudioRecorderManager
│   ├── core-ml              # NUEVO: ML Kit y Gemini Nano detrás de interfaces
│   └── core-testing         # NUEVO: fakes y MainDispatcherRule
└── feature/
    ├── feature-auth  feature-feed  feature-post   (existen)
    └── feature-profile  feature-settings  feature-circles  feature-notifications
        feature-insights  feature-search  feature-camera  feature-widget   (NUEVOS)
```

Regla: un módulo `feature-*` solo depende de módulos `core-*`, nunca de otro `feature-*`. Cada feature expone su `NavGraphBuilder.xxxNavGraph()`, como ya hacen feed y post.

---

## 7. Roadmap recomendado

**Fase 0: arreglar lo roto (1 o 2 semanas)**
- Bugs B1-B5 y B10-B11: video, música, tipo de historia, ubicación, detalle, respuesta a historias, ciclo de vida y botón atrás.
- P1: la transacción de reacciones.
- S1 y S2: historias caducadas y suplantación.
- M6: migraciones de Room.
- M2: CI básico.

Una app con funciones rotas pesa más en contra que cualquier función nueva a favor.

**Fase 1: cerrar lo básico, con sello propio**
1. `feature-settings`: cerrar sesión (limpiando Room, ver B21), borrar cuenta y tema con Material You y por mood (propuesta 4).
2. Editar y borrar posts, y **reportar y bloquear**. Las dos cosas las exige Play.
3. `feature-profile` básico, conectado a `onAuthorClick`.
4. **Outbox con WorkManager** (propuesta 10), la compresión de media (P2) y `core-model` (M1).
5. Victoria rápida: compartir hacia Nexus y atajos (propuesta 9).

**Fase 2: la identidad (círculos, ánimo y ecos)**
- Círculos (1), después notificaciones por círculo (2).
- Diario de ánimo (3), después el widget (5).
- Modo escucha (6).

Van en este orden porque cada pieza depende de la anterior.

**Fase 3: escaparate avanzado**
- Diseño adaptativo (11).
- IA en el teléfono (7).
- Cámara (8).
- Al final, Baseline Profiles y Macrobenchmark (12), para publicar cifras reales en el README.
