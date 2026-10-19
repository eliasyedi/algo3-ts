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
 *   Ejercicio 2 - ExperimentoBMas: imprime las cuatro tablas del enunciado.
 *   A (estructura sintetica con 12 arboles), B (construccion con IMDb),
 *   C (busquedas: B+ contra secuencial) y D (rangos por ancho). Ninguna se
 *   transcribe a mano. Ver docs/tp3/10-experimento.md.
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

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class ExperimentoBMas {

    private static final long SEMILLA = 2026;
    private static final int[] TAM_PAGINAS = {128, 256, 512, 1024, 2048, 4096};

    private static final String INDICE = "indice.dat";
    private static final String DATOS = "datos.dat";
    private static final String INDICE_SINTETICO = "indice-sintetico.dat";

    private static final int CLAVES_SINTETICAS = 1000000;
    private static final int CONSULTAS = 1000;
    private static final int CONSULTAS_SECUENCIAL = 50;
    private static final int[] ANCHOS_RANGO = {10, 100, 1000, 10000};
    private static final int RANGOS_POR_ANCHO = 100;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("uso:");
            System.out.println("  sintetico [n=1000000]      tabla A");
            System.out.println("  imdb <tsv> [filas=300000]  tabla B");
            System.out.println("  busquedas                  tablas C y D");
            return;
        }

        String comando = args[0];
        if (comando.equals("sintetico")) {
            tablaA((int) valorEntero(args, "n", CLAVES_SINTETICAS));
        } else if (comando.equals("imdb")) {
            if (args.length < 2) {
                System.out.println("falta el TSV: imdb <tsv> [filas=300000]");
                return;
            }
            tablaB(args[1], (int) valorEntero(args, "filas", 300000));
        } else if (comando.equals("busquedas")) {
            busquedas();
        } else {
            System.out.println("comando desconocido: " + comando);
        }
    }

    private static void tablaA(int n) throws IOException {
        System.out.println("TABLA A - estructura, claves 1.." + n + ", posicion = clave");
        System.out.println();
        System.out.printf("%6s %5s %5s  %-10s %8s %9s %12s %14s %6s%n",
                "P", "M", "L", "orden", "niveles", "hojas", "ocupHojas", "ocupInternos", "inv");
        System.out.println("  ----------------------------------------"
                + "---------------------------------------");

        long[] creciente = new long[n];
        for (int i = 0; i < n; i++) {
            creciente[i] = i + 1;
        }
        long[] permutado = new long[n];
        for (int i = 0; i < n; i++) {
            permutado[i] = i + 1;
        }
        barajar(permutado, n);

        for (int p = 0; p < TAM_PAGINAS.length; p++) {
            filaTablaA(TAM_PAGINAS[p], "creciente", creciente, n);
            filaTablaA(TAM_PAGINAS[p], "permutado", permutado, n);
        }
        new File(INDICE_SINTETICO).delete();
    }

    private static void filaTablaA(int tamPagina, String orden, long[] claves, int n)
            throws IOException {
        int niveles;
        int hojas;
        double ocupacionHojas;
        double ocupacionInternos;
        int m;
        int l;
        String invariantes;

        try (ArbolBMas arbol = ArbolBMas.crear(INDICE_SINTETICO, tamPagina)) {
            for (int i = 0; i < n; i++) {
                arbol.insertar(claves[i], claves[i]);
            }
            EstadisticasBMas e = arbol.estadisticas();
            niveles = arbol.niveles();
            hojas = e.hojas;
            ocupacionHojas = e.ocupacionHojas;
            ocupacionInternos = e.ocupacionInternos;
            m = arbol.ordenInterno();
            l = arbol.capacidadHoja();
            invariantes = arbol.verificarInvariantes();
        }

        System.out.printf("%6d %5d %5d  %-10s %8d %9d %11.2f%% %13.2f%% %6s%n",
                tamPagina, m, l, orden, niveles, hojas,
                ocupacionHojas * 100, ocupacionInternos * 100,
                "OK".equals(invariantes) ? "OK" : invariantes);
    }

    private static void tablaB(String tsv, int filas) throws IOException {
        System.out.println("TABLA B - construccion con IMDb, primeras " + filas + " filas");
        System.out.println();

        BMasApp.Construccion enOrden =
                BMasApp.construirIndice(tsv, filas, 4096, false, INDICE, DATOS);
        BMasApp.Construccion aleatorio =
                BMasApp.construirIndice(tsv, filas, 4096, true, INDICE, DATOS);

        System.out.printf("%-22s %18s %18s%n", "", "orden del archivo", "--aleatorio");
        System.out.println("  ------------------------------------------------------------");
        filaB("registros", enOrden.registros, aleatorio.registros);
        filaB("datos.dat (bytes)", enOrden.bytesDatos, aleatorio.bytesDatos);
        filaB("indice.dat (bytes)", enOrden.bytesIndice, aleatorio.bytesIndice);
        filaB("P", enOrden.tamPagina, aleatorio.tamPagina);
        filaB("M", enOrden.ordenInterno, aleatorio.ordenInterno);
        filaB("L", enOrden.capacidadHoja, aleatorio.capacidadHoja);
        filaB("niveles", enOrden.niveles, aleatorio.niveles);
        filaB("paginas", enOrden.paginas, aleatorio.paginas);
        filaB("hojas", enOrden.hojas, aleatorio.hojas);
        filaB("internos", enOrden.internos, aleatorio.internos);
        filaTexto("ocupacion hojas", BMasApp.porcentaje(enOrden.ocupacionHojas),
                BMasApp.porcentaje(aleatorio.ocupacionHojas));
        filaTexto("ocupacion internos", BMasApp.porcentaje(enOrden.ocupacionInternos),
                BMasApp.porcentaje(aleatorio.ocupacionInternos));
        filaB("paginas escritas", enOrden.paginasEscritas, aleatorio.paginasEscritas);
        filaTexto("tiempo datos.dat (ms)", String.format("%.1f", enOrden.milisDatos),
                String.format("%.1f", aleatorio.milisDatos));
        filaTexto("tiempo indice.dat (ms)", String.format("%.1f", enOrden.milisIndice),
                String.format("%.1f", aleatorio.milisIndice));

        System.out.println();
        System.out.println("queda construido el indice aleatorio; para las tablas C y D hay que");
        System.out.println("reconstruir en orden del archivo con BMasApp construir");
    }

    private static void filaB(String nombre, long enOrden, long aleatorio) {
        System.out.printf("%-22s %18d %18d%n", nombre, enOrden, aleatorio);
    }

    private static void filaTexto(String nombre, String enOrden, String aleatorio) {
        System.out.printf("%-22s %18s %18s%n", nombre, enOrden, aleatorio);
    }

    private static void busquedas() throws IOException {
        try (ArbolBMas arbol = ArbolBMas.abrir(INDICE);
             ArchivoDatos datos = ArchivoDatos.abrir(DATOS)) {

            int n = (int) arbol.cantidadClaves();
            long[] claves = new long[n];
            long[] posiciones = new long[n];
            final int[] guardadas = new int[1];
            arbol.buscarRango(Long.MIN_VALUE, Long.MAX_VALUE, (clave, posicion) -> {
                claves[guardadas[0]] = clave;
                posiciones[guardadas[0]] = posicion;
                guardadas[0]++;
            });

            long minimo = claves[0];
            long maximo = claves[n - 1];
            Random rng = new Random(SEMILLA);

            long[] existentes = new long[CONSULTAS];
            for (int i = 0; i < CONSULTAS; i++) {
                existentes[i] = claves[rng.nextInt(n)];
            }
            long[] inexistentes = new long[CONSULTAS];
            int cuantas = 0;
            while (cuantas < CONSULTAS) {
                long x = minimo + (long) (rng.nextDouble() * (maximo - minimo + 1));
                if (!contiene(claves, n, x)) {
                    inexistentes[cuantas++] = x;
                }
            }

            tablaC(arbol, datos, existentes, inexistentes);
            tablaD(arbol, claves, n, rng);
        }
    }

    private static void tablaC(ArbolBMas arbol, ArchivoDatos datos,
                               long[] existentes, long[] inexistentes) throws IOException {
        System.out.println("TABLA C - busquedas (indice construido en orden del archivo)");
        System.out.println("(se corre una vuelta de calentamiento antes de medir, para que el JIT");
        System.out.println(" no le cobre a la primera fila lo que no es del algoritmo)");
        System.out.println();
        System.out.printf("%-42s %10s %12s %12s %12s%n",
                "metodo", "consultas", "pags/cons", "regs/cons", "us/cons");
        System.out.println("  -----------------------------------------------"
                + "-------------------------------------");

        for (int i = 0; i < existentes.length; i++) {
            long pos = arbol.buscar(existentes[i]);
            if (pos >= 0) {
                datos.leer(pos);
            }
            arbol.buscar(inexistentes[i]);
        }

        arbol.reiniciarContadores();
        datos.reiniciarContadores();
        long arranque = System.nanoTime();
        for (int i = 0; i < existentes.length; i++) {
            long pos = arbol.buscar(existentes[i]);
            if (pos >= 0) {
                datos.leer(pos);
            }
        }
        long fin = System.nanoTime();
        filaC("B+ con clave existente (con el registro)", existentes.length,
                arbol.paginasLeidas(), datos.registrosLeidos(), arranque, fin);

        arbol.reiniciarContadores();
        datos.reiniciarContadores();
        arranque = System.nanoTime();
        for (int i = 0; i < inexistentes.length; i++) {
            arbol.buscar(inexistentes[i]);
        }
        fin = System.nanoTime();
        filaC("B+ con clave inexistente", inexistentes.length,
                arbol.paginasLeidas(), datos.registrosLeidos(), arranque, fin);

        datos.reiniciarContadores();
        arranque = System.nanoTime();
        for (int i = 0; i < CONSULTAS_SECUENCIAL; i++) {
            datos.busquedaSecuencial(existentes[i]);
        }
        fin = System.nanoTime();
        filaC("secuencial con clave existente", CONSULTAS_SECUENCIAL,
                0, datos.registrosLeidos(), arranque, fin);

        datos.reiniciarContadores();
        arranque = System.nanoTime();
        for (int i = 0; i < CONSULTAS_SECUENCIAL; i++) {
            datos.busquedaSecuencial(inexistentes[i]);
        }
        fin = System.nanoTime();
        filaC("secuencial con clave inexistente", CONSULTAS_SECUENCIAL,
                0, datos.registrosLeidos(), arranque, fin);
        System.out.println();
    }

    private static void filaC(String metodo, int consultas, long paginas, long registros,
                              long arranque, long fin) {
        System.out.printf("%-42s %10d %12.2f %12.2f %12.1f%n",
                metodo, consultas,
                (double) paginas / consultas,
                (double) registros / consultas,
                (fin - arranque) / 1000.0 / consultas);
    }

    private static void tablaD(ArbolBMas arbol, long[] claves, int n, Random rng)
            throws IOException {
        System.out.println("TABLA D - rangos (" + RANGOS_POR_ANCHO + " por ancho)");
        System.out.println();
        System.out.printf("%8s %14s %14s %14s %12s%n",
                "w", "regs/cons", "pags/cons", "hojas/cons", "us/cons");
        System.out.println("  ---------------------------------------"
                + "----------------------------");

        final int[] entregados = new int[1];
        for (int a = 0; a < ANCHOS_RANGO.length; a++) {
            int w = ANCHOS_RANGO[a];
            entregados[0] = 0;
            arbol.reiniciarContadores();
            long arranque = System.nanoTime();
            for (int i = 0; i < RANGOS_POR_ANCHO; i++) {
                long x = claves[rng.nextInt(n)];
                entregados[0] += arbol.buscarRango(x, x + w - 1, (clave, posicion) -> { });
            }
            long fin = System.nanoTime();

            double paginasPorConsulta = (double) arbol.paginasLeidas() / RANGOS_POR_ANCHO;
            System.out.printf("%8d %14.2f %14.2f %14.2f %12.1f%n",
                    w,
                    (double) entregados[0] / RANGOS_POR_ANCHO,
                    paginasPorConsulta,
                    paginasPorConsulta - (arbol.niveles() - 1),
                    (fin - arranque) / 1000.0 / RANGOS_POR_ANCHO);
        }
    }

    private static boolean contiene(long[] ordenadas, int n, long x) {
        int bajo = 0;
        int alto = n - 1;
        while (bajo <= alto) {
            int medio = (bajo + alto) >>> 1;
            if (ordenadas[medio] == x) {
                return true;
            }
            if (ordenadas[medio] < x) {
                bajo = medio + 1;
            } else {
                alto = medio - 1;
            }
        }
        return false;
    }

    private static void barajar(long[] a, int n) {
        Random rng = new Random(SEMILLA);
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            long t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    private static long valorEntero(String[] args, String nombre, long porOmision) {
        String prefijo = nombre + "=";
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith(prefijo)) {
                return Long.parseLong(args[i].substring(prefijo.length()));
            }
        }
        return porOmision;
    }

}
