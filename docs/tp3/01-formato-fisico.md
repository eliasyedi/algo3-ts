# 01 — Formato físico de `indice.dat`

## La idea de fondo

El árbol **no vive en memoria**. `indice.dat` es una secuencia de páginas de
`P` bytes y la página `k` empieza en el byte `k·P`. Un "nodo" es una página, y
un "puntero" es un número de página (un `int`), nunca una referencia de Java.

La unidad de costo es la página: **una E/S = una página completa**. Por eso
`leerPagina` siempre trae los `P` bytes aunque necesitemos 8, y por eso los
contadores cuentan páginas y no bytes ni comparaciones.

## De dónde salen L y M

No se eligen, se calculan en el constructor a partir de `P`.

Toda página de nodo arranca con 12 bytes comunes: `int tipo`, `int n`,
`int siguiente`.

**Hoja.** Después de los 12 bytes van `L` pares ⟨clave, posición⟩, y cada par
son dos `long` = 16 bytes:

```
12 + 16·L ≤ P   ⟹   L = ⌊(P − 12) / 16⌋
```

**Interno.** Un nodo con `M` hijos tiene `M − 1` claves separadoras. Los hijos
son `int` (4 bytes) y las claves son `long` (8 bytes). El bloque de hijos
**reserva siempre lugar para los M**, aunque el nodo no esté lleno, para que
las claves arranquen en un offset fijo y no dependan de `n`:

```
12 + 4·M + 8·(M − 1) ≤ P   ⟹   12·M ≤ P − 4   ⟹   M = ⌊(P − 4) / 12⌋
```

En el código son `capacidadHojaPara` y `ordenInternoPara`, con las constantes
`NODO_CABECERA = 12`, `BYTES_PAR = 16`, `BYTES_HIJO = 4`, `BYTES_CLAVE = 8`.

### Valores

| P | L | M | bytes libres en hoja | bytes libres en interno |
|---|---|---|---|---|
| 60 | 3 | 4 | `60 − (12 + 48) = 0` | `60 − (12 + 16 + 24) = 8` |
| 4096 | 255 | 341 | `4096 − (12 + 4080) = 4` | `4096 − (12 + 1364 + 2720) = 0` |

### Por qué M ≠ L

Un par de hoja pesa 16 bytes y un hijo de interno pesa 4. En la misma página
entran muchos más hijos que pares, así que el *fan-out* del interno es mayor
que la capacidad de la hoja. Que con `P = 60` den 3 y 4 es **casualidad del
número chico**: con `P = 4096` dan 255 y 341.

## Mapa de offsets

### Cabecera — página 0 (40 bytes usados, ocupa la página entera)

| offset | tipo | campo | constante |
|---|---|---|---|
| 0 | int | `MAGIC = 0x41454433` ("AED3") | `CAB_MAGIC` |
| 4 | int | versión = 1 | `CAB_VERSION` |
| 8 | int | `P` | `CAB_TAM_PAGINA` |
| 12 | int | `M` | `CAB_M` |
| 16 | int | `L` | `CAB_L` |
| 20 | int | página de la raíz | `CAB_RAIZ` |
| 24 | int | cantidad de páginas, **incluida la cabecera** | `CAB_PAGINAS` |
| 28 | int | niveles (raíz hoja = 1) | `CAB_NIVELES` |
| 32 | long | cantidad de claves | `CAB_CLAVES` |

La página 0 **no es un nodo**: no tiene `tipo`, `n` ni `siguiente`, y no entra
en los contadores.

### Hoja

```
0        4        8        12          28          44       …
+--------+--------+--------+-----------+-----------+
| tipo=0 |   n    |  sig   | clave0 pos0 | clave1 pos1 | …
+--------+--------+--------+-----------+-----------+
```

- `clave_i` en `12 + 16i` → `offsetClaveHoja(i)`
- `posicion_i` en `12 + 16i + 8` → `offsetPosicionHoja(i)`
- `siguiente` = número de página de la hoja siguiente, o `−1` en la última.

### Interno

```
0        4        8        12   16   20   24   28      36      44    …
+--------+--------+--------+----+----+----+----+-------+-------+
| tipo=1 |   n    |  -1    | h0 | h1 | h2 | h3 | clave0 | clave1 | …
+--------+--------+--------+----+----+----+----+-------+-------+
                           \___ M hijos ___/   \___ M−1 claves ___/
```

- `hijo_i` en `12 + 4i`, con `i = 0..n` → `offsetHijo(i)`
- `clave_i` en `12 + 4M + 8i`, con `i = 0..n−1` → `offsetClaveInterno(i)`
- `n` cuenta **claves**; los hijos son siempre `n + 1`.
- `siguiente` vale siempre `−1`: un interno no está en la cadena de hojas.

## Verificación byte a byte

Árbol recién creado con `P = 60`. El archivo mide `2 · 60 = 120` bytes:

```
0x00: 41454433 00000001 0000003c 00000004   MAGIC "AED3" | ver 1 | P=60 | M=4
0x10: 00000003 00000001 00000002 00000001   L=3 | raíz=#1 | págs=2 | niveles=1
0x20: 00000000 00000000                     cantidadClaves = 0 (long)
0x3c: 00000000 00000000 ffffffff            página #1: tipo=hoja | n=0 | sig=-1
```

La página 1 arranca exactamente en el byte 60 = `0x3C`, que es `k·P` con
`k = 1`. El resto de los bytes quedan en 0, que es lo que pide el enunciado.

## Enteros a mano, big-endian

`escribirInt`/`leerInt`/`escribirLong`/`leerLong` arman los enteros byte a byte
en lugar de usar `ByteBuffer`, para que el formato quede explícito en el código
(es lo que se corrige byte a byte).

**Big-endian** = el byte más significativo primero, que es el orden en que
`RandomAccessFile.writeInt` escribe. Por eso un `42` se ve como
`00 00 00 2a` y no al revés.

El `& 0xFF` de `leerInt` **no es decorativo**: en Java `byte` es con signo
(−128..127). Sin la máscara, un byte `0xFF` se promueve a `int` como `−1`
(extensión de signo) y arruina el `or` con el resto. Con `& 0xFF` se lo fuerza
a `255`. En `leerLong` la máscara es `0xFFL`, con la `L`, para que la promoción
sea a `long` y no a `int`.

## El cast en el `seek`

```java
archivo.seek((long) nro * tamPagina);
```

`nro` y `tamPagina` son `int`. Sin el cast, Java multiplica en 32 bits y
**desborda en silencio** (no tira excepción): con `P = 4096`, a partir de
`nro ≈ 524 288` el producto pasa los 2³¹ y el resultado se vuelve negativo.
Ahí sí explota, pero recién en el `seek`, con un offset negativo y un mensaje
que no dice nada del problema real. Con el cast la multiplicación se hace en
64 bits y el archivo puede crecer sin límite práctico.
