# 05 — `insertar`: split de hoja, separador y raíz nueva

Cubre todo `insertar` menos el split de interno, que es el
[paso 5](README.md). Si el padre está lleno, hoy lanza
`UnsupportedOperationException` a propósito.

## Recordar el camino

Antes de bajar se reservan tres arreglos de tamaño `niveles` (que se sabe de la
cabecera, sin leer nada):

| arreglo | qué guarda | para qué |
|---|---|---|
| `paginasCamino[k]` | qué página se leyó en el nivel `k` | para reescribirla al subir |
| `indicesCamino[k]` | por qué hijo se bajó | **dónde** entra el separador en ese padre |
| `contenidoCamino[k]` | los `P` bytes de esa página | para no tener que releerla |

El tercero es el que no es obvio: **no se puede releer el padre al subir**. El
contrato dice `R = niveles()`, y releer el padre sumaría lecturas de más. Como
ya lo tuvimos en la mano al bajar, nos lo guardamos.

Son `O(h)` páginas en memoria, que es justo lo que el enunciado permite.

## El caso fácil: la hoja tiene lugar

`buscarEnHoja` devuelve el punto de inserción en negativo. Si la clave ya
estaba (resultado ≥ 0) se retorna `false` sin escribir nada: `R = niveles`,
`W = 0`, y `cantidadClaves` no cambia.

Si hay lugar (`n < L`), `insertarParEnHoja` corre los pares de la derecha **de
atrás para adelante**:

```java
for (int k = n - 1; k >= pos; k--) {
    ponerParHoja(hoja, k + 1, claveHoja(hoja, k), posicionHoja(hoja, k));
}
```

De atrás para adelante porque si fuera de adelante para atrás cada par pisaría
al siguiente antes de haberlo leído. Una escritura: `W = 1 = 2·0 + 1`.

## Split de hoja

La hoja está llena y hay que meter uno más: son `L + 1` pares, que **no entran
en la página**. Por eso se arman dos arreglos de `L + 1` en memoria y se hace el
*merge* en una sola pasada, metiendo el par nuevo en su lugar:

```java
for (int k = 0, origen = 0; k < L + 1; k++) {
    if (k == posicionInsercion) { claves[k] = clave;  posiciones[k] = posicion; }
    else                        { claves[k] = claveHoja(hoja, origen++); … }
}
```

El corte:

```java
int izquierda = (L + 2) / 2;      // = ceil((L + 1) / 2)
```

Con `L = 3` son 4 pares y `izquierda = 2`; con `L = 255` son 256 y
`izquierda = 128`. La división entera trunca, así que `(L + 2) / 2` es la forma
de escribir `⌈(L+1)/2⌉` sin usar `Math.ceil` ni punto flotante.

- La hoja original **se queda en su misma página** con los primeros `izquierda`
  pares. No se mueve: si se moviera, habría que actualizar al padre y al
  `siguiente` de la hoja anterior, que no está en el camino.
- El resto va a una página nueva, al final del archivo
  (`nroNueva = cantidadPaginas`, y después `cantidadPaginas++`).

### `limpiarColaHoja`

Después del corte, la original todavía tiene en el disco los pares viejos de la
mitad derecha. No molestan, porque `n` dice cuántos son válidos, pero el
enunciado pide que **los bytes no usados queden en 0** y un volcado de bytes lo
mostraría. Se ponen en cero de `offsetClaveHoja(izquierda)` hasta
`offsetClaveHoja(L)`.

### La cadena, en este orden

```java
ponerSiguiente(nueva, siguiente(hoja));
ponerSiguiente(hoja, nroNueva);
```

Primero la nueva hereda el `siguiente` de la original, **después** la original
apunta a la nueva. Al revés se pierde el resto de la cadena: si primero hacés
`hoja.siguiente = nueva`, ya no sabés a quién apuntaba antes, y la cadena se
corta en el medio.

Se ve en `insertar(40)`, que parte una hoja **del medio**: `#1` apuntaba a `#2`,
y queda `#1 → #5 → #2 → #4`. El test lo verifica leyendo los `sig=` de las
cuatro hojas.

### Qué sube

El separador es la **primera clave de la hoja nueva, copiada**:
`claves[izquierda]`. Copiada, no movida: en una hoja la clave es un **dato**, y
si desapareciera perderíamos el par. Después del split, 30 está a la vez en el
interno `#3(30,…)` y en la hoja `#5[30,…]`.

(En un interno es al revés, y eso es el paso 5: ahí la clave es solo un cartel y
sube **desapareciendo** del nivel.)

## Subir: `propagar`

Dos casos.

**El padre tiene lugar** (`n < M − 1` claves). `insertarEnInterno` corre claves
e hijos, también de atrás para adelante, y los mete donde corresponde:

```java
ponerClaveInterno(interno, i, clave);     // la clave va en claves[i]
ponerHijo(interno, i + 1, nroHijo);       // el hijo nuevo va en hijos[i+1]
```

donde `i` es **el índice por el que se bajó**, que quedó en `indicesCamino`.
Esa asimetría (`i` para la clave, `i + 1` para el hijo) es el error clásico: el
hijo viejo sigue en `hijos[i]`, el nuevo es su hermano de la derecha, y la clave
es el cartel que va entre los dos.

Una escritura más: `W = 2 + 1 = 3 = 2·1 + 1`.

**No hay padre** (`nivel < 0`): se partió la raíz. Se **crea una página nueva**
(la vieja raíz *no se convierte* en interno, sigue donde estaba) con dos hijos
—la raíz vieja y la hoja nueva— y el separador entre ellos. Recién ahí
`niveles++`: es el **único** caso en que el árbol cambia de altura.

Se ve en `insertar(10)`: la raíz hoja `#1[20,50,80]` se parte en `#1[10,20]` y
`#2[50,80]`, y aparece la raíz `#3(50)`. Páginas asignadas en orden: primero la
hoja `#2`, después la raíz `#3` — de abajo hacia arriba, como pide el
enunciado. `W = 3`, `cantidadPaginas = 4`.

## La traza, fila por fila

El test reproduce las 11 primeras filas con `R`, `W`, `cantidadPaginas` y
`volcarArbol` carácter por carácter:

```
op            R  W  págs
insertar(50)  1  1  2
insertar(20)  1  1  2
insertar(80)  1  1  2
insertar(10)  1  3  4     ← split de raíz hoja, aparece #3
insertar(30)  2  1  4
insertar(60)  2  1  4
insertar(90)  2  3  5     ← split de la última hoja, aparece #4
insertar(40)  2  3  6     ← split de una hoja del medio, aparece #5
insertar(70)  2  1  6
insertar(25)  2  1  6
insertar(35)  2  1  6
```

Fijate que `R` pasa de 1 a 2 justo después de `insertar(10)`: el árbol ganó un
nivel y desde ahí toda inserción lee la raíz **y** la hoja.
