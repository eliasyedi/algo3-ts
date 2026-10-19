# TP3 — U4: Árboles B+ persistentes en disco

**Algoritmos y Estructura de Datos III — Año 2026, 2do Periodo**
**Grupo:** g_ts5 — **Sección:** TS
**Integrantes:** Figueredo Pistilli, Aurelio (CIC 4.010.315) · Olmedo Echeverría, Elías Rubén (CIC 4.653.503)

---

## 1. Decisiones de diseño

**La página `byte[]` es la representación, no hay clase `Nodo`.** Las operaciones trabajan con
accesores finos (`claveHoja`, `hijo`, `ponerClaveInterno`, …) sobre el arreglo de `P` bytes que
devolvió el disco. Un objeto `Nodo` no serviría: un split necesita `L + 1` pares, que no entran en una
página, y traducir los `P` bytes enteros para leer una clave es trabajo al pedo. En memoria solo están
la cabecera y las páginas del camino raíz–hoja.

**`L` y `M` se calculan en el constructor, nunca son constantes.** `L = ⌊(P−12)/16⌋` y
`M = ⌊(P−4)/12⌋`. `abrir` los **recalcula desde `P`** y los compara con los guardados: no son datos
independientes, son función de `P`. Un archivo que trajera `M = 7` con `P = 60` haría que las claves se
leyeran del offset equivocado, sin error y devolviendo basura que parece un árbol.

**El contador vive afuera de la E/S.** `leerPaginaCruda` hace el `seek` + `readFully`; `leerPagina` la
llama y suma al contador. El diagnóstico (`volcarPagina`, `volcarArbol`, `estadisticas`,
`verificarInvariantes`) usa la cruda y no toca los contadores. La cabecera tiene sus propios métodos
porque **no es un nodo** y no se cuenta.

**La validación de `P` no está en el constructor.** Ante el mismo `P` imposible, `crear` lanza
`TamPaginaInvalidoException` (el programador pidió mal) y `abrir` lanza `FormatoInvalidoException` (el
archivo está corrupto). Si la validación viviera en el constructor, abrir un archivo corrupto culparía
a quien llama por un `P` que nunca eligió.

**Excepciones: chequeada vs. no chequeada.** `FormatoInvalidoException` extiende `IOException` porque
el estado del disco es **externo**: el archivo puede estar corrupto, truncado o ser de otro programa, y
quien llama no puede descartarlo antes de abrirlo —pero sí puede recuperarse, reconstruyendo el índice.
`RangoInvalidoException` (`desde > hasta`) y `TamPaginaInvalidoException` (`P < 40`, `L < 2`, `M < 3`)
son **no chequeadas**: son errores de programación de quien llama, se arreglan cambiando el código.

**El separador se copia en una hoja y se mueve en un interno.** En una hoja la clave es un **dato**: si
desapareciera se perdería el par. En un interno es un **cartel**, y el padre ya lo va a dar. Después de
`insertar(45)` el 50 **ya no está** en `#3` ni en `#7`; el 30, que subió de una hoja, sigue en `#5`.

**El orden del reenlace de la cadena.** `nueva.siguiente = original.siguiente` y **después**
`original.siguiente = nueva`. Al revés se pierde el resto de la cadena.

---

## 2. Análisis

### 2.1 Capacidad de página

Toda página de nodo arranca con 12 bytes (`tipo`, `n`, `siguiente`).

- **Hoja:** `12 + 16·L ≤ P` ⟹ **`L = ⌊(P−12)/16⌋`** (un par son dos `long`).
- **Interno:** `M` hijos (`int`) y `M−1` claves (`long`), con el bloque de hijos reservando siempre los
  `M` lugares para que las claves arranquen en un offset fijo: `12 + 4M + 8(M−1) ≤ P` ⟹ `12M ≤ P−4`
  ⟹ **`M = ⌊(P−4)/12⌋`**.

| P | L | M | bytes libres en hoja | bytes libres en interno |
|---|---|---|---|---|
| 60 | 3 | 4 | `60 − (12+48) = 0` | `60 − (12+16+24) = 8` |
| 4096 | 255 | 341 | `4096 − (12+4080) = 4` | `4096 − (12+1364+2720) = 0` |

