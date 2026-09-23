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
 *   Ejercicio 2 - TestIndiceDoble: clase de prueba del indice doble.
 *   Reproduce la traza obligatoria (Fases A a D) imprimiendo en cada fase
 *   el dump() de la tabla, el toString() del arbol, ambos tamanhos y
 *   ambos contadores, y genera las dos tablas del experimento: indice
 *   doble con rehash, y tabla sola con m = 97 fijo sin rehash.
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

public class TestIndiceDoble {

    private static final int[] CLAVES_TRAZA = {50, 30, 70, 20, 40, 60, 80, 35, 65};

    private static int fallas = 0;

    public static void main(String[] args) throws ClaveInexistenteException {
        IndiceDoble<Integer, String> indice = new IndiceDoble<>();

        faseA(indice);
        faseB(indice);
        faseC(indice);
        faseD(indice);
        experimentoIndiceDoble();
        experimentoTablaSola();

        System.out.println();
        System.out.println(fallas == 0 ? "RESULTADO: todos los chequeos pasaron." : "RESULTADO: " + fallas + " chequeo(s) fallaron.");
    }

    private static void faseA(IndiceDoble<Integer, String> indice) {
        System.out.println("=====================================================================");
        System.out.println("FASE A - agregar via IndiceDoble las nueve claves de la traza del Ej. 1");
        System.out.println("=====================================================================");

        for (int clave : CLAVES_TRAZA) {
            indice.agregar(clave, "P" + clave);
        }

        imprimirEstado(indice);
        System.out.println("cubetas ocupadas esperadas: [ 2] 35  [ 3] 80  [ 4] 70  [ 5] 60  [ 6] 50  [ 7] 40  [ 8] 30  [ 9] 20  [10] 65");
        fila("size() arbol == size() tabla == 9", indice.arbol().size() + "/" + indice.tabla().size(),
                indice.arbol().size() == 9 && indice.tabla().size() == 9);
        fila("capacidad() == 11 (sin rehash)", indice.tabla().capacidad(), indice.tabla().capacidad() == 11);
        System.out.println();
    }

    private static void faseB(IndiceDoble<Integer, String> indice) {
        System.out.println("=====================================================================");
        System.out.println("FASE B - agregar(61) y agregar(41): colisiones 61=50 (mod 11) y 41=30 (mod 11)");
        System.out.println("=====================================================================");

        indice.agregar(61, "P61");
        indice.agregar(41, "P41");

        imprimirEstado(indice);
        fila("n == 11 y m sigue en 11 (n > m es falso)", indice.tabla().size() + "/" + indice.tabla().capacidad(),
                indice.tabla().size() == 11 && indice.tabla().capacidad() == 11);
        fila("size() de arbol y tabla coinciden", indice.arbol().size() + "/" + indice.tabla().size(),
                indice.arbol().size() == indice.tabla().size());
        System.out.println();
    }

    private static void faseC(IndiceDoble<Integer, String> indice) throws ClaveInexistenteException {
        System.out.println("=====================================================================");
        System.out.println("FASE C - la regla del ejercicio: indice.obtener(70) NO incrementa arbol.visitas()");
        System.out.println("=====================================================================");

        indice.arbol().reiniciarVisitas();
        indice.tabla().reiniciarSondas();

        String resultado = indice.obtener(70);
        long visitasArbol = indice.arbol().visitas();
        long sondasTabla = indice.tabla().sondas();

        System.out.printf("%-34s %-12s %12s  %s%n", "que se mide", "valor", "esperado", "chequeo");
        fila2("indice.obtener(70)", resultado, "\"P70\"", "P70".equals(resultado));
        fila2("arbol.visitas()", visitasArbol, "0", visitasArbol == 0);
        fila2("tabla.sondas()", sondasTabla, ">= 1", sondasTabla >= 1);

        indice.arbol().reiniciarVisitas();
        String resultadoArbol = indice.arbol().obtener(70);
        long visitasArbolDirecto = indice.arbol().visitas();
        fila2("arbol.obtener(70) (mismo indice)", resultadoArbol, "\"P70\"", "P70".equals(resultadoArbol));
        fila2("arbol.visitas() tras ese obtener", visitasArbolDirecto, "2", visitasArbolDirecto == 2);
        System.out.println("(camino 50 -> 70: el indice doble no es un envoltorio cosmetico del arbol)");
        System.out.println();
    }

