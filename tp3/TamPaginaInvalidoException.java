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
 *   Ejercicio 1 - TamPaginaInvalidoException: excepcion NO CHEQUEADA. La
 *   lanza crear cuando el P pedido no admite el formato de pagina del
 *   enunciado: P < 40, o L < 2, o M < 3.
 *
 *   Por que no chequeada: el P es un argumento que elige el programador en
 *   el momento de crear el indice, es un dato constante del programa y no
 *   algo que llegue del disco o del usuario. Pedir P = 20 es un error de
 *   programacion, igual que pasar un indice negativo a un arreglo.
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

public class TamPaginaInvalidoException extends RuntimeException {

    public TamPaginaInvalidoException() {
        super();
    }

    public TamPaginaInvalidoException(String message) {
        super(message);
    }

}
