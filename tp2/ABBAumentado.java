package tp2;

import java.util.Iterator;

public class ABBAumentado<K extends Comparable<? super K>, V> {
    private Nodo<K, V> raiz;
    private long visitas;


    public static class Nodo<K extends Comparable<? super K>, V> {
        Nodo<K, V> padre;
        Nodo<K, V> izq;
        Nodo<K, V> der;
        K clave;
        V valor;
        int tamanho;


        public Nodo(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
            this.tamanho = 1;
        }

        public K clave() {
            return this.clave;
        }

        public V valor() {
            return this.valor;
        }

        public int tamanho() {
            return tamanho;
        }

    }

    public ABBAumentado() {
    }


    //todo
    /*
   Inserta el par. No se admiten claves duplicadas: cada
clave ocupa un solo nodo, así que si la clave ya estaba
reemplaza el valor y no cambia la estructura ni los
tamaños. Lanza ClaveNulaException si clave es null.
     */
    public void agregar(K clave, V valor) {

        if (clave == null) throw new ClaveNulaException();

        Nodo<K, V> nodo = new Nodo<K, V>(clave, valor);
        Nodo<K, V> actual = this.raiz;
        Nodo<K, V> previo = null;
        if (raiz == null) {
            this.raiz = nodo;
            return;
        }


        boolean inserted = false;
        while (!inserted) {

            if (actual.clave.equals(clave)) {
                actual.valor = valor;
                inserted = true;
            } else if (clave.compareTo(actual.clave) > 0) {
                previo = actual;
                actual.tamanho = actual.tamanho + 1;
                actual = actual.der;
            } else {
                previo = actual;
                actual.tamanho = actual.tamanho + 1;
                actual = actual.izq;
            }

            if (actual == null) {
                if (clave.compareTo(previo.clave) > 0) {
                    previo.der = nodo;
                    nodo.padre = previo;
                    inserted = true;
                } else {
                    previo.izq = nodo;
                    nodo.padre = previo;
                    inserted = true;
                }
            }
        }

    }


    /*
    Elimina la clave y retorna su valor. Lanza la excepción
chequeada ClaveInexistenteException si no está, y
ClaveNulaException si es null.
     */
    public V eliminar(K clave) throws ClaveInexistenteException {
        if (clave == null) throw new ClaveNulaException();

        if (this.raiz == null) throw new ClaveInexistenteException();

        Nodo<K, V> actual = this.raiz;
        Nodo<K, V> previo = null;

        while (actual != null && !clave.equals(actual.clave)) {
            previo = actual;
            if (clave.compareTo(actual.clave) > 0) {
                actual = actual.der;
            } else {
                actual = actual.izq;
            }
        }
        //si el actual es null no existe entonces en el arbol la clave
        if (actual == null) throw new ClaveInexistenteException();


        V valorEliminado = actual.valor;

        //raiz caso
        if (actual.der != null && actual.izq != null) {
            Nodo<K, V> sucesor = actual.der;
            Nodo<K, V> padreSucesor;
            while (sucesor.izq != null) {
                sucesor = sucesor.izq;
            }
            padreSucesor = sucesor.padre;

            if (sucesor.padre != actual) {
                Nodo<K, V> succ = sucesor.padre;
                succ.izq = sucesor.der;

                if (sucesor.der != null) {
                    sucesor.der.padre = succ;
                }


                sucesor.der = actual.der;
                sucesor.der.padre = sucesor;
            }

            sucesor.izq = actual.izq;
            sucesor.izq.padre = sucesor;


            sucesor.padre = actual.padre;

            if (actual.padre == null) this.raiz = sucesor;
            else if (actual.padre.izq == actual) actual.padre.izq = sucesor;
            else actual.padre.der = sucesor;

            if (padreSucesor == actual) actualizarAntecesores(sucesor);
            else actualizarAntecesores(padreSucesor);

        } else {
            Nodo<K, V> hijo;
            if (actual.izq == null)
                hijo = actual.der;
            else hijo = actual.izq;

            if (actual.padre == null) {
                this.raiz = hijo;
                if (hijo != null) hijo.padre = actual.padre;
            } else {

                if (actual.padre.izq == actual)
                    actual.padre.izq = hijo;
                else actual.padre.der = hijo;

                if (hijo != null)
                    hijo.padre = actual.padre;
            }
            actualizarAntecesores(actual.padre);
        }


        return valorEliminado;

    }