    private static void faseD(IndiceDoble<Integer, String> indice) throws ClaveInexistenteException {
        System.out.println("=====================================================================");
        System.out.println("FASE D - indice.eliminar(30): sale de las DOS estructuras");
        System.out.println("=====================================================================");

        String valorEliminado = indice.eliminar(30);

        imprimirEstado(indice);

        String esperado = "20(1) 35(4) 40(2) 41(1) 50(10) 60(3) 61(1) 65(2) 70(5) 80(1)";
        System.out.printf("%-34s %-12s %12s  %s%n", "que se mide", "valor", "esperado", "chequeo");
        fila2("eliminar(30) retorno", valorEliminado, "\"P30\"", "P30".equals(valorEliminado));
        fila2("toString() del arbol", "(arriba)", "traza", esperado.equals(indice.arbol().toString()));
        fila2("indice.contiene(30)", indice.contiene(30), "false", !indice.contiene(30));
        fila2("arbol.contiene(30)", indice.arbol().contiene(30), "false", !indice.arbol().contiene(30));
        fila2("kEsimo(2) (el nodo que se mudo)", indice.kEsimo(2), "35", indice.kEsimo(2) == 35);
        fila2("indice.obtener(35)", indice.obtener(35), "\"P35\"", "P35".equals(indice.obtener(35)));
        fila2("size() arbol / tabla", indice.arbol().size() + "/" + indice.tabla().size(), "10/10",
                indice.arbol().size() == 10 && indice.tabla().size() == 10);
        if (!esperado.equals(indice.arbol().toString())) {
            System.out.println("   esperado: " + esperado);
        }
        System.out.println("(la cubeta 8 se queda solo con 41)");
        System.out.println();
    }

    private static void experimentoIndiceDoble() throws ClaveInexistenteException {
        System.out.println("=====================================================================");
        System.out.println("EXPERIMENTO 1 - indice doble con rehash (permutacion semilla 2026)");
        System.out.println("=====================================================================");
        System.out.printf("%6s %15s %18s %10s %8s%n", "N", "vis_ABB_get", "sondas_hash_get", "alfa", "m");

        int[] enes = {2000, 4000, 6000, 8000, 10000};

        for (int n : enes) {
            int[] permutacion = permutacionAleatoria(n);

            IndiceDoble<Integer, String> indice = new IndiceDoble<>();
            for (int i = 0; i < n; i++) {
                indice.agregar(permutacion[i], "P" + permutacion[i]);
            }

            indice.arbol().reiniciarVisitas();
            indice.tabla().reiniciarSondas();

            for (int i = 0; i < n; i++) {
                indice.arbol().obtener(permutacion[i]);
            }
            for (int i = 0; i < n; i++) {
                indice.obtener(permutacion[i]);
            }

            System.out.printf("%6d %15d %18d %10.4f %8d%n",
                    n, indice.arbol().visitas(), indice.tabla().sondas(),
                    indice.tabla().factorCarga(), indice.tabla().capacidad());
        }
        System.out.println("(con m >= N y claves 1..N el modulo casi no colisiona: una sonda por busqueda)");
        System.out.println();
    }

    private static void experimentoTablaSola() {
        System.out.println("=====================================================================");
        System.out.println("EXPERIMENTO 2 - tabla sola, m = 97 fijo, sin rehash");
        System.out.println("=====================================================================");
        System.out.printf("%6s %10s %12s%n", "N", "alfa", "sondas/N");

        int[] enes = {2000, 4000, 6000, 8000, 10000};

        for (int n : enes) {
            TablaEncadenada<Integer, Integer> tabla = new TablaEncadenada<>(97, Double.POSITIVE_INFINITY);
            for (int i = 1; i <= n; i++) {
                tabla.insertar(i, i);
            }

            tabla.reiniciarSondas();
            for (int i = 1; i <= n; i++) {
                tabla.obtener(i);
            }

            System.out.printf("%6d %10.4f %12.4f%n", n, tabla.factorCarga(), (double) tabla.sondas() / n);
        }
        System.out.println("(alfa = N/97 crece y sondas/N crece lineal con alfa: Theta(1 + alfa))");
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

    private static void imprimirEstado(IndiceDoble<Integer, String> indice) {
        System.out.println("dump() de la tabla:");
        System.out.print(indice.tabla().dump());
        System.out.println("toString() del arbol: " + indice.arbol().toString());
        System.out.printf("size() arbol = %d   size() tabla = %d   capacidad() = %d   factorCarga() = %.4f%n",
                indice.arbol().size(), indice.tabla().size(), indice.tabla().capacidad(), indice.tabla().factorCarga());
        System.out.printf("arbol.visitas() = %d   tabla.sondas() = %d%n",
                indice.arbol().visitas(), indice.tabla().sondas());
    }

    private static void fila(String que, Object valor, boolean ok) {
        System.out.printf("%-42s %-12s  %s%n", que, String.valueOf(valor), verificar(ok));
    }

    private static void fila2(String que, Object valor, String esperado, boolean ok) {
        System.out.printf("%-34s %-12s %12s  %s%n", que, String.valueOf(valor), esperado, verificar(ok));
    }

    private static String verificar(boolean ok) {
        if (!ok) fallas++;
        return ok ? "OK" : "FALLA";
    }
}
