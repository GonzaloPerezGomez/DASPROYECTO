<?php
/**
 * tareas.php — Centraliza todo el CRUD remoto de tareas
 * 
 * Método: POST
 * Parámetros: accion (getTareas, insertTarea, updateTarea, deleteTarea, deleteCompletadas), usuario_id, etc.
 * Respuesta: JSON
 */
require_once("config.php");

$accion     = isset($_POST["accion"])     ? $_POST["accion"]     : "";
$usuario_id = isset($_POST["usuario_id"]) ? (int)$_POST["usuario_id"] : 0;

if (empty($accion) || $usuario_id <= 0) {
    echo json_encode(["exito" => false, "mensaje" => "Falta acción o usuario_id"]);
    mysqli_close($conexion);
    exit();
}

$respuesta = ["exito" => false, "mensaje" => "Acción desconocida"];

switch ($accion) {
    case "getTareas":
        // Param opcional: ocultar_completadas (0 o 1)
        // Param opcional: orden (prioridad o fecha)
        $ocultar = isset($_POST["ocultar_completadas"]) && $_POST["ocultar_completadas"] == "1";
        $orden   = isset($_POST["orden"]) && $_POST["orden"] == "prioridad" ? "prioridad DESC" : "fechaLimite ASC";

        $sql = "SELECT * FROM tareas WHERE usuario_id = ?";
        if ($ocultar) {
            $sql .= " AND completada = 0";
        }
        $sql .= " ORDER BY " . $orden;

        $stmt = mysqli_prepare($conexion, $sql);
        mysqli_stmt_bind_param($stmt, "i", $usuario_id);
        mysqli_stmt_execute($stmt);
        $resultado = mysqli_stmt_get_result($stmt);

        $tareas = [];
        while ($fila = mysqli_fetch_assoc($resultado)) {
            // Asegurar tipos para el cliente Java
            $fila["id"] = (int)$fila["id"];
            $fila["prioridad"] = (int)$fila["prioridad"];
            $fila["completada"] = (int)$fila["completada"];
            $fila["latitud"] = $fila["latitud"] !== null ? (float)$fila["latitud"] : null;
            $fila["longitud"] = $fila["longitud"] !== null ? (float)$fila["longitud"] : null;
            $tareas[] = $fila;
        }

        $respuesta = ["exito" => true, "tareas" => $tareas];
        mysqli_stmt_close($stmt);
        break;

    case "insertTarea":
        $titulo      = isset($_POST["titulo"]) ? trim($_POST["titulo"]) : "";
        $desc        = isset($_POST["descripcion"]) ? trim($_POST["descripcion"]) : "";
        $prioridad   = isset($_POST["prioridad"]) ? (int)$_POST["prioridad"] : 0;
        $fechaLimite = isset($_POST["fechaLimite"]) ? $_POST["fechaLimite"] : null;
        if (empty($fechaLimite)) $fechaLimite = null; // para la BD
        $latitud     = isset($_POST["latitud"]) && $_POST["latitud"] !== "" ? (float)$_POST["latitud"] : null;
        $longitud    = isset($_POST["longitud"]) && $_POST["longitud"] !== "" ? (float)$_POST["longitud"] : null;
        $direccion   = isset($_POST["direccion"]) ? trim($_POST["direccion"]) : null;

        if (empty($titulo)) {
            $respuesta = ["exito" => false, "mensaje" => "El título es obligatorio"];
            break;
        }

        $stmt = mysqli_prepare($conexion, "INSERT INTO tareas (usuario_id, titulo, descripcion, prioridad, fechaLimite, latitud, longitud, direccion) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        mysqli_stmt_bind_param($stmt, "issisdds", $usuario_id, $titulo, $desc, $prioridad, $fechaLimite, $latitud, $longitud, $direccion);
        
        if (mysqli_stmt_execute($stmt)) {
            $respuesta = ["exito" => true, "tarea_id" => mysqli_insert_id($conexion)];
        } else {
            $respuesta = ["exito" => false, "mensaje" => "Error al insertar: " . mysqli_error($conexion)];
        }
        mysqli_stmt_close($stmt);
        break;

    case "updateTarea":
        $tarea_id = isset($_POST["tarea_id"]) ? (int)$_POST["tarea_id"] : 0;
        if ($tarea_id <= 0) {
            $respuesta = ["exito" => false, "mensaje" => "ID de tarea inválido"];
            break;
        }

        // Si mandan "completada", significa que es un update rápido de estado (check/uncheck)
        if (isset($_POST["completada"])) {
            $completada = (int)$_POST["completada"];
            $stmt = mysqli_prepare($conexion, "UPDATE tareas SET completada = ? WHERE id = ? AND usuario_id = ?");
            mysqli_stmt_bind_param($stmt, "iii", $completada, $tarea_id, $usuario_id);
            if (mysqli_stmt_execute($stmt)) {
                $respuesta = ["exito" => true, "mensaje" => "Estado actualizado"];
            } else {
                $respuesta = ["exito" => false, "mensaje" => "Error update estado"];
            }
            mysqli_stmt_close($stmt);
            break;
        }

        // Actualización completa (desde EditTareaActivity)
        $titulo      = trim($_POST["titulo"]);
        $desc        = trim($_POST["descripcion"]);
        $prioridad   = (int)$_POST["prioridad"];
        $fechaLimite = isset($_POST["fechaLimite"]) && !empty($_POST["fechaLimite"]) ? $_POST["fechaLimite"] : null;
        $latitud     = isset($_POST["latitud"]) && $_POST["latitud"] !== "" ? (float)$_POST["latitud"] : null;
        $longitud    = isset($_POST["longitud"]) && $_POST["longitud"] !== "" ? (float)$_POST["longitud"] : null;
        $direccion   = isset($_POST["direccion"]) ? trim($_POST["direccion"]) : null;

        $stmt = mysqli_prepare($conexion, "UPDATE tareas SET titulo=?, descripcion=?, prioridad=?, fechaLimite=?, latitud=?, longitud=?, direccion=? WHERE id=? AND usuario_id=?");
        mysqli_stmt_bind_param($stmt, "ssisddsii", $titulo, $desc, $prioridad, $fechaLimite, $latitud, $longitud, $direccion, $tarea_id, $usuario_id);
        
        if (mysqli_stmt_execute($stmt)) {
            $respuesta = ["exito" => true, "mensaje" => "Tarea actualizada"];
        } else {
            $respuesta = ["exito" => false, "mensaje" => "Error al actualizar"];
        }
        mysqli_stmt_close($stmt);
        break;

    case "deleteTarea":
        $tarea_id = isset($_POST["tarea_id"]) ? (int)$_POST["tarea_id"] : 0;
        if ($tarea_id <= 0) {
            $respuesta = ["exito" => false, "mensaje" => "ID de tarea inválido"];
            break;
        }

        $stmt = mysqli_prepare($conexion, "DELETE FROM tareas WHERE id = ? AND usuario_id = ?");
        mysqli_stmt_bind_param($stmt, "ii", $tarea_id, $usuario_id);
        if (mysqli_stmt_execute($stmt)) {
            $respuesta = ["exito" => true, "mensaje" => "Tarea eliminada"];
        } else {
            $respuesta = ["exito" => false, "mensaje" => "Error al eliminar"];
        }
        mysqli_stmt_close($stmt);
        break;

    case "deleteCompletadas":
        $stmt = mysqli_prepare($conexion, "DELETE FROM tareas WHERE usuario_id = ? AND completada = 1");
        mysqli_stmt_bind_param($stmt, "i", $usuario_id);
        if (mysqli_stmt_execute($stmt)) {
            $respuesta = ["exito" => true, "mensaje" => "Tareas completadas eliminadas"];
        } else {
            $respuesta = ["exito" => false, "mensaje" => "Error al eliminar"];
        }
        mysqli_stmt_close($stmt);
        break;
}

echo json_encode($respuesta);
mysqli_close($conexion);
?>
