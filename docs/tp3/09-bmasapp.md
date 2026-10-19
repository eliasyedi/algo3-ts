# 09 — `BMasApp`: la línea de comandos

## La regla que ordena todo

**Cada comando es una ejecución separada de la JVM.** `construir` arma
`datos.dat` e `indice.dat` desde el TSV, y **ningún otro comando vuelve a
leer el TSV**: todos trabajan solo con esos dos archivos.

Eso es lo que prueba que el árbol vive en disco. Si hiciera falta releer el
TSV o reconstruir algo al arrancar, el índice no sería persistente — sería una
estructura en RAM que se rearma cada vez, que es justo lo que el enunciado
califica con 0.

```bash
java -cp out BMasApp construir dataset/title.basics.tsv filas=300000 tamPagina=4096 [--aleatorio]
java -cp out BMasApp buscar tt0111161          # o el numero: 111161
java -cp out BMasApp rango 100000 100999 mostrar=10
java -cp out BMasApp secuencial 111161
java -cp out BMasApp info
java -cp out BMasApp verificar
java -cp out BMasApp pagina 345
```

## La clave

`tt0111161` → `111161`. `claveDeTconst` saca el prefijo `tt`, se come los
ceros a la izquierda y parsea el resto. Acepta las dos formas, con `tt` y sin
`tt`, porque la defensa puede pedir cualquiera de las dos.

Si el campo no es un `tconst` válido devuelve `-1` y `construir` saltea esa
fila, en vez de reventar en medio de 300 000 registros.

## `construir`: dos pasadas

**Pasada 1 — `datos.dat`.** Lee el TSV línea por línea con un
`BufferedReader` en UTF-8, saltea el encabezado, y por cada fila: saca la
clave del primer campo y agrega la línea entera a `datos.dat`. Va guardando
`claves[i]` y `posiciones[i]` en dos arreglos `long[]` paralelos.

**Pasada 2 — `indice.dat`.** Inserta los `⟨clave, posición⟩` en el árbol.

¿Por qué dos pasadas y no insertar mientras lee? Porque `--aleatorio` lo
necesita: `datos.dat` se escribe **siempre en el orden del archivo** (si no,
no sería el mismo archivo de datos), y lo único que cambia es el **orden en que
se insertan las claves en el índice**. Tener los dos arreglos permite barajar
entre una pasada y la otra. Dejamos las dos pasadas también para el caso
normal, así los dos caminos son el mismo código y la comparación es justa.

Los arreglos `long[]` en memoria son 2 × 300 000 × 8 bytes = 4,8 MB. No
violan nada: la restricción es sobre **el árbol**, que nunca está en RAM. El
enunciado pide explícitamente estos dos arreglos para `--aleatorio`.

### Fisher-Yates

```java
Random rng = new Random(2026);
for (int i = n - 1; i > 0; i--) {
    int j = rng.nextInt(i + 1);
    // intercambiar claves[i] con claves[j], y tambien posiciones[i] con posiciones[j]
}
```

Los dos arreglos se intercambian **juntos**: son paralelos, `posiciones[i]` es
la posición de `claves[i]`. Si barajáramos solo uno, cada clave apuntaría al
registro equivocado y el índice quedaría mintiendo sin que nada se queje.

La semilla fija (2026) es lo que hace el experimento **reproducible**: la misma
permutación en cualquier máquina, así los números de la Tabla B se pueden
comparar.

El recorrido va de atrás para adelante y sortea `j` en `[0, i]`, no en
`[0, n)`. Esa es la diferencia entre Fisher-Yates y el barajado ingenuo: el
ingenuo no produce todas las permutaciones con la misma probabilidad.

### Lo que imprime, y lo que muestra

Construcción con las primeras 300 000 filas y `P = 4096`:

| | orden del archivo | `--aleatorio` |
|---|---|---|
| `datos.dat` | 27 101 166 bytes | 27 101 166 bytes |
| `indice.dat` | 9 658 368 bytes | **7 434 240 bytes** |
| páginas | 2358 | **1815** |
| hojas / internos | 2343 / 14 | 1805 / 9 |
| ocupación hojas | **50,21 %** | **65,18 %** |
| ocupación internos | 49,35 % | 59,07 % |
| niveles | 3 | 3 |

`datos.dat` es idéntico, como tiene que ser: lo único que cambió es el orden de
inserción en el índice.

**Por qué la inserción en orden creciente deja las hojas al 50 %.** Si las
claves llegan ordenadas, siempre se inserta en la **última** hoja. Cuando se
llena, se parte en dos mitades: la izquierda queda con `⌈(L+1)/2⌉` pares y
**nunca más recibe nada**, porque todas las claves que vienen son mayores.
Queda congelada a la mitad. La derecha sigue recibiendo hasta que se llena y se
vuelve a partir. Resultado: todas las hojas menos la última quedan al 50 %.

Con orden aleatorio, en cambio, los splits caen en cualquier lado y las hojas
ya partidas siguen recibiendo claves, así que suben al ~65 %. Por eso el índice
aleatorio pesa 23 % menos.

## `buscar`

```
paginas leidas: 3 (#345 -> #861 -> #854)
pos=9842488  tt0111161	movie	The Shawshank Redemption	...
```

El `3` sale de `paginasLeidas()` después de `buscar`, con los contadores
reiniciados justo antes: es el costo real de la consulta, `niveles()` páginas.

El camino se obtiene con `caminoBusqueda`, que es un **segundo descenso** y por
eso no se suma al número informado: está solo para mostrar por dónde se bajó.
Es diagnóstico, igual que los volcados.

Una clave inexistente también informa 3 páginas y su camino: el costo no
depende de que la clave esté.

> El camino `#345 -> #861 -> #854` coincide **exactamente** con el del ejemplo
> del enunciado (página 13). La posición da 9842488 contra 9842010 porque el
> dataset de IMDb se actualizó desde que se escribió el enunciado y algunos
> títulos cambiaron de largo; los **números de página** no dependen del
> contenido, solo del algoritmo, y por eso dan iguales.

## `rango`

Informa cuántos registros entró y desglosa las páginas:

```
registros en el rango: 966
paginas leidas:        11  (2 del descenso + 9 hojas)
```

El desglose es `niveles − 1` del descenso más el resto, que son hojas de la
cadena. Es la fórmula del análisis: un rango cuesta el descenso **una sola
vez** más una página por cada hoja que toca.

`mostrar=N` limita cuántos registros se imprimen, no cuántos se recorren. La
lambda que recibe `buscarRango` guarda las primeras `N` posiciones en un
`long[]` y descarta el resto — no se arma ninguna lista, que es para lo que
existe `ConsumidorEntrada`.

## `secuencial`

El rival. Sobre el mismo archivo:

| consulta | costo |
|---|---|
| `buscar tt0111161` | 3 páginas de índice + 1 registro |
| `secuencial 111161` | **108 633 registros** (16,5 ms) |
| `secuencial 9999999` (inexistente) | **300 000 registros** (23,3 ms) |

Una clave inexistente obliga a recorrer el archivo entero. El B+ la descarta
leyendo las mismas 3 páginas que cuesta una que sí está.

## `info`, `verificar`, `pagina`

- `info` abre **solo la cabecera** del índice y suma `estadisticas()`. Es la
  prueba más directa de que reabrir no reconstruye nada.
- `verificar` imprime `verificarInvariantes()`. Sobre los dos índices
  construidos (en orden y aleatorio) da `OK`.
- `pagina N` imprime `volcarPagina(N)`. Sirve para que en la defensa se pueda
  abrir cualquier página y mirarla.
