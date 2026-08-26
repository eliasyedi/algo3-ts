public class ComandoBorrar implements Comando {

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
