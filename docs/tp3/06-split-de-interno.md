# 06 — Split de interno, propagación y raíz nueva

## La diferencia que define todo

| | hoja | interno |
|---|---|---|
| qué es la clave | un **dato** | un **cartel** |
| el separador que sube | se **copia** | se **mueve** |
| ¿queda en el nivel? | sí | **no, desaparece** |

En una hoja, si la clave que sube desapareciera, perderíamos el par
⟨clave, posición⟩: es información. En un interno la clave solo dice "de acá
para la derecha", y esa indicación ya la va a dar el padre; con una copia
alcanza, y dejar dos sería redundante.

Por eso, después de `insertar(45)`, el **50 ya no está** en `#3` ni en `#7`:
subió a la raíz `#8(50)`. Y el 30, que subió de una hoja, sigue estando en la
hoja `#5[30,35]`.

## El split

Llega un separador y el padre ya tiene `M − 1` claves. Con el nuevo serían
`M` claves y `M + 1` hijos: uno más de lo que entra. Se arman en memoria los
dos arreglos, metiendo lo nuevo en su lugar:

```java
long[] claves = new long[M];        // separador va en claves[i]
int[]  hijos  = new int[M + 1];     // nroDerecha va en hijos[i+1]
```

El corte se mide **en hijos**, no en claves:

```java
int izquierda = (M + 2) / 2;        // = ceil((M + 1) / 2) hijos
```

| se queda en la página original | sube | va al interno nuevo |
|---|---|---|
| `hijos[0 .. izquierda−1]` | `claves[izquierda−1]` | `hijos[izquierda .. M]` |
| `claves[0 .. izquierda−2]` | | `claves[izquierda .. M−1]` |

Con `M = 4`: `izquierda = 3`, así que quedan 3 hijos con las 2 claves que hay
entre ellos, sube la tercera, y al nuevo van 2 hijos con 1 clave. Las cuentas
cierran: `2 + 1 + 1 = 4 = M` claves y `3 + 2 = 5 = M + 1` hijos.

**El interno nuevo arranca y termina con un hijo**, igual que cualquier
interno: `hijos = claves + 1`.

### `insertar(45)` paso a paso

La hoja `#5[30,35,40]` está llena y llega el 45:

```
en memoria: 30 35 40 45          izquierda = 2
  #5 queda [30,35]   #6 nueva [40,45]   sube 40 (copiado)
```

El padre `#3` tenía 3 claves (`M − 1 = 3`), o sea lleno. Con el 40:

```
en memoria: #1 30 #5 40 #6 50 #2 80 #4        izquierda = 3 hijos
  #3 queda (30,40) con hijos #1 #5 #6
  sube 50, y DESAPARECE de este nivel
  #7 nuevo  (80)   con hijos #2 #4
```

`#3` ya no tiene padre, así que se crea la raíz `#8(50)` con hijos `#3` y `#7`,
y `niveles` pasa de 2 a 3.

Páginas asignadas, **de abajo hacia arriba**: `#6` (hoja), `#7` (interno),
`#8` (raíz). Escrituras: 2 por cada uno de los 2 splits, más 1 de la raíz
nueva = **5 = 2·2 + 1**. Lecturas: 2, que era `niveles()` antes de crecer.

## La propagación

`propagar` se llama a sí misma con `nivel − 1`:

```java
propagar(…, nivel - 1, claves[izquierda - 1], paginasCamino[nivel], nroNuevo);
```

Tres casos, y son todos los que hay:

1. **el padre tiene lugar** → se mete la clave, se escribe, se termina (`W` suma 1);
2. **el padre está lleno** → se parte (`W` suma 2) y se sube un nivel;
3. **no hay padre** (`nivel < 0`) → raíz nueva (`W` suma 1) y `niveles++`.

Los casos 1 y 3 **cortan** la recursión; el 2 es el único que sigue. Por eso la
cuenta siempre da `2s + 1`: cada split aporta 2 y el último paso, el que sea,
aporta 1.

El `paginasCamino[nivel]` que se pasa como hijo izquierdo es la página que se
acaba de partir: **se quedó donde estaba**. Esto vale tanto para hojas como
para internos, y es lo que evita tener que avisarle a nadie más.

## `limpiarColaInterno`

Después del corte, la página original conserva en el disco los hijos y claves
de la mitad derecha. Como en la hoja, no molestan porque `n` manda, pero el
enunciado pide los bytes no usados en 0. Se limpian los dos bloques por
separado, porque no son contiguos:

```java
for (int b = offsetHijo(hijosQueQuedan); b < offsetHijo(M); b++) …
for (int b = offsetClaveInterno(hijosQueQuedan - 1); b < offsetClaveInterno(M - 1); b++) …
```

`offsetHijo(M)` es exactamente donde arranca el bloque de claves, así que el
primer loop no se pasa.

## La prueba más fuerte del TP

El test construye el árbol con 13 `insertar` y lo compara **byte a byte** con
el archivo armado a mano desde la figura del enunciado
(`construirArbolDeReferencia`). Son idénticos, los 600 bytes.

Eso prueba de una sola vez: los números de página asignados, el orden de
asignación, qué clave sube y cuál se copia, la cadena de hojas, los `n` de cada
nodo, el relleno en 0 y la cabecera. Si cualquiera de esas cosas estuviera un
byte corrida, el test dice en qué offset.
