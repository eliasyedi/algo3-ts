/*
 * =====================================================================
 * Algoritmos y Estructura de Datos III - Anho 2026, 2do Periodo
 * Trabajo Practico 3 - U4 (Arboles B+ persistentes en disco)
 *
 * Grupo: g_ts5                                          Seccion: TS
 *
 * Integrantes:
 *   - Figueredo Pistilli, Aurelio        - CIC: 4.010.315 - Seccion: TS
 *   - Olmedo Echeverria, Elias Ruben     - CIC: 4.653.503 - Seccion: TS
 *
 * Tarea:
 *   Ejercicio 2 - ArchivoDatos: el archivo plano datos.dat, con los
 *   registros de longitud variable uno detras de otro. No es un arbol: el
 *   indice guarda <clave, posicion> y la posicion es el byte donde empieza
 *   el registro. Ver docs/tp3/08-archivo-datos.md.
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class ArchivoDatos implements AutoCloseable {

    // Cada registro es: long clave | int largo | largo bytes del texto.
    // Los 12 bytes de cabecera estan para poder saltar al registro siguiente
    // sin leer el texto.
    private static final int CAB_REGISTRO = 12;
    private static final int OFF_CLAVE = 0;
    private static final int OFF_LARGO = 8;

    private static final int BYTES_BUFFER = 1 << 16;

    private final String ruta;
    private final RandomAccessFile archivo;
    private final boolean soloLectura;

    private long finDeArchivo;
    private long cantidadRegistros;
    private long registrosLeidos;

    private boolean cerrado;

    private ArchivoDatos(String ruta, RandomAccessFile archivo, boolean soloLectura)
            throws IOException {
        this.ruta = ruta;
        this.archivo = archivo;
        this.soloLectura = soloLectura;
        this.finDeArchivo = archivo.length();
    }

    public static ArchivoDatos crear(String ruta) throws IOException {
        RandomAccessFile archivo = new RandomAccessFile(new File(ruta), "rw");
        archivo.setLength(0);
        return new ArchivoDatos(ruta, archivo, false);
    }

    // Se abre en "r": los comandos de consulta no tienen por que escribir, y
    // si un bug lo intenta preferimos que salte antes de romper el archivo.
    public static ArchivoDatos abrir(String ruta) throws IOException {
        File f = new File(ruta);
        if (!f.isFile()) {
            throw new FormatoInvalidoException("no existe el archivo " + ruta);
        }
        return new ArchivoDatos(ruta, new RandomAccessFile(f, "r"), true);
    }

    public long agregar(long clave, String linea) throws IOException {
        if (soloLectura) {
            throw new IllegalStateException(ruta + " se abrio para leer, no para agregar");
        }

        // El largo va en BYTES, no en caracteres: en UTF-8 una e con acento
        // ocupa 2 y un ideograma 3. Si guardaramos linea.length() el registro
        // siguiente arrancaria en medio del texto del anterior.
        byte[] texto = linea.getBytes(StandardCharsets.UTF_8);
        byte[] registro = new byte[CAB_REGISTRO + texto.length];
        escribirLong(registro, OFF_CLAVE, clave);
        escribirInt(registro, OFF_LARGO, texto.length);
        for (int i = 0; i < texto.length; i++) {
            registro[CAB_REGISTRO + i] = texto[i];
        }

        // Un solo write con el registro entero armado. Con writeLong/writeInt/
        // write serian tres llamadas al sistema por registro: 900 000 en vez de
        // 300 000 para el dataset.
        long posicion = finDeArchivo;
        archivo.seek(posicion);
        archivo.write(registro);
        finDeArchivo += registro.length;
        cantidadRegistros++;
        return posicion;
    }

    public String leer(long pos) throws IOException {
        if (pos < 0 || pos + CAB_REGISTRO > finDeArchivo) {
            throw new FormatoInvalidoException("posicion fuera del archivo: " + pos);
        }

        archivo.seek(pos);
        byte[] cabecera = new byte[CAB_REGISTRO];
        archivo.readFully(cabecera);

        // Se valida antes de reservar: una posicion inventada podria traer un
        // largo disparatado y un new byte[largo] de gigabytes.
        int largo = leerInt(cabecera, OFF_LARGO);
        if (largo < 0 || pos + CAB_REGISTRO + largo > finDeArchivo) {
            throw new FormatoInvalidoException(
                    "el registro en " + pos + " dice medir " + largo + " bytes");
        }
        byte[] texto = new byte[largo];
        archivo.readFully(texto);

        registrosLeidos++;
        return new String(texto, StandardCharsets.UTF_8);
    }

    public long claveEn(long pos) throws IOException {
        archivo.seek(pos);
        byte[] cabecera = new byte[CAB_REGISTRO];
        archivo.readFully(cabecera);
        registrosLeidos++;
        return leerLong(cabecera, OFF_CLAVE);
    }

    // El rival del B+: recorre desde el byte 0. Dos cosas:
    // 1) solo lee la cabecera de cada registro y saltea el texto, que no sirve
    //    para comparar;
    // 2) va por un stream con buffer y no con seek por registro, que serian dos
    //    llamadas al sistema por registro. Asi el tiempo que medimos es el del
    //    algoritmo y no el de nuestra forma de leer.
    public long busquedaSecuencial(long clave) throws IOException {
        try (DataInputStream entrada = new DataInputStream(
                new BufferedInputStream(new FileInputStream(ruta), BYTES_BUFFER))) {
            long pos = 0;
            while (true) {
                // Si no hay ni 8 bytes mas, no hay otro registro.
                long leida;
                try {
                    leida = entrada.readLong();
                } catch (EOFException fin) {
                    return -1;
                }
                int largo = entrada.readInt();
                registrosLeidos++;
                if (leida == clave) {
                    return pos;
                }
                saltar(entrada, largo);
                pos += CAB_REGISTRO + largo;
            }
        }
    }

    // skipBytes puede saltar menos de lo pedido, asi que se insiste.
    private static void saltar(DataInputStream entrada, int bytes) throws IOException {
        int faltan = bytes;
        while (faltan > 0) {
            int saltados = entrada.skipBytes(faltan);
            if (saltados <= 0) {
                throw new EOFException("el archivo termina en medio de un registro");
            }
            faltan -= saltados;
        }
    }

    public long tamano() {
        return finDeArchivo;
    }

    public long cantidadRegistros() {
        return cantidadRegistros;
    }

    public long registrosLeidos() {
        return registrosLeidos;
    }

    // agregar no cuenta: la construccion no es una consulta que se mida, igual
    // que crear en el indice.
    public void reiniciarContadores() {
        registrosLeidos = 0;
    }

    private static void escribirInt(byte[] p, int off, int v) {
        p[off]     = (byte) (v >>> 24);
        p[off + 1] = (byte) (v >>> 16);
        p[off + 2] = (byte) (v >>> 8);
        p[off + 3] = (byte) v;
    }

    private static int leerInt(byte[] p, int off) {
        return ((p[off] & 0xFF) << 24)
                | ((p[off + 1] & 0xFF) << 16)
                | ((p[off + 2] & 0xFF) << 8)
                | (p[off + 3] & 0xFF);
    }

    private static void escribirLong(byte[] p, int off, long v) {
        for (int i = 0; i < 8; i++) {
            p[off + i] = (byte) (v >>> (56 - 8 * i));
        }
    }

    private static long leerLong(byte[] p, int off) {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (p[off + i] & 0xFFL);
        }
        return v;
    }

    @Override
    public void close() throws IOException {
        if (cerrado) {
            return;
        }
        cerrado = true;
        archivo.close();
    }

}
