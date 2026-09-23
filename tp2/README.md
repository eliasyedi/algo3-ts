# TP2 — U2–U3: Análisis, ABB aumentado y Tablas de Dispersión

**Algoritmos y Estructura de Datos III — Año 2026, 2do Periodo**
**Grupo:** g_ts5 — **Sección:** TS
**Integrantes:** Figueredo Pistilli, Aurelio (CIC 4.010.315) · Olmedo Echeverría, Elías Rubén (CIC 4.653.503)

---

## 1. Decisiones de diseño

**Una sola fuente de verdad para el tamaño.** El árbol no lleva un contador `size` aparte:
`size()` devuelve `tamano(raiz)`. El campo `tamano` de cada nodo se recalcula en `actualizarAntecesores`,
que sube desde el nodo tocado hasta la raíz rehaciendo `n.tamano = 1 + tamano(n.izq) + tamano(n.der)` y la
altura. Así el invariante no puede desincronizarse; `tamanosConsistentes()` lo verifica en la clase de prueba.

**Puntero al padre (campo extra permitido).** Cada `Nodo` guarda `padre`. Eso permite que `agregar`,
`eliminar` y la actualización de tamaños sean **iterativos**: no hay recursión en ninguna operación del TAD.
Es lo que hace que el experimento con inserción ordenada y N = 10 000 (altura 9 999) corra sin agotar la pila
de la JVM con los parámetros por defecto.

**`eliminar` con mudanza, nunca copia de clave.** En el caso de dos hijos, el sucesor inorden se *relinkea*
(se le reasignan `izq`/`der`/`padre` a la posición del eliminado). No se copia la clave de un nodo a otro.
Esta decisión es la que sostiene al Ejercicio 2: la tabla guarda **referencias a objetos `Nodo`**, y si
copiáramos la clave, esas referencias quedarían apuntando a una clave que ya no les corresponde. La Fase D de
la traza es la prueba: tras `eliminar(30)`, `indice.obtener(35)` sigue devolviendo `"P35"` y `kEsimo(2)` sigue
siendo `35` — el objeto que se mudó.

**Contador de visitas como instrumento, no como parte del TAD.** Se incrementa sólo en las operaciones que
descienden por el árbol (`agregar`, `eliminar`, `obtener`, `contiene`, `kEsimo`, `cuantosMenores`,
`consultarRango`, `consultarRangoIngenuo`, `rango`, `sucesor`, `predecesor`). No lo tocan `toString`,
`iterator`, `tamanosConsistentes`, `size` ni `altura`. `obtenerNodo` tampoco: es el helper interno que usa el
índice doble para capturar la referencia, no una consulta de cliente.

**Excepciones: chequeada vs. no chequeada.** `ClaveInexistenteException` extiende `Exception` porque
representa una situación que el cliente **puede prever y manejar**: buscar un paquete que ya se despachó es un
uso normal del código. Las otras tres extienden `RuntimeException` porque representan **errores de
programación de quien llama**: `ClaveNulaException` (pasar `null`), `RangoInvalidoException` (`a > b` en
`consultarRango`) y `IndiceFueraDeRangoException` (`kEsimo(0)` no lo provoca un cliente, lo provoca un índice
mal calculado).

**Tabla con cadenas propias.** `TablaEncadenada` usa un arreglo crudo de `NodoHash` propios, uno por cubeta.
No se usa ninguna estructura de `java.util`. Compresión `(k.hashCode() & 0x7fffffff) % m`; la máscara evita el
índice negativo que produciría `Integer.MIN_VALUE`. Rehash **después** de insertar, cuando `n > m · alfaMax`,
duplicando `m`. El constructor `TablaEncadenada(int m, double alfaMax)` con `alfaMax = POSITIVE_INFINITY`
desactiva el rehash y habilita el experimento con `m = 97` fijo.

---

## 2. Análisis asintótico — Ejercicio 1

**(1) Recurrencia de `kEsimo` en función de la altura.** En cada nodo se leen `tamano(n.izq)` y se hacen tres
comparaciones de enteros; después se baja a un solo hijo. El subárbol al que se baja tiene altura a lo sumo
`h − 1`:

```
T(h) = T(h-1) + Θ(1),  T(1) = Θ(1)
```

Desenrollando: `T(h) = T(h-2) + 2c = ... = T(1) + (h-1)c`, o sea **T(h) = Θ(h)**. No hay inorden: se recorre
un único camino raíz→nodo.

**(2) Búsqueda (o `kEsimo`) en un ABB perfectamente balanceado.** Ahí `h = Θ(log n)` y cada paso descarta la
mitad de las claves:

```
T(n) = T(n/2) + Θ(1),  T(1) = Θ(1)
```

