/*
 * =====================================================================
 * Algoritmos y Estructura de Datos III - Anho 2026, 2do Periodo
 * Trabajo Practico 2 - U2–U3 (Análisis, ABB y Tablas de Dispersión)
 *
 * Grupo: g_ts5                                          Seccion: TS
 *
 * Integrantes:
 *   - Figueredo Pistilli, Aurelio        - CIC: 4.010.315 - Seccion: TS
 *   - Olmedo Echeverria, Elias Ruben     - CIC: 4.653.503 - Seccion: TS
 *
 * Tarea:
 *   Ejercicio 1 - TestABBAumentado: clase de prueba del ABBAumentado.
 *   Reproduce la traza del enunciado (Fase A construccion, Fase B
 *   consultas con el contador de visitas, Fase C eliminar(30)), recorre
 *   el arbol con for-each y genera la tabla de visitas del experimento
 *   para N en {2000, 4000, 6000, 8000, 10000}.
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

public class TestABBAumentado {

    private static final int[] CLAVES_TRAZA = {50, 30, 70, 20, 40, 60, 80, 35, 65};

    private static final String[] TOSTRING_ESPERADO = {
            "50(1)",
            "30(1) 50(2)",
            "30(1) 50(3) 70(1)",
            "20(1) 30(2) 50(4) 70(1)",
            "20(1) 30(3) 40(1) 50(5) 70(1)",
            "20(1) 30(3) 40(1) 50(6) 60(1) 70(2)",
            "20(1) 30(3) 40(1) 50(7) 60(1) 70(3) 80(1)",
            "20(1) 30(4) 35(1) 40(2) 50(8) 60(1) 70(3) 80(1)",
            "20(1) 30(4) 35(1) 40(2) 50(9) 60(2) 65(1) 70(4) 80(1)"
    };

    private static final int[] ALTURA_ESPERADA = {0, 1, 1, 2, 2, 2, 2, 3, 3};

    private static int fallas = 0;

    public static void main(String[] args) throws ClaveInexistenteException {
        ABBAumentado<Integer, String> arbol = new ABBAumentado<>();

        faseA(arbol);
        faseB(arbol);
        recorridoForEach(arbol);
        faseC(arbol);
        tablaDeVisitas();

        System.out.println();
        System.out.println(fallas == 0 ? "RESULTADO: todos los chequeos pasaron." : "RESULTADO: " + fallas + " chequeo(s) fallaron.");
    }

    private static void faseA(ABBAumentado<Integer, String> arbol) {
        System.out.println("=====================================================================");
        System.out.println("FASE A - construccion: agregar 50, 30, 70, 20, 40, 60, 80, 35, 65");
        System.out.println("=====================================================================");
        System.out.printf("%-14s %-50s %7s %6s %13s  %s%n", "operacion", "toString()", "altura", "size", "consistente", "chequeo");

        System.out.printf("%-14s %-50s %7d %6d %13s  %s%n", "(inicial)", "", arbol.altura(), arbol.size(), arbol.tamanosConsistentes(), verificar(arbol.altura() == -1 && arbol.size() == 0 && arbol.toString().isEmpty()));

        for (int i = 0; i < CLAVES_TRAZA.length; i++) {
            int clave = CLAVES_TRAZA[i];
            arbol.agregar(clave, "P" + clave);
            imprimirEstado("agregar(" + clave + ")", arbol, TOSTRING_ESPERADO[i], ALTURA_ESPERADA[i]);
        }
        System.out.println();
    }

    private static void faseB(ABBAumentado<Integer, String> arbol) throws ClaveInexistenteException {
        System.out.println("=====================================================================");
        System.out.println("FASE B - consultas sobre ese arbol (reiniciarVisitas() antes de cada fila)");
        System.out.println("=====================================================================");
        System.out.printf("%-30s %-10s %10s %12s  %s%n", "operacion", "resultado", "visitas", "esperado", "chequeo");

        arbol.reiniciarVisitas();
        Integer kEsimo6 = arbol.kEsimo(6);
        long visKEsimo6 = arbol.visitas();
        fila("kEsimo(6)", kEsimo6, visKEsimo6, "3", kEsimo6 == 60 && visKEsimo6 == 3);

        arbol.reiniciarVisitas();
        int menores65 = arbol.cuantosMenores(65);
        long visMenores65 = arbol.visitas();
        fila("cuantosMenores(65)", menores65, visMenores65, "4", menores65 == 6 && visMenores65 == 4);

        arbol.reiniciarVisitas();
        int menores35 = arbol.cuantosMenores(35);
        long visMenores35 = arbol.visitas();
        fila("cuantosMenores(35)", menores35, visMenores35, "4", menores35 == 2 && visMenores35 == 4);

        arbol.reiniciarVisitas();
        int rangoAumentado = arbol.consultarRango(35, 65);
        long visRangoAumentado = arbol.visitas();
        fila("consultarRango(35,65)", rangoAumentado, visRangoAumentado, "O(h)", rangoAumentado == 5);

        arbol.reiniciarVisitas();
        int rangoIngenuo = arbol.consultarRangoIngenuo(35, 65);
        long visRangoIngenuo = arbol.visitas();
        fila("consultarRangoIngenuo(35,65)", rangoIngenuo, visRangoIngenuo, "9", rangoIngenuo == 5 && visRangoIngenuo == 9);

        arbol.reiniciarVisitas();
        Integer sucesor40 = arbol.sucesor(40);
        fila("sucesor(40)", sucesor40, arbol.visitas(), "-", sucesor40 != null && sucesor40 == 50);

        arbol.reiniciarVisitas();
        Integer sucesor80 = arbol.sucesor(80);
        fila("sucesor(80)", sucesor80, arbol.visitas(), "-", sucesor80 == null);

        arbol.reiniciarVisitas();
        Integer predecesor35 = arbol.predecesor(35);
        fila("predecesor(35)", predecesor35, arbol.visitas(), "-", predecesor35 != null && predecesor35 == 30);

        arbol.reiniciarVisitas();
        int rango50 = arbol.rango(50);
        fila("rango(50)", rango50, arbol.visitas(), "-", rango50 == 5);

        System.out.println();
    }

    private static void recorridoForEach(ABBAumentado<Integer, String> arbol) {
        System.out.println("=====================================================================");
        System.out.println("RECORRIDO for-each (inorden)");
        System.out.println("=====================================================================");

        StringBuilder claves = new StringBuilder();
        boolean enOrden = true;
        Integer anterior = null;
        int cantidad = 0;
        for (Integer clave : arbol) {
            if (anterior != null && anterior.compareTo(clave) >= 0) enOrden = false;
            anterior = clave;
            cantidad++;
            if (claves.length() > 0) claves.append(" ");
            claves.append(clave);
        }
        System.out.println("claves   : " + claves);
        System.out.println("en orden : " + enOrden + "   " + verificar(enOrden && cantidad == arbol.size()));
        System.out.println();
    }

    private static void faseC(ABBAumentado<Integer, String> arbol) throws ClaveInexistenteException {
        System.out.println("=====================================================================");
        System.out.println("FASE C - eliminar(30): dos hijos, el sucesor 35 SE MUDA (no se copia la clave)");
        System.out.println("=====================================================================");
        System.out.printf("%-14s %-50s %7s %6s %13s  %s%n", "operacion", "toString()", "altura", "size", "consistente", "chequeo");

        String valorEliminado = arbol.eliminar(30);
        imprimirEstado("eliminar(30)", arbol, "20(1) 35(3) 40(1) 50(8) 60(2) 65(1) 70(4) 80(1)", 3);

        System.out.println();
        System.out.printf("%-30s %-10s  %s%n", "verificacion posterior", "resultado", "chequeo");
        fila("eliminar(30) retorno", valorEliminado, "P30", "P30".equals(valorEliminado));
        fila("kEsimo(2)", arbol.kEsimo(2), "35", arbol.kEsimo(2) == 35);
        fila("consultarRango(35,65)", arbol.consultarRango(35, 65), "5", arbol.consultarRango(35, 65) == 5);
        fila("sucesor(20)", arbol.sucesor(20), "35", arbol.sucesor(20) == 35);
        fila("contiene(30)", arbol.contiene(30), "false", !arbol.contiene(30));
        fila("tamanosConsistentes()", arbol.tamanosConsistentes(), "true", arbol.tamanosConsistentes());

        StringBuilder inorden = new StringBuilder();
        for (Integer clave : arbol) {
            if (inorden.length() > 0) inorden.append(" ");
            inorden.append(clave);
        }
        fila("for-each", inorden.toString(), "20 35 40 50 60 65 70 80", "20 35 40 50 60 65 70 80".contentEquals(inorden));
        System.out.println();
    }

    /*
     * TABLA DE VISITAS - lectura de las tendencias (desarrollado en el README.md).
     *
     * h_ord = N-1: insertar 1,2,...,N en ese orden hace que cada clave nueva sea
     * mayor que todas las anteriores, asi que siempre baja por la derecha. El ABB
     * degenera en una lista: un solo camino de N nodos, altura N-1. El campo tamano
     * sigue siendo correcto, pero h ya no es log N y O(h) deja de ser barato.
     *
     * vis_kEsimo_ord = N/2: en esa lista cada nodo tiene tamano(izq) = 0, asi que
     * kEsimo siempre cae en el caso k > l+1 y baja a la derecha restando 1 a k.
     * Para llegar al k-esimo visita exactamente k nodos; con k = N/2 son N/2 visitas.
     * kEsimo sigue siendo O(h), lo que se rompio es h, no el algoritmo.
     *
     * vis_rango_aum en decenas vs vis_rango_ing = N: consultarRango aumentado hace
     * dos cuantosMenores y un contiene, o sea tres descensos de a lo sumo h pasos
     * cada uno: Theta(h). En el arbol aleatorio h ~ 2*log2(N), asi que 3*h queda en
     * unas decenas aun con N = 10000. El ingenuo recorre el inorden completo y toca
     * los N nodos siempre, sin importar la forma del arbol ni el ancho del intervalo:
     * es Theta(N) en todo caso. Esa es exactamente la diferencia que paga el campo
     * tamano.
     *
     * Por que en el arbol chico de la traza el aumentado visita MAS que el ingenuo:
     * con n = 9 el ingenuo gasta 9 visitas y el aumentado gasta 3*h mas el costo de
     * contiene, que en un arbol de altura 3 ya ronda o supera 9. Las constantes
     * pesan mas que el orden cuando n es chico. No hay contradiccion: Theta(h) vs
     * Theta(n) es una afirmacion sobre el crecimiento, no sobre un n fijo. El cruce
     * se ve recien cuando n >> h, y por eso el experimento se corre con N de miles,
     * donde el aumentado queda en decenas y el ingenuo en miles.
     */
    private static void tablaDeVisitas() {
        System.out.println("=====================================================================");
        System.out.println("TABLA DE VISITAS - arbol aleatorio (Fisher-Yates, semilla 2026) vs ordenado");
        System.out.println("=====================================================================");
        System.out.printf("%6s %9s %9s %18s %16s %15s %15s%n",
                "N", "h_aleat", "h_ord", "vis_kEsimo_aleat", "vis_kEsimo_ord", "vis_rango_aum", "vis_rango_ing");

        int[] enes = {2000, 4000, 6000, 8000, 10000};

        for (int n : enes) {
            int[] permutacion = permutacionAleatoria(n);

            ABBAumentado<Integer, Integer> aleatorio = new ABBAumentado<>();
            for (int i = 0; i < n; i++) {
                aleatorio.agregar(permutacion[i], permutacion[i]);
            }

            ABBAumentado<Integer, Integer> ordenado = new ABBAumentado<>();
            for (int i = 1; i <= n; i++) {
                ordenado.agregar(i, i);
            }

            int hAleatorio = aleatorio.altura();
            int hOrdenado = ordenado.altura();

            aleatorio.reiniciarVisitas();
            aleatorio.kEsimo(n / 2);
            long visKEsimoAleatorio = aleatorio.visitas();

            ordenado.reiniciarVisitas();
            ordenado.kEsimo(n / 2);
            long visKEsimoOrdenado = ordenado.visitas();

            Integer a = aleatorio.kEsimo(n / 4);
            Integer b = aleatorio.kEsimo(3 * n / 4);

            aleatorio.reiniciarVisitas();
            aleatorio.consultarRango(a, b);
            long visRangoAumentado = aleatorio.visitas();

            aleatorio.reiniciarVisitas();
            aleatorio.consultarRangoIngenuo(a, b);
            long visRangoIngenuo = aleatorio.visitas();

            System.out.printf("%6d %9d %9d %18d %16d %15d %15d%n",
                    n, hAleatorio, hOrdenado, visKEsimoAleatorio, visKEsimoOrdenado, visRangoAumentado, visRangoIngenuo);

            verificar(hOrdenado == n - 1);
            verificar(visKEsimoOrdenado == n / 2);
            verificar(visRangoIngenuo == n);
        }

        System.out.println("(columnas deterministas: h_ord = N-1, vis_kEsimo_ord = N/2, vis_rango_ing = N)");
        System.out.println();
    }

    private static int[] permutacionAleatoria(int n) {
        Random rng = new Random(2026);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i + 1;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int intercambio = a[i];
            a[i] = a[j];
            a[j] = intercambio;
        }
        return a;
    }

    private static void imprimirEstado(String operacion, ABBAumentado<Integer, String> arbol,
                                       String toStringEsperado, int alturaEsperada) {
        String obtenido = arbol.toString();
        boolean ok = obtenido.equals(toStringEsperado)
                && arbol.altura() == alturaEsperada
                && arbol.tamanosConsistentes();
        System.out.printf("%-14s %-50s %7d %6d %13s  %s%n",
                operacion, obtenido, arbol.altura(), arbol.size(), arbol.tamanosConsistentes(), verificar(ok));
        if (!ok) {
            System.out.println("               esperado: " + toStringEsperado + " / altura " + alturaEsperada);
        }
    }

    private static void fila(String operacion, Object resultado, long visitas, String esperado, boolean ok) {
        System.out.printf("%-30s %-10s %10d %12s  %s%n", operacion, String.valueOf(resultado), visitas, esperado, verificar(ok));
    }

    private static void fila(String operacion, Object resultado, String esperado, boolean ok) {
        System.out.printf("%-30s %-10s  %s%n", operacion, String.valueOf(resultado), verificar(ok) + "  (esperado: " + esperado + ")");
    }

    private static String verificar(boolean ok) {
        if (!ok) fallas++;
        return ok ? "OK" : "FALLA";
    }
}
