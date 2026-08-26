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
 *   Ejercicio 1 - Version ingenua del buffer (insertar desplazando todo el arreglo a la derecha), usada como linea base para la tabla comparativa de desplazamientos.
 *
 * ---------------------------------------------------------------------
 * DECLARACION DE HONOR
 *   [PENDIENTE] Pegar aqui el texto exacto de la Declaracion de Honor
 *   entregado en la primera clase.
 * =====================================================================
 */

import java.util.Iterator;

public class BufferGapIngenuo<E> implements Iterable<E> {

    private E[] datos;

    private int cursor;

    private int size;

    private long desplazamientos;

    private static final int TAM_INICIAL = 16;

    public BufferGapIngenuo() {
        this.datos = (E[]) new Object[TAM_INICIAL];
        this.desplazamientos = 0;
        this.cursor = 0;
        this.size = 0;
    }


    public void insertar(E obj) {

        if (this.size == this.datos.length) resize();
        for (int i = size; i > cursor ; i--) {
            this.datos[i] = this.datos[i-1];
            this.desplazamientos ++;
        }
        this.datos[cursor++] = obj;
        this.size++;


    }

    private void resize() {

        E[] newArray = (E[]) new Object[this.datos.length * 2];

        for(int i = 0; i < this.size; i++){
            newArray[i] = this.datos[i];
            this.desplazamientos++;
        }
        this.datos = newArray;

    }

    public void moverCursor(int delta) {

        int nuevaPosicion = cursor + delta;

        if (nuevaPosicion < 0 || nuevaPosicion > size) {
            throw new IndexOutOfBoundsException();
        }

        cursor = nuevaPosicion;
    }

    public void reiniciarDesplazamientos() {
        this.desplazamientos = 0;
    }

    public long desplazamientos() {
        return this.desplazamientos;
    }



    @Override

    public Iterator<E> iterator() {

        return new BufferIterator();

    }


    private class BufferIterator implements Iterator<E> {

        private int index;

        private final E[] snapshot = datos;

        private final int sizeSnapshot = size;


        public BufferIterator() {

            this.index = 0;

        }

        @Override

        public boolean hasNext() {

            return index < sizeSnapshot;

        }

        @Override

        public E next() {
            return snapshot[index++];
        }

    }


    @Override
    public String toString() {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append('`');

        for (E d : this) {

            stringBuilder.append(d);

        }

        stringBuilder.append('`');

        return stringBuilder.toString();

    }

}
