<?php
/**
 * actualizar_fcm_token.php — Actualiza el token FCM de un usuario
 * 
 * Método: POST
 * Parámetros: usuario_id, fcm_token
 * Respuesta JSON: { "exito": true } o { "exito": false, "mensaje": "..." }
 */
require_once("config.php");

$usuario_id = isset($_POST["usuario_id"]) ? (int)$_POST["usuario_id"] : 0;
$fcm_token  = isset($_POST["fcm_token"])  ? trim($_POST["fcm_token"]) : "";

if ($usuario_id <= 0 || empty($fcm_token)) {
    echo json_encode(["exito" => false, "mensaje" => "Parámetros insuficientes"]);
    mysqli_close($conexion);
    exit();
}

$stmt = mysqli_prepare($conexion, "UPDATE usuarios SET fcm_token = ? WHERE id = ?");
mysqli_stmt_bind_param($stmt, "si", $fcm_token, $usuario_id);

if (mysqli_stmt_execute($stmt)) {
    echo json_encode(["exito" => true, "mensaje" => "Token actualizado"]);
} else {
    echo json_encode(["exito" => false, "mensaje" => "Error al actualizar BD: " . mysqli_error($conexion)]);
}

mysqli_stmt_close($stmt);
mysqli_close($conexion);
?>
