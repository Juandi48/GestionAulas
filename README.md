# 🏫 Sistema Distribuido de Gestión de Aulas - Facultad ↔ Servidor ↔ ProgramaAcadémico

Este proyecto simula un sistema distribuido de gestión de aulas universitarias, donde:
- Las **facultades** se inscriben al **servidor central**.
- Los **programas académicos** envían solicitudes de salones y laboratorios a las facultades.
- Las facultades reenvían esas solicitudes al servidor, quien decide si puede asignar los recursos.

---

## ⚙️ Estructura del sistema

```
ProgramaAcadémico ─► Facultad ─► Servidor
                           ▲         │
                           └─────────┘
```

---

## ✅ Requisitos

- Java 11 o superior
- Apache Maven 3.6+
- Librería [JeroMQ](https://github.com/zeromq/jeromq) (ya incluida en `pom.xml`)
- Sistema operativo compatible con ZeroMQ (Linux, Windows, Mac)

---

## 🧭 Ejecución del sistema

### 1️⃣ Compila todo el proyecto

```bash
mvn clean compile
```

---

### 2️⃣ Ejecuta el **servidor**

```bash
mvn exec:java -Dexec.mainClass="servidor.Servidor" -Dexec.args="async"
```

El servidor escuchará conexiones desde facultades y procesará solicitudes.

---

### 3️⃣ Ejecuta múltiples facultades desde el mismo proceso

Usa la clase `FacultadesLauncher.java` para evitar errores de puerto ocupado (`Errno 48`).

```bash
mvn exec:java -Dexec.mainClass="facultades.FacultadesLauncher" -Dexec.args="Matematicas Fisica Quimica"
```

Esto:
- Usa **un solo socket REP (puerto 6000)**
- Crea un **hilo por cada facultad**

---

### 4️⃣ Ejecuta un **programa académico** que envía solicitudes a una facultad

```bash
mvn exec:java -Dexec.mainClass="programas.ProgramaAcademico" -Dexec.args="Matematicas1 Matematicas 2 2 1 localhost"
```

Donde los argumentos son:

```
<nombrePrograma> <facultadDestino> <semestre> <salones> <laboratorios> <ipFacultad>
```

Ejemplo:

```bash
mvn exec:java -Dexec.mainClass="programas.ProgramaAcademico" -Dexec.args="FisicaAvanzada Fisica 4 1 2 localhost"
```

---

## 📂 Persistencia

Las solicitudes asignadas o rechazadas se guardan automáticamente por semestre:

```
/data/asignaciones/semestre_1.txt
/data/rechazos/semestre_1.txt
```

---

## 🛠 Problemas comunes

### ❌ Errno 48: Dirección en uso

> Causa: intentaste ejecutar dos facultades en procesos separados.

> Solución: usa `FacultadesLauncher` para levantar varias facultades en hilos, **no `FacultadMain` varias veces**.

---

## 🧑‍💻 Autores

- Sara Albarracín Niño
- Proyecto académico - Pontificia Universidad Javeriana  
- Curso: Introducción a los Sistemas Distribuidos  
- Año: 2025

---