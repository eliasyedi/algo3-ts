public class HistorialEdicion implements HistorialAcciones {

    private PilaES<Comando> deshacer;
    private PilaES<Comando> rehacer;


    public HistorialEdicion() {
        this.deshacer = new PilaES<Comando>();
        this.rehacer = new PilaES<Comando>();
    }

    @Override
    public void ejecutar(Comando c) {
        c.ejecutar();
        deshacer.apilar(c);
        rehacer = new PilaES<Comando>();
    }

    @Override
    public boolean deshacer() {
        if (sizeDeshacer() == 0)
            return false;
        Comando comando = deshacer.desapilar();
        comando.deshacer();
        rehacer.apilar(comando);
        return true;
    }

    @Override
    public boolean rehacer() {
        if (sizeRehacer() == 0)
            return false;
        Comando comando = rehacer.desapilar();
        comando.ejecutar();
        deshacer.apilar(comando);
        return true;
    }

    @Override
    public int sizeDeshacer() {
        return deshacer.size();
    }

    @Override
    public int sizeRehacer() {
        return rehacer.size();
    }
}

interface HistorialAcciones {
    void ejecutar(Comando c);
    boolean deshacer();
    boolean rehacer();
    int sizeDeshacer();
    int sizeRehacer();
}

