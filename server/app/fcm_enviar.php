<?php
/**
 * fcm_enviar.php — Envío de notificaciones Push FCM V1 (Interfaz de prueba)
 * 
 * Este script implementa la API FCM V1 de Google.
 * Requiere el archivo 'fcm-auth.json' con las credenciales de la cuenta de servicio.
 */

require_once("config.php");

$mensaje_resultado = "";
$error = false;
$auth_file = "fcm-auth.json";

/**
 * Función para codificar en Base64Url (requerido para JWT)
 */
function base64UrlEncode($data) {
    return str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($data));
}

/**
 * Función para obtener el Access Token de Google OAuth2 (v1)
 */
function getGoogleAccessToken($keyFile) {
    if (!file_exists($keyFile)) {
        throw new Exception("Archivo de credenciales '$keyFile' no encontrado.");
    }

    $json = json_decode(file_get_contents($keyFile), true);
    $client_email = $json['client_email'];
    $private_key = $json['private_key'];

    $header = json_encode(['alg' => 'RS256', 'typ' => 'JWT']);
    $now = time();
    $payload = json_encode([
        'iss' => $client_email,
        'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
        'aud' => 'https://oauth2.googleapis.com/token',
        'exp' => $now + 3600,
        'iat' => $now
    ]);

    $base64Header = base64UrlEncode($header);
    $base64Payload = base64UrlEncode($payload);

    $signature = '';
    if (!openssl_sign($base64Header . "." . $base64Payload, $signature, $private_key, 'SHA256')) {
        throw new Exception("Error al firmar el JWT: " . openssl_error_string());
    }
    $base64Signature = base64UrlEncode($signature);

    $jwt = $base64Header . "." . $base64Payload . "." . $base64Signature;

    $ch = curl_init('https://oauth2.googleapis.com/token');
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query([
        'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion' => $jwt
    ]));

    $response = curl_exec($ch);
    $data = json_decode($response, true);
    curl_close($ch);

    if (isset($data['access_token'])) {
        return $data['access_token'];
    } else {
        throw new Exception("Error obteniendo Access Token: " . ($data['error_description'] ?? $response));
    }
}

