# TP3 — U4: Árbol B+ persistente en disco

Acá vive el *cómo* y el *por qué* de cada algoritmo. El código de `tp3/` no
lleva comentarios salvo en `ArbolBMas` y `ArchivoDatos`: el resto se lee de acá.

## Documentos

| doc | cubre |
|---|---|
| [01-formato-fisico.md](01-formato-fisico.md) | páginas, cálculo de L y M, offsets, big-endian |
| [02-api-y-excepciones.md](02-api-y-excepciones.md) | `crear`/`abrir`/`close`, contadores, las 3 excepciones |
| [03-codec-y-volcado.md](03-codec-y-volcado.md) | accesores de hoja e interno, `volcarPagina`, leer sin contar |
| [04-busqueda.md](04-busqueda.md) | regla de descenso, binaria propia, `buscar`, `caminoBusqueda`, `volcarArbol` |
| [05-insercion-split-de-hoja.md](05-insercion-split-de-hoja.md) | camino, split de hoja, cadena, separador, raíz nueva |
| [06-split-de-interno.md](06-split-de-interno.md) | la clave del medio sube y desaparece, propagación, `2s + 1` |
| [07-rango-invariantes-estadisticas.md](07-rango-invariantes-estadisticas.md) | cadena de hojas, regla de parada, I1–I5, ocupaciones |
| [08-archivo-datos.md](08-archivo-datos.md) | `datos.dat`, registro de longitud variable, UTF-8, búsqueda secuencial |
| [09-bmasapp.md](09-bmasapp.md) | comandos, construcción, Fisher-Yates, ocupación 50 % vs 65 % |
| [10-experimento.md](10-experimento.md) | las cuatro tablas, con los números medidos y su explicación |

## Estado del Ejercicio 1

| paso | qué | estado |
|---|---|---|
| 1 | L y M desde P, cabecera, `crear`/`abrir`/`close`, `leerPagina`/`escribirPagina` | listo |
| 2 | codificar/decodificar hoja e interno, `volcarPagina` | listo |
| 3 | `buscar`, `caminoBusqueda` (búsqueda binaria propia), `volcarArbol` | listo |
| 4 | `insertar` con split de hoja | listo |
| 5 | split de interno y raíz nueva | listo |
| 6 | `buscarRango`, `verificarInvariantes`, `estadisticas` | listo |
| 7 | `TestArbolBMas` con los 12 casos y la traza impresa | listo (152 casos) |

**Ejercicio 1 y Ejercicio 2 completos.** `TestArbolBMas` da 152 OK y
`TestArchivoDatos` 30 OK, las cuatro tablas las imprime `ExperimentoBMas`.

El entregable `tp3/README.md` está escrito: decisiones de diseño, el análisis de
los 6 puntos, las cuatro tablas y la sección «Uso de IA».

**Estos documentos quedan AFUERA del zip de la entrega**, por decisión del grupo.
El zip lleva solo los 11 `.java` y el `README.md`. Estos docs son para estudiar
para la defensa.

`ArbolBMas` y `ArchivoDatos` llevan comentarios en el cuerpo; el resto de las
clases no.

## Estado del Ejercicio 2

| parte | estado |
|---|---|
| `ArchivoDatos` + `TestArchivoDatos` | listo (30 casos) |
| `BMasApp` | listo |
| `ExperimentoBMas` (tablas A, B, C, D) | listo |

El dataset está bajado en `dataset/` (ignorado por git, no se entrega).
Índice construido con las primeras 300 000 filas y `P = 4096`: `datos.dat` de
27,1 MB, `indice.dat` de 9,7 MB, 3 niveles, `verificar` → `OK`.

Falta también el `README.md` de la entrega, que es un entregable aparte: las
cuatro tablas, el análisis pedido y la sección «Uso de IA». Estos docs son la
materia prima, no lo reemplazan.

## Los 12 casos del enunciado

| # | caso | dónde |
|---|---|---|
| 1 | árbol inicialmente vacío | `caso1ArbolVacio` |
| 2 | inserciones sin división (50, 20, 80) | `caso2SinDivision` |
| 3 | división de hoja: raíz hoja (10) y hoja intermedia (40) | `caso3DivisionDeHoja` |
| 4 | división de un nodo interno (45) | `caso4DivisionDeInterno` |
| 5 | división de la raíz y raíz nueva (45) | `caso5DivisionDeRaiz` |
| 6 | crecimiento a tres niveles | `caso6TresNiveles` |
| 7 | primera y última clave, `caminoBusqueda` | `caso7PrimeraYUltima` |
| 8 | clave inexistente | `caso8Inexistente` |
| 9 | rango dentro de una hoja | `caso9RangoEnUnaHoja` |
| 10 | rango por varias hojas, regla de parada | `caso10RangoVariasHojas` |
| 11 | cerrar y reabrir, archivo inválido | `caso11CerrarYReabrir` |
| 12 | repetir búsquedas después de reabrir | `caso12BusquedasDespuesDeReabrir` |

El caso 13 (dataset de 20 MB) es el Ejercicio 2.

## Correr

```bash
javac -d out tp3/*.java && java -cp out TestArbolBMas
javac -d out tp3/*.java && java -cp out TestArchivoDatos
```
