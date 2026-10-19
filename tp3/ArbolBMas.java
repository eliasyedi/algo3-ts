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
 *   Ejercicio 1 - ArbolBMas: arbol B+ que vive en el archivo indice.dat,
 *   una pagina de P bytes por nodo. En memoria solo estan la cabecera y
 *   las paginas del camino raiz-hoja de la operacion en curso.
 *
 *   El como y el por que de cada algoritmo estan en docs/.
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
import java.io.RandomAccessFile;

public class ArbolBMas implements AutoCloseable {

    private static final int MAGIC = 0x41454433;
    private static final int VERSION = 1;

    private static final int CAB_MAGIC = 0;
    private static final int CAB_VERSION = 4;
    private static final int CAB_TAM_PAGINA = 8;
    private static final int CAB_M = 12;
    private static final int CAB_L = 16;
    private static final int CAB_RAIZ = 20;
    private static final int CAB_PAGINAS = 24;
    private static final int CAB_NIVELES = 28;
    private static final int CAB_CLAVES = 32;
    private static final int CAB_BYTES = 40;

    private static final int NODO_TIPO = 0;
    private static final int NODO_N = 4;
    private static final int NODO_SIGUIENTE = 8;
    private static final int NODO_CABECERA = 12;

    private static final int TIPO_HOJA = 0;
    private static final int TIPO_INTERNO = 1;

    private static final int BYTES_PAR = 16;
    private static final int BYTES_HIJO = 4;
    private static final int BYTES_CLAVE = 8;

    private static final int TAM_PAGINA_MINIMO = 40;

    private static final int SIN_HOJA_PREVIA = -2;

    private static class Verificacion {
        boolean[] alcanzada;
        int esperadoEnCadena;
        long totalClaves;
    }

    private final RandomAccessFile archivo;

    private final int tamPagina;
    private final int M;
    private final int L;

    private int raiz;
    private int cantidadPaginas;
    private int niveles;
    private long cantidadClaves;

    private long paginasLeidas;
    private long paginasEscritas;

    private boolean cerrado;

    // El constructor solo calcula. La validacion de P no va aca porque ante el
    // mismo P imposible crear y abrir tienen que fallar distinto: uno es un
    // error del programador y el otro es un archivo corrupto.
    private ArbolBMas(RandomAccessFile archivo, int tamPagina) {
        this.archivo = archivo;
        this.tamPagina = tamPagina;
        this.L = capacidadHojaPara(tamPagina);
        this.M = ordenInternoPara(tamPagina);
    }

    public static ArbolBMas crear(String ruta, int tamPagina) throws IOException {
        if (!tamPaginaValido(tamPagina)) {
            throw new TamPaginaInvalidoException(
                    "P = " + tamPagina + " no admite el formato: da L = "
                            + capacidadHojaPara(tamPagina) + " y M = "
                            + ordenInternoPara(tamPagina)
                            + "; se necesita P >= 40, L >= 2 y M >= 3");
        }

        RandomAccessFile archivo = new RandomAccessFile(new File(ruta), "rw");
        archivo.setLength(0);

        ArbolBMas arbol = new ArbolBMas(archivo, tamPagina);
        arbol.raiz = 1;
        arbol.cantidadPaginas = 2;
        arbol.niveles = 1;
        arbol.cantidadClaves = 0;

        arbol.escribirCabecera();
        arbol.escribirPagina(1, arbol.paginaNodoVacia(TIPO_HOJA));
        // Los contadores arrancan en 0: armar el archivo no es una operacion
        // del arbol que se mida.
        arbol.reiniciarContadores();
        return arbol;
    }

