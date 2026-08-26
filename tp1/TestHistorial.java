public class TestHistorial {

    public static void main(String[] args) {
        BufferGap<Character> buffer = new BufferGap<>();
        HistorialEdicion historial = new HistorialEdicion();

        // --- PREPARACIÓN DEL ESTADO INICIAL ---
        // Partiendo del buffer en el estado "HoX | la" (cursor en 3)
        buffer.insertar('H');
        buffer.insertar('o');
        buffer.insertar('X');
        buffer.insertar('l');
        buffer.insertar('a');
        buffer.moverCursor(-2);

        System.out.println("Estado inicial listo: " + buffer.toString() + "\n");
        System.out.println("#  | Operación             | Contenido  | desh. | reh.");
        System.out.println("---------------------------------------------------------");

        historial.ejecutar(new ComandoInsertar('!', buffer));
        imprimirFila(1, "Insertar('!')", buffer, historial);

        historial.ejecutar(new ComandoInsertar('?', buffer));
        imprimirFila(2, "Insertar('?')", buffer, historial);

        boolean r3 = historial.deshacer();
        imprimirFila(3, "deshacer() -> " + r3, buffer, historial);

        boolean r4 = historial.deshacer();
        imprimirFila(4, "deshacer() -> " + r4, buffer, historial);

        boolean r5 = historial.rehacer();
        imprimirFila(5, "rehacer() -> " + r5, buffer, historial);

        historial.ejecutar(new ComandoMoverCursor(buffer, -4));
        imprimirFila(6, "MoverCursor(-4)", buffer, historial);

        boolean r7 = historial.rehacer();
        imprimirFila(7, "rehacer() -> " + r7, buffer, historial);

        boolean r8 = historial.deshacer();
        imprimirFila(8, "deshacer() -> " + r8, buffer, historial);

        historial.ejecutar(new ComandoBorrar(buffer));
        imprimirFila(9, "Borrar()", buffer, historial);

        boolean r10 = historial.deshacer();
        imprimirFila(10, "deshacer() -> " + r10, buffer, historial);

        boolean r11 = historial.deshacer();
        imprimirFila(11, "deshacer() -> " + r11, buffer, historial);

        boolean r12 = historial.deshacer();
        imprimirFila(12, "deshacer() -> " + r12, buffer, historial);
    }

    private static void imprimirFila(int paso, String operacion, BufferGap<Character> buffer, HistorialEdicion historial) {
        System.out.printf("%-2d | %-21s | %-10s | %-5d | %-4d%n",
                paso, operacion, buffer.toString(), historial.sizeDeshacer(), historial.sizeRehacer());
    }
}