**Por qué `M ≠ L`:** un par de hoja pesa 16 bytes y un hijo pesa 4. En la misma página entran muchos
más hijos que pares, así que el *fan-out* del interno es mayor que la capacidad de la hoja. Que con
`P = 60` den 3 y 4 es casualidad del número chico.

### 2.2 Altura

Con `N` claves, los niveles están acotados por (`h = 1` es raíz hoja):

```
mínimo (todo lleno):          h = 1 + ⌈log_M ⌈N/L⌉⌉
máximo (ocupación mínima I3): h = 1 + ⌈log_⌈M/2⌉ ⌈N/⌈L/2⌉⌉⌉
```

Para `N = 10⁶` y `P = 4096`: mínimo `1 + ⌈log₃₄₁ 3922⌉ = 3`, máximo `1 + ⌈log₁₇₁ 7813⌉ = 3`. La Tabla A
da **3** en los dos órdenes. Para `P = 128`: mínimo 7, máximo 9; la Tabla A da 8 (creciente) y 7
(permutado), dentro de la cota. Para IMDb (`N = 3·10⁵`, `P = 4096`) la cota da 3 y la Tabla B da **3**.

Un **ABB perfectamente balanceado** con un nodo por página necesita `⌈log₂(N+1)⌉` lecturas: **19** para
300 000 claves y **20** para un millón, contra **3** del B+. La altura crece como `log_M N` con `M` en
los cientos, no como `log₂ N`.

### 2.3 E/S por operación

- **`buscar`:** `niveles()` lecturas, **en todo caso**. No hay salida anticipada: hasta no llegar a la
  hoja no se sabe si la clave está. La Tabla C lo confirma: 3,00 páginas por consulta, exista o no.
- **`insertar`:** `niveles()` lecturas y **`2s + 1`** escrituras, con `s` = cantidad de splits. Cada
  split escribe 2 páginas (la original, que se queda en su lugar, y la nueva) y el último paso —el padre
  que absorbe el separador, o la raíz nueva— escribe 1. En `propagar` hay tres finales: padre con lugar
  (escribe 1, corta), padre lleno (escribe 2, sigue) y sin padre (escribe 1, corta). Solo el del medio
  recursa, y de ahí sale la fórmula.
- Un duplicado lee `niveles()` y escribe 0.

Como `niveles = Θ(log_M N)`, son **`Θ(log_M N)` accesos a disco**. Dentro de cada página la búsqueda es
binaria sobre `n ≤ M−1` claves, así que las comparaciones son
`Θ(log_M N · log M) = ` **`Θ(log N)`**. Las comparaciones son gratis comparadas con una E/S: `10⁻⁹ s`
contra `10⁻⁴ s` en un SSD.

### 2.4 Ocupación

**Por qué la inserción creciente deja las hojas al 50 %.** Con claves ordenadas siempre se inserta en la
**última** hoja. Al partirse, la izquierda queda con `⌈(L+1)/2⌉` pares y **no recibe nunca más nada**,
porque todo lo que viene es mayor: queda congelada. La ocupación resultante es exactamente

```
⌈(L+1)/2⌉ / L     →   1/2  cuando L crece
```

y coincide al centésimo con la Tabla A en los seis valores de `L`: 57,14 % (L=7), 53,33 % (15),
51,61 % (31), 50,79 % (63), 50,39 % (127) y 50,20 % (255).

**Predicción exacta de la columna `hojas / creciente`:**

```
hojas = ⌈(N − L) / ⌈(L+1)/2⌉⌉ + 1
```

La primera hoja aguanta `L` pares antes del primer split; después cada split congela `⌈(L+1)/2⌉` y la
última queda abierta. Da **250 000, 125 000, 62 500, 31 250, 15 625 y 7812**: los seis valores medidos.

**Contra la inserción aleatoria.** Los splits caen en cualquier lado y las hojas ya partidas siguen
recibiendo, así que la ocupación sube al ~70 %. Consecuencias: con `P = 128` el índice usa 196 962 hojas
en vez de 250 000; con **`P = 2048` el creciente necesita 4 niveles y el permutado 3** — ahí la
diferencia se paga en **cada** búsqueda, para siempre. En IMDb el índice aleatorio pesa **23 % menos**
(7,43 MB contra 9,66 MB).