Por **Teorema Maestro**: `a = 1`, `b = 2`, `f(n) = Θ(1)`. Entonces `n^(log_b a) = n^(log_2 1) = n^0 = 1`.
Como `f(n) = Θ(1) = Θ(n^(log_b a))`, estamos en el **caso 2**, y la solución es
`T(n) = Θ(n^(log_b a) · log n) = ` **Θ(log n)**.

**(3) Búsqueda en un ABB degenerado (inserción ordenada).** Cada nodo tiene un solo hijo, así que el
subproblema baja de a uno:

```
T(n) = T(n-1) + Θ(1)
```

Desarrollando: `T(n) = T(n-1) + c = T(n-2) + 2c = ... = T(1) + (n-1)c`, o sea **T(n) = Θ(n)**.
El **Teorema Maestro no aplica**: exige subproblemas de tamaño `n/b` con `b > 1`, y acá el subproblema es
`n − 1`, que no es una fracción constante de `n`.

**(4) `consultarRango` aumentado vs. ingenuo.** El aumentado hace dos `cuantosMenores` y un `contiene`: tres
descensos de a lo sumo `h` pasos, sin recorrer el árbol. Es **Θ(h)** en tiempo y **Θ(1)** en espacio extra
(implementación iterativa; sería Θ(h) de pila si fuera recursiva). El ingenuo recorre el inorden completo y
toca los `n` nodos **siempre**, sin importar la forma del árbol ni el ancho del intervalo: **Θ(n)** en todo
caso — mejor, peor y promedio.

**(5) Coste espacial del árbol.** Un objeto `Nodo` por clave, con un número constante de campos
(`clave`, `valor`, `izq`, `der`, `padre`, `tamano`, `altura`): **Θ(n)**. El entero `tamano` agregado por nodo
suma Θ(1) por nodo, así que **no cambia el orden**: sigue siendo Θ(n). Es el intercambio central del TP —
Θ(n) de espacio extra constante por nodo a cambio de bajar `kEsimo` y `consultarRango` de Θ(n) a Θ(h).

### Tabla 1 — Visitas del ABB (Fisher–Yates, semilla 2026)

| N | h_aleat | h_ord | vis_kEsimo_aleat | vis_kEsimo_ord | vis_rango_aum | vis_rango_ing |
|---:|---:|---:|---:|---:|---:|---:|
| 2000 | 27 | 1999 | 14 | 1000 | 47 | 2000 |
| 4000 | 26 | 3999 | 18 | 2000 | 59 | 4000 |
| 6000 | 27 | 5999 | 21 | 3000 | 46 | 6000 |
| 8000 | 29 | 7999 | 11 | 4000 | 58 | 8000 |
| 10000 | 27 | 9999 | 16 | 5000 | 52 | 10000 |

**(6) Lectura de la tabla, columna por columna.**

`h_ord = N − 1` exacto: insertar `1..N` en orden hace que cada clave nueva sea mayor que todas las anteriores,
así que siempre baja por la derecha. El ABB degenera en una lista. Es el caso (3).

`h_aleat` se queda entre 26 y 29 mientras N se multiplica por 5. `2·log2(10000) ≈ 26,6`: la altura esperada de
un ABB aleatorio es Θ(log n), el caso (2). Que `h_aleat` no crezca de forma monótona (26 en N = 4000, 29 en
N = 8000) es ruido de una sola permutación por N, no una tendencia.

`vis_kEsimo_ord = N/2` exacto: en la lista, cada nodo tiene `tamano(izq) = 0`, así que `kEsimo` cae siempre en
el caso `k > l + 1` y baja a la derecha restando 1 a `k`. Para llegar al k-ésimo visita exactamente `k` nodos;
con `k = N/2` son `N/2` visitas. `kEsimo` sigue siendo Θ(h) — lo que se rompió es `h`, no el algoritmo. Ésa es
la diferencia entre (1) y (3).

`vis_kEsimo_aleat` entre 11 y 21, contra 1000–5000 del ordenado: el mismo código, sobre Θ(log n) en vez de
Θ(n).

`vis_rango_aum` en unas decenas (46–59) contra `vis_rango_ing = N` exacto: es el punto (4). El aumentado paga
tres descensos, `3h ≈ 80` como cota superior; el ingenuo paga `N`. Con N = 10 000 la diferencia es de 52 contra
10 000, un factor de 190.

**Por qué en el árbol chico de la traza el aumentado visita *más* que el ingenuo.** En la Fase B, con n = 9,
`consultarRango(35,65)` gasta **12** visitas y `consultarRangoIngenuo(35,65)` gasta **9**. No hay
contradicción con el párrafo anterior: Θ(h) vs. Θ(n) es una afirmación sobre el *crecimiento*, no sobre un `n`
fijo. Con n = 9 las constantes pesan más que el orden — el aumentado hace tres recorridos de altura 3 más el
costo de `contiene`, y eso ya supera los 9 nodos del árbol entero. El cruce se ve recién cuando `n >> h`, que
es exactamente por qué el experimento se corre con N de miles.

