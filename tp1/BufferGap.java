import java.util.Iterator;

public class BufferGap<E> implements Iterable<E> {

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

        //todos los elementos son fisicamente movidos
        this.desplazamientos += size();

        E[] newArray = (E[]) new Object[this.datos.length * 2];

        for (int i = 0; i < this.inicioHueco; i++) {
            newArray[i] = this.datos[i];
        }


        //finHueco - 1 nunca puede ser negativo
        //hay que empujar al fin del nuevo array copiar de atras a adelante puede funcionar o
        //probablemente hace la suma del offset
        int newFinHueco = newArray.length - (size() - this.inicioHueco);


        for (int i = this.finHueco, j = newFinHueco; i < this.datos.length; i++, j++) {
            newArray[j] = this.datos[i];
        }
        this.finHueco = newFinHueco;

        this.datos = newArray;

    }


    /**
     * @return elemento borrado
     * @throws BufferVacioException exception lanzada si el cursor se encuentra en posicion 0
     */

    public E borrar() throws BufferVacioException {

        if (this.inicioHueco == 0) throw new BufferVacioException("cursor en posicion 0");

        E target = this.datos[this.inicioHueco - 1];

        //no necesario, podemos solo desplazar el inicio huevo
//        this.datos[this.inicioHueco - 1] = null;
        inicioHueco--;

        return target;

    }


    //negativo hacia la izquierda, positivo hacia la derecha

    //lanza una exception no chequeada si el lcursor quedaria fuera de [0,size()]

    public void moverCursor(int delta) {

        if (delta == 0) return;

        int absDelta = Math.abs(delta);
        int preMoveInicioHueco = this.inicioHueco + delta;
        int preMoveFinHueco = this.finHueco + delta;

        //inicio hueco solo puede llegar a la ultima celda
        // n -> tamanho de arreglo inicio hueco puede tomar valores [0,n-1] sin realizar un resize ih = n deberia
        //triggerear un resize
        if ((preMoveInicioHueco > size() || preMoveInicioHueco < 0)) throw new PosicionInvalidaException();

        if (delta < 0) {
            for (int i = 0; i < absDelta; i++) {
                this.datos[finHueco - 1 - i] = this.datos[inicioHueco - 1 - i];
                this.datos[inicioHueco - 1 - i] = null;
            }
        } else {
            for (int i = 0; i < absDelta; i++) {
                this.datos[inicioHueco + i] = this.datos[finHueco + i];
                this.datos[finHueco + i] = null;
            }
        }
        inicioHueco = preMoveInicioHueco;
        finHueco = preMoveFinHueco;
        this.desplazamientos += absDelta;

    }

    //retorna la posicion del cursor

    public int posicionCursor() {

        return this.inicioHueco;

    }


    //retorna el elemento en la posicion logica index mediante la formula de traduccion sin recorrer la estructura

    //note: probablemente if index > posicion de cursor sumarle el tamanho del hueco

    public E get(int index) {

        if (index > this.size() - 1 || index < 0) throw new PosicionInvalidaException();

        if (index < inicioHueco) return this.datos[index];

        int tamanhoHueco = this.finHueco - this.inicioHueco;

        return this.datos[tamanhoHueco + index];

    }


    //reemplaza el elemento en la posicion logica index y retorna el anterior. Lanza una exception no chequeada si index fuera de los limites


    public E set(E obj, int index) {

        if (index > this.size() - 1 || index < 0) throw new PosicionInvalidaException();
        E anterior = null;
        if (index < inicioHueco) {
            anterior = this.datos[index];
            this.datos[index] = obj;
            return anterior;
        }
        int huecoSize = finHueco - inicioHueco;
        anterior = this.datos[huecoSize + index];
        this.datos[huecoSize + index] = obj;

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


    private class BufferIterator implements Iterator<E> {

        private int index;

        private final E[] snapshot = datos;

        private final int huecoSize = finHueco - inicioHueco;

        private final int size = size();

        public BufferIterator() {

            this.index = 0;

        }

        @Override

        public boolean hasNext() {

            return index < size();

        }

        @Override

        public E next() {

            E next = null;

            if (index < inicioHueco)
                return snapshot[index++];

            return snapshot[huecoSize + index++];

        }

    }


    @Override
    public String toString() {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append('`');

        int i = 0;
        for (E d : this) {


            if (i == posicionCursor()) stringBuilder.append("|");
            stringBuilder.append(d);
            i++;

        }

        stringBuilder.append('`');

        return stringBuilder.toString();

    }


    public void printRawArray() {
        System.out.println("current capacity -> " + this.datos.length + " inicio, finHueco" + this.inicioHueco + "," + this.finHueco);
        for (int i = 0; i < this.datos.length; i++) {
            System.out.println(this.datos[i]);
        }
    }

    public static void main(String[] args) {

        BufferGap<Character> buff = new BufferGap<>();

        buff.insertar('e');
        buff.insertar('l');
        buff.insertar('i');
        buff.insertar('a');
        buff.insertar('s');
        buff.insertar(' ');
        buff.insertar('r');
        buff.insertar('u');
        buff.insertar('b');
        buff.insertar('e');
        buff.insertar('n');
        buff.insertar(' ');
        buff.insertar('o');
        buff.insertar('l');
        buff.insertar('m');
        buff.insertar('e');
        buff.insertar('d');
        buff.insertar('o');
        buff.insertar(' ');
        buff.insertar('e');
        buff.insertar('c');
        buff.insertar('h');
        buff.insertar('e');
        buff.insertar('v');
        buff.insertar('e');
        buff.insertar('r');
        buff.insertar('r');
        buff.insertar('i');
        buff.insertar('a');


        System.out.println(buff);
        buff.printRawArray();
        buff.moverCursor(-1);
        buff.printRawArray();
        buff.moverCursor(1);
        buff.printRawArray();
        buff.moverCursor(-4);
        buff.printRawArray();

//        buff.resize();
//        buff.printRawArray();

    }

}


//un comportamiento que puede ocurrir y debe ser tratado,
// como realizar un backspace y ya tener el buffer vacio
class BufferVacioException extends Exception {

    public BufferVacioException() {

        super();

    }

    public BufferVacioException(String message) {

        super(message);

    }

}

//es de este tipo porque el usuario deberia de saber los constraints del buffer
//si trata de acceder un indice fuera de lugar este deberia indicarle que es una operacion invalida
//seria mas de un error del que llama y deberia de tener en cuenta
class PosicionInvalidaException extends RuntimeException {


    public PosicionInvalidaException() {
        super();
    }

    public PosicionInvalidaException(String message) {
        super(message);
    }

}
 