**Las claves de IMDb llegan en orden creciente**: el TSV está ordenado por `tconst` y la clave es su
número. Por eso la construcción normal cae justo en el peor caso: **50,21 %**, casi idéntico al 50,20 %
teórico para `L = 255`.

### 2.5 Secuencial contra índice

De la Tabla C, sobre `N = 300 000`:

| | registros leídos | páginas de índice | µs |
|---|---|---|---|
| B+, clave existente | 1 | 3,00 | 6,1 |
| B+, clave inexistente | 0 | 3,00 | 4,1 |
| secuencial, existente | **152 493** | 0 | 4063 |
| secuencial, inexistente | **300 000** | 0 | 7304 |

La secuencial lee `N/2` en promedio si la clave está —152 493 ≈ 150 000— y **las `N`** si no está,
porque no puede parar antes del final. Crecimiento **`Θ(N)`**.

El B+ lee `niveles() = Θ(log_M N)` páginas en los dos casos. Para pasar de 3 a 4 niveles con `P = 4096`
harían falta unos `341 × 255 × 341 ≈ 3·10⁷` registros: cien veces más datos para **una** lectura más.
En tiempo, el factor medido es **~670×** contra una existente y **~1780×** contra una inexistente.

### 2.6 Rango

Un rango que entrega `s` registros cuesta

```
páginas = (niveles − 1) + ⌈s / (ocupación · L)⌉ + 1
```

El descenso se paga **una sola vez**; después la cadena de hojas da `s` registros a razón de
`ocupación · L ≈ 0,5021 · 255 ≈ 128` por hoja. El `+1` es la hoja parcial donde arranca el rango.

| w | s medido | hojas predichas | hojas medidas |
|---|---|---|---|
| 10 | 9,56 | ~1 | 1,03 |
| 100 | 95,36 | ~1,7 | 1,70 |
| 1000 | 956,33 | ~8,5 | 8,44 |
| 10000 | 9351,75 | ~74 | 73,99 |

**Contra `s` búsquedas independientes:** para `w = 10 000` el rango entrega 9352 registros leyendo
**76 páginas**; 9352 `buscar` costarían `9352 × 3 = 28 056`, **369 veces más**. Esa es la razón de ser de
la cadena de hojas, y es también por qué `buscarRango` pasa a la hoja siguiente **solo si la última
clave de la actual es `< hasta`**: sin esa regla se leería una hoja de más en cada consulta.

---

## 3. Tablas

Las imprime `ExperimentoBMas`, no están transcritas a mano.

### Tabla A — estructura (claves 1..1 000 000, posición = clave)

```
     P     M     L  orden       niveles     hojas    ocupHojas   ocupInternos    inv
   128    10     7  creciente         8    250000       57.14%         60.00%     OK
   128    10     7  permutado         7    196962       72.53%         71.69%     OK
   256    21    15  creciente         6    125000       53.33%         52.39%     OK
   256    21    15  permutado         6     94394       70.63%         69.88%     OK
   512    42    31  creciente         5     62500       51.61%         52.40%     OK
   512    42    31  permutado         5     46245       69.75%         67.65%     OK
  1024    85    63  creciente         4     31250       50.79%         50.66%     OK
  1024    85    63  permutado         4     22847       69.48%         62.82%     OK
  2048   170   127  creciente         4     15625       50.39%         50.54%     OK
  2048   170   127  permutado         3     11438       68.84%         58.09%     OK
  4096   341   255  creciente         3      7812       50.20%         50.09%     OK
  4096   341   255  permutado         3      5627       69.69%         57.18%     OK
```

`inv` es `verificarInvariantes()` de cada árbol: los 12 dan `OK`.

### Tabla B — construcción con IMDb (primeras 300 000 filas, P = 4096)

```
                        orden del archivo        --aleatorio
registros                          300000             300000
datos.dat (bytes)                27101166           27101166
indice.dat (bytes)                9658368            7434240
P / M / L                   4096 / 341 / 255   4096 / 341 / 255
niveles                                 3                  3
paginas                              2358               1815
hojas                                2343               1805
internos                               14                  9
ocupacion hojas                   50.21 %            65.18 %
ocupacion internos                49.35 %            59.07 %
paginas escritas                   304708             303622
tiempo datos.dat (ms)               508.3              431.2
tiempo indice.dat (ms)              934.2             1030.7
```

