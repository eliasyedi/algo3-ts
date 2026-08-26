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
 *   Ejercicio 2 - PilaES<E>: pila enlazada generica propia (apilar, desapilar, tope, estaVacia, size) con Nodo como inner class privada.
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

public class PilaES<T> implements LIFO<T> {

    private Nodo<T> topNode = null;
    private int size = 0;

    public PilaES() {
        this.topNode = null;
        this.size = 0;
    }

    @Override
    public void apilar(T value) {
        Nodo<T> newNode = new Nodo<T>(value, topNode);
        topNode = newNode;
        size++;
    }

    @Override
    public T desapilar() {
        if (estaVacia()) {
            throw new IllegalStateException("La pila está vacía");
        }

        Nodo<T> popNode = topNode;
        this.topNode = popNode.getNextNode();
        this.size--;
        return popNode.getValue();
    }

    @Override
    public T tope() {
        if (estaVacia()) {
            throw new IllegalStateException("La pila está vacía");
        }
        return topNode.getValue();
    }

    @Override
    public boolean estaVacia() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    private static class Nodo<T> {

        private T value;
        private Nodo<T> nextNode;

        public Nodo(T value, Nodo<T> nextNode) {
            this.value = value;
            this.nextNode = nextNode;
        }

        public Nodo<T> getNextNode() {
            return nextNode;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public void setNextNode(Nodo<T> nextNode) {
            this.nextNode = nextNode;
        }

        public T getValue() {
            return value;
        }
    }
}

interface LIFO<T> {
    void apilar(T value);
    T desapilar();
    T tope();
    boolean estaVacia();
    int size();
}
