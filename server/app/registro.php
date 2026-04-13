<?php
/**
 * registro.php — Registro de nuevos usuarios
 * 
 * Método: POST
 * Parámetros: nombre, email, password
 * Respuesta JSON: { "exito": true, "usuario_id": X } o { "exito": false, "mensaje": "..." }
 */
require_once("config.php");

// Recoger parámetros POST
$nombre   = isset($_POST["nombre"])   ? trim($_POST["nombre"])   : "";
$email    = isset($_POST["email"])    ? trim($_POST["email"])    : "";
$password = isset($_POST["password"]) ? $_POST["password"]       : "";

// Validaciones básicas
if (empty($nombre) || empty($email) || empty($password)) {
    echo json_encode(["exito" => false, "mensaje" => "Todos los campos son obligatorios"]);
    mysqli_close($conexion);
    exit();
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    echo json_encode(["exito" => false, "mensaje" => "Email no válido"]);
    mysqli_close($conexion);
    exit();
}

// Comprobar si el email ya existe
$stmt = mysqli_prepare($conexion, "SELECT id FROM usuarios WHERE email = ?");
mysqli_stmt_bind_param($stmt, "s", $email);
mysqli_stmt_execute($stmt);
$resultado = mysqli_stmt_get_result($stmt);

if (mysqli_num_rows($resultado) > 0) {
    echo json_encode(["exito" => false, "mensaje" => "El email ya está registrado"]);
    mysqli_stmt_close($stmt);
    mysqli_close($conexion);
    exit();
}
mysqli_stmt_close($stmt);

// Hash de la contraseña
$passwordHash = password_hash($password, PASSWORD_DEFAULT);

// Insertar usuario
$stmt = mysqli_prepare($conexion, "INSERT INTO usuarios (nombre, email, password) VALUES (?, ?, ?)");
mysqli_stmt_bind_param($stmt, "sss", $nombre, $email, $passwordHash);

if (mysqli_stmt_execute($stmt)) {
    $nuevoId = mysqli_insert_id($conexion);
    echo json_encode([
        "exito"      => true,
        "usuario_id" => $nuevoId,
        "nombre"     => $nombre,
        "email"      => $email
    ]);
} else {
    echo json_encode(["exito" => false, "mensaje" => "Error al registrar: " . mysqli_error($conexion)]);
}

mysqli_stmt_close($stmt);
mysqli_close($conexion);
?>