### Tabla C — búsquedas (índice construido en orden del archivo)

```
metodo                                      consultas    pags/cons    regs/cons      us/cons
B+ con clave existente (con el registro)         1000         3.00         1.00          6.1
B+ con clave inexistente                         1000         3.00         0.00          4.1
secuencial con clave existente                     50         0.00    152493.28       4063.1
secuencial con clave inexistente                   50         0.00    300000.00       7304.1
```

Antes de medir se corre una vuelta de calentamiento: sin ella la primera fila daba 64 µs en vez de 6,1,
que es el JIT de la JVM y no el algoritmo.

### Tabla D — rangos (100 rangos por ancho)

```
       w      regs/cons      pags/cons     hojas/cons      us/cons
      10           9.56           3.03           1.03         11.0
     100          95.36           3.70           1.70          5.6
    1000         956.33          10.44           8.44         17.9
   10000        9351.75          75.99          73.99        122.9
```

`hojas/cons = pags/cons − (niveles − 1)`.

---

## 4. Cómo ejecutar

```bash
javac -d out tp3/*.java

java -cp out TestArbolBMas          # los 12 casos del enunciado + la traza (152 OK)
java -cp out TestArchivoDatos       # formato de datos.dat (30 OK)

java -cp out BMasApp construir dataset/title.basics.tsv filas=300000 tamPagina=4096 [--aleatorio]
java -cp out BMasApp buscar tt0111161
java -cp out BMasApp rango 100000 100999 mostrar=10
java -cp out BMasApp secuencial 111161
java -cp out BMasApp info
java -cp out BMasApp verificar
java -cp out BMasApp pagina 345

java -Xmx2g -cp out ExperimentoBMas sintetico n=1000000      # Tabla A (~48 s)
java -cp out ExperimentoBMas imdb dataset/title.basics.tsv   # Tabla B
java -cp out BMasApp construir dataset/title.basics.tsv      # deja el índice en orden
java -cp out ExperimentoBMas busquedas                       # Tablas C y D
```

`title.basics.tsv` se baja de <https://datasets.imdbws.com/> y **no se incluye en la entrega**, igual
que `datos.dat` e `indice.dat`.

Como comprobación extra, `TestArbolBMas` construye el árbol de la traza con 13 `insertar` y lo compara
**byte a byte** con el mismo árbol escrito a mano desde la figura del enunciado: los 600 bytes son
idénticos. Y el camino de páginas de `buscar tt0111161` da `#345 → #861 → #854`, el mismo del ejemplo de
la página 13.

---
## 5. Uso de IA

**Este trabajo fue asistido por inteligencia artificial**, en dos sesiones con objetivos distintos:

1. **Estudio conceptual** (Claude, chat en claude.ai), antes de escribir código: entender el problema
   y el árbol B+ en disco.
2. **Construcción** (Claude Code, sesión interactiva sobre el repositorio): construcción parcial del
   código, los tests y este documento.

La forma de trabajo fue por pasos en las dos sesiones: una pieza por vez, desglosando el trabajo a
realizar y verificando el resultado contra la traza del enunciado antes de seguir con la siguiente, y
explicando cada parte con nuestras palabras antes de darla por terminada.

### 5.1 Parte por parte

#### Sesión 1: estudio conceptual (claude.ai)

Sesión guiada por preguntas. La IA no daba la solución: planteaba casos chicos con P = 60 (L = 3, M = 4),
los resolvíamos a mano y ella corregía. Esta sesión no produjo código del TP, salvo un pseudocódigo del
recorrido de `verificarInvariantes`. Se cerró con un archivo de contexto (`CLAUDE.md`) que se usó para
arrancar la sesión 2.

