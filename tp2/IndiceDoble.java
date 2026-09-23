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
 *   Ejercicio 2 - IndiceDoble<K,V>: no reimplementa nada, compone un
 *   ABBAumentado (duenho de los nodos) y una TablaEncadenada que guarda
 *   una REFERENCIA al nodo del arbol, no una copia del valor. obtener
 *   resuelve solo por la tabla y por eso no incrementa arbol.visitas().
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

public class IndiceDoble<K extends Comparable<K>, V> {

    private ABBAumentado<K, V> arbol;
    private TablaEncadenada<K, ABBAumentado.Nodo<K, V>> tabla;

    public IndiceDoble() {
        this.arbol = new ABBAumentado<>();
        this.tabla = new TablaEncadenada<>();
    }

    public void agregar(K clave, V valor) {
        arbol.agregar(clave, valor);
        ABBAumentado.Nodo<K, V> nodoRef = arbol.obtenerNodo(clave);
        tabla.insertar(clave, nodoRef);
    }

    public V obtener(K clave) throws ClaveInexistenteException {
        ABBAumentado.Nodo<K, V> nodoRef = tabla.obtener(clave);
        if (nodoRef == null) throw new ClaveInexistenteException();
        return nodoRef.valor(); // Llamamos a valor() del Nodo
    }

    public boolean contiene(K clave) {
        return tabla.contiene(clave);
    }

    public V eliminar(K clave) throws ClaveInexistenteException {
        V valorEliminado = arbol.eliminar(clave);
        tabla.eliminar(clave);
        return valorEliminado;
    }

    public int size() {
        return arbol.size();
    }

    public K kEsimo(int k) {
        return arbol.kEsimo(k);
    }

    public int cuantosMenores(K clave) {
        return arbol.cuantosMenores(clave);
    }

    public int consultarRango(K a, K b) {
        return arbol.consultarRango(a, b);
    }

    public int rango(K clave) throws ClaveInexistenteException {
        return arbol.rango(clave);
    }

    public ABBAumentado<K, V> arbol() {
        return this.arbol;
    }

    public TablaEncadenada<K, ABBAumentado.Nodo<K, V>> tabla() {
        return this.tabla;
    }

    public long visitasArbol() {
        return arbol.visitas();
    }

    public void reiniciarVisitasArbol() {
        arbol.reiniciarVisitas();
    }
}
