/*
 * =====================================================================
 * Algoritmos y Estructura de Datos III - Anho 2026, 2do Periodo
 * Trabajo Practico 1 - U1 (POO y TAD en Java)
 *
 * Grupo: g_ts5                                          Seccion: TS
 *
 * Integrantes:
 *   - Figueredo Pistilli, Aurelio        - CIC: 4.010.315 - Seccion: TS
 *   - Olmedo Echeverria, Elias Ruben     - CIC: 4.653.503 - Seccion: TS
 *
 * Tarea:
 *   Ejercicio 1 - Clase de prueba: reproduce la traza del enunciado, verifica 100.000 caracteres aleatorios con for-each y genera la tabla de desplazamientos.
 *
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

import java.util.Random;

public class TestBufferGap {

    //1
    public static void testTablas() throws BufferVacioException {

        BufferGap<Character> bufferGap = new BufferGap<>();

        imprimirEncabezado();
        imprimirEstado("(inicial)", bufferGap);

        bufferGap.insertar('H');
        imprimirEstado("insertar('H')", bufferGap);
        bufferGap.insertar('O');
        imprimirEstado("insertar('O')", bufferGap);
        bufferGap.insertar('L');
        imprimirEstado("insertar('L')", bufferGap);
        bufferGap.insertar('A');
        imprimirEstado("insertar('A')", bufferGap);

        bufferGap.moverCursor(-2);
        imprimirEstado("moverCursor(-2)", bufferGap);

        bufferGap.insertar('X');
        imprimirEstado("insertar('X')", bufferGap);
        bufferGap.get(4);
        imprimirEstado("get(4)", bufferGap);
        bufferGap.borrar();
        imprimirEstado("borrar()", bufferGap);

    }

    private static void imprimirEncabezado() {
        System.out.printf("%-16s | %-12s | %-11s | %-8s | %-9s | %s%n",
                "Operación", "Contenido", "inicioHueco", "finHueco", "capacidad", "desplazamientos");
        System.out.println("-----------------------------------------------------------------------------------------");
    }

    private static void imprimirEstado(String operacion, BufferGap<Character> b) {
        int inicioHueco = b.posicionCursor();
        int finHueco = inicioHueco + b.capacidad() - b.size();
        System.out.printf("%-16s | %-12s | %-11d | %-8d | %-9d | %d%n",
                operacion, b.toString(), inicioHueco, finHueco, b.capacidad(), b.desplazamientos());
    }

    //2
    public static void testRamdomCharacters() {

        BufferGap<Character> buffer = new BufferGap<>();
        Random random = new Random();
        Character[] chars = new Character[100000];

        for (int i = 0; i < 100000; i++) {
            chars[i] = (char) ('a' + random.nextInt(26));
        }

        for (Character c : chars) {
            buffer.insertar(c);
        }

        //assertions
//        assert chars.length == buffer.size();
        System.out.println("longitud the input -> " + chars.length + " longitud de bufferGap -> " + buffer.size());

        int i = 0;
        for (Character c : chars) {
//            assert chars[i] == c;
            if (chars[i] != c) {
                System.out.println(chars[i] + " != " + c);
            }
            i++;
        }


    }

    public static void testDesplazamientos() {
        System.out.println("\n--- Conteo de desplazamientos en el medio ---");

        final int CANTIDAD_INSERTAR = 10000;

        for (int n = 100000; n <= 1000000; n += 100000) {

            BufferGap<Character> buffer = new BufferGap<>();

            for (int i = 0; i < n; i++) {
                buffer.insertar((char) ('a' + (i % 26)));
            }

            buffer.moverCursor(-(n / 2));

            buffer.reiniciarDesplazamientos();

            for (int i = 0; i < CANTIDAD_INSERTAR; i++) {
                buffer.insertar((char) ('a' + (i % 26)));
            }

            System.out.println(
                    "n = " + n +
                            " | BufferGap desplazamientos = " +
                            buffer.desplazamientos()
            );
        }
    }





    public static void testDesplazamientoIngenuo(){
        final int CANTIDAD_INSERTAR = 10000;

        System.out.println("\n--- Comparación de desplazamientos ---");

        for (int n = 100000; n <= 1000000; n += 100000) {

            // =========================
            // BufferGap
            // =========================

            BufferGap<Character> bufferGap = new BufferGap<>();

            for (int i = 0; i < n; i++) {
                bufferGap.insertar((char) ('a' + (i % 26)));
            }

            bufferGap.moverCursor(-(n / 2));
            bufferGap.reiniciarDesplazamientos();

            for (int i = 0; i < CANTIDAD_INSERTAR; i++) {
                bufferGap.insertar((char) ('a' + (i % 26)));
            }

            long desplazamientosGap = bufferGap.desplazamientos();

            BufferIngenuo<Character> ingenuo = new BufferIngenuo<>();

            for (int i = 0; i < n; i++) {
                ingenuo.insertar((char) ('a' + (i % 26)));
            }

            ingenuo.moverCursor(-(n / 2));
            ingenuo.reiniciarDesplazamientos();

            for (int i = 0; i < CANTIDAD_INSERTAR; i++) {
                ingenuo.insertar((char) ('a' + (i % 26)));
            }

            long desplazamientosIngenuo = ingenuo.desplazamientos();

            System.out.println(
                    "n = " + n + "\t" +
                           "BufferGap = "+ desplazamientosGap + "\t\t" +
                            "BufferIngenuo = "+  desplazamientosIngenuo
            );
        }
    }


    public static void main(String[] args) throws BufferVacioException {
        testRamdomCharacters();

        testTablas();

        testDesplazamientos();

        testDesplazamientoIngenuo();

    }


}