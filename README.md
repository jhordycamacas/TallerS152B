# TallerS152B

## Descripción general

**TallerS152B** es un proyecto desarrollado en **Scala 3** utilizando principios de **programación funcional y reactiva**, cuyo propósito es el **procesamiento eficiente de datos estructurados**, principalmente a partir de archivos **CSV**, aplicando transformaciones y análisis, y posteriormente **almacenando o exportando los resultados** en formatos como **JSON** o en una **base de datos MySQL**.

El proyecto está enfocado en el uso de buenas prácticas de ingeniería de software y en el aprovechamiento de librerías modernas del ecosistema Scala para el manejo de flujos de datos (*streaming*).

---

## Objetivo del proyecto

El objetivo principal del proyecto es implementar un flujo de procesamiento de datos de tipo **ETL (Extract, Transform, Load)**:

- **Extract**: lectura de datos desde archivos CSV.
- **Transform**: limpieza, filtrado y análisis de los datos utilizando programación funcional.
- **Load**: persistencia de los datos procesados en una base de datos MySQL y/o exportación a formato JSON.

Este enfoque permite trabajar con grandes volúmenes de datos sin cargar toda la información en memoria.

---

## Tecnologías utilizadas

- **Scala 3.3.7**  
  Lenguaje de programación principal del proyecto.

- **SBT (Scala Build Tool)**  
  Herramienta para la compilación y gestión de dependencias.

- **FS2**  
  Librería para el procesamiento de flujos de datos de manera funcional y reactiva.

- **fs2-data-csv**  
  Librería para la lectura y decodificación de archivos CSV mediante streams.

- **Circe**  
  Manejo de serialización y deserialización de datos en formato JSON.

- **Doobie**  
  Acceso funcional a bases de datos relacionales.

- **MySQL**  
  Sistema de gestión de bases de datos utilizado para la persistencia de información.

- **Typesafe Config**  
  Gestión de archivos de configuración externos.

- **SLF4J**  
  Sistema de logging para el seguimiento y depuración del programa.

---

## Estructura del proyecto

