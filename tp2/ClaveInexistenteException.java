package tp2;

//usuario no tiene manejo de la estructura interna, por ende se levante exception para indicar que no existe la clave
public class ClaveInexistenteException extends Exception {


    public ClaveInexistenteException() {
        super();
    }

    public ClaveInexistenteException(String message) {
        super(message);
    }

}
