# MiPet 🐾

**MiPet** es una aplicación móvil moderna diseñada para ayudar a los propietarios de mascotas a centralizar, organizar y realizar un seguimiento detallado del cuidado de sus compañeros animales. 

Este proyecto ha sido desarrollado siguiendo los más altos estándares académicos y profesionales en el ecosistema Android, demostrando el uso de arquitecturas robustas y tecnologías de vanguardia.

## 🚀 Características Principales

- **Autenticación Segura:** Registro e inicio de sesión gestionados mediante Firebase Authentication.
- **Gestión de Mascotas:** CRUD completo (Crear, Leer, Actualizar, Eliminar) para múltiples mascotas asociadas a un solo dueño.
- **Controles de Salud:** Historial detallado de vacunaciones, desparasitaciones y revisiones veterinarias.
- **Seguimiento de Actividades:** Registro de tareas diarias como alimentación, paseos y medicación con sistema de check-list.
- **Asistente de IA:** Integración con API de Inteligencia Artificial para ofrecer recomendaciones generales sobre el cuidado animal.
- **Arquitectura MVVM:** Separación clara de responsabilidades (Model - View - ViewModel) para un código mantenible y escalable.

## 🛠️ Stack Tecnológico

- **Lenguaje:** [Kotlin](https://kotlinlang.org/)
- **Interfaz de Usuario:** [Jetpack Compose](https://developer.android.com/jetpack/compose) con Material 3
- **Arquitectura:** MVVM + Repository Pattern
- **Backend:** [Firebase](https://firebase.google.com/) (Auth, Cloud Firestore, Storage)
- **Networking:** [Retrofit](https://square.github.io/retrofit/) + OkHttp para consumo de APIs REST
- **Manejo de Estados:** Kotlin Coroutines & StateFlow
- **Carga de Imágenes:** Coil

## 🏗️ Arquitectura del Proyecto
La aplicación implementa un flujo de datos unidireccional:
`UI (Compose) ↔️ ViewModel ↔️ Repository ↔️ Firebase / API Externa`

## ⚖️ Aviso Legal (IA)
El Asistente de IA incluido en MiPet funciona únicamente como un recurso informativo y educativo. **No sustituye bajo ninguna circunstancia el diagnóstico o consejo de un médico veterinario profesional.**

---
*Desarrollado con ❤️ para los amantes de las mascotas.*
