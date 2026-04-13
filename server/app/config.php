<?php
/**
 * Configuración de conexión a la base de datos.
 * =============================================
 * IMPORTANTE: Cambia estos valores por los de tu servidor Google Cloud.
 */

$DB_SERVER = "db";      // Ej: "34.175.100.50"
$DB_USER = "admin";          // Ej: "root"
$DB_PASS = "test";         // Ej: "miClave123"
$DB_DATABASE = "dasproyecto";

// Establecer conexión
$conexion = mysqli_connect($DB_SERVER, $DB_USER, $DB_PASS, $DB_DATABASE);

// Comprobar conexión
if (mysqli_connect_errno()) {
    http_response_code(500);
    echo json_encode(["error" => "Error de conexión: " . mysqli_connect_error()]);
    exit();
}

// Forzar charset UTF-8
mysqli_set_charset($conexion, "utf8mb4");
?>