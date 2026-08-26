public class ComandoMoverCursor implements Comando{


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

