<?php
/**
 * perfil.php — Subida de foto de perfil
 * 
 * Método: POST
 * Parámetros: usuario_id, imagen_base64
 * Respuesta JSON: URL de la foto subida
 */
require_once("config.php");

$usuario_id = isset($_POST["usuario_id"]) ? (int)$_POST["usuario_id"] : 0;
$imagen_b64 = isset($_POST["imagen_base64"]) ? $_POST["imagen_base64"] : "";

if ($usuario_id <= 0 || empty($imagen_b64)) {
    echo json_encode(["exito" => false, "mensaje" => "Faltan parámetros"]);
    exit();
}

// 1. Directorio donde guardaremos las foros (crearlo si no existe)
$directorio_subida = "uploads/perfiles/";
if (!file_exists($directorio_subida)) {
    mkdir($directorio_subida, 0777, true);
}

// 2. Decodificar Base64
// Se asume que viene el base64 puro, sin "data:image/jpeg;base64," 
// (Si lo trae, hay que hacer un explode y quedarnos con [1])
if (strpos($imagen_b64, 'base64,') !== false) {
    $partes = explode('base64,', $imagen_b64);
    $imagen_b64 = $partes[1];
}

$datos_imagen = base64_decode($imagen_b64);
if ($datos_imagen === false) {
    echo json_encode(["exito" => false, "mensaje" => "Error al decodificar la imagen"]);
    exit();
}

// 3. Generar nombre de archivo único
$nombre_archivo = "foto_user_" . $usuario_id . "_" . time() . ".jpg";
$ruta_archivo = $directorio_subida . $nombre_archivo;

// 4. Guardar archivo
if (file_put_contents($ruta_archivo, $datos_imagen)) {
    
    // Asumimos que la URL base del servidor se forma así (ajustar en prod)
    $protocolo = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http";
    $host = $_SERVER['HTTP_HOST'];
    $ruta_base = dirname($_SERVER['REQUEST_URI']);
    if ($ruta_base == '/') $ruta_base = ''; // Evitar url mal formada
    
    // Esta es la URL pública para acceder a la foto
    $foto_url = $protocolo . "://" . $host . $ruta_base . "/" . $ruta_archivo;
    
    // 5. Actualizar en la BD
    $stmt = mysqli_prepare($conexion, "UPDATE usuarios SET foto_url = ? WHERE id = ?");
    mysqli_stmt_bind_param($stmt, "si", $foto_url, $usuario_id);
    
    if (mysqli_stmt_execute($stmt)) {
        echo json_encode([
            "exito" => true, 
            "mensaje" => "Foto subida correctamente",
            "foto_url" => $foto_url
        ]);
    } else {
        echo json_encode(["exito" => false, "mensaje" => "Error al actualizar la URL en BD"]);
    }
    mysqli_stmt_close($stmt);

} else {
    echo json_encode(["exito" => false, "mensaje" => "Error al guardar el archivo en disco"]);
}

mysqli_close($conexion);
?>
