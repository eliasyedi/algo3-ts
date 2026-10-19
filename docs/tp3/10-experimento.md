# 10 — `ExperimentoBMas`: las cuatro tablas

Las tablas **no se transcriben a mano**: las imprime el programa.

```bash
java -Xmx2g -cp out ExperimentoBMas sintetico n=1000000      # Tabla A  (~48 s)
java -cp out ExperimentoBMas imdb dataset/title.basics.tsv   # Tabla B
java -cp out BMasApp construir dataset/title.basics.tsv      # deja el indice en orden
java -cp out ExperimentoBMas busquedas                       # Tablas C y D
```

El orden importa: `imdb` deja construido el índice **aleatorio**, y las tablas
C y D se miden sobre el construido **en orden del archivo**. El programa lo
avisa al terminar la Tabla B.

Todas las mediciones son **sin buffer de páginas**: no implementamos el buffer
opcional, así que cada lectura de página va al disco (al cache del sistema
operativo, en realidad — por eso reportamos páginas *y* tiempo).

## Tabla A — estructura

Claves 1..1 000 000 con posición = clave, para `P ∈ {128, 256, 512, 1024,
2048, 4096}`, dos árboles por cada `P`: inserción creciente e inserción de la
permutación de Fisher-Yates con semilla 2026.

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

La columna `inv` es `verificarInvariantes()` de cada árbol: los 12 dan `OK`.

### La columna `ocupHojas / creciente` no es aproximada

Es **exactamente** `⌈(L+1)/2⌉ / L`:

| L | `⌈(L+1)/2⌉/L` | medido |
|---|---|---|
| 7 | 4/7 = 57,14 % | 57,14 % |
| 15 | 8/15 = 53,33 % | 53,33 % |
| 31 | 16/31 = 51,61 % | 51,61 % |
| 63 | 32/63 = 50,79 % | 50,79 % |
| 127 | 64/127 = 50,39 % | 50,39 % |
| 255 | 128/255 = 50,20 % | 50,20 % |

Los seis coinciden al centésimo. **Por qué:** con claves crecientes siempre se
inserta en la última hoja. Cuando se parte, la izquierda queda con
`⌈(L+1)/2⌉` pares y **no recibe nunca más nada**, porque todo lo que viene es
mayor. Queda congelada ahí. Solo la última hoja está en otro estado.

Y por eso la ocupación tiende a 50 % cuando `L` crece: `⌈(L+1)/2⌉/L → 1/2`.

### La columna `hojas / creciente` también se predice

```
hojas = ⌈(N − L) / ⌈(L+1)/2⌉⌉ + 1
```

La primera hoja aguanta `L` pares antes del primer split; de ahí en más cada
split agrega `⌈(L+1)/2⌉` pares congelados, y la última hoja es la que queda
abierta. Da exacto en los seis casos: 250 000, 125 000, 62 500, 31 250,
15 625 y 7812.

### Aleatorio contra creciente

El permutado llega al ~70 % porque los splits caen en cualquier lado y las
hojas ya partidas siguen recibiendo claves. Consecuencias:

- **menos hojas**: 196 962 contra 250 000 con `P = 128`;
- **a veces un nivel menos**: con `P = 2048` el creciente da 4 niveles y el
  permutado 3. Ese es el caso donde la diferencia se paga en cada búsqueda.

## Tabla B — construcción con IMDb

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

`datos.dat` es **idéntico** en las dos columnas: se escribe siempre en el orden
del archivo. Lo único que cambia es el orden de inserción en el índice.

**Las claves de IMDb llegan en orden creciente**: el TSV está ordenado por
`tconst`, y la clave es el número del `tconst`. Por eso la construcción normal
cae justo en el peor caso de ocupación: 50,21 %, casi idéntico al 50,20 %
teórico de la Tabla A para `L = 255`.

El índice aleatorio pesa **23 % menos** (7,43 MB contra 9,66 MB) y usa 543
páginas menos, con los mismos 3 niveles.

`paginas escritas` ≈ 304 708 para 300 000 inserciones: son `2s + 1` por
inserción, o sea 300 000 escrituras de las que no parten nada más unas 2350
de splits.

## Tabla C — búsquedas

1000 claves existentes y 1000 inexistentes, sorteadas con `Random(2026)`. Las
inexistentes se sortean en `[min, max]` y se descartan las que están, con
**búsqueda binaria propia** sobre el arreglo de claves que devolvió
`buscarRango(Long.MIN_VALUE, Long.MAX_VALUE, …)`.

```
metodo                                      consultas    pags/cons    regs/cons      us/cons
B+ con clave existente (con el registro)         1000         3.00         1.00          6.1
B+ con clave inexistente                         1000         3.00         0.00          4.1
secuencial con clave existente                     50         0.00    152493.28       4063.1
secuencial con clave inexistente                   50         0.00    300000.00       7304.1
```

- El B+ cuesta **3,00 páginas exactas** siempre, exista la clave o no: es
  `niveles()`, y no hay varianza porque no hay salida anticipada.
- La secuencial en una clave existente lee 152 493 registros en promedio, que
  es `N/2` como se espera de un recorrido lineal con la clave uniforme.
- La secuencial en una inexistente lee **los 300 000**, porque no puede parar
  antes de llegar al final.
- En tiempo: **6,1 µs contra 4063 µs**, un factor de ~670. Contra una
  inexistente, ~1780.

La fila del B+ existente cuesta 2 µs más que la inexistente: es `datos.leer`,
el acceso aleatorio a `datos.dat` que la inexistente no hace.

### Calentamiento

Antes de medir se corre una vuelta completa sin cronómetro. Sin eso, la
primera fila medida pagaba el JIT de la JVM y daba 64 µs en vez de 6 — diez
veces más, por algo que no es del algoritmo. Es la diferencia entre medir el
programa y medir el arranque de Java.

## Tabla D — rangos

100 rangos por ancho, con `x = claves[rng.nextInt(n)]` y el mismo `rng` que
siguió de la Tabla C.

```
       w      regs/cons      pags/cons     hojas/cons      us/cons
      10           9.56           3.03           1.03         11.0
     100          95.36           3.70           1.70          5.6
    1000         956.33          10.44           8.44         17.9
   10000        9351.75          75.99          73.99        122.9
```

`hojas/cons = pags/cons − (niveles − 1)`, o sea menos las 2 del descenso.

### La fórmula

```
páginas ≈ (niveles − 1) + ⌈s / (ocupación · L)⌉ + 1
```

Con `ocupación · L ≈ 0,5021 · 255 ≈ 128` pares por hoja:

| w | s medido | `s/128` | hojas predichas | hojas medidas |
|---|---|---|---|---|
| 10 | 9,56 | 0,07 | ~1 | 1,03 |
| 100 | 95,36 | 0,75 | ~1,7 | 1,70 |
| 1000 | 956,33 | 7,47 | ~8,5 | 8,44 |
| 10000 | 9351,75 | 73,06 | ~74 | 73,99 |

El `+1` es la hoja parcial donde arranca el rango: casi nunca empieza justo en
el primer par de una hoja.

### Contra `s` búsquedas independientes

Para `w = 10 000`, el rango entrega 9352 registros leyendo **76 páginas**.
Hacer 9352 `buscar` independientes costaría `9352 × 3 = 28 056` páginas: **369
veces más**. Esa es toda la razón de ser de la cadena de hojas — el descenso se
paga una sola vez.
