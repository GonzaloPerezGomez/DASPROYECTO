# Plan de Desarrollo: App de Tareas Dinámica y Personalizable

## Requisitos Mínimos (Obligatorios para aprobar)
- [x] **Configuración del Proyecto**: Crear proyecto en Android Studio (Java, API 29+) y configurar el repositorio Git.
- [x] **Base de Datos Local (SQLite/Room)**: Diseñar e implementar la tabla `Tareas` (id, titulo, descripcion, prioridad, fechaLimite, direccion, completada).
- [x] **Interfaz Principal (RecyclerView)**: Implementar el listado de tareas usando `CardView` para un diseño atractivo.
- [x] **Operaciones CRUD**: Crear actividades/formularios para añadir nuevas tareas, editarlas y eliminarlas de la BBDD.
- [x] **Interacción con Diálogos**: Implementar `DatePickerDialog` para fechas y un `AlertDialog` de confirmación para borrar tareas.
- [x] **Notificaciones Locales**: Programar avisos que salten en el móvil cuando una tarea esté próxima a su fecha límite.
- [x] **Gestión de la Pila (Backstack)**: Verificar que la navegación entre pantallas no cierre la app y mantenga el estado.
- [ ] **Añadir layout de ver detalles de tarea**
- [ ] **Poner botones para editar, completar y eliminar en nuevo layout**

## Requisitos Avanzados (Para alcanzar el Excelente)
- [ ] **Panel de Navegación (Navigation Drawer)**: Crear menú lateral para filtrar por categorías (Trabajo, Personal, Ocio).
- [ ] **ToolBar Personalizada**: Sustituir la ActionBar por defecto por una `Toolbar` con iconos de acción (buscar, filtrar).
- [ ] **Uso de Fragments**: Implementar una vista de "Detalle" que se cargue dinámicamente (especialmente útil para modo horizontal).
- [ ] **Sistema de Preferencias (SharedPreferences)**: Opción para guardar ajustes de usuario (ej. orden de la lista, mostrar/ocultar completadas).
- [ ] **Estilos y Temas Propios**: Definir colores, fuentes y formas de botones en `themes.xml` para alejar la app del diseño por defecto.
- [ ] **Multiidioma**: Localizar todos los strings de la aplicación al Castellano, Euskera e Inglés.
- [ ] **Ficheros de Texto**: Implementar función para "Exportar Notas" de una tarea específica a un archivo `.txt` en el almacenamiento interno.

## Documentación y Entrega (30% de la nota)
- [ ] **Memoria Técnica**: Redactar máximo 20 páginas incluyendo diagramas de clases y de la base de datos.
- [ ] **Manual de Usuario**: Crear guía con capturas de pantalla de la interfaz final.
- [ ] **Generación de APK**: Compilar el fichero `.apk` final y verificar su funcionamiento en un emulador o dispositivo real.
- [ ] **Subida a eGela**: Preparar el .zip con el APK, la Memoria y el enlace al repositorio Git antes del 9 de marzo.