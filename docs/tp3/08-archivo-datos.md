# 08 — `ArchivoDatos`: el archivo plano `datos.dat`

## Qué es y qué no es

`datos.dat` **no es un árbol**. Es un archivo plano: los registros van uno
detrás de otro, en el orden en que se agregaron, sin ningún orden por clave y
sin punteros entre ellos.

La división del trabajo es:

| archivo | guarda | sirve para |
|---|---|---|
| `indice.dat` | pares ⟨clave, posición⟩ ordenados | encontrar **dónde** está un registro |
| `datos.dat` | los registros completos | traer el registro **de esa posición** |

La **posición** de un registro es el byte donde empieza su clave. Eso es lo
único que viaja al índice: 8 bytes por registro, en vez de la línea entera.

## El registro

```
long clave | int largo | largo bytes
```

- `clave` (8 bytes): el número del `tconst`, `tt0111161` → `111161`.
- `largo` (4 bytes): cuántos bytes ocupa el texto.
- el texto: la línea TSV completa en UTF-8, **sin el salto de línea**.

Son 12 bytes de cabecera, igual que un nodo del índice, pero por otra razón:
acá los 12 bytes están para poder **saltar al registro siguiente** sin leer el
texto.

### Volcado real

Dos registros, con `\t` mostrado como `09`:

```
0x00: 00 00 00 00 00 00 00 01   clave = 1
0x08: 00 00 00 1f               largo = 31
0x0c: 74 74 30 30 ...           "tt0000001\tshort\tCarmencita\t1894"
0x2b: 00 00 00 00 00 00 00 02   clave = 2        <- 12 + 31 = 43 = 0x2b
0x33: 00 00 00 1e               largo = 30
0x37: 74 74 30 30 ...           "tt0000002\tshort\tPauvre Pierrot"
```

El segundo registro arranca exactamente en `12 + 31`. Esa cuenta es toda la
navegación que existe en este archivo.

## El largo se mide en BYTES, no en caracteres

`linea.getBytes(StandardCharsets.UTF_8).length` **no** es `linea.length()`.
En UTF-8 un carácter puede ocupar 1, 2, 3 o 4 bytes: una `é` ocupa 2 y un
ideograma japonés ocupa 3. Los títulos de IMDb están llenos de los dos.

Si guardáramos `linea.length()`, el `largo` quedaría corto, el registro
siguiente arrancaría **en medio del texto del anterior**, y a partir de ahí
todo el archivo queda corrido. El test lo cubre con una línea multibyte,
comprobando que tiene más bytes que caracteres y que la posición del registro
siguiente usa el número de bytes.

Lo mismo al leer: `new String(texto, StandardCharsets.UTF_8)` con el charset
explícito. Sin él, Java usa el charset por omisión de la máquina, que puede ser
otro y rompe los acentos en la computadora del profesor y no en la nuestra.

## Dos modos: `crear` y `abrir`

| | `crear` | `abrir` |
|---|---|---|
| modo del `RandomAccessFile` | `"rw"` | `"r"` |
| para qué | la construcción | las consultas |
| `agregar` | sí | lanza `IllegalStateException` |

`crear` arranca con `setLength(0)`: si el archivo existía, lo reemplaza.
`abrir` lo deja como está y no permite escribir, que es lo que corresponde para
los comandos de consulta — si un bug intentara escribir ahí, salta en vez de
corromper el archivo.

`IllegalStateException` es **no chequeada**: agregar sobre un archivo abierto
para leer es un error de programación, no una condición del disco. Mismo
criterio que `TamPaginaInvalidoException`.

## `agregar`: un solo `write`

El registro se arma entero en un `byte[]` y se escribe de una:

```java
byte[] registro = new byte[CAB_REGISTRO + texto.length];
escribirLong(registro, OFF_CLAVE, clave);
escribirInt(registro, OFF_LARGO, texto.length);
for (int i = 0; i < texto.length; i++) registro[CAB_REGISTRO + i] = texto[i];
archivo.seek(posicion);
archivo.write(registro);
```

Podríamos haber hecho `writeLong`, `writeInt` y `write` sobre el
`RandomAccessFile`, pero son **tres llamadas al sistema operativo por registro**
en vez de una. Con 300 000 registros eso es 900 000 syscalls contra 300 000.

La copia del texto es un `for` a mano porque **`System.arraycopy` está
prohibido**.

`finDeArchivo` se lleva en memoria y se devuelve como posición antes de
avanzar. No hace falta preguntarle el largo al archivo en cada `agregar`.

## `leer(pos)`: una posición, un registro

`seek(pos)`, leer los 12 bytes de cabecera, sacar el `largo`, leer esos bytes.
Antes valida que `pos` y `pos + 12 + largo` entren en el archivo: una posición
inventada daría `EOFException` o, peor, un `largo` disparatado y un
`new byte[largo]` de gigabytes. Con la validación da
`FormatoInvalidoException` con un mensaje que dice qué pasó.

Suma 1 a `registrosLeidos`. Ese contador es el equivalente de
`paginasLeidas` del índice: es la unidad en la que vamos a comparar el acceso
por índice contra la búsqueda secuencial.

## `busquedaSecuencial`: el rival a vencer

Recorre el archivo desde el byte 0 hasta encontrar la clave, o hasta el final.
Es `O(N)` registros contra los `niveles()` del B+, y es justamente lo que el
Ejercicio 2 quiere medir.

Dos detalles de implementación:

1. **Solo lee la cabecera de cada registro.** Lee `clave` y `largo`, compara, y
   si no es la buscada **saltea** el texto con `skipBytes`. No tiene sentido
   traer 90 bytes de título para descartarlos.
2. **Usa un stream con buffer**, no `seek` por registro:

   ```java
   new DataInputStream(new BufferedInputStream(new FileInputStream(ruta), 64 KB))
   ```

   Con `seek` + `read` por registro serían dos syscalls por registro: 600 000
   para un archivo de 300 000. Con un buffer de 64 KB el sistema operativo trae
   el archivo de a bloques grandes y la cuenta baja a unos pocos cientos. El
   enunciado permite explícitamente `Buffered*` y `Data*Stream` (página 11).

   Esto importa para que el tiempo que mida la Tabla C sea el del **algoritmo**
   y no el de nuestra forma de leer.

`skipBytes` puede saltar menos de lo pedido, así que `saltar` lo llama en un
bucle hasta completar. Si devuelve 0 o menos, el archivo se cortó en medio de
un registro y eso es `EOFException`.

El fin de archivo se detecta con el `EOFException` del primer `readLong`: si no
hay ni 8 bytes más, no hay otro registro.

## `claveEn(pos)`

Devuelve solo la clave de una posición, sin traer el texto. No está en la lista
del enunciado; lo agregamos porque `ExperimentoBMas` necesita recorrer claves y
no tiene por qué pagar la lectura del título.

## Contadores

`registrosLeidos()` cuenta los registros que tocaron `leer`, `claveEn` y
`busquedaSecuencial`. `agregar` **no** cuenta: la construcción no es una
consulta que se mida, igual que `crear` en el índice.

`tamano()` devuelve el tamaño en **bytes** del archivo, que es lo que pide la
Tabla B. La cantidad de registros está en `cantidadRegistros()`.
