# MiPet 🐾

**MiPet** es una aplicación móvil moderna diseñada para ayudar a los propietarios de mascotas a centralizar, organizar y realizar un seguimiento detallado del cuidado de sus compañeros animales. 

Este proyecto ha sido desarrollado siguiendo los más altos estándares académicos y profesionales en el ecosistema Android, demostrando el uso de arquitecturas robustas y tecnologías de vanguardia.

## 🚀 Características Principales

- **Autenticación Segura:** Registro e inicio de sesión con Firebase Auth, incluyendo visibilidad de contraseña y opción de recordatorio.
- **Gestión Multi-Mascota:** Registro completo de mascotas con soporte para múltiples fotografías (hasta 5) y acceso a cámara/galería con gestión de permisos en tiempo de ejecución.
- **Historial de Salud:** Control detallado de vacunaciones, desparasitaciones y chequeos médicos con categorización visual.
- **Seguimiento de Actividades:** Registro de tareas diarias y cuidados generales.
- **Asistente de IA:** Integración para recomendaciones personalizadas sobre el cuidado animal.
- **Arquitectura MVVM:** Implementación limpia y escalable con Jetpack Compose y StateFlow.

## 🛠️ Stack Tecnológico

- **Lenguaje:** [Kotlin](https://kotlinlang.org/)
- **Interfaz de Usuario:** [Jetpack Compose](https://developer.android.com/jetpack/compose) con Material 3 y Shapes personalizados.
- **Arquitectura:** MVVM + Repository Pattern.
- **Backend:** [Firebase](https://firebase.google.com/) (Auth, Cloud Firestore, Storage).
- **Networking:** [Retrofit](https://square.github.io/retrofit/) + OkHttp.
- **Manejo de Estados:** Coroutines & StateFlow.
- **Carga de Imágenes:** Coil.

## 🛡️ Seguridad
El proyecto incluye mejores prácticas de seguridad:
- Gestión de permisos en tiempo de ejecución (Android 6.0+).
- Exclusión de archivos sensibles (`google-services.json`) mediante `.gitignore`.
- Flujo de autenticación robusto.

## 🏗️ Arquitectura del Proyecto
La aplicación implementa un flujo de datos unidireccional:
`UI (Compose) ↔️ ViewModel ↔️ Repository ↔️ Firebase / API Externa`

## ⚖️ Aviso Legal (IA)
El Asistente de IA incluido en MiPet funciona únicamente como un recurso informativo y educativo. **No sustituye bajo ninguna circunstancia el diagnóstico o consejo de un médico veterinario profesional.**

---
*Desarrollado con ❤️ para los amantes de las mascotas.*
