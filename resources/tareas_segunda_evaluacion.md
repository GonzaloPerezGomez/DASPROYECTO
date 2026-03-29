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
- [ ] `ProximidadService`
- [ ] Permisos FOREGROUND_SERVICE + location
- [ ] Location tracking en servicio
- [ ] Lógica de distancia <200m
- [ ] Broadcast service ↔ activity
- [ ] Controles iniciar/detener

### Hito 7: Content Provider
- [ ] `TareasContentProvider`
- [ ] URIs y authority
- [ ] CRUD vía ContentResolver
- [ ] Registrar en manifest

### Hito 8: Widget
- [ ] `TareasWidgetProvider`
- [ ] Layout XML del widget
- [ ] `widget_info.xml`
- [ ] Registrar en manifest
- [ ] Fetch + actualización periódica
- [ ] Click → abrir app

### Hito 9: FCM
- [ ] Firebase Console + `google-services.json`
- [ ] Dependencias Firebase
- [ ] `MiFirebaseMessagingService`
- [ ] Token FCM → servidor
- [ ] `fcm_enviar.php` funcional
- [ ] Registrar servicio en manifest


### Posteriores
- [ ] Arreglar selector de color en ajustes
- [ ] Bonton de lapiz en el nav_drawer de main para acceder a formulario de editar perfil
- [ ] La imagen de perfil de nav_drawer no abre la camara si no que amplia la imagen
- [ ] Editar imagen perfil se mueve a formulario de editar perfil
