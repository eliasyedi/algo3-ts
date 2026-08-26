import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ComandoInsertar implements Comando {

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
