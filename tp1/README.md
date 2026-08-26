# TP1 — U1 (POO y TAD en Java) — BufferGap e HistorialEdicion

**Grupo:** `g_ts5` — Sección TS
**Integrantes:** Figueredo Pistilli, Aurelio (CIC 4.010.315) · Olmedo Echeverría, Elías Rubén (CIC 4.653.503)

## Decisiones de diseño

**Ubicación del hueco al duplicar.** Al agotarse el hueco (`inicioHueco == finHueco`) se duplica la capacidad y el espacio nuevo queda **en la posición del cursor**, no al final. Motivo: el caso típico es seguir escribiendo donde se está; si el hueco quedara al final, la inserción siguiente obligaría a mudar el hueco de nuevo hasta el cursor, pagando otro traslado completo. Ambas opciones cuestan lo mismo al duplicar; la diferencia está en la operación siguiente.

**El cursor es `inicioHueco`.** No hay campo `cursor` aparte: una sola fuente de verdad evita que las dos se desincronicen.

**Traducción de índice lógico a físico.** `get`/`set` resuelven con una comparación y una suma (`index < inicioHueco ? datos[index] : datos[index + (finHueco - inicioHueco)]`), sin recorrer el arreglo.

**Contador de desplazamientos.** Se incrementa solo cuando un elemento **cambia de celda física**: en `moverCursor` y al crecer. `insertar` y `borrar` no lo tocan, porque escribir en el hueco o retroceder `inicioHueco` no mueve a nadie.

**Excepciones.** `BufferVacioException` es **chequeada** (extiende `Exception`) porque borrar con el buffer vacío es una situación que quien llama puede prever y manejar razonablemente. `PosicionInvalidaException` es **no chequeada** (extiende `RuntimeException`) porque pedir un índice fuera de rango es un error de programación del que llama, no un estado a manejar en tiempo de ejecución.

**Estado mínimo por comando (Ej. 2).** `ComandoInsertar` guarda el carácter insertado (deshacer = borrar). `ComandoBorrar` guarda el carácter que borró, obtenido del retorno de `borrar()`; sin eso el dato se pierde y no hay forma de reponerlo. `ComandoMoverCursor` guarda el **delta**, no la posición previa: deshacer es aplicar `-delta`, lo que lo hace independiente de dónde esté el cursor.

**Invalidación de rehacer.** `ejecutar` descarta la pila de rehacer: al escribir algo nuevo después de deshacer, la historia se bifurca y el futuro deshecho ya no es alcanzable.

**Estructuras propias.** No se usa ninguna estructura de `java.util`: los únicos tipos importados son `Iterator` (más `Iterable`, del lenguaje) y `Random` en la clase de prueba. La pila es `PilaES<E>`, enlazada, con `Nodo` como *inner class* privada. El crecimiento copia celda por celda (sin `System.arraycopy`) justamente para que el contador vea cada movimiento.

## Tabla de desplazamientos (Ejercicio 1, punto 3)

Cursor en n/2, contador reiniciado, luego 10.000 inserciones:

| n | BufferGap | Arreglo simple (BufferIngenuo) |
|---|---|---|
| 100.000 | 0 | 500.000.000 |
| 200.000 | 0 | 1.000.000.000 |
| 300.000 | 0 | 1.500.000.000 |
| 400.000 | 0 | 2.000.000.000 |
| 500.000 | 0 | 2.500.000.000 |
| 600.000 | 0 | 3.000.000.000 |
| 700.000 | 0 | 3.500.000.000 |
| 800.000 | 0 | 4.000.000.000 |
| 900.000 | 0 | 4.500.000.000 |
| 1.000.000 | 0 | 5.000.000.000 |

**Lectura.** `BufferGap` da **0 en las diez filas**, sin importar cuánto crezca n: insertar escribe en una celda del hueco y avanza `inicioHueco`, sin tocar ninguna otra celda. El arreglo simple crece linealmente con n, porque cada una de las 10.000 inserciones corre a la derecha los ~n/2 elementos que están después del cursor: 10.000 × n/2 es exactamente la segunda columna (500.000.000 para n = 100.000, y así).

## Cómo ejecutar

Requiere JDK 11+. Todas las clases están en el paquete por defecto.

Desde esta carpeta:

```bash
javac -d out *.java         # compilar
java -cp out TestBufferGap  # Ejercicio 1: traza, 100k aleatorios, tabla de desplazamientos
java -cp out TestHistorial  # Ejercicio 2: traza obligatoria de deshacer/rehacer
```

Windows (PowerShell): `javac -d out (Get-ChildItem *.java)`