    private void actualizarAntecesores(final Nodo<K, V> nodoPartida) {
        Nodo<K, V> nodoAuxiliar = nodoPartida;
        while (nodoAuxiliar != null) {
            nodoAuxiliar.tamanho = 1;
            if (nodoAuxiliar.izq != null)
                nodoAuxiliar.tamanho = nodoAuxiliar.tamanho + nodoAuxiliar.izq.tamanho;
            if (nodoAuxiliar.der != null)
                nodoAuxiliar.tamanho = nodoAuxiliar.tamanho + nodoAuxiliar.der.tamanho;
            nodoAuxiliar = nodoAuxiliar.padre;
        }
    }

//    private Nodo<K,V> successorInOrder(Nodo<K,V>)

    //Retorna el valor. Lanza ClaveInexistenteException si
    //no está.
    public V obtener(K clave) throws ClaveInexistenteException {
        if (clave == null) throw new ClaveNulaException();
        Nodo<K, V> actual = this.raiz;
        while (actual != null) {
            if(clave.equals(actual.clave) )  return actual.valor;
            if(clave.compareTo(actual.clave)>0) actual = actual.der;
            else  actual = actual.izq;
        }
        throw new ClaveInexistenteException();
    }


    public boolean contiene(K clave) {
        if (clave == null) throw new ClaveNulaException();
        Nodo<K, V> actual = this.raiz;
        while (actual != null) {
            if(clave.equals(actual.clave) )  return true;
            if(clave.compareTo(actual.clave)>0) actual = actual.der;
            else  actual = actual.izq;
        }
        return false;
    }


    //[1,n] 1 minimo y el maximo posible es size()
    public K kEsimo(int k) throws IndiceFueraDeRangoException{
        if (k < 1 || k > size()) throw new IndiceFueraDeRangoException();

        Nodo<K, V> actual = this.raiz;
        int l;
        boolean found = false;
        while(actual != null ) {
            l = tamano(actual.izq);
            if (k == l + 1) return actual.clave ;
            else if (k <= l) actual = actual.izq;
            else if (k > l+1) {
                k = k - l - 1;
                actual = actual.der;
            }
        }
        throw new IndiceFueraDeRangoException();
    }


    //Cantidad de claves estrictamente menores que clave.
    //clave no tiene por qué estar en el árbol.
    public int cuantosMenores(K clave) {
        if (clave == null) throw new ClaveNulaException();
        Nodo<K, V> actual = this.raiz;
        //size() - tamano de nodo limite
        int cuantosMenores = 0;
        while (actual != null) {
            if (clave.compareTo(actual.clave)>= 0 ) actual = actual.izq;
            else {
                cuantosMenores = tamano(actual.izq) + 1;
                actual = actual.der;
            }
        }
        return cuantosMenores;
    }


    public int consultarRango(K a, K b) {

    }


    public int consultarRangoIngenuo(K a, K b) {

    }


    public int rango(K clave) {

    }

    public K sucesor(K clave) {

    }


    public K predecesor(K clave) {

    }

    private int tamano(Nodo<K, V> nodo) {
        return nodo == null ? 0 : nodo.tamanho;
    }

    public int size() {
        return this.raiz.tamanho;
    }

    public int altura() {

    }


    public long visitas() {

    }

    public void reiniciarVisitas() {

    }

    public boolean tamanosConsistentes() {

    }


    public Iterator<K> iterator() {

    }


    public static void main(String[] args) {

    }


}


