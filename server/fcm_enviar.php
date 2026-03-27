<?php
/**
 * fcm_enviar.php — Envío de notificaciones Push FCM (forma manual/prueba)
 * 
 * Es importante tener Firebase configurado, este archivo asume el uso
 * de la API Legacy o HTTP v1. Se utilizará un enfoque simple Curl para Legacy.
 */
require_once("config.php");

// Clave del servidor (Obtener desde Firebase Console -> Cloud Messaging)
$FCM_SERVER_KEY = "PON_TU_FCM_SERVER_KEY_AQUI"; 

$mensaje = "";

if ($_SERVER["REQUEST_METHOD"] == "POST") {
    $usuario_id = isset($_POST["usuario_id"]) ? (int)$_POST["usuario_id"] : 0;
    $titulo = isset($_POST["titulo"]) ? $_POST["titulo"] : "Nueva Notificación";
    $cuerpo = isset($_POST["cuerpo"]) ? $_POST["cuerpo"] : "Tienes un mensaje";

    if ($usuario_id > 0) {
        // Recuperar el token FCM de la BD
        $stmt = mysqli_prepare($conexion, "SELECT fcm_token FROM usuarios WHERE id = ?");
        mysqli_stmt_bind_param($stmt, "i", $usuario_id);
        mysqli_stmt_execute($stmt);
        $resultado = mysqli_stmt_get_result($stmt);
        
        $token_destino = null;
        if ($fila = mysqli_fetch_assoc($resultado)) {
            $token_destino = $fila["fcm_token"];
        }
        mysqli_stmt_close($stmt);

        if (!empty($token_destino)) {
            // Preparar el cuerpo JSON para FCM
            $data = [
                "to" => $token_destino,
                "notification" => [
                    "title" => $titulo,
                    "body" => $cuerpo,
                    "sound" => "default"
                ],
                "data" => [ // Datos extra opcionales para intent
                    "extra_info" => "Prueba FCM"
                ]
            ];
            
            $headers = [
                'Authorization: key=' . $FCM_SERVER_KEY,
                'Content-Type: application/json'
            ];
            
            // Usar CURL
            $ch = curl_init();
            curl_setopt($ch, CURLOPT_URL, 'https://fcm.googleapis.com/fcm/send');
            curl_setopt($ch, CURLOPT_POST, true);
            curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
            curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
            curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
            
            $result = curl_exec($ch);
            if ($result === FALSE) {
                $mensaje = "Error Curl: " . curl_error($ch);
            } else {
                $mensaje = "Respuesta FCM: " . $result;
            }
        } else {
            $mensaje = "El usuario seleccionado no tiene un FCM Token registrado.";
        }
    } else {
        $mensaje = "Selecciona un usuario válido.";
    }
}
?>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Enviar FCM Push</title>
    <style>
        body { font-family: Arial, sans-serif; padding: 20px; }
        .form-group { margin-bottom: 15px; }
        label { display: block; margin-bottom: 5px; font-weight: bold; }
        input, select, textarea { width: 100%; max-width: 400px; padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
        button { padding: 10px 20px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; }
        button:hover { background: #0056b3; }
        .msg { margin-top: 20px; padding: 15px; background: #e9ecef; border-left: 4px solid #007bff; max-width: 400px; word-wrap: break-word;}
    </style>
</head>
<body>
    <h2>Enviar Notificación Push (FCM)</h2>
    
    <form method="POST" action="">
        <div class="form-group">
            <label>Usuario Destinatario</label>
            <select name="usuario_id" required>
                <option value="">Selecciona un usuario...</option>
                <?php
                // Obtener todos los usuarios con token FCM
                $query = "SELECT id, nombre, email FROM usuarios WHERE fcm_token IS NOT NULL AND fcm_token != ''";
                $result_usuarios = mysqli_query($conexion, $query);
                while($u = mysqli_fetch_assoc($result_usuarios)){
                    echo "<option value='".$u["id"]."'>".$u["nombre"]." (".$u["email"].")</option>";
                }
                ?>
            </select>
        </div>
        
        <div class="form-group">
            <label>Título Notificación</label>
            <input type="text" name="titulo" value="Notificación de prueba DAS" required>
        </div>
        
        <div class="form-group">
            <label>Mensaje / Cuerpo</label>
            <textarea name="cuerpo" rows="3" required>Hola, esta es una prueba de FCM para el proyecto.</textarea>
        </div>
        
        <button type="submit">Enviar Push Notification</button>
    </form>

    <?php if(!empty($mensaje)): ?>
        <div class="msg"><b>Resultado:</b><br><?php echo htmlentities($mensaje); ?></div>
    <?php endif; ?>
    
    <?php mysqli_close($conexion); ?>
</body>
</html>