/**
 * Lógica principal de procesamiento
 */
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    try {
        $tipo_envio = $_POST["tipo_envio"] ?? "topic";
        $titulo = $_POST["titulo"] ?? "Aviso DAS";
        $cuerpo = $_POST["cuerpo"] ?? "";
        
        // 1. Obtener Token de Acceso
        $accessToken = getGoogleAccessToken($auth_file);
        
        // 2. Preparar el destinatario
        $target = [];
        if ($tipo_envio === "topic") {
            $target = ["topic" => "nueva_version"];
        } else {
            $usuario_id = (int)$_POST["usuario_id"];
            $stmt = mysqli_prepare($conexion, "SELECT fcm_token FROM usuarios WHERE id = ?");
            mysqli_stmt_bind_param($stmt, "i", $usuario_id);
            mysqli_stmt_execute($stmt);
            $res = mysqli_stmt_get_result($stmt);
            if ($u = mysqli_fetch_assoc($res)) {
                if (empty($u["fcm_token"])) throw new Exception("El usuario no tiene un token registrado.");
                $target = ["token" => $u["fcm_token"]];
            } else {
                throw new Exception("Usuario no encontrado.");
            }
        }

        // 3. Construir Payload FCM V1
        $payload = [
            "message" => array_merge($target, [
                "notification" => [
                    "title" => $titulo,
                    "body" => $cuerpo
                ],
                "data" => [
                    "mensaje" => $cuerpo,
                    "fecha" => date("d/m/Y H:i:s"),
                    "click_action" => "AVISO"
                ],
                "android" => [
                    "priority" => "high",
                    "notification" => [
                        "click_action" => "AVISO",
                        "sound" => "default"
                    ]
                ]
            ])
        ];

        // 4. Enviar vía CURL a la API V1
        $json_creds = json_decode(file_get_contents($auth_file), true);
        $project_id = $json_creds['project_id'];
        $url = "https://fcm.googleapis.com/v1/projects/$project_id/messages:send";

        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            "Authorization: Bearer $accessToken",
            "Content-Type: application/json"
        ]);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
        
        $result = curl_exec($ch);
        $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($http_code == 200) {
            $mensaje_resultado = "¡Notificación enviada con éxito! (V1 API)";
        } else {
            $error = true;
            $mensaje_resultado = "Error de FCM ($http_code): " . $result;
        }

    } catch (Exception $e) {
        $error = true;
        $mensaje_resultado = "Error: " . $e->getMessage();
    }
}
?>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Panel FCM V1 - DAS Proyecto</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #4f46e5;
            --primary-hover: #4338ca;
            --bg: #f9fafb;
            --card: #ffffff;
            --text: #1f2937;
            --success: #10b981;
            --error: #ef4444;
        }

        body {
            font-family: 'Inter', sans-serif;
            background-color: var(--bg);
            color: var(--text);
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
            padding: 20px;
        }

        .container {
            width: 100%;
            max-width: 500px;
            background: var(--card);
            padding: 40px;
            border-radius: 16px;
            box-shadow: 0 10px 25px rgba(0,0,0,0.05);
        }

        h1 {
            font-size: 24px;
            font-weight: 700;
            margin-bottom: 8px;
            text-align: center;
        }

        p.subtitle {
            color: #6b7280;
            text-align: center;
            margin-bottom: 32px;
            font-size: 14px;
        }

        .alert {
            padding: 16px;
            border-radius: 8px;
            margin-bottom: 24px;
            font-size: 14px;
            line-height: 1.5;
        }

        .alert-success { background: #ecfdf5; color: #065f46; border: 1px solid #a7f3d0; }
        .alert-error { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
        .alert-info { background: #eff6ff; color: #1e40af; border: 1px solid #bfdbfe; }

        .form-group { margin-bottom: 20px; }
        label { display: block; font-weight: 600; margin-bottom: 8px; font-size: 14px; }
        
        input, select, textarea {
            width: 100%;
            padding: 12px;
            border: 1px solid #d1d5db;
            border-radius: 8px;
            font-size: 14px;
            box-sizing: border-box;
            transition: border-color 0.2s;
        }

        input:focus, select:focus, textarea:focus {
            outline: none;
            border-color: var(--primary);
            ring: 2px solid rgba(79, 70, 229, 0.1);
        }

        button {
            width: 100%;
            padding: 14px;
            background-color: var(--primary);
            color: white;
            border: none;
            border-radius: 8px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.2s;
            margin-top: 10px;
        }

        button:hover { background-color: var(--primary-hover); }

        .footer {
            margin-top: 32px;
            text-align: center;
            font-size: 12px;
            color: #9ca3af;
        }

        .topic-badge {
            display: inline-block;
            background: #eef2ff;
            color: #4338ca;
            padding: 2px 8px;
            border-radius: 4px;
            font-weight: bold;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>FCM V1 Sender</h1>
        <p class="subtitle">Enviando notificaciones desde Google Cloud</p>

        <?php if (!file_exists($auth_file)): ?>
            <div class="alert alert-error">
                <strong>Falta configuración:</strong> No se encuentra el archivo <code>fcm-auth.json</code> en el servidor. Súbelo para activar el envío.
            </div>
        <?php endif; ?>

        <?php if ($mensaje_resultado): ?>
            <div class="alert <?= $error ? 'alert-error' : 'alert-success' ?>">
                <?= htmlspecialchars($mensaje_resultado) ?>
            </div>
        <?php endif; ?>

        <form method="POST">
            <div class="form-group">
                <label>Destinatario</label>
                <select name="tipo_envio" id="tipo_envio" onchange="toggleUsers()">
                    <option value="topic">Todos los dispositivos (Topic: nueva_version)</option>
                    <option value="user">Usuario específico (Individual)</option>
                </select>
            </div>

            <div class="form-group" id="user_select" style="display:none;">
                <label>Seleccionar Usuario</label>
                <select name="usuario_id">
                    <?php
                    $result_u = mysqli_query($conexion, "SELECT id, nombre, email FROM usuarios WHERE fcm_token != ''");
                    while($u = mysqli_fetch_assoc($result_u)) {
                        echo "<option value='{$u['id']}'>{$u['nombre']} ({$u['email']})</option>";
                    }
                    ?>
                </select>
            </div>

            <div class="form-group">
                <label>Título de la Notificación</label>
                <input type="text" name="titulo" value="Notificación de Prueba" required>
            </div>

            <div class="form-group">
                <label>Mensaje (Cuerpo)</label>
                <textarea name="cuerpo" rows="3" placeholder="Escribe aquí tu mensaje..." required></textarea>
            </div>

            <button type="submit">Enviar Notificación Push</button>
        </form>

        <div class="footer">
            Utilizando Google OAuth2 & FCM HTTP v1 API
        </div>
    </div>

    <script>
        function toggleUsers() {
            const val = document.getElementById('tipo_envio').value;
            document.getElementById('user_select').style.display = (val === 'user') ? 'block' : 'none';
        }
    </script>
</body>
</html>
<?php mysqli_close($conexion); ?>
