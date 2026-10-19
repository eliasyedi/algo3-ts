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
 *   Ejercicio 2 - BMasApp: la linea de comandos. Cada comando es una
 *   ejecucion separada de la JVM: construir arma datos.dat e indice.dat
 *   desde el TSV, y todos los demas trabajan SOLO con esos dos archivos,
 *   sin volver a leer el TSV. Ver docs/tp3/09-bmasapp.md.
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class BMasApp {

    private static final String INDICE = "indice.dat";
    private static final String DATOS = "datos.dat";

    private static final int FILAS_POR_OMISION = 300000;
    private static final int TAM_PAGINA_POR_OMISION = 4096;
    private static final int MOSTRAR_POR_OMISION = 10;
    private static final long SEMILLA = 2026;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            uso();
            return;
        }

        String comando = args[0];
        if (comando.equals("construir")) {
            construir(args);
        } else if (comando.equals("buscar")) {
            buscar(args);
        } else if (comando.equals("rango")) {
            rango(args);
        } else if (comando.equals("secuencial")) {
            secuencial(args);
        } else if (comando.equals("info")) {
            info();
        } else if (comando.equals("verificar")) {
            verificar();
        } else if (comando.equals("pagina")) {
            pagina(args);
        } else {
            System.out.println("comando desconocido: " + comando);
            uso();
        }
    }

    public static class Construccion {
        public int registros;
        public long repetidas;
        public long bytesDatos;
        public long bytesIndice;
        public int tamPagina;
        public int ordenInterno;
        public int capacidadHoja;
        public int niveles;
        public int paginas;
        public int hojas;
        public int internos;
        public double ocupacionHojas;
        public double ocupacionInternos;
        public long paginasEscritas;
        public boolean aleatorio;
        public double milisDatos;
        public double milisIndice;
    }

    public static Construccion construirIndice(String tsv, int filas, int tamPagina,
                                               boolean aleatorio, String rutaIndice,
                                               String rutaDatos) throws IOException {
        long[] claves = new long[filas];
        long[] posiciones = new long[filas];
        int cantidad = 0;

        long arranque = System.nanoTime();
        try (ArchivoDatos datos = ArchivoDatos.crear(rutaDatos);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     new FileInputStream(tsv), StandardCharsets.UTF_8))) {

            entrada.readLine();

            String linea;
            while (cantidad < filas && (linea = entrada.readLine()) != null) {
                long clave = claveDeTconst(primerCampo(linea));
                if (clave < 0) {
                    continue;
                }
                claves[cantidad] = clave;
                posiciones[cantidad] = datos.agregar(clave, linea);
                cantidad++;
            }
        }
        long finDatos = System.nanoTime();

        if (aleatorio) {
            barajar(claves, posiciones, cantidad);
        }

        Construccion c = new Construccion();
        c.aleatorio = aleatorio;
        c.registros = cantidad;
        c.milisDatos = (finDatos - arranque) / 1e6;

        long arranqueIndice = System.nanoTime();
        try (ArbolBMas arbol = ArbolBMas.crear(rutaIndice, tamPagina)) {
            for (int i = 0; i < cantidad; i++) {
                if (!arbol.insertar(claves[i], posiciones[i])) {
                    c.repetidas++;
                }
            }
            c.milisIndice = (System.nanoTime() - arranqueIndice) / 1e6;

            EstadisticasBMas e = arbol.estadisticas();
            c.tamPagina = arbol.tamPagina();
            c.ordenInterno = arbol.ordenInterno();
            c.capacidadHoja = arbol.capacidadHoja();
            c.niveles = arbol.niveles();
            c.paginas = arbol.cantidadPaginas();
            c.hojas = e.hojas;
            c.internos = e.internos;
            c.ocupacionHojas = e.ocupacionHojas;
            c.ocupacionInternos = e.ocupacionInternos;
            c.paginasEscritas = arbol.paginasEscritas();
        }
        c.bytesDatos = new java.io.File(rutaDatos).length();
        c.bytesIndice = new java.io.File(rutaIndice).length();
        return c;
    }

    private static void construir(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("falta el TSV: construir <tsv> [filas=N] [tamPagina=P] [--aleatorio]");
            return;
        }
        Construccion c = construirIndice(args[1],
                (int) valorEntero(args, "filas", FILAS_POR_OMISION),
                (int) valorEntero(args, "tamPagina", TAM_PAGINA_POR_OMISION),
                tieneBandera(args, "--aleatorio"), INDICE, DATOS);

        System.out.println("orden de insercion: " + (c.aleatorio
                ? "aleatorio (Fisher-Yates, semilla " + SEMILLA + ")"
                : "el del archivo"));
        System.out.println("registros:          " + c.registros
                + (c.repetidas > 0 ? "  (" + c.repetidas + " repetidas, no insertadas)" : ""));
        System.out.println("datos.dat:          " + c.bytesDatos + " bytes");
        System.out.println("indice.dat:         " + c.bytesIndice + " bytes");
        System.out.println("P / M / L:          " + c.tamPagina + " / " + c.ordenInterno
                + " / " + c.capacidadHoja);
        System.out.println("niveles:            " + c.niveles);
        System.out.println("paginas:            " + c.paginas);
        System.out.println("hojas / internos:   " + c.hojas + " / " + c.internos);
        System.out.println("ocupacion hojas:    " + porcentaje(c.ocupacionHojas));
        System.out.println("ocupacion intern:   " + porcentaje(c.ocupacionInternos));
        System.out.println("paginas escritas:   " + c.paginasEscritas);
        System.out.println("tiempo datos.dat:   " + String.format("%.1f", c.milisDatos) + " ms");
        System.out.println("tiempo indice.dat:  " + String.format("%.1f", c.milisIndice) + " ms");
    }

    static void barajar(long[] claves, long[] posiciones, int n) {
        Random rng = new Random(SEMILLA);
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            long c = claves[i];
            claves[i] = claves[j];
            claves[j] = c;
            long p = posiciones[i];
            posiciones[i] = posiciones[j];
            posiciones[j] = p;
        }
    }

    private static void buscar(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("falta la clave: buscar <tconst|numero>");
            return;
        }
        long clave = claveDeTconst(args[1]);

        try (ArbolBMas arbol = ArbolBMas.abrir(INDICE);
             ArchivoDatos datos = ArchivoDatos.abrir(DATOS)) {

            arbol.reiniciarContadores();
            long pos = arbol.buscar(clave);
            long paginas = arbol.paginasLeidas();
            int[] camino = arbol.caminoBusqueda(clave);

            System.out.println("paginas leidas: " + paginas + " (" + textoCamino(camino) + ")");
            if (pos < 0) {
                System.out.println("no esta la clave " + clave);
                return;
            }
            System.out.println("pos=" + pos + "  " + datos.leer(pos));
        }
    }

    private static void rango(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("faltan los extremos: rango <desde> <hasta> [mostrar=N]");
            return;
        }
        long desde = claveDeTconst(args[1]);
        long hasta = claveDeTconst(args[2]);
        final int mostrar = (int) valorEntero(args, "mostrar", MOSTRAR_POR_OMISION);

        try (ArbolBMas arbol = ArbolBMas.abrir(INDICE);
             ArchivoDatos datos = ArchivoDatos.abrir(DATOS)) {

            final long[] aMostrar = new long[mostrar];
            final int[] guardados = new int[1];

            arbol.reiniciarContadores();
            int entregados = arbol.buscarRango(desde, hasta, (clave, posicion) -> {
                if (guardados[0] < mostrar) {
                    aMostrar[guardados[0]++] = posicion;
                }
            });
            long paginas = arbol.paginasLeidas();

            System.out.println("registros en el rango: " + entregados);
            System.out.println("paginas leidas:        " + paginas
                    + "  (" + (arbol.niveles() - 1) + " del descenso + "
                    + (paginas - arbol.niveles() + 1) + " hojas)");
            for (int i = 0; i < guardados[0]; i++) {
                System.out.println("  pos=" + aMostrar[i] + "  " + datos.leer(aMostrar[i]));
            }
            if (entregados > guardados[0]) {
                System.out.println("  ... y " + (entregados - guardados[0]) + " mas");
            }
        }
    }

    private static void secuencial(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("falta la clave: secuencial <tconst|numero>");
            return;
        }
        long clave = claveDeTconst(args[1]);

        try (ArchivoDatos datos = ArchivoDatos.abrir(DATOS)) {
            datos.reiniciarContadores();
            long arranque = System.nanoTime();
            long pos = datos.busquedaSecuencial(clave);
            long fin = System.nanoTime();

            System.out.println("registros leidos: " + datos.registrosLeidos()
                    + "   (" + milis(arranque, fin) + " ms)");
            if (pos < 0) {
                System.out.println("no esta la clave " + clave);
                return;
            }
            System.out.println("pos=" + pos + "  " + datos.leer(pos));
        }
    }

    private static void info() throws IOException {
        try (ArbolBMas arbol = ArbolBMas.abrir(INDICE)) {
            System.out.println("P:                " + arbol.tamPagina());
            System.out.println("M:                " + arbol.ordenInterno());
            System.out.println("L:                " + arbol.capacidadHoja());
            System.out.println("raiz:             #" + arbol.raiz());
            System.out.println("niveles:          " + arbol.niveles());
            System.out.println("paginas:          " + arbol.cantidadPaginas());
            System.out.println("claves:           " + arbol.cantidadClaves());
            System.out.println("indice.dat:       " + new java.io.File(INDICE).length() + " bytes");
            imprimirOcupacion(arbol);
        }
        try (ArchivoDatos datos = ArchivoDatos.abrir(DATOS)) {
            System.out.println("datos.dat:        " + datos.tamano() + " bytes");
        }
    }

    private static void imprimirOcupacion(ArbolBMas arbol) throws IOException {
        EstadisticasBMas e = arbol.estadisticas();
        System.out.println("hojas / internos: " + e.hojas + " / " + e.internos);
        System.out.println("ocupacion hojas:  " + porcentaje(e.ocupacionHojas));
        System.out.println("ocupacion intern: " + porcentaje(e.ocupacionInternos));
    }

    private static void verificar() throws IOException {
        try (ArbolBMas arbol = ArbolBMas.abrir(INDICE)) {
            System.out.println(arbol.verificarInvariantes());
        }
    }

    private static void pagina(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("falta el numero: pagina <nro>");
            return;
        }
        int nro = (int) Long.parseLong(args[1]);
        try (ArbolBMas arbol = ArbolBMas.abrir(INDICE)) {
            System.out.println(arbol.volcarPagina(nro));
        }
    }

    static long claveDeTconst(String texto) {
        String limpio = texto.trim();
        if (limpio.startsWith("tt")) {
            limpio = limpio.substring(2);
        }
        int desde = 0;
        while (desde < limpio.length() - 1 && limpio.charAt(desde) == '0') {
            desde++;
        }
        limpio = limpio.substring(desde);
        for (int i = 0; i < limpio.length(); i++) {
            char c = limpio.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
        }
        return limpio.isEmpty() ? -1 : Long.parseLong(limpio);
    }

    private static String primerCampo(String linea) {
        int corte = linea.indexOf('\t');
        return corte < 0 ? linea : linea.substring(0, corte);
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

    private static boolean tieneBandera(String[] args, String bandera) {
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals(bandera)) {
                return true;
            }
        }
        return false;
    }

    private static String textoCamino(int[] camino) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < camino.length; i++) {
            if (i > 0) {
                s.append(" -> ");
            }
            s.append('#').append(camino[i]);
        }
        return s.toString();
    }

    static String porcentaje(double x) {
        return String.format("%.2f %%", x * 100);
    }

    private static String milis(long desdeNano, long hastaNano) {
        return String.format("%.1f", (hastaNano - desdeNano) / 1e6);
    }

    private static void uso() {
        System.out.println("uso:");
        System.out.println("  construir <tsv> [filas=300000] [tamPagina=4096] [--aleatorio]");
        System.out.println("  buscar <tconst|numero>");
        System.out.println("  rango <desde> <hasta> [mostrar=10]");
        System.out.println("  secuencial <tconst|numero>");
        System.out.println("  info");
        System.out.println("  verificar");
        System.out.println("  pagina <nro>");
    }

}
