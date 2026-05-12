# TallerS152B: Pipeline ETL de Streaming con Scala y MySQL
  - *Jhordy Camacas*
    
Este proyecto es una implementación robusta de un proceso **ETL (Extract, Transform, Load)** desarrollado en **Scala 3**. El objetivo principal es demostrar la integración de aplicaciones funcionales con bases de datos relacionales mediante **JDBC**, aplicando patrones de diseño para la gestión de datos en tiempo real.

---

##  Arquitectura del Sistema

El proyecto sigue una arquitectura de **N-Capas**, lo que garantiza que la lógica de negocio esté separada de la persistencia de datos:

### 1. Capa de Configuración (`config/`)
- **`Database.scala`**: Utiliza un enfoque de "Singleton" para gestionar el ciclo de vida de la conexión JDBC. Lee dinámicamente las propiedades del entorno desde el archivo `application.conf`.

### 2. Capa de Acceso a Datos (`dao/`)
- **`EstudianteDAO.scala`**: Implementa el patrón **DAO**. Centraliza todas las operaciones SQL (CRUD), protegiendo al resto de la aplicación de la complejidad de las consultas directas.

### 3. Capa de Modelado (`models/`)
- **`Estudiante.scala`**: Uso de `case classes` de Scala para representar entidades inmutables, facilitando la transformación de datos sin efectos secundarios.

### 4. Orquestador (`StreamingInsertMain.scala`)
- Es el cerebro del pipeline. Gestiona el flujo de **streaming**, coordinando el momento exacto en que los datos son transformados e insertados en el destino final.

---

##  Stack Tecnológico
* **Lenguaje:** Scala 3 (Programación Funcional y Orientada a Objetos)[cite: 1].
* **Construcción:** sbt (Scala Build Tool) para la gestión automática de dependencias[cite: 1].
* **Persistencia:** MySQL Server 8.0+[cite: 1].
* **Conectividad:** MySQL Connector/J (JDBC)[cite: 1].

---

##  Guía de Configuración Local

Para replicar este entorno en tu máquina local, sigue estos pasos:

### 1. Preparación de la Base de Datos
Ejecuta el siguiente script SQL en tu terminal de MySQL:
```sql
CREATE DATABASE taller_db;
USE taller_db;
CREATE TABLE estudiantes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100),
    carrera VARCHAR(100),
    promedio DOUBLE
);

## 2. Configuración Sensible (Seguridad)

Por motivos de seguridad, las credenciales están excluidas del control de versiones. Debes crear el archivo manualmente en la siguiente ruta:

```
src/main/resources/application.conf
```

E inserta tus datos de acceso local:

```hocon
db {
  driver   = "com.mysql.cj.jdbc.Driver"
  url      = "jdbc:mysql://localhost:3306/taller_db"
  user     = "tu_usuario"
  password = "tu_password"
}
```

---

## 3. Instalación de Dependencias

Asegúrate de tener **sbt** instalado. Al abrir el proyecto por primera vez, ejecuta en tu terminal de Git Bash:

```bash
sbt update
```

---

##  Ejecución del Proyecto

Para poner en marcha el pipeline, sigue esta secuencia en la raíz de tu proyecto:

```bash
# 1. Limpiar y compilar los archivos de Scala
sbt clean compile

# 2. Ejecutar el orquestador ETL de streaming
sbt run
```

---

##  Decisiones Técnicas y Mejora Continua

- **Manejo de Errores:** Se implementó una gestión de excepciones en el DAO para evitar caídas del pipeline durante el streaming, asegurando la continuidad del proceso incluso ante registros corruptos.

- **Seguridad de Datos:** Se aplicó una limpieza profunda del historial de Git (`git rm --cached`) para asegurar que ninguna credencial de acceso a la base de datos sea filtrada en el repositorio público.

- **Escalabilidad:** La estructura por capas permite añadir nuevos modelos (como `Docente` o `Curso`) simplemente extendiendo los paquetes de `models` y `dao` sin afectar el núcleo del sistema.

---

*Desarrollado con dedicación académica para la **UTPL** — 2026.*
