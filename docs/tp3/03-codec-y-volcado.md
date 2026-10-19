# 03 — Codificar y decodificar una página, y `volcarPagina`

## Por qué no hay una clase `Nodo`

La tentación es hacer `class Nodo { long[] claves; int[] hijos; … }` y traducir
página ↔ objeto. No lo hacemos, por dos razones:

1. **La regla del enunciado.** El árbol no puede mantenerse como objetos Java
   enlazados. Un `Nodo` suelto por página sería legal mientras no se encadene,
   pero invita a guardarlos, y de ahí a un árbol en RAM hay un paso.
2. **No sirve de nada.** Un split necesita `L + 1` pares, que **no entran** en
   una página; y el resto de las operaciones solo necesita mirar un campo por
   vez. Traducir los `P` bytes enteros a un objeto para leer una clave es
   trabajo al pedo.

La representación en memoria **es** el `byte[]` de `P` bytes que devolvió el
disco. El "codec" son accesores finos que leen o escriben un campo en su
offset, sin copiar nada:

| sobre cualquier nodo | sobre una hoja | sobre un interno |
|---|---|---|
| `tipo`, `esHoja` | `claveHoja(pag, i)` | `hijo(pag, i)` |
| `cantidadClavesNodo`, `ponerCantidadClavesNodo` | `posicionHoja(pag, i)` | `ponerHijo(pag, i, nro)` |
| `siguiente`, `ponerSiguiente` | `ponerParHoja(pag, i, clave, pos)` | `claveInterno(pag, i)`, `ponerClaveInterno` |

Los de hoja son `static` porque sus offsets dependen solo de `i`. Los de clave
de interno **no pueden serlo**: el offset es `12 + 4·M + 8·i` y necesita la `M`
de la instancia.

`n` significa siempre **cantidad de claves**. En una hoja eso es la cantidad de
pares; en un interno los hijos son `n + 1`. Por eso `hijo(pag, i)` se puede
pedir hasta `i = n`, uno más que las claves.

## Leer contando y leer sin contar

`volcarPagina`, `volcarArbol`, `estadisticas` y `verificarInvariantes` son
**diagnóstico**: miran el disco pero no forman parte de la operación que se
mide, así que no pueden tocar los contadores. Si contaran, cualquier `OK/FALLA`
del test que imprima el árbol después de insertar arruinaría la columna `W` de
la traza.

Por eso la lectura está partida en dos:

```java
private byte[] leerPaginaCruda(int nro)   // hace el seek + readFully
private byte[] leerPagina(int nro)        // llama a la cruda y hace paginasLeidas++
```

Las operaciones del árbol usan `leerPagina`. El diagnóstico usa
`leerPaginaCruda`. Es la misma idea que `escribirCabecera` versus
`escribirPagina`: un solo lugar hace la E/S y el contador se agrega afuera.

## `volcarPagina(nro)`

Se corrige **carácter por carácter**, así que el formato está armado a mano con
`StringBuilder` (que es `java.lang`, no `java.util`).

### Hoja

```
#5 H n=2 sig=6 [30:300 35:350]
```

`#` + número de página, `H`, `n=`, `sig=`, y entre corchetes los pares
`clave:posicion` separados por **un** espacio. El espacio va *antes* de cada par
menos del primero (`if (i > 0)`), que es lo que evita el espacio sobrante antes
del `]`. Una hoja vacía da `#1 H n=0 sig=-1 []`.

### Interno

```
#3 I n=3 [#1 20 #9 30 #5 40 #6]
```

Alternan hijo y clave, y **empieza y termina con un hijo**: son `n + 1` hijos y
`n` claves. El loop escribe `#hijo_i clave_i ` para `i = 0..n−1` y después el
último `#hijo_n`, así que el `]` queda pegado sin espacio.

### El guard

`nro` tiene que estar en `[1, cantidadPaginas − 1]`. La página 0 es la cabecera,
no un nodo, y una página más allá del final no existe. Lanza
`IllegalArgumentException` (no chequeada) porque pedir una página que no existe
es un error de programación del que llama, el mismo criterio que
`TamPaginaInvalidoException`.

## Cómo se prueba sin tener `insertar`

`volcarPagina` se puede verificar **antes** que la inserción: el test arma a
mano el archivo del árbol final de la traza (10 páginas, raíz `#8`, 3 niveles,
13 claves) escribiendo los bytes con `RandomAccessFile.writeInt` y
`writeLong`, abre el índice con `abrir` y compara los dos volcados que el
enunciado da como ejemplo.

Eso prueba dos cosas de una:

- que el **formato** coincide con el del enunciado, porque `writeInt`/`writeLong`
  de Java son big-endian y nuestro `leerInt`/`leerLong` los lee bien: si la
  máscara `& 0xFF` o el orden de los bytes estuvieran mal, los números saldrían
  distintos;
- que el **texto** sale carácter por carácter como el ejemplo.

El mismo árbol de referencia (`construirArbolDeReferencia`) se reusa después
para probar `buscar`, `caminoBusqueda`, `volcarArbol` y `buscarRango` sin
depender de que `insertar` ya funcione.
