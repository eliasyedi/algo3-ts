# 02 — Persistencia, contadores y excepciones

## `crear(ruta, P)`

1. Valida `P` con `tamPaginaValido`: `P ≥ 40`, `L ≥ 2`, `M ≥ 3`. Si no pasa,
   lanza `TamPaginaInvalidoException`.
2. Abre el archivo en modo `"rw"` y hace `setLength(0)`: si existía, lo
   reemplaza (lo pide el enunciado).
3. Deja el estado del árbol vacío: `raiz = 1`, `cantidadPaginas = 2`
   (#0 cabecera + #1 hoja), `niveles = 1`, `cantidadClaves = 0`.
4. Escribe la cabecera y la página #1 como hoja vacía.
5. **`reiniciarContadores()`**.

### Por qué el paso 5

El enunciado dice que los contadores *arrancan en 0 al crear o abrir*, y la
traza de referencia lo confirma: `crear` tiene `R = –` y `W = –`, y recién
`insertar(50)` muestra `W = 1`. Construir el archivo no es una operación del
árbol que se mida; es la preparación. Entonces `crear` escribe sus 2 páginas
con los mismos métodos de siempre y después pone los contadores en 0.

### Por qué la validación de P no está en el constructor

El constructor es privado y **solo calcula**: guarda `archivo` y `tamPagina`, y
deriva `L` y `M`. La validación no puede estar ahí porque ante el **mismo** `P`
imposible los dos puntos de entrada tienen que fallar distinto:

| entrada | `P` imposible significa | excepción |
|---|---|---|
| `crear(ruta, 20)` | el programador pidió un `P` que no sirve | `TamPaginaInvalidoException` (no chequeada) |
| `abrir(ruta)` con `P = 20` en la cabecera | el archivo está corrupto o es de otro programa | `FormatoInvalidoException` (chequeada) |

Si la validación viviera en el constructor, `abrir` sobre un archivo corrupto
tiraría `TamPaginaInvalidoException`, que es mentira: nadie pidió ese `P`, se
leyó del disco.

## `abrir(ruta)`

Lee **solo los 40 bytes de la cabecera**. No recorre el árbol, no lo
reconstruye: el árbol ya está en el disco, la cabecera alcanza para saber por
dónde empezar (`raiz`) y cuánto bajar (`niveles`).

Valida, en orden, y ante cualquier falla lanza `FormatoInvalidoException`:

| chequeo | por qué |
|---|---|
| el archivo existe y mide ≥ 40 bytes | si no, no hay ni cabecera que leer |
| `MAGIC == 0x41454433` | es de nuestro formato y no cualquier `.dat` |
| `versión == 1` | un formato futuro tendría otros offsets |
| `P` admite `L ≥ 2` y `M ≥ 3` | un `P` basura hace que todo lo demás no tenga sentido |
| `M` y `L` guardados == los recalculados desde `P` | ver abajo |
| `cantidadPaginas ≥ 2` | el mínimo es cabecera + raíz |
| largo del archivo == `cantidadPaginas · P` | detecta truncado o pisado |
| `1 ≤ raiz < cantidadPaginas` | un `seek` a una página inexistente |
| `niveles ≥ 1` y `cantidadClaves ≥ 0` | valores imposibles |

Si algo falla, cierra el `RandomAccessFile` antes de propagar: no queda un
descriptor abierto por un archivo que no sirve.

### Por qué recalcula M y L en vez de confiar en los bytes 12 y 16

Lo pide el enunciado ("valida … que `M` y `L` correspondan a `P`"), pero la
razón es más fuerte que eso. `M` y `L` **no son datos independientes**: son
función de `P`. Están guardados por comodidad y para documentar el archivo,
pero la fuente de verdad es `P`.

Si el programa confiara en ellos y el archivo trajera `M = 7` con `P = 60`,
el código calcularía `offsetClaveInterno(i) = 12 + 4·7 + 8i` y **leería las
claves del lugar equivocado** — sin error, sin excepción, devolviendo basura
que parece un árbol. Esa es la clase de bug que no se detecta nunca. El chequeo
es barato (dos divisiones) y convierte una corrupción silenciosa en una
excepción con mensaje.

No es solo "verificar correctitud" de nuestro código: es defenderse de un
archivo **ajeno** o modificado, que es exactamente lo que `abrir` recibe.

## `close()`

Escribe la cabecera y cierra. Es lo único que hace falta, porque todas las
páginas modificadas ya se escribieron dentro de cada `insertar`: el enunciado
exige que al retornar `insertar` no quede nada pendiente en RAM.

Es idempotente (`cerrado`), así que un `close` repetido o un
`try-with-resources` anidado no rompe nada. La clase implementa
`AutoCloseable` para poder usarse con `try (ArbolBMas a = …)`.

## Qué cuenta y qué no en los contadores

`paginasLeidas` y `paginasEscritas` cuentan **páginas de nodo** transferidas
desde/hacia el disco. Una transferencia = una página de `P` bytes.

| operación | cuenta |
|---|---|
| `leerPagina(nro)` | sí, `paginasLeidas++` |
| `escribirPagina(nro, pag)` | sí, `paginasEscritas++` |
| leer/escribir la cabecera | **no** |
| `volcarPagina`, `volcarArbol`, `estadisticas`, `verificarInvariantes` | **no** |

### Por qué la cabecera no cuenta

Porque **no es un nodo**: es metadata del archivo, no parte del árbol. El
enunciado lo dice explícito ("la cabecera se reescribe cuando cambia y **no se
cuenta**"), y la traza lo necesita para cerrar: `insertar(10)` parte la raíz
hoja y da `W = 3` = hoja original + hoja nueva + raíz nueva. La cabecera
también cambió en esa inserción (cambió `raiz`, `cantidadPaginas` y `niveles`),
así que si contara daría `W = 4` y no cerraría con la tabla.

Por eso en el código hay dos pares de métodos separados: `leerPagina` /
`escribirPagina` cuentan, y `escribirCabecera` no. Es la única razón por la que
están duplicados.

El contrato que tiene que cumplirse cuando estén las operaciones:

| operación | lecturas | escrituras |
|---|---|---|
| `buscar(x)`, exista o no `x` | `niveles()` | 0 |
| `insertar` con `s` splits | `niveles()` | `2s + 1` |
| `insertar` de una clave repetida | `niveles()` | 0 |

## Las tres excepciones

El criterio es el mismo del TP2: **chequeada** si la causa es externa al
programa y el que llama puede recuperarse; **no chequeada** si es un error de
programación que se arregla cambiando el código.

| excepción | tipo | causa | por qué |
|---|---|---|---|
| `FormatoInvalidoException` | chequeada, extiende `IOException` | `abrir` sobre algo que no es un índice válido | el estado del disco es externo: el archivo puede estar corrupto, truncado o ser de otro programa, y quien llama no puede descartarlo antes de abrirlo. Sí puede recuperarse: avisar y reconstruir el índice. Y es un error de E/S, así que hereda de `IOException` como el resto de la familia |
| `RangoInvalidoException` | no chequeada | `buscarRango(desde, hasta)` con `desde > hasta` | un intervalo dado vuelta no depende del disco ni de nada externo; lo armó mal el que llama. Se arregla corrigiendo ese código, no atrapando la excepción |
| `TamPaginaInvalidoException` | no chequeada | `crear` con `P < 40`, `L < 2` o `M < 3` | el `P` es un argumento que elige el programador, normalmente una constante del programa. Pedir `P = 20` es un error de programación, igual que pasar un índice negativo a un arreglo |

La justificación está también en la cabecera de cada archivo fuente, porque el
enunciado la pide **en un comentario**.

## Las dos clases chicas

**`ConsumidorEntrada`** — interface propia de un solo método,
`void aceptar(long clave, long posicion)`. Al tener un solo método abstracto es
funcional, así que `buscarRango` se puede llamar con una lambda y **no necesita
armar ninguna lista**, que es justo lo que no podemos hacer (prohibido
`java.util`). Es el mismo patrón que `Comando` en el TP1: la estructura define
la interface y el cliente decide qué hacer con cada elemento.

**`EstadisticasBMas`** — campos públicos (`hojas`, `internos`,
`ocupacionHojas`, `ocupacionInternos`). Es una tupla de resultados de solo
lectura, no un objeto con comportamiento: no tiene invariante que proteger, así
que ponerle getters sería ruido. El enunciado pide explícitamente campos
públicos.
