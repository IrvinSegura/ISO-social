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

## 5. Estructura del proyecto

```
Nexus/
├── app/                     # Entry point
├── core/
│   ├── core-common/         # Result, Dispatchers
│   ├── core-ui/             # Design system (NexusTheme, componentes)
│   └── core-datastore/      # Sesión local con DataStore
└── feature/
    └── feature-auth/        # Login, Register, Google Sign-In
        ├── data/            # DataSource, Repository impl, DTOs
        ├── domain/          # Modelos, Repository interface, Use Cases
        └── presentation/    # ViewModels (MVI), Screens (Compose)
```
