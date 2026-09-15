package tp2;

//el usuario deberia de hacer los checks correspondientes del input
public class ClaveNulaException extends RuntimeException {


    public ClaveNulaException() {
        super();
    }

    public ClaveNulaException(String message) {
        super(message);
    }

}
