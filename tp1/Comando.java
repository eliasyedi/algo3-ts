/*
 * =====================================================================
 * Algoritmos y Estructura de Datos III - Anho 2026, 2do Periodo
 * Trabajo Practico 1 - U1 (POO y TAD en Java)
 *
 * Grupo: g_ts5                                          Seccion: TS
 *
 * Integrantes:
 *   - Figueredo Pistilli, Aurelio        - CIC: 4.010.315 - Seccion: TS
 *   - Olmedo Echeverria, Elias Ruben     - CIC: 4.653.503 - Seccion: TS
 *
 * Tarea:
 *   Ejercicio 2 - Interface Comando y los comandos concretos ComandoInsertar, ComandoBorrar y ComandoMoverCursor, cada uno con el estado minimo para revertirse.
 *
 * ---------------------------------------------------------------------
 * DECLARACION DE HONOR
 *   Nosotros, Aurelio Figueredo Pistilli y Elias Ruben Olmedo Echeverria:
 *
 *   - No hemos discutido el codigo fuente de nuestra tarea con ningun otro
 *     grupo, solo con el Profesor o el AER.
 *   - No hemos usado codigo obtenido de otro estudiante o de cualquier otra
 *     fuente no autorizada, modificada o no modificada.
 *   - Cualquier codigo o documentacion utilizada en nuestro programa obtenido
 *     de fuentes, tales como libros o notas de curso, ha sido claramente
 *     indicada en nuestra tarea.
 * =====================================================================
 */

public interface Comando {
    void ejecutar() throws BufferVacioException;
    void deshacer() throws BufferVacioException;
    String descripcion();
}

class ComandoBorrar implements Comando {

    private Character character;
    private BufferGap<Character> bufferGap;

    public ComandoBorrar( BufferGap<Character> bufferGap) {
        this.bufferGap = bufferGap;
    }

    @Override
    public void ejecutar() throws BufferVacioException {
        character = bufferGap.borrar();
    }

    @Override
    public void deshacer() {
        bufferGap.insertar(character);
    }

    @Override
    public String descripcion() {
        return character == null ? "Borrar()" : "Borrar('" + character + "')";
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
    public void deshacer() throws BufferVacioException {
        bufferGap.borrar();
    }

    @Override
    public String descripcion() {
        return "Insertar('" + character + "')";
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
        return "MoverCursor(" + delta + ")";
    }
}

