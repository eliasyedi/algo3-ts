import java.util.Iterator;
 
public class BufferGap<E> implements Iterable<E>{

    private E[] datos;

    private int inicioHueco;

    private int finHueco;

    private long desplazamientos;

    private static final int TAM_INICIAL = 16;
 
    public BufferGap() {

        this.datos = (E[]) new Object[TAM_INICIAL];

        this.desplazamientos = 0;

        this.inicioHueco = 0;

        this.finHueco = TAM_INICIAL;

    }
 
    /*

    TODO check this out my duuuuude tamanhoHueco = finHueco-inicioHueco

        ej = TAM_INICIAL = 4

        insertas H

        H _ _ _

        inicioHueco = 1

        finHueco = 4

        tamanho = 3

        size() = capacidad - tamanhoHueco

        meeaniiing size() = datos.lenght - tamanhoHueco;

        H _ _ A

        inicioHueco = 1

        finHueco = 3

     */
 
 
    //                                       [----hueco----]

    //hay que desplazar me imagino //[h,o,l,a,,,,,,,,,,,,,,]

    //                                   [----hueco----]

    //hay que desplazar me imagino //[h,o,,,,,,,,,,,,,,,l,a]

    public void insertar(E obj) {

        if (this.inicioHueco == this.finHueco) {

            resize();

        }

        this.datos[inicioHueco++] = obj;

    }
 
    //todo implementar un rezise barraco aca

    private void resize() {

        E[] newArray = (E[]) new Object[this.datos.length + TAM_INICIAL];

        for (int i = 0; i < this.inicioHueco; i++) {

            newArray[i] = this.datos[i];

        }
 
 
        //finHueco - 1 nunca puede ser negativo

        //hay que empujar al fin del nuevo array copiar de atras a adelante puede funcionar o

        //probablemente hace la suma del offset

        int sizeAfterHueco = this.datos.length - this.finHueco;

        for (int i = this.finHueco, j = newArray.length - sizeAfterHueco - 1; i < this.datos.length; i++, j++) {

            newArray[j] = this.datos[i];

        }

        this.datos = newArray;

    }
 
 
    /**

     *

     * @return elemento borrado

     * @throws BufferVacioException exception lanzada si el cursor se encuentra en posicion 0

     */

    public E borrar() throws BufferVacioException {

        if (this.inicioHueco == 0) throw new BufferVacioException("cursor en posicion 0");

        E target = this.datos[this.inicioHueco - 1];

        this.datos[this.inicioHueco - 1] = null;

        return target;

    }
 
 
    //negativo hacia la izquierda, positivo hacia la derecha

    //lanza una exception no chequeada si el lcursor quedaria fuera de [0,size()] eg. IndexOutOfBoundsException

    public void moverCursor(int delta) {

        int preMove = inicioHueco + delta;

        if( preMove > size() || preMove < 0 ) throw new IndexOutOfBoundsException();
 
 
        this.desplazamientos = Math.abs(delta);
 
        this.inicioHueco = preMove;

    }
 
    //retorna la posicion del cursor

    public int posicionCursor() {

        return this.inicioHueco;

    }
 
 
    //retorna el elemento en la posicion logica index mediante la formula de traduccion sin recorrer la estructura

    //note: probablemente if index > posicion de cursor sumarle el tamanho del hueco

    public E get(int index) {

        if ( index > this.size() - 1 || index < 0) throw new IndexOutOfBoundsException();
 
        if ( index < inicioHueco) return this.datos[index];

        int tamanhoHueco = this.finHueco - this.inicioHueco;

        return this.datos[tamanhoHueco + index];

    }
 
 
    //reemplaza el elemento en la posicion logica index y retorna el anterior. Lanza una exception no chequeada si index fuera de los limites

    //IndexOutOfBoundsException probably

    public E set(E obj, int index) {

        if ( index > this.size() - 1 || index < 0) throw new IndexOutOfBoundsException();
 
        E anterior = this.datos[index];

        this.datos[index] = obj;

        return anterior;

    }
 
 
    //retorna la cantidad de elementos guardados

    public int size() {

        int tamanhoHueco = this.finHueco - this.inicioHueco;

        return this.datos.length - tamanhoHueco;

    }
 
 
    public int capacidad() {

        return datos.length;

    }
 
 
    //retorna el tamanho actual de desplazamientos

    public long desplazamientos() {
 
        return this.desplazamientos;

    }
 
 
    public void reiniciarDesplazamientos() {

        this.desplazamientos = 0;

    }
 
    @Override

    public Iterator<E> iterator() {

        return new BufferIterator();

    }
 
 
    private class BufferIterator implements Iterator<E>{
 
        private int index;

        private final E[] snapshot = datos;

        private final int huecoSize = finHueco - inicioHueco;

        private final int size = size();

        public BufferIterator(){

            this.index = 0;

        }

        @Override

        public boolean hasNext() {

            return index < size();

        }
 
        @Override

        public E next() {

            E next = null;

            if(index<inicioHueco)

                return snapshot[index++];

            return snapshot[huecoSize + index];

        }

    }
 
 
    @Override

    public String toString() {

        //TODO devolver el contenido en orden logico con ``

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append('`');

        for (E d : this){

            stringBuilder.append(d);

        }

        stringBuilder.append('`');

        return stringBuilder.toString();

    }
 
    public static void main(String[] args){

        BufferGap<Character> buff = new BufferGap<>();

        buff.insertar('e');

        buff.insertar('l');

        buff.insertar('i');

        buff.insertar('a');

        buff.insertar('s');


        System.out.println(buff);

    }
 
}
 
 
class BufferVacioException extends Exception {
 
    public BufferVacioException() {

        super();

    }
 
    public BufferVacioException(String message) {

        super(message);

    }

}
 