    public static ArbolBMas abrir(String ruta) throws IOException {
        File f = new File(ruta);
        if (!f.isFile()) {
            throw new FormatoInvalidoException("no existe el archivo " + ruta);
        }

        RandomAccessFile archivo = new RandomAccessFile(f, "rw");
        try {
            if (archivo.length() < CAB_BYTES) {
                throw new FormatoInvalidoException(
                        "el archivo mide " + archivo.length()
                                + " bytes, no entra ni la cabecera");
            }

            byte[] cab = new byte[CAB_BYTES];
            archivo.seek(0);
            archivo.readFully(cab);

            int magic = leerInt(cab, CAB_MAGIC);
            if (magic != MAGIC) {
                throw new FormatoInvalidoException(
                        "MAGIC invalido: 0x" + Integer.toHexString(magic));
            }
            int version = leerInt(cab, CAB_VERSION);
            if (version != VERSION) {
                throw new FormatoInvalidoException("version invalida: " + version);
            }

            int tamPagina = leerInt(cab, CAB_TAM_PAGINA);
            if (!tamPaginaValido(tamPagina)) {
                throw new FormatoInvalidoException("P invalido: " + tamPagina);
            }
            // M y L no se crean, se recalculan desde P. No son datos sueltos,
            // son funcion de P.
            int m = leerInt(cab, CAB_M);
            int l = leerInt(cab, CAB_L);
            if (m != ordenInternoPara(tamPagina) || l != capacidadHojaPara(tamPagina)) {
                throw new FormatoInvalidoException(
                        "M = " + m + " y L = " + l + " no corresponden a P = " + tamPagina);
            }

            int cantidadPaginas = leerInt(cab, CAB_PAGINAS);
            if (cantidadPaginas < 2) {
                throw new FormatoInvalidoException(
                        "cantidadPaginas = " + cantidadPaginas + ", el minimo es 2");
            }
            long largoEsperado = (long) cantidadPaginas * tamPagina;
            if (archivo.length() != largoEsperado) {
                throw new FormatoInvalidoException(
                        "el archivo mide " + archivo.length()
                                + " bytes y la cabecera dice " + largoEsperado);
            }

            int raiz = leerInt(cab, CAB_RAIZ);
            if (raiz < 1 || raiz >= cantidadPaginas) {
                throw new FormatoInvalidoException("raiz fuera de rango: " + raiz);
            }
            int niveles = leerInt(cab, CAB_NIVELES);
            if (niveles < 1) {
                throw new FormatoInvalidoException("niveles invalido: " + niveles);
            }
            long claves = leerLong(cab, CAB_CLAVES);
            if (claves < 0) {
                throw new FormatoInvalidoException("cantidadClaves invalida: " + claves);
            }

            ArbolBMas arbol = new ArbolBMas(archivo, tamPagina);
            arbol.raiz = raiz;
            arbol.cantidadPaginas = cantidadPaginas;
            arbol.niveles = niveles;
            arbol.cantidadClaves = claves;
            return arbol;
        } catch (IOException e) {
            archivo.close();
            throw e;
        }
    }

    // L y M no se eligen, salen de P. Una hoja son los 12 bytes de cabecera
    // mas L pares de 16: 12 + 16L <= P.
    private static int capacidadHojaPara(int tamPagina) {
        return (tamPagina - NODO_CABECERA) / BYTES_PAR;
    }

    // Un interno son 12 bytes, M hijos de 4 y M-1 claves de 8. El bloque de
    // hijos reserva siempre los M lugares, asi que las claves arrancan en un
    // offset fijo y no se mueven cuando el nodo gana o pierde un hijo.
    private static int ordenInternoPara(int tamPagina) {
        return (tamPagina - BYTES_HIJO) / (BYTES_HIJO + BYTES_CLAVE);
    }

    private static boolean tamPaginaValido(int tamPagina) {
        return tamPagina >= TAM_PAGINA_MINIMO
                && capacidadHojaPara(tamPagina) >= 2
                && ordenInternoPara(tamPagina) >= 3;
    }

    private static int offsetClaveHoja(int i) {
        return NODO_CABECERA + BYTES_PAR * i;
    }

    private static int offsetPosicionHoja(int i) {
        return offsetClaveHoja(i) + BYTES_CLAVE;
    }

    private static int offsetHijo(int i) {
        return NODO_CABECERA + BYTES_HIJO * i;
    }

    private int offsetClaveInterno(int i) {
        return NODO_CABECERA + BYTES_HIJO * M + BYTES_CLAVE * i;
    }

    private static int tipo(byte[] pagina) {
        return leerInt(pagina, NODO_TIPO);
    }

    private static boolean esHoja(byte[] pagina) {
        return tipo(pagina) == TIPO_HOJA;
    }

    private static int cantidadClavesNodo(byte[] pagina) {
        return leerInt(pagina, NODO_N);
    }

    private static void ponerCantidadClavesNodo(byte[] pagina, int n) {
        escribirInt(pagina, NODO_N, n);
    }