| tema                                       | qué hicimos nosotros                                   | qué aportó la IA                                                      |
|--------------------------------------------|--------------------------------------------------------|-----------------------------------------------------------------------|
| Formato de página, `L` y `M`               | Dedujimos las fórmulas desde el layout byte a byte     | Corrigió el término `M − 1` de las claves del interno                 |
| Split de hoja y cadena de hojas            | Trazamos a mano las inserciones de 25 y 40             | Marcó errores al contar páginas (la cabecera #0 cuenta)               |
| Split de interno y raíz nueva              | Trazamos la inserción de 45 (páginas #6, #7, #8)       | Explicó por qué en la hoja la clave se copia y en el interno se mueve |
| Búsqueda y contrato de E/S                 | Resolvimos caminos y calculamos R y W por inserción    | Formalizó `R = niveles` y `W = 2s + 1`                                |
| `buscarRango`                              | Resolvimos rangos y la regla de parada                 | Aclaró que se entrega al consumidor, sin armar listas                 |
| Camino sin `java.util`                     | Razonamos el tamaño de los arreglos                    | Propuso guardar página, índice del hijo y contenido por nivel         |
| `verificarInvariantes`                     | Calculamos los límites (min, max) de cada hijo         | Dio un pseudocódigo del recorrido recursivo                           |

#### Sesión 2: construcción (Claude Code)

| parte                                      | cómo                                                                 |
|--------------------------------------------|----------------------------------------------------------------------|
| Formato de página y cálculo de `M` y `L`   | generada por IA y modificada                                         |
| `buscar` y `caminoBusqueda`                | generada por IA y modificada                                         |
| Split de hoja, cadena de hojas y separador | generada por IA y modificada                                         |
| Split de interno y raíz nueva              | generada por IA y modificada                                         |
| `buscarRango`                              | generada por IA y modificada                                         |
| Persistencia (`crear` / `abrir` / `close`) | generada por IA y modificada                                         |
| `verificarInvariantes`                     | generada por IA y modificada                                         |
| `TestArbolBMas`                            | generada por IA y modificada                                         |
| `ArchivoDatos` y `BMasApp`                 | generada por IA y modificada                                         |
| `ExperimentoBMas`                          | generada por IA y modificada                                         |
| Análisis de este README                    | generado por IA a partir de las tablas medidas, revisado y corregido |

Lo que puso el grupo en cada parte: el orden en que se construyó, las decisiones de diseño y de estilo,
la verificación contra la traza y contra los formatos exactos del enunciado, y las correcciones del
punto 5.3. Todo el código entregado se revisó línea por línea y se puede explicar.

### 5.2 Prompts relevantes, textuales

```
Necesito entender la problematica del problema y como funciona un arbol b+ [...]
Quiero entender esto, asi que vamos de apoco, y por partes. Como regla principal:
Tampoco quiero que me digas todo, quiero ir por partes, quiero yo razonar y entender,
no tragar noams informacion.

cual es el calculo de L y M?

Espera, podemos hacer de vuelta los dos casos que trazamos?

Dame un ejemplo de 2 niveles y mas hijos. Porque quiero saber donde como
calcular donde va a subir la nueva clave.

```

### 5.3 Qué se corrigió de lo que propuso la IA

1. **Un `UnsupportedOperationException` dejado como andamio** donde todavía faltaba el split de interno.
   Al probarlo se comprobó que **dejaba el índice corrupto**: el split de hoja ya se había escrito en
   disco, la página nueva quedaba inalcanzable desde la raíz y `cantidadClaves` contaba claves que
   `buscar` no encontraba. Se reemplazó por el split real.
2. **Dos expectativas del test estaban mal calculadas.** `buscarRango(MIN, MAX)` lee **8** páginas y no
   9 (el descenso lee 2 internos; la hoja la lee el loop de la cadena), y `ocupacionInternos` del árbol
   de la traza es **8/12** y no 9/12 (la raíz tiene solo 2 hijos). Se verificaron a mano y se corrigió la
   expectativa, no el código.
3. **Una aserción del caso 4 del test comparaba texto de forma tramposa** (un `||` que la hacía pasar
   casi siempre). Se reemplazó por la comprobación real de que el 50 desaparece del nivel 2.
4. **Faltaba una vuelta de calentamiento antes de medir la Tabla C.** Sin ella la primera fila medía
   64 µs en vez de 6,1: estaba midiendo el arranque de la JVM y no el algoritmo.
5. **Quedó código muerto en el test** (un intento de restaurar contadores que no hacía falta porque se
   grababan antes). Se eliminó.
6. **Nombre de `RangoInvalidoException`.** Hubo un choque con la clase homónima del TP2 al tener las dos
   carpetas como *source roots* del mismo módulo. Se resolvió a nivel de proyecto y se mantuvo el nombre
   que pide el enunciado.