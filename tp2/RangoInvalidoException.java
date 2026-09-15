package tp2;



//la validacion de los inputs deberia recaer al usuario
public class RangoInvalidoException extends RuntimeException {


    public RangoInvalidoException() {
        super();
    }

    public RangoInvalidoException(String message) {
        super(message);
    }

}
