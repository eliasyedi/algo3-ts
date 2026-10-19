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
 *   Ejercicio 1 - EstadisticasBMas: contenedor de solo datos que devuelve
 *   estadisticas(). Campos publicos porque es una tupla de resultados, no
 *   un objeto con invariante que proteger. Ver docs/02-api-y-excepciones.md.
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

public class EstadisticasBMas {

    public int hojas;
    public int internos;
    public double ocupacionHojas;
    public double ocupacionInternos;

    @Override
    public String toString() {
        return "hojas=" + hojas
                + " internos=" + internos
                + " ocupacionHojas=" + ocupacionHojas
                + " ocupacionInternos=" + ocupacionInternos;
    }

}
