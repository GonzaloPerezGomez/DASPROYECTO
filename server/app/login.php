<?php
/**
 * login.php — Autenticación de usuarios
 * 
 * Método: POST
 * Parámetros: email, password
 * Respuesta JSON: { "exito": true, "usuario_id": X, "nombre": "...", "email": "...", "foto_url": "..." }
 *             o  { "exito": false, "mensaje": "..." }
 */
require_once("config.php");

// Recoger parámetros POST
$email    = isset($_POST["email"])    ? trim($_POST["email"]) : "";
$password = isset($_POST["password"]) ? $_POST["password"]     : "";

// Validaciones básicas
if (empty($email) || empty($password)) {
    echo json_encode(["exito" => false, "mensaje" => "Email y contraseña son obligatorios"]);
    mysqli_close($conexion);
    exit();
}

// Buscar usuario por email
$stmt = mysqli_prepare($conexion, "SELECT id, nombre, email, password, foto_url FROM usuarios WHERE email = ?");
mysqli_stmt_bind_param($stmt, "s", $email);
mysqli_stmt_execute($stmt);
$resultado = mysqli_stmt_get_result($stmt);

if (mysqli_num_rows($resultado) == 0) {
    echo json_encode(["exito" => false, "mensaje" => "Email o contraseña incorrectos"]);
    mysqli_stmt_close($stmt);
    mysqli_close($conexion);
    exit();
}

$fila = mysqli_fetch_assoc($resultado);

// Verificar contraseña
if (password_verify($password, $fila["password"])) {
    echo json_encode([
        "exito"      => true,
        "usuario_id" => (int)$fila["id"],
        "nombre"     => $fila["nombre"],
        "email"      => $fila["email"],
        "foto_url"   => $fila["foto_url"]
    ]);
} else {
    echo json_encode(["exito" => false, "mensaje" => "Email o contraseña incorrectos"]);
}

mysqli_stmt_close($stmt);
mysqli_close($conexion);
?>
