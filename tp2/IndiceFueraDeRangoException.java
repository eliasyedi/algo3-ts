package tp2;

//el usuario no tiene control sobre la estructura interna, entonces corresponde levantar exception al user
public class IndiceFueraDeRangoException extends Exception {


    public IndiceFueraDeRangoException() {
        super();
    }

    public IndiceFueraDeRangoException(String message) {
        super(message);
    }

}
