public class PilaES<T> implements LIFO<T> {

    private Node<T> topNode = null;
    private int size = 0;

    public PilaES() {
        this.topNode = null;
        this.size = 0;
    }

    @Override
    public void apilar(T value) {
        Node<T> newNode = new Node<T>(value, topNode);
        topNode = newNode;
        size++;
    }

    @Override
    public T desapilar() {
        if (estaVacia()) {
            throw new IllegalStateException("La pila está vacía");
        }

        Node<T> popNode = topNode;
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
}

class Node<T> {

    private T value;
    private Node<T> nextNode;

    public Node(T value, Node<T> nextNode) {
        this.value = value;
        this.nextNode = nextNode;
    }

    public Node<T> getNextNode() {
        return nextNode;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void setNextNode(Node<T> nextNode) {
        this.nextNode = nextNode;
    }

    public T getValue() {
        return value;
    }
}



interface LIFO<T> {
    void apilar(T value);
    T desapilar();
    T tope();
    boolean estaVacia();
    int size();
}
