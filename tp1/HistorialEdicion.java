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
 *   Ejercicio 2 - HistorialEdicion: deshacer/rehacer sobre dos PilaES, con invalidacion de la pila de rehacer al ejecutar un comando nuevo.
 *
 * ---------------------------------------------------------------------
 * DECLARACION DE HONOR
 *   [PENDIENTE] Pegar aqui el texto exacto de la Declaracion de Honor
 *   entregado en la primera clase.
 * =====================================================================
 */

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

