public interface Comando {
    void ejecutar();
    void deshacer();
    String descripcion();
}

class ComandoBorrar implements Comando {

    private Character character;
    private BufferGap<Character> bufferGap;

    public ComandoBorrar( BufferGap<Character> bufferGap) {
        this.bufferGap = bufferGap;
    }

    @Override
    public void ejecutar() {
        try {
            character = bufferGap.borrar();
        } catch (BufferVacioException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deshacer() {
        bufferGap.insertar(character);
    }

    @Override
    public String descripcion() {
        return "";
    }
}

class ComandoInsertar implements Comando {

    private BufferGap<Character> bufferGap;
    private Character character;

    public ComandoInsertar(Character character, BufferGap<Character> bufferGap) {
        this.bufferGap = bufferGap;
        this.character = character;
    }

    @Override
    public void ejecutar() {
        bufferGap.insertar(character);
    }

    @Override
    public void deshacer() {
        try {
            bufferGap.borrar();
        } catch (BufferVacioException e) {

        }
    }

    @Override
    public String descripcion() {
        return "";
    }
}

class ComandoMoverCursor implements Comando {


    private BufferGap<Character> bufferGap;
    private int delta = 0;

    public ComandoMoverCursor(BufferGap<Character> bufferGap, int delta) {
        this.bufferGap = bufferGap;
        this.delta = delta;
    }

    @Override
    public void ejecutar() {
        bufferGap.moverCursor(delta);
    }

    @Override
    public void deshacer() {
        bufferGap.moverCursor(delta*-1);
    }

    @Override
    public String descripcion() {
        return "";
    }
}