    private static int siguiente(byte[] pagina) {
        return leerInt(pagina, NODO_SIGUIENTE);
    }

    private static void ponerSiguiente(byte[] pagina, int nroPagina) {
        escribirInt(pagina, NODO_SIGUIENTE, nroPagina);
    }

    private static long claveHoja(byte[] pagina, int i) {
        return leerLong(pagina, offsetClaveHoja(i));
    }

    private static long posicionHoja(byte[] pagina, int i) {
        return leerLong(pagina, offsetPosicionHoja(i));
    }

    private static void ponerParHoja(byte[] pagina, int i, long clave, long posicion) {
        escribirLong(pagina, offsetClaveHoja(i), clave);
        escribirLong(pagina, offsetPosicionHoja(i), posicion);
    }

    private static int hijo(byte[] pagina, int i) {
        return leerInt(pagina, offsetHijo(i));
    }

    private static void ponerHijo(byte[] pagina, int i, int nroPagina) {
        escribirInt(pagina, offsetHijo(i), nroPagina);
    }

    private long claveInterno(byte[] pagina, int i) {
        return leerLong(pagina, offsetClaveInterno(i));
    }

    private void ponerClaveInterno(byte[] pagina, int i, long clave) {
        escribirLong(pagina, offsetClaveInterno(i), clave);
    }

    private byte[] paginaNodoVacia(int tipo) {
        byte[] pagina = new byte[tamPagina];
        escribirInt(pagina, NODO_TIPO, tipo);
        escribirInt(pagina, NODO_N, 0);
        escribirInt(pagina, NODO_SIGUIENTE, -1);
        return pagina;
    }

    private byte[] leerPaginaCruda(int nro) throws IOException {
        byte[] pagina = new byte[tamPagina];
        // Cast a long: sin eso la multiplicacion se hace en 32 bits y
        // desborda en silencio con archivos grandes.
        archivo.seek((long) nro * tamPagina);
        archivo.readFully(pagina);
        return pagina;
    }

    private byte[] leerPagina(int nro) throws IOException {
        byte[] pagina = leerPaginaCruda(nro);
        paginasLeidas++;
        return pagina;
    }

    private void escribirPagina(int nro, byte[] pagina) throws IOException {
        archivo.seek((long) nro * tamPagina);
        archivo.write(pagina);
        paginasEscritas++;
    }

    private void escribirCabecera() throws IOException {
        byte[] pagina = new byte[tamPagina];
        escribirInt(pagina, CAB_MAGIC, MAGIC);
        escribirInt(pagina, CAB_VERSION, VERSION);
        escribirInt(pagina, CAB_TAM_PAGINA, tamPagina);
        escribirInt(pagina, CAB_M, M);
        escribirInt(pagina, CAB_L, L);
        escribirInt(pagina, CAB_RAIZ, raiz);
        escribirInt(pagina, CAB_PAGINAS, cantidadPaginas);
        escribirInt(pagina, CAB_NIVELES, niveles);
        escribirLong(pagina, CAB_CLAVES, cantidadClaves);
        archivo.seek(0);
        archivo.write(pagina);
    }

    private static void escribirInt(byte[] p, int off, int v) {
        p[off]     = (byte) (v >>> 24);
        p[off + 1] = (byte) (v >>> 16);
        p[off + 2] = (byte) (v >>> 8);
        p[off + 3] = (byte) v;
    }

    // El & 0xFF no es decorativo: byte en Java es con signo, asi que sin la
    // mascara un 0xFF se promueve a -1 (todo unos) y pisa el resto del or.
    private static int leerInt(byte[] p, int off) {
        return ((p[off] & 0xFF) << 24)
                | ((p[off + 1] & 0xFF) << 16)
                | ((p[off + 2] & 0xFF) << 8)
                | (p[off + 3] & 0xFF);
    }

    private static void escribirLong(byte[] p, int off, long v) {
        for (int i = 0; i < 8; i++) {
            p[off + i] = (byte) (v >>> (56 - 8 * i));
        }
    }

