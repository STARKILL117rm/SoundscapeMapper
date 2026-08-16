# REPORTE TÉCNICO Y DOCUMENTACIÓN DEL PROYECTO FINAL

---

## PORTADA

```
========================================================================================
                         UNIVERSIDAD TECNOLÓGICA / POLITÉCNICA
                           DIVISIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN
                           INGENIERÍA EN DESARROLLO Y GESTIÓN DE SOFTWARE
========================================================================================

                                     PROYECTO FINAL:
                                  SoundscapeMapper
                  Sistema Móvil Multisensor de Monitoreo Acústico,
                      Confort Lumínico y Geolocalización Urbana

----------------------------------------------------------------------------------------
ASIGNATURA:
    Aplicaciones Móviles II

PROFESOR:
    Ing. / Mtro. Miguel Ángel Montoya Cerro

CUATRIMESTRE Y GRUPO:
    09_01

INTEGRANTES DEL EQUIPO:
    • Cristopher López Suárez
    • Román Méndez Meneses
    • Jovanny Hernández Hernández

FECHA DE ENTREGA:
    Agosto 2026
----------------------------------------------------------------------------------------
                                   PACHUCA, HIDALGO, MÉXICO
========================================================================================
```

---

## ÍNDICE DE CONTENIDO

1. [Portada](#portada)
2. [Índice de Páginas](#índice-de-contenido)
3. [Índice de Figuras](#índice-de-figuras)
4. [Resumen](#resumen)
5. [Introducción](#introducción)
6. [Objetivo General](#objetivo-general)
7. [Objetivos Específicos](#objetivos-específicos)
8. [Planteamiento del Problema](#planteamiento-del-problema)
9. [Estado del Arte](#estado-del-arte)
10. [Desarrollo del Proyecto](#desarrollo-del-proyecto)
    * 10.1. [Diseño de la Aplicación y Arquitectura](#101-diseño-de-la-aplicación-y-arquitectura)
    * 10.2. [Diseño de Base de Datos y Persistencia](#102-diseño-de-base-de-datos-y-persistencia)
    * 10.3. [Implementación Multisensor y Lógica de Negocio](#103-implementación-multisensor-y-lógica-de-negocio)
    * 10.4. [Desarrollo de las Pantallas e Interfaces de Usuario](#104-desarrollo-de-las-pantallas-e-interfaces-de-usuario)
    * 10.5. [Pruebas Realizadas y Validación](#105-pruebas-realizadas-y-validación)
11. [Conclusiones](#conclusiones)
12. [Referencias Bibliográficas](#referencias-bibliográficas)
13. [Glosario de Términos](#glosario-de-términos)

---

## ÍNDICE DE FIGURAS

* **Figura 1.** Pantalla de inicio (*Splash Screen*) con logo animado y arranque instantáneo.
* **Figura 2.** Pantalla principal (*Hoy*) con medidor circular de arco, dosis de ruido y estado de confort.
* **Figura 3.** Pantalla interactiva de *Mapa* con OpenStreetMap, pines por decibelios y refugios sonoros de Pachuca.
* **Figura 4.** Pantalla de *Registro e Historial OMS* con tarjetas de exposición, buscador y filtros multisensor.
* **Figura 5.** Función de búsqueda y filtrado dinámico de lugares específicos en el historial.
* **Figura 6.** Pantalla *Analizar Entorno*: Proceso de muestreo multisensor de 5 segundos (ruido, lux y GPS).
* **Figura 7.** Pantalla *Analizar Entorno*: Resultados del análisis y clasificación semafórica de la OMS.
* **Figura 8.** Pantalla *Analizar Entorno*: Asignación de nombre y emoji contextual al lugar medido.
* **Figura 9.** Modal de detalle del registro espacial con datos de iluminación, coordenadas GPS y acciones.
* **Figura 10.** Pantalla *Guía y Ajustes - Configuración*: Control del servicio en segundo plano y umbral de alerta.
* **Figura 11.** Configuración del límite auditivo de decibelios y selección de modo de alerta (Vibración/Notificación/Sonido).
* **Figura 12.** Pestaña *Aprende OMS*: Calculadora interactiva de tiempo de exposición segura por decibelios.
* **Figura 13.** Pestaña *Aprende OMS*: Escala y clasificación semafórica de niveles de ruido ambiental.
* **Figura 14.** Pestaña *Aprende OMS*: Recomendaciones técnicas de iluminancia (Lux) y hábitos saludables auditivos.
* **Figura 15.** Pestaña *Mis Stats*: Panel de analítica con métricas históricas reales (Room DB), lugar más ruidoso/silencioso y gráfica de barras.

---

## RESUMEN

El presente proyecto detalla el diseño, arquitectura, desarrollo, evaluación y pruebas de **SoundscapeMapper**, una aplicación móvil nativa para el sistema operativo Android orientada a la monitorización multisensor de la contaminación acústica y lumínica en entornos urbanos. Desarrollada bajo el paradigma declarativo de **Jetpack Compose** y el lenguaje **Kotlin**, la herramienta integra en tiempo real la captura del micrófono del dispositivo ($dB_{SPL}$), el sensor de iluminancia ambiental (Lux) y el sistema de posicionamiento global (GPS Fused Location). Los datos recabados son evaluados bajo las directrices de salud auditiva de la Organización Mundial de la Salud (OMS) y persistidos localmente mediante una base de datos relacional **Room (SQLite)**. La solución incorpora mapas interactivos con *OpenStreetMap* para georreferenciar zonas de confort y refugios sonoros, un servicio en segundo plano (*Foreground Service*) para alertas preventivas de sobreexposición y un motor analítico visual con gráficas dinámicas de exposición histórica.

**Palabras clave:** *Jetpack Compose, Room Database, Contaminación Acústica, Confort Lumínico, OMS, Sensor de Luz, Micrófono, GPS, Foreground Service.*

---

## INTRODUCCIÓN

En las sociedades contemporáneas, la urbanización acelerada y el incremento del tráfico vehicular, la actividad comercial y la densidad poblacional han convertido al ruido ambiental en uno de los contaminantes físicos más invasivos e invisibilizados. De acuerdo con la Organización Mundial de la Salud (OMS), la exposición crónica a niveles de presión sonora superiores a 70–80 dB no solo ocasiona pérdida auditiva neurosensorial progresiva (tinnitus y presbiacusia inducida por ruido), sino que incrementa significativamente el estrés fisiológico, eleva los niveles de cortisol en sangre, propicia trastornos del sueño e induce fatiga cognitiva.

A este fenómeno se suma la inadecuada iluminación en espacios de estudio y trabajo urbano: niveles deficientes (<100 lux) o excesivos (>2500 lux) provocan fatiga visual, cefaleas y disminución del rendimiento académico y profesional.

A pesar de la gravedad de esta problemática, los ciudadanos carecen comúnmente de instrumentos accesibles para diagnosticar en tiempo real las condiciones ambientales de los sitios que habitan o transitan. Ante esta necesidad nace **SoundscapeMapper**, una aplicación móvil moderna que transforma cualquier teléfono inteligente Android en una estación multisensor de diagnóstico ambiental, permitiendo a los usuarios cartografiar su ciudad, descubrir refugios de silencio, registrar su dosis de ruido diario y recibir alertas preventivas.

---

## OBJETIVO GENERAL

Desarrollar una aplicación móvil nativa en plataforma Android empleando el lenguaje Kotlin, Jetpack Compose y la base de datos Room, que integre múltiples sensores físicos del dispositivo (micrófono, sensor de luz ambiental y GPS) para monitorizar, registrar, clasificar y geolocalizar niveles de ruido y luminosidad con base en las directrices de la Organización Mundial de la Salud (OMS), promoviendo la salud auditiva y el confort urbano de los usuarios.

---

## OBJETIVOS ESPECÍFICOS

1. **Implementar el procesamiento de señales de audio en tiempo real**: Desarrollar un motor de adquisición mediante `AudioRecord` que calcule la presión sonora eficaz (RMS) y la convierta a decibelios ponderados ($dB_{SPL}$), clasificando los niveles en Seguro (<70 dB), Precaución (70–80 dB) y Riesgo (>80 dB).
2. **Integrar sensores ambientales complementarios**: Capturar las lecturas del sensor de iluminación física (`Sensor.TYPE_LIGHT`) en Lux y del sensor de geolocalización satelital (`FusedLocationProviderClient`) para correlacionar ruido, luz y coordenadas espaciales.
3. **Diseñar y construir una arquitectura de persistencia robusta**: Implementar una base de datos local mediante **Room Database** con entidades normalizadas, operaciones CRUD y consultas de agregación analítica (promedios, máximos históricos, filtrados dinámicos).
4. **Desarrollar una interfaz de usuario interactiva y accesible**: Crear vistas con Jetpack Compose y Material Design 3 compuestas por 6 módulos principales: Splash Screen, Monitoreo Hoy, Cartografía Espacial, Registro/Historial, Analizador de 5 Segundos y Guía/Ajustes/Stats.
5. **Implementar servicios en segundo plano y notificaciones preventivas**: Diseñar un `ForegroundService` con canales de notificación de alta prioridad y vibración para alertar al usuario cuando su entorno sobrepase el umbral de decibelios configurado.
6. **Validar el sistema mediante pruebas unitarias e instrumentadas**: Evaluar algoritmos de distancia espacial (Haversine), integridad de persistencia y respuesta de sensores en escenarios de prueba.

---

## PLANTEAMIENTO DEL PROBLEMA

En zonas urbanas como la ciudad de Pachuca de Soto y su zona metropolitana, los estudiantes, profesionistas y ciudadanos en general se encuentran constantemente expuestos a fuentes acústicas desreguladas: transporte público, tráfico pesado en avenidas principales, obras de construcción y establecimientos comerciales con bocinas de alta potencia.

La carencia de datos ambientales geolocalizados y personalizados genera las siguientes problemáticas directas:
* **Incapacidad de identificar espacios óptimos**: Dificultad para encontrar bibliotecas, cafeterías o parques con condiciones acústicas (<65 dB) y lumínicas (300–750 lux) propicias para el estudio o trabajo.
* **Invisibilidad del daño acumulativo**: El usuario no es consciente de cuántas horas continuas pasa expuesto a niveles peligrosos de ruido (>80 dB), acumulando dosis que deterioran su salud auditiva a mediano y largo plazo.
* **Falta de herramientas integradas**: La mayoría de las aplicaciones disponibles en las tiendas de software miden únicamente decibelios de forma aislada, sin georreferenciación, sin análisis de iluminación, sin almacenamiento local seguro y sin un enfoque educativo preventivo alineado a la OMS.

---

## ESTADO DEL ARTE

A nivel internacional y comercial existen diversas aplicaciones móviles enfocadas en la medición de sonido, las cuales presentan características y limitaciones técnicas importantes:

| Aplicación | Sensores Utilizados | Base de Datos | Mapeo Geográfico | Confort Lumínico | Enfoque Preventivo OMS |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Sound Meter (Decibel)** | Micrófono | No (o plana básica) | No | No | No |
| **Decibel X** | Micrófono | SQLite básico (pago) | Parcial | No | Parcial |
| **Lux Light Meter** | Sensor de luz | No | No | Sí (solo luz) | No |
| **NoiseTorch / NIOSH SLM** | Micrófono | Archivos CSV | No | No | Sí (Laboral) |
| **SoundscapeMapper (Propuesta)** | **Micrófono + Luz + GPS** | **Room DB (SQLite)** | **Sí (OSM nativo)** | **Sí (300-750 Lux)** | **Sí (Alertas + Stats + Guía)** |

### Ventajas Diferenciales de SoundscapeMapper:
1. **Multisensorialidad**: Correlación simultánea de decibelios acústicos, iluminancia lumínica y coordenadas de latitud/longitud.
2. **Privacidad Total**: Procesamiento en el borde (*Edge Computing*); las muestras de audio se transforman numéricamente a decibelios sin grabar conversaciones ni enviar audios a servidores externos.
3. **Persistencia y Analítica Local**: Almacenamiento continuo con Room, permitiendo generar resúmenes diarios, gráficas de barras animadas y detección de lugares críticos sin requerir conexión a internet.
4. **Algoritmos de Proximidad a Refugios**: Integración del algoritmo Haversine para calcular distancias a zonas tranquilas curadas como el Parque Ben Gurión o la Biblioteca Central Ricardo Garibay.

---

## DESARROLLO DEL PROYECTO

### 10.1. Diseño de la Aplicación y Arquitectura

La aplicación adopta el patrón arquitectónico recomendado por Google: **MVVM (Model - View - ViewModel)** combinado con componentes de arquitectura de Android Jetpack:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      CAPA DE PRESENTACIÓN (UI)                          │
│  [SplashScreen] [HoyScreen] [MapaScreen] [RegistroScreen] [YoScreen]    │
│                       (Jetpack Compose + Material 3)                    │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ Estados / Eventos
┌────────────────────────────────────▼────────────────────────────────────┐
│                       CAPA DE CONTROLADORES                             │
│       [RegistroViewModel] [MapaViewModel] [SensorStateHolder]          │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ Coroutines (Dispatchers.IO)
┌────────────────────────────────────▼────────────────────────────────────┐
│                    CAPA DE DATOS Y SERVICIOS                            │
│  [Room Database: AppDatabase] ─── [MedicionDao] ─── [Medicion Entity]   │
│  [AudioEngine (AudioRecord)]  ─── [AudioMonitorService (Foreground)]    │
│  [SensorManager (Sensor.LIGHT)] ── [LocationServices (GPS Fused)]       │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 10.2. Diseño de Base de Datos y Persistencia

Se implementó **Room Database** con abstracción sobre SQLite nativo.

#### Esquema de la Tabla de Mediciones (`tabla_mediciones`):
* `id` (`INTEGER PRIMARY KEY AUTOINCREMENT`): Identificador único.
* `nombreLugar` (`TEXT NOT NULL`): Nombre descriptivo asignado por el usuario.
* `categoria` (`TEXT NOT NULL`): Clasificación ("Tranquilo" o "Estresante").
* `contextoEmoji` (`TEXT NOT NULL`): Icono semántico representativo (📍, ☕, 📚, 🏞️, 🚗, etc.).
* `decibelios` (`REAL NOT NULL`): Nivel de presión sonora registrado en dB.
* `nivelLuz` (`REAL NOT NULL`): Lectura en Lux del sensor de iluminancia.
* `latitud` (`REAL NOT NULL`): Coordenada geográfica de latitud.
* `longitud` (`REAL NOT NULL`): Coordenada geográfica de longitud.
* `fechaHora` (`TEXT NOT NULL`): Estampa temporal formateada (`dd/MM/yyyy HH:mm`).

#### Operaciones en Data Access Object (`MedicionDao.kt`):
* Inserción y actualización reactiva (`insertarMedicion`).
* Eliminación de registros (`eliminarMedicion`).
* Consulta cronológica completa (`obtenerTodasLasMediciones`).
* Consultas analíticas: `obtenerPromedioDecibelios()`, `obtenerMaximoDecibelios()`, `contarMediciones()`, `obtenerLugarMasRuidoso()` y `obtenerLugarMasSilencioso()`.

---

### 10.3. Implementación Multisensor y Lógica de Negocio

1. **Sensor 1 — Micrófono (`AudioEngine.kt`)**:
   Lee un búfer de muestras PCM de 16 bits a 44.1 kHz mediante `AudioRecord`. Se calcula el valor eficaz cuadrático medio (RMS):
   $$RMS = \sqrt{\frac{1}{N} \sum_{i=1}^{N} x_i^2}$$
   $$dB_{SPL} = 20 \cdot \log_{10}\left(\frac{RMS}{Referencia}\right)$$
2. **Sensor 2 — Sensor de Luz (`SensorEventListener`)**:
   Monitorea `Sensor.TYPE_LIGHT`, actualizando el valor atómico en `SensorStateHolder.nivelLuz`.
3. **Sensor 3 — GPS (`FusedLocationProviderClient`)**:
   Obtiene la última ubicación conocida de alta precisión con permisos de `ACCESS_FINE_LOCATION`.
4. **Algoritmo Haversine de Distancia Espacial (`MapaLogica.kt`)**:
   $$d = 2R \cdot \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta \lambda}{2}\right)}\right)$$
   Permite calcular la cercanía exacta del usuario a los refugios sonoros de la ciudad.

---

### 10.4. Desarrollo de las Pantallas e Interfaces de Usuario

A continuación se documentan las 15 interfaces y flujos de la aplicación móvil con sus respectivas figuras ilustrativas:

#### 1. Inicio y Marca (*Splash Screen*)
* **Figura 1:** Muestra la pantalla inicial animada de *SoundscapeMapper* con el logo ecualizador en degradado turquesa/menta, ondas de sonido tipo sonar y barras de carga activas. Gracias a la configuración en `themes.xml` y `splash_background.xml`, la app arranca de forma instantánea sin pantallas blancas intermedias.

#### 2. Pantalla Principal (*Hoy*)
* **Figura 2:** Presenta el saludo dinámico según la hora del día, carrusel de *Insights* diarios, medidor de arco con la lectura en tiempo real (47.1 dB en calma, Escudo Activo) y el porcentaje de exposición en zonas seguras (99% en zonas confortables).

#### 3. Cartografía Acústica (*Mapa*)
* **Figura 3:** Mapa interactivo que despliega el diario espacial del usuario. Muestra el marcador con la ubicación de las estancias registradas (ej. 26 dB en verde "Seguro"), selector de capas (Diario, Refugios y Mapa de Calor) y vista general de Pachuca.

#### 4. Historial y Búsqueda (*Registro*)
* **Figura 4:** Panel central de registro con tarjeta superior de *Resumen de Exposición Personal* (33.2 dB promedio), barra de búsqueda en tiempo real, filtros semánticos (Todos, Tranquilos, Estresantes, Bien Iluminados) y listado de lugares guardados.
* **Figura 5:** Demostración de búsqueda dinámica donde al ingresar el término "Casa", el listado filtra instantáneamente los registros que coinciden.

#### 5. Módulo de Medición Rápida (*Analizar Entorno*)
* **Figura 6:** Proceso de captura interactiva de 5 segundos con barra de progreso, espectrograma de muestras en vivo y lectura simultánea de sensores: Luz (271 lux), GPS (20.110, -98.731) y Pico Máximo (76 dB).
* **Figura 7:** Pantalla de diagnóstico final que categoriza el entorno como **Lugar Tranquilo** (38 dB) e indica que el ambiente es idóneo para la concentración.
* **Figura 8:** Formulario de asignación de metadatos donde el usuario selecciona un emoji de contexto (cafetería, biblioteca, montaña, auto, etc.) y visualiza el historial previo de mediciones.
* **Figura 9:** Ventana modal desplegable que expone los detalles completos de una medición guardada (38.8 dB Seguro, 286 lux de luz, coordenadas geográficas, fecha y opciones para marcar como estresante o eliminar de la base de datos).

#### 6. Configuración, Educación y Analítica (*Guía y Ajustes*)
* **Figura 10:** Pestaña *Configuración* con interruptores para activar el monitoreo en segundo plano, pausar captura por privacidad y ajustar el deslizador de umbral de decibelios.
* **Figura 11:** Selección del modo de alerta preferido (Vibración, Notificación emergente o Aviso Sonoro), información del sensor de confort visual y advertencias dinámicas según el umbral seleccionado.
* **Figura 12:** Pestaña *Aprende OMS* con la **Calculadora de Exposición OMS**, que calcula el tiempo máximo de estancia segura (ej. 80 dB = hasta 8 horas continuas).
* **Figura 13:** Guía educativa con la escala de niveles de ruido (0–65 dB Tranquilo, 65–80 dB Precaución, 80+ dB Peligro).
* **Figura 14:** Recomendaciones de iluminación en Lux (100–250 lux descanso, 300–750 lux trabajo/estudio, >2500 lux riesgo de fatiga visual) y hábitos de higiene sonora.
* **Figura 15:** Pestaña *Mis Stats* con datos reales extraídos de Room: total de mediciones (4), promedio histórico (34 dB), máximo registrado (38 dB), tarjeta del lugar más ruidoso y silencioso, y gráfica de barras semanal animada.

---

### 10.5. Pruebas Realizadas y Validación

Se diseñaron y ejecutaron cuatro categorías de pruebas:

1. **Pruebas Unitarias de Lógica Espacial (`MapaPuntosTest.kt`)**:
   * Validación del cálculo de distancias Haversine con precisión menor a 5 metros de error.
   * Verificación de la clasificación OMS para valores de frontera (69.9 dB -> Seguro, 70.0 dB -> Moderado, 80.1 dB -> Riesgo).
2. **Pruebas de Sensores en Dispositivo Físico**:
   * *Prueba de Micrófono*: Generación de tonos y calibración contra sonómetro patrón.
   * *Prueba de Luz*: Variación de iluminancia con fuentes artificiales y sombra, verificando transiciones de 50 lux a 3000 lux.
   * *Prueba de GPS*: Desplazamiento en ruta urbana verificando actualización de coordenadas.
3. **Pruebas de Base de Datos y Persistencia**:
   * Inserción, lectura, actualización y borrado concurrente con Kotlin Coroutines (`Dispatchers.IO`), asegurando cero bloqueos en el hilo principal (`Main Thread`).
4. **Pruebas del Servicio en Segundo Plano (`AudioMonitorService`)**:
   * Verificación de persistencia del proceso en suspensión de pantalla y disparo de notificación con vibración al sobrepasar el umbral fijado en 80 dB.

---

## CONCLUSIONES

El desarrollo de **SoundscapeMapper** permitió consolidar de forma integral las competencias avanzadas de desarrollo móvil en Android:

1. **Integración Multisensor Completa**: Se superó el requerimiento mínimo de dos sensores al incorporar con éxito tres sensores físicos fundamentales (audio/micrófono, fotómetro ambiental/luz y geolocalización satelital GPS).
2. **Arquitectura Limpia y Moderna**: La combinación de Jetpack Compose, Material 3 y el patrón MVVM permitió desacoplar la lógica de negocio de la interfaz, ofreciendo una experiencia fluida, reactiva y visualmente atractiva.
3. **Persistencia Local Escalable**: La integración de Room Database garantiza la soberanía y privacidad de los datos del usuario, ofreciendo capacidades analíticas avanzadas sin depender de servicios en la nube.
4. **Impacto Social Positivo**: La aplicación no se limita a ser una herramienta de medición pasiva; educa al usuario con las normas de la OMS y lo alerta proactivamente para proteger su audición y optimizar su confort ambiental cotidiano.

---

## REFERENCIAS BIBLIOGRÁFICAS

1. **Organización Mundial de la Salud (OMS)**. (2018). *Environmental Noise Guidelines for the European Region*. World Health Organization Regional Office for Europe.
2. **Google Developers**. (2024). *Jetpack Compose UI App Development Guidelines*. Android Open Source Project. Recuperado de https://developer.android.com/jetpack/compose
3. **Google Developers**. (2024). *Save data in a local database using Room*. Android Developers Documentation. Recuperado de https://developer.android.com/training/data-storage/room
4. **OpenStreetMap Foundation**. (2024). *OpenStreetMap API and Osmdroid Android Library*. Recuperado de https://www.openstreetmap.org
5. **Beranek, L. L., & Ver, I. L.** (2012). *Noise and Vibration Control Engineering: Principles and Applications*. John Wiley & Sons.
6. **Illuminating Engineering Society (IES)**. (2020). *The Lighting Handbook: Reference and Application* (10th ed.). IESNA.

---

## GLOSARIO DE TÉRMINOS

* **$dB_{SPL}$ (Decibel Sound Pressure Level)**: Unidad logarítmica que expresa la magnitud de la presión acústica relativa al umbral de audición humano ($20\ \mu\text{Pa}$).
* **Lux ($lx$)**: Unidad derivada del Sistema Internacional para medir la iluminancia o flujo luminoso incidente por metro cuadrado ($1\ lx = 1\ lm/m^2$).
* **Room Database**: Capa de persistencia oficial de Google para Android que proporciona una abstracción orientada a objetos sobre SQLite nativo.
* **DAO (Data Access Object)**: Patrón de diseño y componente de Room que define las operaciones de consulta, inserción y borrado de la base de datos.
* **Foreground Service**: Tipo de servicio en Android que ejecuta tareas perceptibles para el usuario y requiere una notificación persistente activa para evitar ser destruido por el sistema operativo.
* **Haversine**: Ecuación trigonométrica astronómica que calcula la distancia ortodrómica entre dos pares de coordenadas geográficas en una esfera.
* **Jetpack Compose**: Kit de herramientas moderno y declarativo de Android para diseñar interfaces de usuario nativas mediante funciones `@Composable`.
* **Coroutines**: Patrón de diseño de concurrencia en Kotlin que permite ejecutar tareas asíncronas de entrada/salida (I/O) sin bloquear el hilo principal de la interfaz de usuario.
* **Dosis de Ruido**: Medida acumulativa de la cantidad de energía acústica a la que una persona ha estado expuesta a lo largo de un período de tiempo en relación con el límite máximo seguro.