---

## 3. Análisis asintótico — Ejercicio 2

**(7) Búsqueda con encadenamiento.** Con hash uniforme simple, la cubeta esperada tiene `α = n/m` elementos y
la búsqueda recorre su cadena: tiempo **esperado Θ(1 + α)** (el `1` es calcular el hash e indexar; el `α`, la
cadena). **Peor caso Θ(n)**: todas las claves en una sola cubeta. Espacio **Θ(n + m)**: `n` nodos de cadena más
el arreglo de `m` cubetas.

**(8) `agregar` del índice doble.** Inserta en el árbol y registra la referencia en la tabla:
**Θ(h) + Θ(1 + α)** en caso promedio. **Peor caso Θ(n)**, en la llamada que dispara el rehash, porque hay que
reubicar las `n` claves. Pero como `m` se duplica cada vez, el trabajo total de rehash al insertar `n` claves
desde vacío suma `n + n/2 + n/4 + ⋯ < 2n`, o sea **O(n) repartido entre todas las inserciones**: O(1)
amortizado por inserción. El rehash es caro una vez cada tanto, no caro siempre.

### Tabla 2 — Índice doble con rehash (permutación semilla 2026)

| N | vis_ABB_get | sondas_hash_get | alfa | m |
|---:|---:|---:|---:|---:|
| 2000 | 28 165 | 2 000 | 0,7102 | 2 816 |
| 4000 | 62 114 | 4 000 | 0,7102 | 5 632 |
| 6000 | 92 072 | 6 000 | 0,5327 | 11 264 |
| 8000 | 127 687 | 8 000 | 0,7102 | 11 264 |
| 10000 | 155 535 | 10 000 | 0,8878 | 11 264 |

### Tabla 3 — Tabla sola, m = 97 fijo, sin rehash

| N | alfa | sondas/N |
|---:|---:|---:|
| 2000 | 20,6186 | 10,8150 |
| 4000 | 41,2371 | 21,1208 |
| 6000 | 61,8557 | 31,4288 |
| 8000 | 82,4742 | 41,7386 |
| 10000 | 103,0928 | 52,0468 |

**(9) Lectura de las dos tablas.**

En la **Tabla 2**, `sondas_hash_get = N` exacto: una sonda por búsqueda. El rehash dejó siempre `m ≥ N`
(2 816 ≥ 2 000, …, 11 264 ≥ 10 000) y las claves son `1..N`, así que `k mod m = k` y **cada clave cae en una
cubeta distinta**: es un hash perfecto sobre un universo chico. `α ≤ 1` y las sondas se quedan planas. Eso no
es un bug ni una trampa: es el caso `α ≤ 1` del punto (7), donde Θ(1 + α) es Θ(1).

`vis_ABB_get` crece **más rápido que lineal**: al pasar de N = 2 000 a N = 10 000 (factor 5), las visitas pasan
de 28 165 a 155 535 (factor 5,52). Por búsqueda son ≈ 14,1 y ≈ 15,6 visitas respectivamente — el costo *por
operación* crece como Θ(log n), así que el total crece como Θ(n log n). **Ésa es la razón de ser del índice
doble**: el cliente que llama con el número exacto paga 1 sonda en vez de ~16 comparaciones de claves, y el
árbol queda reservado para `kEsimo` y `consultarRango`, que la tabla no sabe responder.

En la **Tabla 3**, con `m = 97` fijo, `α = N/97` crece linealmente y `sondas/N` crece **con él**: 10,8 → 52,0
mientras `α` va de 20,6 a 103,1. El cociente `(sondas/N) / α` se mantiene en ≈ 0,50 en las cinco filas
(0,5245 · 0,5122 · 0,5081 · 0,5061 · 0,5049), que es exactamente la búsqueda exitosa esperada `1 + α/2`. Esto
confirma **Θ(1 + α)** y no Θ(1): si el contador de sondas nos hubiera dado una constante acá, el contador no
estaría contando. Las dos tablas juntas muestran los dos regímenes del mismo análisis — con rehash, `α` acotado
y costo constante; sin rehash, `α` sin techo y costo lineal en `α`.

---

## 4. Cómo ejecutar

Todos los `.java` están en un solo directorio, sin `package`.

```
javac *.java
java TestABBAumentado
java TestIndiceDoble
```

Ambas clases de prueba verifican sus propios resultados contra los valores del enunciado e imprimen `OK` o
`FALLA` por fila, cerrando con un resumen. En la corrida de entrega, las dos terminan con
`RESULTADO: todos los chequeos pasaron.`