    private static long leerLong(byte[] p, int off) {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (p[off + i] & 0xFFL);
        }
        return v;
    }

    // Cuantas claves del nodo son <= x. Binaria propia, Arrays.binarySearch
    // esta prohibido y ademas no sirve: no queremos la posicion de la clave.
    // El <= (y no <) es lo que manda a la derecha una clave igual al separador.
    private int indiceHijo(byte[] pagina, long clave) {
        int bajo = 0;
        int alto = cantidadClavesNodo(pagina) - 1;
        int cantidadMenoresOIguales = 0;
        while (bajo <= alto) {
            int medio = (bajo + alto) >>> 1;
            if (claveInterno(pagina, medio) <= clave) {
                cantidadMenoresOIguales = medio + 1;
                bajo = medio + 1;
            } else {
                alto = medio - 1;
            }
        }
        return cantidadMenoresOIguales;
    }

    private static int buscarEnHoja(byte[] pagina, long clave) {
        int bajo = 0;
        int alto = cantidadClavesNodo(pagina) - 1;
        while (bajo <= alto) {
            int medio = (bajo + alto) >>> 1;
            long c = claveHoja(pagina, medio);
            if (c == clave) {
                return medio;
            }
            if (c < clave) {
                bajo = medio + 1;
            } else {
                alto = medio - 1;
            }
        }
        // Si no esta, devuelve el punto de insercion en negativo. Hoy buscar
        // solo mira el signo, pero insertar necesita justo ese numero.
        return -(bajo + 1);
    }

    // Lee siempre niveles() paginas, este o no este la clave: hasta no llegar
    // a la hoja no se sabe, asi que no hay salida anticipada.
    public long buscar(long clave) throws IOException {
        int nro = raiz;
        for (int nivel = 1; nivel < niveles; nivel++) {
            byte[] interno = leerPagina(nro);
            nro = hijo(interno, indiceHijo(interno, clave));
        }
        byte[] hoja = leerPagina(nro);
        int i = buscarEnHoja(hoja, clave);
        return i >= 0 ? posicionHoja(hoja, i) : -1;
    }

    public int[] caminoBusqueda(long clave) throws IOException {
        int[] camino = new int[niveles];
        int nro = raiz;
        for (int nivel = 0; nivel < niveles - 1; nivel++) {
            camino[nivel] = nro;
            byte[] interno = leerPagina(nro);
            nro = hijo(interno, indiceHijo(interno, clave));
        }
        camino[niveles - 1] = nro;
        return camino;
    }

    public boolean insertar(long clave, long posicion) throws IOException {
        // Tres arreglos del tamanho de la altura. El tercero guarda el
        // contenido de cada pagina del camino porque al subir NO se puede
        // releer el padre: romperia el contrato R = niveles.
        int[] paginasCamino = new int[niveles];
        int[] indicesCamino = new int[niveles];
        byte[][] contenidoCamino = new byte[niveles][];

        int nro = raiz;
        for (int nivel = 0; nivel < niveles - 1; nivel++) {
            byte[] interno = leerPagina(nro);
            int i = indiceHijo(interno, clave);
            paginasCamino[nivel] = nro;
            indicesCamino[nivel] = i;
            contenidoCamino[nivel] = interno;
            nro = hijo(interno, i);
        }

        byte[] hoja = leerPagina(nro);
        paginasCamino[niveles - 1] = nro;
        contenidoCamino[niveles - 1] = hoja;

        int encontrada = buscarEnHoja(hoja, clave);
        if (encontrada >= 0) {
            return false;
        }

        int posicionInsercion = -(encontrada + 1);
        int n = cantidadClavesNodo(hoja);

        if (n < L) {
            insertarParEnHoja(hoja, n, posicionInsercion, clave, posicion);
            escribirPagina(nro, hoja);
            cantidadClaves++;
            return true;
        }

        // La hoja esta llena y hay que meter uno mas: L+1 pares, que no entran
        // en la pagina. Se arman en memoria con el nuevo en su lugar.
        long[] claves = new long[L + 1];
        long[] posiciones = new long[L + 1];
        for (int k = 0, origen = 0; k < L + 1; k++) {
            if (k == posicionInsercion) {
                claves[k] = clave;
                posiciones[k] = posicion;
            } else {
                claves[k] = claveHoja(hoja, origen);
                posiciones[k] = posicionHoja(hoja, origen);
                origen++;
            }
        }

        // (L+2)/2 es ceil((L+1)/2) escrito con division entera.
        // La original se queda en SU MISMA pagina: si se moviera habria que
        // avisarle al padre y a la hoja anterior, que no esta en el camino.
        int izquierda = (L + 2) / 2;
        int nroNueva = cantidadPaginas;
        cantidadPaginas++;
        byte[] nueva = paginaNodoVacia(TIPO_HOJA);

        for (int k = 0; k < izquierda; k++) {
            ponerParHoja(hoja, k, claves[k], posiciones[k]);
        }
        ponerCantidadClavesNodo(hoja, izquierda);
        limpiarColaHoja(hoja, izquierda);

        for (int k = izquierda; k < L + 1; k++) {
            ponerParHoja(nueva, k - izquierda, claves[k], posiciones[k]);
        }
        ponerCantidadClavesNodo(nueva, L + 1 - izquierda);

        // En este orden: primero la nueva hereda el siguiente, DESPUES la
        // original apunta a la nueva. Al reves se pierde el resto de la cadena.
        ponerSiguiente(nueva, siguiente(hoja));
        ponerSiguiente(hoja, nroNueva);

        escribirPagina(nro, hoja);
        escribirPagina(nroNueva, nueva);

        cantidadClaves++;
        // Sube la primera clave de la hoja nueva, COPIADA: en una hoja la clave
        // es un dato y no puede desaparecer de abajo.
        propagar(paginasCamino, indicesCamino, contenidoCamino, niveles - 2,
                claves[izquierda], nro, nroNueva);
        return true;
    }

    private static void insertarParEnHoja(byte[] hoja, int n, int pos, long clave, long posicion) {
        // De atras para adelante. Al reves cada par pisaria al siguiente antes
        // de haberlo leido.
        for (int k = n - 1; k >= pos; k--) {
            ponerParHoja(hoja, k + 1, claveHoja(hoja, k), posicionHoja(hoja, k));
        }
        ponerParHoja(hoja, pos, clave, posicion);
        ponerCantidadClavesNodo(hoja, n + 1);
    }

    private void limpiarColaHoja(byte[] hoja, int desde) {
        for (int b = offsetClaveHoja(desde); b < offsetClaveHoja(L); b++) {
            hoja[b] = 0;
        }
    }

    private void propagar(int[] paginasCamino, int[] indicesCamino, byte[][] contenidoCamino,
                          int nivel, long separador, int nroIzquierda, int nroDerecha)
            throws IOException {
        // Se partio la raiz. Se CREA una pagina nueva: la raiz vieja no se
        // convierte en nada, se queda donde estaba. Unico caso en que cambia
        // la altura.
        if (nivel < 0) {
            int nroRaiz = cantidadPaginas;
            cantidadPaginas++;
            byte[] nuevaRaiz = paginaNodoVacia(TIPO_INTERNO);
            ponerHijo(nuevaRaiz, 0, nroIzquierda);
            ponerHijo(nuevaRaiz, 1, nroDerecha);
            ponerClaveInterno(nuevaRaiz, 0, separador);
            ponerCantidadClavesNodo(nuevaRaiz, 1);
            escribirPagina(nroRaiz, nuevaRaiz);
            raiz = nroRaiz;
            niveles++;
            return;
        }

        byte[] padre = contenidoCamino[nivel];
        int n = cantidadClavesNodo(padre);
        int i = indicesCamino[nivel];

        if (n < M - 1) {
            insertarEnInterno(padre, n, i, separador, nroDerecha);
            escribirPagina(paginasCamino[nivel], padre);
            return;
        }

        // El padre esta lleno: con el nuevo serian M claves y M+1 hijos.
        long[] claves = new long[M];
        for (int k = 0, origen = 0; k < M; k++) {
            claves[k] = k == i ? separador : claveInterno(padre, origen++);
        }
        int[] hijos = new int[M + 1];
        for (int k = 0, origen = 0; k < M + 1; k++) {
            hijos[k] = k == i + 1 ? nroDerecha : hijo(padre, origen++);
        }

        int izquierda = (M + 2) / 2;
        int nroNuevo = cantidadPaginas;
        cantidadPaginas++;
        byte[] nuevo = paginaNodoVacia(TIPO_INTERNO);

        for (int k = 0; k < izquierda; k++) {
            ponerHijo(padre, k, hijos[k]);
        }
        for (int k = 0; k < izquierda - 1; k++) {
            ponerClaveInterno(padre, k, claves[k]);
        }
        ponerCantidadClavesNodo(padre, izquierda - 1);
        limpiarColaInterno(padre, izquierda);

        for (int k = izquierda; k < M + 1; k++) {
            ponerHijo(nuevo, k - izquierda, hijos[k]);
        }
        for (int k = izquierda; k < M; k++) {
            ponerClaveInterno(nuevo, k - izquierda, claves[k]);
        }
        ponerCantidadClavesNodo(nuevo, M - izquierda);

        escribirPagina(paginasCamino[nivel], padre);
        escribirPagina(nroNuevo, nuevo);

        // Aca la clave del medio SUBE Y DESAPARECE de este nivel: en un interno
        // es solo un cartel, y el padre ya lo va a dar. Esta es la diferencia
        // con el split de hoja, donde la clave se copia.
        propagar(paginasCamino, indicesCamino, contenidoCamino, nivel - 1,
                claves[izquierda - 1], paginasCamino[nivel], nroNuevo);
    }

    private void limpiarColaInterno(byte[] interno, int hijosQueQuedan) {
        for (int b = offsetHijo(hijosQueQuedan); b < offsetHijo(M); b++) {
            interno[b] = 0;
        }
        for (int b = offsetClaveInterno(hijosQueQuedan - 1); b < offsetClaveInterno(M - 1); b++) {
            interno[b] = 0;
        }
    }

    private void insertarEnInterno(byte[] interno, int n, int i, long clave, int nroHijo) {
        for (int k = n - 1; k >= i; k--) {
            ponerClaveInterno(interno, k + 1, claveInterno(interno, k));
        }
        for (int k = n; k >= i + 1; k--) {
            ponerHijo(interno, k + 1, hijo(interno, k));
        }
        // La clave va en claves[i] y el hijo nuevo en hijos[i+1]. El hijo viejo
        // sigue estando en hijos[i]: el nuevo es su hermano de la derecha y la
        // clave es el cartel que va entre los dos.
        ponerClaveInterno(interno, i, clave);
        ponerHijo(interno, i + 1, nroHijo);
        ponerCantidadClavesNodo(interno, n + 1);
    }

    public int buscarRango(long desde, long hasta, ConsumidorEntrada c) throws IOException {
        if (desde > hasta) {
            throw new RangoInvalidoException("rango invalido: " + desde + " > " + hasta);
        }

        int nro = raiz;
        for (int nivel = 1; nivel < niveles; nivel++) {
            byte[] interno = leerPagina(nro);
            nro = hijo(interno, indiceHijo(interno, desde));
        }

        int entregados = 0;
        while (nro != -1) {
            byte[] hoja = leerPagina(nro);
            int n = cantidadClavesNodo(hoja);
            for (int i = 0; i < n; i++) {
                long clave = claveHoja(hoja, i);
                if (clave > hasta) {
                    return entregados;
                }
                if (clave >= desde) {
                    c.aceptar(clave, posicionHoja(hoja, i));
                    entregados++;
                }
            }
            // Pasa a la hoja siguiente SOLO si la ultima clave de esta es
            // < hasta. Sin esta regla se lee una hoja de mas en cada consulta.
            if (n == 0 || claveHoja(hoja, n - 1) >= hasta) {
                return entregados;
            }
            nro = siguiente(hoja);
        }
        return entregados;
    }

    public String volcarPagina(int nro) throws IOException {
        if (nro < 1 || nro >= cantidadPaginas) {
            throw new IllegalArgumentException(
                    "pagina " + nro + " fuera de rango [1, " + (cantidadPaginas - 1) + "]");
        }
        byte[] pagina = leerPaginaCruda(nro);
        return esHoja(pagina) ? volcarHoja(nro, pagina) : volcarInterno(nro, pagina);
    }

    private String volcarHoja(int nro, byte[] pagina) {
        int n = cantidadClavesNodo(pagina);
        StringBuilder s = new StringBuilder();
        s.append('#').append(nro).append(" H n=").append(n)
                .append(" sig=").append(siguiente(pagina)).append(" [");
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                s.append(' ');
            }
            s.append(claveHoja(pagina, i)).append(':').append(posicionHoja(pagina, i));
        }
        return s.append(']').toString();
    }

    private String volcarInterno(int nro, byte[] pagina) {
        int n = cantidadClavesNodo(pagina);
        StringBuilder s = new StringBuilder();
        s.append('#').append(nro).append(" I n=").append(n).append(" [");
        for (int i = 0; i < n; i++) {
            s.append('#').append(hijo(pagina, i)).append(' ')
                    .append(claveInterno(pagina, i)).append(' ');
        }
        return s.append('#').append(hijo(pagina, n)).append(']').toString();
    }

    public String volcarArbol() throws IOException {
        StringBuilder s = new StringBuilder();
        int[] nivelPaginas = new int[] { raiz };
        int cantidadNivel = 1;

        for (int nivel = 1; nivel <= niveles; nivel++) {
            if (nivel > 1) {
                s.append('\n');
            }
            s.append(nivel).append(": ");

            int[] hijos = new int[cantidadNivel * 2];
            int cantidadHijos = 0;

            for (int i = 0; i < cantidadNivel; i++) {
                if (i > 0) {
                    s.append(" | ");
                }
                int nro = nivelPaginas[i];
                byte[] pagina = leerPaginaCruda(nro);
                int n = cantidadClavesNodo(pagina);
                s.append('#').append(nro);

                if (esHoja(pagina)) {
                    s.append('[');
                    for (int k = 0; k < n; k++) {
                        if (k > 0) {
                            s.append(',');
                        }
                        s.append(claveHoja(pagina, k));
                    }
                    s.append(']');
                } else {
                    s.append('(');
                    for (int k = 0; k < n; k++) {
                        if (k > 0) {
                            s.append(',');
                        }
                        s.append(claveInterno(pagina, k));
                    }
                    s.append(')');
                    for (int k = 0; k <= n; k++) {
                        if (cantidadHijos == hijos.length) {
                            hijos = copiar(hijos, hijos.length * 2 + 1);
                        }
                        hijos[cantidadHijos++] = hijo(pagina, k);
                    }
                }
            }

            nivelPaginas = hijos;
            cantidadNivel = cantidadHijos;
        }
        return s.toString();
    }

    public EstadisticasBMas estadisticas() throws IOException {
        EstadisticasBMas e = new EstadisticasBMas();
        long[] sumas = new long[2];
        recorrerEstadisticas(raiz, e, sumas);
        e.ocupacionHojas = e.hojas == 0 ? 0 : (double) sumas[0] / ((long) e.hojas * L);
        e.ocupacionInternos = e.internos == 0 ? 0 : (double) sumas[1] / ((long) e.internos * M);
        return e;
    }

    private void recorrerEstadisticas(int nro, EstadisticasBMas e, long[] sumas)
            throws IOException {
        byte[] pagina = leerPaginaCruda(nro);
        int n = cantidadClavesNodo(pagina);
        if (esHoja(pagina)) {
            e.hojas++;
            sumas[0] += n;
            return;
        }
        e.internos++;
        sumas[1] += n + 1;
        for (int i = 0; i <= n; i++) {
            recorrerEstadisticas(hijo(pagina, i), e, sumas);
        }
    }

    public String verificarInvariantes() throws IOException {
        long largoEsperado = (long) cantidadPaginas * tamPagina;
        if (archivo.length() != largoEsperado) {
            return "el archivo mide " + archivo.length() + " bytes y la cabecera dice "
                    + largoEsperado;
        }

        Verificacion v = new Verificacion();
        v.alcanzada = new boolean[cantidadPaginas];
        v.esperadoEnCadena = SIN_HOJA_PREVIA;

        String error = verificar(raiz, Long.MIN_VALUE, Long.MAX_VALUE, 1, v);
        if (error != null) {
            return error;
        }
        if (v.esperadoEnCadena != -1) {
            return "la cadena de hojas termina en " + v.esperadoEnCadena + " y no en -1";
        }
        if (v.totalClaves != cantidadClaves) {
            return "el arbol tiene " + v.totalClaves + " claves y la cabecera dice "
                    + cantidadClaves;
        }
        for (int nro = 1; nro < cantidadPaginas; nro++) {
            if (!v.alcanzada[nro]) {
                return "la pagina #" + nro + " no se alcanza desde la raiz";
            }
        }
        return "OK";
    }

    private String verificar(int nro, long min, long max, int nivel, Verificacion v)
            throws IOException {
        if (nro < 1 || nro >= cantidadPaginas) {
            return "referencia a una pagina inexistente: #" + nro;
        }
        if (v.alcanzada[nro]) {
            return "la pagina #" + nro + " se alcanza mas de una vez";
        }
        v.alcanzada[nro] = true;

        byte[] pagina = leerPaginaCruda(nro);
        int n = cantidadClavesNodo(pagina);
        boolean esRaiz = nro == raiz;

        if (esHoja(pagina)) {
            if (nivel != niveles) {
                return "la hoja #" + nro + " esta en el nivel " + nivel + " y no en " + niveles;
            }
            if (esRaiz ? n > L : n < (L + 1) / 2 || n > L) {
                return "la hoja #" + nro + " tiene " + n + " pares, fuera de su rango de ocupacion";
            }
            for (int i = 0; i < n; i++) {
                long clave = claveHoja(pagina, i);
                if (i > 0 && clave <= claveHoja(pagina, i - 1)) {
                    return "la hoja #" + nro + " no tiene las claves estrictamente crecientes";
                }
                if (clave < min || clave >= max) {
                    return "la clave " + clave + " de la hoja #" + nro + " viola sus separadores";
                }
            }
            // La cadena se verifica con UNA sola variable: como el recorrido va
            // de izquierda a derecha, alcanza con recordar a quien apuntaba la
            // hoja anterior.
            if (v.esperadoEnCadena != SIN_HOJA_PREVIA && nro != v.esperadoEnCadena) {
                return "la cadena de hojas esperaba #" + v.esperadoEnCadena + " y llego a #" + nro;
            }
            v.esperadoEnCadena = siguiente(pagina);
            v.totalClaves += n;
            return null;
        }

        if (nivel >= niveles) {
            return "el interno #" + nro + " esta en el nivel " + nivel + ", donde van las hojas";
        }
        if (siguiente(pagina) != -1) {
            return "el interno #" + nro + " tiene siguiente = " + siguiente(pagina) + " y no -1";
        }
        if (esRaiz ? n + 1 < 2 || n + 1 > M : n + 1 < (M + 1) / 2 || n + 1 > M) {
            return "el interno #" + nro + " tiene " + (n + 1)
                    + " hijos, fuera de su rango de ocupacion";
        }
        for (int i = 0; i < n; i++) {
            long clave = claveInterno(pagina, i);
            if (i > 0 && clave <= claveInterno(pagina, i - 1)) {
                return "el interno #" + nro + " no tiene los separadores estrictamente crecientes";
            }
            if (clave < min || clave >= max) {
                return "el separador " + clave + " del interno #" + nro + " viola sus cotas";
            }
        }
        // Cada hijo baja con las cotas que le tocan: el de mas a la izquierda
        // hereda el min del padre y el de mas a la derecha el max. Toda clave
        // del subarbol tiene que cumplir min <= x < max.
        for (int i = 0; i <= n; i++) {
            long minHijo = i == 0 ? min : claveInterno(pagina, i - 1);
            long maxHijo = i == n ? max : claveInterno(pagina, i);
            String error = verificar(hijo(pagina, i), minHijo, maxHijo, nivel + 1, v);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    private static int[] copiar(int[] origen, int largoNuevo) {
        int[] destino = new int[largoNuevo];
        int tope = origen.length < largoNuevo ? origen.length : largoNuevo;
        for (int i = 0; i < tope; i++) {
            destino[i] = origen[i];
        }
        return destino;
    }

    public int niveles() {
        return niveles;
    }

    public int raiz() {
        return raiz;
    }

    public int cantidadPaginas() {
        return cantidadPaginas;
    }

    public long cantidadClaves() {
        return cantidadClaves;
    }

    public int tamPagina() {
        return tamPagina;
    }

    public int ordenInterno() {
        return M;
    }

    public int capacidadHoja() {
        return L;
    }

    public long paginasLeidas() {
        return paginasLeidas;
    }

    public long paginasEscritas() {
        return paginasEscritas;
    }

    public void reiniciarContadores() {
        paginasLeidas = 0;
        paginasEscritas = 0;
    }

    @Override
    public void close() throws IOException {
        if (cerrado) {
            return;
        }
        cerrado = true;
        escribirCabecera();
        archivo.close();
    }

}
