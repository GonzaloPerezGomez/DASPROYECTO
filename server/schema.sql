-- ============================================================
-- DASPROYECTO — Esquema de Base de Datos
-- Ejecutar en MySQL para crear las tablas necesarias
-- ============================================================

CREATE DATABASE IF NOT EXISTS dasproyecto
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE dasproyecto;

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,   -- almacena hash (password_hash)
    foto_url    VARCHAR(500) DEFAULT NULL,
    fcm_token   VARCHAR(500) DEFAULT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Tabla de tareas (cada tarea pertenece a un usuario)
CREATE TABLE IF NOT EXISTS tareas (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  INT NOT NULL,
    titulo      VARCHAR(200) NOT NULL,
    descripcion TEXT,
    prioridad   INT DEFAULT 0,           -- 0=Baja, 1=Media, 2=Alta
    fechaLimite DATE NOT NULL,
    completada  TINYINT DEFAULT 0,       -- 0=pendiente, 1=completada
    direccion VARCHAR(255) DEFAULT NULL,
    latitud     DOUBLE DEFAULT NULL,
    longitud    DOUBLE DEFAULT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;
