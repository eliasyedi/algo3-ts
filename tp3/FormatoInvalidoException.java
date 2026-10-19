/*
 * =====================================================================
 * Algoritmos y Estructura de Datos III - Anho 2026, 2do Periodo
 * Trabajo Practico 3 - U4 (Arboles B+ persistentes en disco)
 *
 * Grupo: g_ts5                                          Seccion: TS
 *
 * Integrantes:
 *   - Figueredo Pistilli, Aurelio        - CIC: 4.010.315 - Seccion: TS
 *   - Olmedo Echeverria, Elias Ruben     - CIC: 4.653.503 - Seccion: TS
 *
 * Tarea:
 *   Ejercicio 1 - FormatoInvalidoException: excepcion CHEQUEADA (extiende
 *   IOException). La lanza abrir cuando el archivo existe pero no es un
 *   indice valido: MAGIC o version distintos, M o L que no corresponden al
 *   P guardado, o largo del archivo != cantidadPaginas * P.
 *
 *   Por que chequeada: que un archivo de disco este corrupto, truncado o
 *   sea de otro programa es una condicion externa que el que llama no puede
 *   descartar antes de abrirlo, y de la que si puede recuperarse (avisar y
 *   reconstruir el indice). Por eso el compilador lo obliga a tratarla, y
 *   es un error de E/S: hereda de IOException como el resto de la familia.
 * ---------------------------------------------------------------------
 * DECLARACION DE HONOR
 *   Nosotros, Aurelio Figueredo Pistilli y Elias Ruben Olmedo Echeverria:
 *
 *   - No hemos discutido el codigo fuente de nuestra tarea con ningun otro
 *     grupo, solo con el Profesor o el AER.
 *   - No hemos usado codigo obtenido de otro estudiante o de cualquier otra
 *     fuente no autorizada, modificada o no modificada.
 *   - Cualquier codigo o documentacion utilizada en nuestro programa obtenido
 *     de fuentes, tales como libros o notas de curso, ha sido claramente
 *     indicada en nuestra tarea.
 * =====================================================================
 */

import java.io.IOException;

public class FormatoInvalidoException extends IOException {

    public FormatoInvalidoException() {
        super();
    }

    public FormatoInvalidoException(String message) {
        super(message);
    }

}
