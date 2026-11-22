# Nexus — Firebase & Google Sign-In Setup

## 1. Crear proyecto en Firebase Console

1. Ve a [console.firebase.google.com](https://console.firebase.google.com)
2. Crea un proyecto llamado **Nexus**
3. Registra una app Android con el package name: `com.nexus.app`
4. Descarga `google-services.json` y colócalo en `app/google-services.json`

## 2. Activar autenticación

En Firebase Console → Authentication → Sign-in method:
- Habilita **Email/Password**
- Habilita **Google**

## 3. Obtener el Web Client ID para Google Sign-In

1. Firebase Console → Authentication → Sign-in method → Google
2. Expande **Web SDK configuration**
3. Copia el **Web client ID**
4. Pégalo en:

```
feature/feature-auth/src/main/kotlin/com/nexus/feature/auth/core/AuthConstants.kt
```

```kotlin
const val GOOGLE_WEB_CLIENT_ID = "TU_WEB_CLIENT_ID_AQUI.apps.googleusercontent.com"
```

## 4. Añadir SHA-1 al proyecto Firebase

En Android Studio: `Gradle → app → Tasks → android → signingReport`

Copia el SHA-1 y agrégalo en Firebase Console → Project Settings → Your apps → Android app.

## 5. Claves locales (`local.properties`)

`local.properties` no se sube a git. Además de `sdk.dir`, añade:

```properties
# GIFs y emojis animados en publicaciones, ecos y mensajes — https://developers.giphy.com/
# Sin ella el selector dice que los GIFs no están disponibles
GIPHY_API_KEY=tu_api_key

# Opcional: enriquece la búsqueda de música de historias. Solo se incluye en
# builds debug; en release la app usa únicamente iTunes.
SPOTIFY_CLIENT_ID=
SPOTIFY_CLIENT_SECRET=

# Archivos (fotos, audio, video, adjuntos) — ver sección 6
SUPABASE_URL=https://xxxx.supabase.co
SUPABASE_ANON_KEY=tu_anon_key
```

## 6. Archivos en Supabase Storage

Firebase Storage exige el plan Blaze, así que los archivos viven en un bucket de
Supabase (plan Free: 1 GB, archivos de hasta 50 MB). Los usuarios siguen iniciando
sesión solo con Firebase: Supabase acepta su token y las políticas del bucket lo
verifican.

1. Crea un proyecto en [supabase.com](https://supabase.com) (plan Free).
2. **Authentication → Sign In / Providers → Third-party Auth → Add provider → Firebase**,
   con el Project ID `iso-social`.
3. **SQL Editor → New query**: pega [`supabase/storage-setup.sql`](supabase/storage-setup.sql) y ejecútalo.
   Crea el bucket público `media` y las políticas: cada usuario solo puede escribir
   dentro de su carpeta (su uid) y nunca sobrescribir.
4. **Project Settings → API**: copia la Project URL y la anon (publishable) key a
   `local.properties` (sección 5). La anon key es pública por diseño; lo que protege
   el bucket son las políticas.
5. En [`firestore.rules`](firestore.rules), cambia `SUPABASE_PROJECT_REF` por el ref del
   proyecto (el `xxxx` de `https://xxxx.supabase.co`) y publica las reglas (sección 7).

Los pasos 4 y 5 los hace un script. Rechaza la clave secreta (service_role) y se
puede volver a ejecutar para apuntar a otro proyecto:

```powershell
./scripts/configure-supabase.ps1 -Url https://xxxx.supabase.co -AnonKey <anon key>
```

Un proyecto Free de Supabase se pausa tras 7 días sin actividad; se reactiva desde el
dashboard.

## 7. Publicar las reglas de seguridad

Las reglas de Firestore viven en el repo y hay que publicarlas cada vez que cambian:
Firebase Console → Firestore Database → Rules, o con Firebase CLI (usa `firebase.json`;
la primera vez, `firebase use --add` para elegir el proyecto):
`firebase deploy --only firestore:rules,firestore:indexes`.

Los índices (`firestore.indexes.json`) también son obligatorios: el feed de Inicio filtra
`circleId == null` y ordena por fecha, y la lista de mensajes filtra por participante y
ordena por `updatedAt`. Sin ellos esas consultas fallan con `FAILED_PRECONDITION`. Un
índice nuevo tarda unos minutos en construirse después del deploy.

Las reglas exigen cosas que solo envía la versión actual de la app (por ejemplo el
campo `lastEcoId` al enviar un eco, `expireAt` en las historias y las URLs de Supabase),
así que hay que publicarlas junto con la nueva versión.

## 8. Estructura del proyecto

```
Nexus/
├── app/                     # Entry point, navegación raíz
├── core/
│   ├── core-common/         # Result, Dispatchers, ApplicationScope, event bus
│   ├── core-ui/             # Design system (NexusTheme, componentes)
│   ├── core-datastore/      # Sesión local con DataStore
│   ├── core-firebase/       # Firebase, acciones compartidas y directorio de círculos
│   ├── core-storage/        # Subida de archivos a Supabase Storage
│   └── core-giphy/          # Cliente de Giphy y selector de GIFs y emojis animados
└── feature/
    ├── feature-auth/        # Login, Register, Google Sign-In
    ├── feature-feed/        # Feed paginado (Paging 3 + Room), historias
    ├── feature-post/        # Crear post, detalle y ecos
    ├── feature-profile/     # Perfil, guardados y ajustes
    ├── feature-circles/     # Círculos privados con código de invitación
    └── feature-messages/    # Mensajes directos entre miembros de un círculo
        ├── data/            # DataSource, Repository impl, DTOs
        ├── domain/          # Modelos, Repository interface, Use Cases
        └── presentation/    # ViewModels (MVI), Screens (Compose)
```
