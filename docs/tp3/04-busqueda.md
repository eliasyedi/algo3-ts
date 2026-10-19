# 04 — `buscar`, `caminoBusqueda` y `volcarArbol`

## La regla de descenso

En un interno se baja por el hijo `i`, donde

> `i` = **cantidad de claves del nodo que son ≤ x**

Con `#3 I n=3 [#1 20 #9 30 #5 40 #6]` y `x = 35`: las claves ≤ 35 son 20 y 30,
son 2, así que se baja por `hijos[2] = #5`. Y efectivamente 35 está en `#5`.

El `≤` (y no `<`) es lo que implementa **"una clave igual a un separador va a
la derecha"**. Con `x = 30`: las claves ≤ 30 son 20 y 30, son 2, se baja por
`#5`, que es la hoja `[30,35]`. Si fuera `<` daría 1 y bajaríamos a `#9 [20,25]`,
donde 30 no está — y `buscar(30)` devolvería −1 teniendo la clave en el árbol.
El test lo cubre con 20, 40 y 50, que son separadores.

## `indiceHijo`: búsqueda binaria propia

`Arrays.binarySearch` está prohibido, y además no sirve: no queremos la
posición de la clave, queremos **cuántas claves son ≤ x**. Es una binaria de
cota superior:

```java
int bajo = 0, alto = n - 1, cantidadMenoresOIguales = 0;
while (bajo <= alto) {
    int medio = (bajo + alto) >>> 1;
    if (claveInterno(pagina, medio) <= clave) {
        cantidadMenoresOIguales = medio + 1;
        bajo = medio + 1;
    } else {
        alto = medio - 1;
    }
}
```

Cada vez que el medio cumple `≤ x`, sabemos que **todas** las de su izquierda
también cumplen, así que hay al menos `medio + 1`; lo anotamos y seguimos
buscando a la derecha por si hay más. Si no cumple, la respuesta está a la
izquierda. Al salir, `cantidadMenoresOIguales` es el índice del hijo.

El `>>> 1` en vez de `/ 2` es un desplazamiento sin signo: con `bajo + alto`
grande evita el desbordamiento clásico de `(bajo + alto) / 2`. Acá `n` es como
mucho `M − 1 = 340`, así que no haría falta, pero es el idioma correcto.

La binaria es **dentro de la página**, que ya está en memoria: no cuesta E/S.
Por eso el costo del árbol es `Θ(log_M N)` **accesos a disco** y
`Θ(log N)` comparaciones.

## `buscarEnHoja`

Binaria clásica sobre los pares de la hoja. Si no encuentra, devuelve
`-(bajo + 1)`, el **punto de inserción** codificado en negativo (el mismo
convenio que `Arrays.binarySearch`). Hoy `buscar` solo mira el signo, pero
`insertar` va a necesitar exactamente ese número: dónde meter el par nuevo.

## `buscar(x)`

```java
int nro = raiz;
for (int nivel = 1; nivel < niveles; nivel++) {
    byte[] interno = leerPagina(nro);
    nro = hijo(interno, indiceHijo(interno, clave));
}
byte[] hoja = leerPagina(nro);
```

El loop corre `niveles − 1` veces (los internos) y después se lee la hoja:
**exactamente `niveles()` páginas**, que es el contrato. No hay early return:
una clave inexistente cuesta lo mismo que una existente, porque hasta no llegar
a la hoja no se sabe. El test lo verifica con `buscar(33) = −1` y 3 lecturas.

Devuelve la posición en `datos.dat`, o `−1`. Nunca lanza excepción: no
encontrar una clave es un resultado normal, no un error.

## `caminoBusqueda(x)`

Devuelve los números de página de la raíz a la hoja: `caminoBusqueda(35)` da
`[8, 3, 5]`.

Lee **`niveles − 1` páginas, no `niveles`**: el número de la hoja lo da el
último interno en su arreglo de hijos, así que no hace falta abrirla para
saber cuál es. Por eso anota `nro` *antes* de leer, y después del loop mete el
último a mano.

No está en la lista de métodos de diagnóstico del enunciado, así que **sí
cuenta** las páginas que lee: es trabajo real sobre el disco.

## `volcarArbol`

Recorre por niveles. Sin `java.util` no hay cola, así que el nivel actual es un
`int[]` de números de página y se arma el siguiente mientras se imprime el
actual:

```
nivelPaginas = { raiz }
por cada nivel:
    por cada pagina del nivel:
        leerla (sin contar), imprimirla
        si es interno, apilar sus n+1 hijos en el arreglo del nivel siguiente
    nivelPaginas = ese arreglo
```

El arreglo de hijos arranca en `cantidadNivel * 2` y se duplica con `copiar`
cuando se llena. `copiar` está escrito a mano porque **`System.arraycopy` está
prohibido**.

### Formato

```
1: #8(50)
2: #3(20,30,40) | #7(80)
3: #1[10,15] | #9[20,25] | #5[30,35] | #6[40,45] | #2[50,60,70] | #4[80,90]
```

Internos entre **paréntesis**, hojas entre **corchetes**, claves separadas por
coma sin espacio, nodos de un mismo nivel separados por `" | "`, cada línea
prefijada con `nivel + ": "`. Las hojas muestran **solo las claves**, no las
posiciones (a diferencia de `volcarPagina`, que muestra `clave:posicion`).
El árbol vacío da `1: #1[]`.

### Lo que no cumple, a propósito

`volcarArbol` tiene un nivel entero de números de página en memoria, así que no
respeta el `O(h)` de las operaciones. Está bien: es diagnóstico, no una
operación del árbol, y su trabajo es justamente mostrarlo todo. Por eso también
lee con `leerPaginaCruda` y no toca los contadores.
