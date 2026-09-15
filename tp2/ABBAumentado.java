package tp2;

import java.util.Iterator;

public class ABBAumentado<K extends Comparable<? super K>, V> implements Iterable<K> {
    private Nodo<K, V> raiz;
    private long visitas;


    public static class Nodo<K extends Comparable<? super K>, V> {
        Nodo<K, V> padre;
        Nodo<K, V> izq;
        Nodo<K, V> der;
        K clave;
        V valor;
        int tamanho;
        int altura;


        public Nodo(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
            this.tamanho = 1;
            this.altura = 0;
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
            if (actual == null) {
                //insertar
                if (clave.compareTo(previo.clave) > 0) {
                    previo.der = nodo;
                    nodo.padre = previo;
                } else {
                    previo.izq = nodo;
                    nodo.padre = previo;
                }
                inserted = true;
                //actualizar antecesores
                actualizarAntecesores(nodo);
            } else if (actual.clave.equals(clave)) {
                this.visitas++;
                actual.valor = valor;
                inserted = true;
            } else if (clave.compareTo(actual.clave) > 0) {
                this.visitas++;
                previo = actual;
                actual = actual.der;
            } else {
                this.visitas++;
                previo = actual;
                actual = actual.izq;
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

        while (actual != null ){
            this.visitas++;
            if(clave.equals(actual.clave)) {
                break;
            }
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
            if (nodoAuxiliar.izq != null) {
                nodoAuxiliar.tamanho = nodoAuxiliar.tamanho + nodoAuxiliar.izq.tamanho;
            }
            if (nodoAuxiliar.der != null) {
                nodoAuxiliar.tamanho = nodoAuxiliar.tamanho + nodoAuxiliar.der.tamanho;
            }
            nodoAuxiliar.altura = 1 + Math.max(altura(nodoAuxiliar.izq), altura(nodoAuxiliar.der));
            this.visitas++;
            nodoAuxiliar = nodoAuxiliar.padre;
        }
    }


    //Retorna el valor. Lanza ClaveInexistenteException si
    //no está.
    public V obtener(K clave) throws ClaveInexistenteException {
        if (clave == null) throw new ClaveNulaException();
        Nodo<K, V> actual = this.raiz;
        while (actual != null) {
            this.visitas++;
            if (clave.equals(actual.clave)) return actual.valor;
            if (clave.compareTo(actual.clave) > 0) actual = actual.der;
            else actual = actual.izq;
        }
        throw new ClaveInexistenteException();
    }


    public boolean contiene(K clave) {
        if (clave == null) throw new ClaveNulaException();
        Nodo<K, V> actual = this.raiz;
        while (actual != null) {
            this.visitas++;
            if (clave.equals(actual.clave)) return true;
            if (clave.compareTo(actual.clave) > 0) actual = actual.der;
            else actual = actual.izq;
        }
        return false;
    }


    //[1,n] 1 minimo y el maximo posible es size()
    public K kEsimo(int k) throws IndiceFueraDeRangoException {
        if (k < 1 || k > size()) throw new IndiceFueraDeRangoException();

        Nodo<K, V> actual = this.raiz;
        int l;
        while (actual != null) {
            this.visitas++;
            l = tamano(actual.izq);
            if (k == l + 1) return actual.clave;
            else if (k <= l) {
                actual = actual.izq;
            } else if (k > l + 1) {
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
            this.visitas++;
            if (clave.compareTo(actual.clave) <= 0) actual = actual.izq;
            else {
                cuantosMenores += tamano(actual.izq) + 1;
                actual = actual.der;
            }
        }
        return cuantosMenores;
    }


    //guess like tamanho de raiz menos tamanho de arbol izquierdo menos tamanho de arbol derecho
    public int consultarRango(K a, K b) {
        if (a == null) throw new ClaveNulaException();
        if (b == null) throw new ClaveNulaException();
        if (a.compareTo(b) > 0) throw new RangoInvalidoException();

        int menoresA = cuantosMenores(a);
        int menoresB = cuantosMenores(b);

        if (contiene(b)) {
            menoresB++;
        }
        return menoresB - menoresA;
    }


    public int consultarRangoIngenuo(K a, K b) {

        if (a == null) throw new ClaveNulaException();
        if (b == null) throw new ClaveNulaException();
        if (a.compareTo(b) > 0) throw new RangoInvalidoException();
        int contadorIntervalo = 0;
        for (K key : this) {
            if (a.compareTo(key) <= 0 && b.compareTo(key) >= 0) contadorIntervalo++;
        }
        return contadorIntervalo;
    }


    public int rango(K clave) throws ClaveInexistenteException {
        if (!contiene(clave)) throw new ClaveInexistenteException();
        return cuantosMenores(clave) + 1;

    }

    public K sucesor(K clave) throws ClaveInexistenteException {
        if (!contiene(clave)) throw new ClaveInexistenteException();

        Nodo<K, V> actual = this.raiz;
        Nodo<K, V> sucesor = null;
        while (actual != null) {
            if (clave.compareTo(actual.clave) < 0) {
                sucesor = actual;
                actual = actual.izq;
                this.visitas++;
            } else {
                this.visitas++;
                actual = actual.der;
            }
        }
        return sucesor != null ? sucesor.clave : null;
    }


    public K predecesor(K clave) throws ClaveInexistenteException {

        if (!contiene(clave)) throw new ClaveInexistenteException();

        Nodo<K, V> actual = this.raiz;
        Nodo<K, V> predecesor = null;
        while (actual != null) {
            if (clave.compareTo(actual.clave) > 0) {
                this.visitas++;
                predecesor = actual;
                actual = actual.der;
            } else {
                this.visitas++;
                actual = actual.izq;
            }
        }
        return predecesor != null ? predecesor.clave : null;
    }

    private int tamano(Nodo<K, V> nodo) {
        return nodo == null ? 0 : nodo.tamanho;
    }

    public int size() {
        return tamano(this.raiz);
    }

    public int altura() {
        return altura(this.raiz);
    }


    public long visitas() {
        return this.visitas;
    }

    public void reiniciarVisitas() {
        this.visitas = 0;
    }

    public boolean tamanosConsistentes() {
        Nodo<K, V> actual = this.raiz;
        Nodo<K, V>[] pila = (Nodo<K, V>[]) new Nodo[size()];
        int tope = 0;

        while (actual != null) {
            pila[tope++] = actual;
            actual = actual.izq;
        }
        //revisa invariante
        while (tope > 0) {
            actual = pila[--tope];
            if (actual.tamanho != 1 + tamano(actual.izq) + tamano(actual.der)) return false;

            actual = actual.der;
            while (actual != null) {
                pila[tope++] = actual;
                actual = actual.izq;
            }
        }
        return true;
    }


    public Iterator<K> iterator() {
        return new ABBAumentadoIterator();
    }


    private class ABBAumentadoIterator implements Iterator<K> {


        Nodo<K, V>[] pila;
        int tope;

        public ABBAumentadoIterator() {
            pila = (Nodo<K, V>[]) new Nodo[size()];
            tope = 0;
            Nodo<K, V> actual = raiz;
            while (actual != null) {
                pila[tope++] = actual;
                actual = actual.izq;
            }
        }

        @Override

        public boolean hasNext() {
            return tope > 0;
        }

        @Override
        public K next() {
            //se podria validar que haya otro no? o tirar exception
            Nodo<K, V> ret = pila[--tope];

            //cargar lo que hay a la derecha en la pila
            Nodo<K, V> actual = ret.der;

            while (actual != null) {
                pila[tope++] = actual;
                actual = actual.izq;
            }


            return ret.clave;

        }

    }


    private int altura(Nodo<K, V> nodo) {
        return nodo == null ? -1 : nodo.altura;
    }


    public static void main(String[] args) {

    }


}


