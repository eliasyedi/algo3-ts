# 07 — `buscarRango`, `verificarInvariantes` y `estadisticas`

## `buscarRango(desde, hasta, c)`

Primero la guarda: `desde > hasta` lanza `RangoInvalidoException`. Es un
intervalo dado vuelta, error de quien llama.

Después, **un solo descenso** buscando `desde`, exactamente igual que `buscar`.
De ahí en adelante **no se vuelve a tocar la raíz**: se sigue la cadena de
hojas con el campo `siguiente`. Eso es lo que hace barato un rango — por eso la
cadena existe.

```java
while (nro != -1) {
    byte[] hoja = leerPagina(nro);
    for (int i = 0; i < n; i++) {
        long clave = claveHoja(hoja, i);
        if (clave > hasta) return entregados;          // parada 1
        if (clave >= desde) { c.aceptar(…); entregados++; }
    }
    if (n == 0 || claveHoja(hoja, n - 1) >= hasta) return entregados;   // parada 2
    nro = siguiente(hoja);
}
```

### Las dos paradas

1. **Una clave > `hasta`**: ya nos pasamos, y como están ordenadas no hay nada
   más que entregar.
2. **La última clave de la hoja es ≥ `hasta`**: no hay que leer la siguiente.

La segunda es la que el enunciado pide explícitamente: *"pasa a la hoja
siguiente solo si la última clave de la actual es < hasta"*. Sin ella leerías
una hoja de más para darte cuenta de que te pasaste. Se ve en
`buscarRango(20, 45)`: la última clave de `#6` es 45, que no es `< 45`, así que
**no se lee `#2`** y el rango cuesta 5 páginas en vez de 6.

### Nada de listas

`c.aceptar(clave, posicion)` se llama en el momento en que el par aparece. No
se arma ninguna colección — no podríamos, `java.util` está prohibido — y el
método devuelve solo **cuántos** entregó. Quien llama decide qué hacer con cada
par: imprimirlo, contarlo, guardarlo. En el test, `Recolector` los va pegando en
un `StringBuilder`.

### Lo que cuesta

| consulta | páginas | de dónde salen |
|---|---|---|
| `buscarRango(31, 35)` | 3 | 2 internos + 1 hoja, para en la primera |
| `buscarRango(22, 62)` | 6 | 2 internos + 4 hojas |
| `buscarRango(20, 45)` | 5 | 2 internos + 3 hojas, la regla de parada ahorra una |
| `buscarRango(MIN, MAX)` | 8 | 2 internos + las 6 hojas |

En general: `(niveles − 1) + hojas recorridas`. Un rango de `s` registros
cuesta `niveles − 1 + ⌈s / ocupación de hoja⌉` páginas, contra las `s · niveles`
que costarían `s` búsquedas independientes.

## `verificarInvariantes()`

Devuelve `"OK"` o **la primera** violación, con su descripción. Recorre el árbol
con `leerPaginaCruda`, así que no toca los contadores.

### Lo global, antes de bajar

El largo del archivo tiene que ser `cantidadPaginas · P`. Si no, no vale la
pena mirar nada más.

### El recorrido: `verificar(nro, min, max, nivel, v)`

La recursión lleva **las cotas que le corresponden a ese subárbol**. La raíz
arranca con `(Long.MIN_VALUE, Long.MAX_VALUE)`, y el hijo `i` recibe:

```java
minHijo = (i == 0) ? min : claveInterno(pagina, i - 1);
maxHijo = (i == n) ? max : claveInterno(pagina, i);
```

El hijo de más a la izquierda hereda el `min` del padre (no tiene separador a su
izquierda) y el de más a la derecha hereda el `max`. Toda clave del subárbol
tiene que cumplir `min ≤ x < max`, que es el invariante **I2** tal cual está
escrito en el enunciado. El `<` a la derecha es el mismo `≤` de la regla de
descenso visto del otro lado.

Esto es lo que caza un separador que miente: si en `#3` cambiás el 20 por un
12, la hoja `#1[10,15]` pasa a tener `max = 12` y el 15 queda fuera de cota.
El test lo hace y el verificador responde:

```
la clave 15 de la hoja #1 viola sus separadores
```

### La cadena, con una sola variable

No hace falta guardar las hojas. Como el recorrido es de izquierda a derecha,
basta `esperadoEnCadena`:

- en la primera hoja (`esperadoEnCadena == SIN_HOJA_PREVIA`) no se compara nada;
- en cada hoja siguiente se exige `nro == esperadoEnCadena`;
- siempre, al salir, `esperadoEnCadena = siguiente(hoja)`;
- al terminar todo, tiene que valer `−1`.

Eso prueba **I4** entero: que la cadena recorre exactamente las hojas del árbol,
en orden, y termina. Si rompés un `siguiente`, salta:

```
la cadena de hojas esperaba #2 y llego a #6
```

### El resto de las comprobaciones

| qué | invariante |
|---|---|
| todas las hojas en el nivel `niveles()` | I1 |
| claves estrictamente crecientes (hojas e internos) | I1 / I2 |
| ocupación: hoja no raíz entre `⌈L/2⌉` y `L`; interno no raíz entre `⌈M/2⌉` y `M` hijos; raíz interna entre 2 y `M` | I3 |
| un interno con `siguiente != −1` | I4 |
| total de claves contadas == la cabecera | consistencia |
| toda página `1..cantidadPaginas−1` alcanzada **exactamente una vez** | I5 |

El "exactamente una vez" se lleva con un `boolean[] alcanzada`: si una página ya
estaba marcada hay un ciclo o un hijo compartido; si al final quedó alguna sin
marcar, es una página huérfana — justo lo que dejaba el
`UnsupportedOperationException` del paso 4.

`⌈L/2⌉` se escribe `(L + 1) / 2` con división entera, el mismo truco que
`izquierda` en los splits.

## `estadisticas()`

Recorre el árbol contando y acumulando:

```
ocupacionHojas    = Σ n        / (hojas    · L)
ocupacionInternos = Σ (n + 1)  / (internos · M)
```

En las hojas se mide en **pares** y en los internos en **hijos**, que es la
unidad de capacidad de cada uno.

Sobre el árbol final de la traza da:

```
hojas=6  internos=3  ocupacionHojas=0.722  ocupacionInternos=0.667
```

Las 13 claves en 6 hojas de capacidad 3 dan `13/18 = 0.72`. Los internos dan
`8/12 = 0.67` y no `9/12`, porque la raíz `#8` tiene **solo 2 hijos**: una raíz
interna puede tener desde 2 (I3), y en un árbol chico eso pesa mucho en el
promedio. Con un millón de claves el efecto desaparece.

La división es `double`, pero los acumuladores son `long` para que la suma no
pierda precisión antes de dividir.
