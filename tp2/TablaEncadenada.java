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
 *   Ejercicio 2 - TablaEncadenada<K,E>: tabla de dispersion generica por
 *   encadenamiento con listas propias (una cadena por cubeta). Compresion
 *   (k.hashCode() & 0x7fffffff) % m, rehash al superar alfaMax, contador
 *   de sondas y dump() para la traza.
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

public class TablaEncadenada<K, E> {
    private static final int TAM_INICIAL = 11;

    private NodoHash[] cubetas;
    private int m; //longitud de tabla
    private int n; //cantidad de insertados
    private long sondas; //contador de visitas
    private double alfaMax;


    private class NodoHash {
        private K clave;
        private E dato;
        private NodoHash siguienteNodo = null;

        public NodoHash(K clave, E dato) {
            this.clave =  clave;
            this.dato = dato;
        }
    }

    public TablaEncadenada() {
        this(TAM_INICIAL, 1);
    }

    public TablaEncadenada(int m, double alfaMax) {
        this.m = m;
        this.alfaMax = alfaMax;
        this.n = 0;
        this.sondas = 0;
        this.cubetas = (NodoHash[]) new TablaEncadenada.NodoHash[m];
    }

    private int hash(K clave) {
        return (clave.hashCode() & 0x7fffffff) % m; //omite signo negativo del hashcode 0x7fffffff
    }

    public void insertar(K clave, E dato) {
        int indice = hash(clave);
        NodoHash actual = cubetas[indice];
        while (actual != null) {
            sondas++;
            if (actual.clave.equals(clave)) {
                actual.dato = dato;
                return; //ya existe;
            }
            actual = actual.siguienteNodo;
        }
        NodoHash nuevoNodo = new NodoHash(clave, dato);
        nuevoNodo.siguienteNodo = cubetas[indice];
        cubetas[indice] = nuevoNodo;
        n++;
        if (n > m * alfaMax) {
            rehash();
        }
    }

    public void rehash() {
        int newM = m * 2;
        NodoHash[] nuevasCubetas= ((NodoHash[]) new TablaEncadenada.NodoHash[newM]);
        this.m = newM;
        for (int i = 0; i < cubetas.length; i++) {
            NodoHash actual = cubetas[i];

            while (actual != null) {
                NodoHash siguienteViejo = actual.siguienteNodo;
                int nuevoIndice = hash(actual.clave);
                actual.siguienteNodo = nuevasCubetas[nuevoIndice];
                nuevasCubetas[nuevoIndice] = actual;
                actual = siguienteViejo;
            }
        }
        this.cubetas = nuevasCubetas;
    }

    public E eliminar(K clave) {
        int indice = hash(clave);
        NodoHash actual = cubetas[indice];
        NodoHash previo = null;

        while (actual != null) {
            sondas++;
            if (actual.clave.equals(clave)){
                if (previo == null) {
                    cubetas[indice] = actual.siguienteNodo;
                } else {
                    previo.siguienteNodo = actual.siguienteNodo;
                }
                n--;
                return actual.dato;
            }
            previo = actual;
            actual = actual.siguienteNodo;
        }
        return null;
    }

    public boolean contiene(K clave) {
        return search(clave) != null;
    }

    public E obtener(K clave) {
        NodoHash encontrado = search(clave);
        return encontrado == null ? null : encontrado.dato;
    }

    private NodoHash search(K clave) {
        int searchPorClave = hash(clave);
        NodoHash actual = cubetas[searchPorClave];
        while (actual != null) {
            sondas++;
            if (actual.clave.equals(clave)) {
                return actual;
            }
            actual = actual.siguienteNodo;
        }
        return null;
    }

    public int capacidad() {
        return this.m;
    }

    public int size() {
        return this.n;
    }

    public double factorCarga() {
        return (double) this.n / this.m;
    }

    public long sondas() {
        return this.sondas;
    }

    public void reiniciarSondas() {
        this.sondas = 0;
    }

    public String dump() {
        StringBuilder salida = new StringBuilder();
        for (int i = 0; i < m; i++) {
            salida.append(String.format("[%2d]", i));
            NodoHash actual = cubetas[i];
            while (actual != null) {
                salida.append(" ").append(actual.clave);
                if (actual.siguienteNodo != null) salida.append(" ->");
                actual = actual.siguienteNodo;
            }
            salida.append(System.lineSeparator());
        }
        return salida.toString();
    }
}
