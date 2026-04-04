# Segunda Evaluación — Task Tracker

## Fase 1-2: Análisis y Backlog

- [x] Identificar los 8 ítems clave
- [x] Auditar código actual
- [x] Documentar estado de cada ítem
- [x] Generar backlog con deuda técnica
- [x] Preguntas de arquitectura respondidas

## Fase 3: Roadmap

- [x] Roadmap detallado con tareas atómicas
- [ ] Aprobación del roadmap por el usuario

## Implementación

### Hito 1: Servidor PHP + MySQL

- [x] Crear BD y tablas en Google Cloud
- [x] `registro.php`
- [x] `login.php`
- [x] `tareas.php` (CRUD)
- [x] `perfil.php` (foto)
- [x] `fcm_enviar.php`

### Hito 2: Login / Registro en Android

- [x] Manifest: INTERNET + cleartext
- [x] build.gradle: WorkManager
- [x] `LoginActivity`
- [x] `RegistroActivity`
- [x] `ConexionWorker`
- [x] Gestión de sesión
- [x] Cerrar sesión

### Hito 3: CRUD Remoto

- [x] Refactorizar `DBmanager` → remoto (inserción completada)
- [x] Adaptar `ListaTareasFragment` a `JSONObject`
- [x] Refactor Editar / Borrar
- [x] Arreglar orden por defecto y seleccion de orden momentaneo
- [x] `EditTareaActivity` + ubicación
- [x] `DetalleTareaFragment` + icono ubicación
- [x] `TareasAdapter` + icono ubicación
- [x] Limpiar SQLite local

### Hito 4: Google Maps + Geolocalización

- [x] Dependencias maps + location
- [x] API Key en manifest
- [x] Permisos de ubicación
- [x] `SeleccionarUbicacionActivity`
- [x] Integrar en formularios
- [x] Icono en lista → abrir Maps
- [x] Icono en detalle → abrir Maps

### Hito 5: Cámara + Foto de Perfil

- [x] Permiso CAMERA
- [x] Header Navigation Drawer con foto
- [x] Captura con cámara
- [x] Subida al servidor
- [x] Carga y visualización (Glide)
- [x] FileProvider

### Hito 6: Servicio Proximidad

- [x] `ProximidadService`
- [x] Permisos FOREGROUND_SERVICE + location
- [x] Location tracking en servicio
- [x] Lógica de distancia <200m
- [x] Broadcast service ↔ activity
- [x] Controles iniciar/detener

### Hito 7: Content Provider

- [x] `TareasContentProvider`
- [x] URIs y authority
- [x] CRUD vía ContentResolver
- [x] Registrar en manifest

### Hito 8: Widget (3 Próximas Tareas)

- [x] Crear `TareasWidgetProvider`
- [x] Crear layout XML para el widget
- [x] Crear configuración XML de widget
- [x] Registrar en manifest
- [x] Reflejar 3 siguientes tareas pendientes localmente (desde Room)
- [x] Click en widget -> Abrir LoginActivity

### Hito 9: FCM + CI/CD (Notificación de Nueva Versión)

**Flujo:** Al hacer push con un `versionCode` nuevo → GitHub Actions detecta el cambio → llama a la API REST de Firebase → todos los dispositivos reciben una notificación de "Nueva versión disponible".

**Android (app):**
- [x] Añadir Firebase al proyecto (`google-services.json` + dependencias)
- [x] Implementar `MiFirebaseMessagingService` para manejar mensajes FCM en foreground
- [x] Suscribirse al topic `"nueva_version"` al arrancar la app
- [x] Registrar servicio en `AndroidManifest.xml`

**Servidor / Backend:**
- [ ] Crear Service Account en Google Cloud con permisos FCM
- [ ] Guardar la clave JSON del Service Account como Secret en GitHub (`FCM_SERVICE_ACCOUNT_KEY`)

**GitHub Actions (CI/CD):**
- [x] Crear workflow `.github/workflows/notify_new_version.yml`
- [x] Step: detectar si `versionCode` cambió respecto al commit anterior
- [x] Step: obtener token OAuth2 usando el Service Account
- [x] Step: llamar a la API HTTP v1 de Firebase para enviar al topic `"nueva_version"`

### Hito 10: Persistencia Moderna (Room)

- [x] Añadir dependencias (Room)
- [x] Crear Entidad `TareaEntity`
- [x] Crear interfaz `TareaDao`
- [x] Implementar `AppDatabase` (Room)
- [x] Crear capa Repository (Single Source of Truth)
- [x] Configurar WorkManager para sincronización periódica
- [x] Refactorizar `TareasContentProvider` para usar Room
- [x] Eliminar `DBconexion` y clases Legacy

### Posteriores

- [x] Arreglar selector de color en ajustes
- [ ] Bonton de lapiz en el nav_drawer de main para acceder a formulario de editar perfil
- [ ] La imagen de perfil de nav_drawer no abre la camara si no que amplia la imagen
- [ ] Editar imagen perfil se mueve a formulario de editar perfil
- [ ] Añadir uso de provide de google calendar para crear eventos en el calendario del usuario
- [ ] Reorganizar archivos y modularizar el codigo