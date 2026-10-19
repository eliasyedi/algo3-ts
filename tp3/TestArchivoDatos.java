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
 *   Ejercicio 2 - TestArchivoDatos: prueba el formato de registro de
 *   datos.dat, las posiciones que devuelve agregar, la lectura aleatoria,
 *   la busqueda secuencial y sus contadores. Imprime OK/FALLA por caso.
 *   Ver docs/tp3/08-archivo-datos.md.
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TestArchivoDatos {

    private static final String RUTA = "datos-test.dat";

    private static final String[] LINEAS = {
        "tt0000001\tshort\tCarmencita\t1894\t1\tDocumentary,Short",
        "tt0000002\tshort\tLe clown et ses chiens\t1892\t5\tAnimation,Short",
        "tt0000003\tshort\tPauvre Pierrot\t1892\t4\tAnimation,Comedy,Romance",
        "tt0000004\tmovie\tAmelie y el Tokio de ayer\t2001\t122\tComedy,Romance",
        "tt0000005\tshort\tBlacksmith Scene\t1893\t1\tComedy,Short"
    };
    private static final long[] CLAVES = {1, 2, 3, 4, 5};

    private static int casos;
    private static int fallas;

    public static void main(String[] args) throws IOException {
        casoFormatoYPosiciones();
        casoLecturaAleatoria();
        casoUtf8();
        casoBusquedaSecuencial();
        casoReabrir();
        casoErrores();

        System.out.println();
        System.out.println("casos: " + casos + "   fallas: " + fallas);
        new File(RUTA).delete();
        if (fallas > 0) {
            System.exit(1);
        }
    }

    private static void casoFormatoYPosiciones() throws IOException {
        titulo("CASO 1 - formato del registro y posiciones que devuelve agregar");
        long[] posiciones = new long[LINEAS.length];
        long esperada = 0;

        try (ArchivoDatos d = ArchivoDatos.crear(RUTA)) {
            for (int i = 0; i < LINEAS.length; i++) {
                posiciones[i] = d.agregar(CLAVES[i], LINEAS[i]);
                verificar("la posicion del registro " + i + " es " + esperada,
                        posiciones[i] == esperada, posiciones[i]);
                esperada += 12 + LINEAS[i].getBytes(StandardCharsets.UTF_8).length;
            }
            verificar("tamano() = suma de 12 + largo de cada registro",
                    d.tamano() == esperada, d.tamano());
            verificar("cantidadRegistros() = " + LINEAS.length,
                    d.cantidadRegistros() == LINEAS.length, d.cantidadRegistros());
            verificar("agregar no cuenta como registro leido",
                    d.registrosLeidos() == 0, d.registrosLeidos());
        }

        verificar("el archivo en disco mide lo mismo que tamano()",
                new File(RUTA).length() == esperada, new File(RUTA).length());
    }

    private static void casoLecturaAleatoria() throws IOException {
        titulo("CASO 2 - leer(pos) devuelve la linea entera");
        long[] posiciones = construir();

        try (ArchivoDatos d = ArchivoDatos.abrir(RUTA)) {
            d.reiniciarContadores();
            for (int i = LINEAS.length - 1; i >= 0; i--) {
                verificarTexto("leer el registro " + i + " salteado", LINEAS[i],
                        d.leer(posiciones[i]));
            }
            verificar("cada leer cuenta un registro leido",
                    d.registrosLeidos() == LINEAS.length, d.registrosLeidos());

            d.reiniciarContadores();
            verificar("reiniciarContadores deja el contador en 0",
                    d.registrosLeidos() == 0, d.registrosLeidos());
            verificar("claveEn devuelve la clave sin leer el texto",
                    d.claveEn(posiciones[3]) == CLAVES[3], d.claveEn(posiciones[3]));
        }
    }

    private static void casoUtf8() throws IOException {
        titulo("CASO 3 - el largo se mide en BYTES, no en caracteres");
        String conAcentos = "tt9999999\tmovie\tAmelie de Montmartre - Tokio\t2001\t122\tComedy";
        String multibyte = "tt9999998\tmovie\t東京物語 Amelie\t1953\t136\tDrama";

        verificar("la linea multibyte tiene mas bytes que caracteres",
                multibyte.getBytes(StandardCharsets.UTF_8).length > multibyte.length(),
                multibyte.length() + " chars / "
                        + multibyte.getBytes(StandardCharsets.UTF_8).length + " bytes");

        long p1;
        long p2;
        try (ArchivoDatos d = ArchivoDatos.crear(RUTA)) {
            p1 = d.agregar(9999999, conAcentos);
            p2 = d.agregar(9999998, multibyte);
            verificar("la segunda posicion usa el largo en bytes de la primera",
                    p2 == 12 + conAcentos.getBytes(StandardCharsets.UTF_8).length, p2);
        }

        try (ArchivoDatos d = ArchivoDatos.abrir(RUTA)) {
            verificarTexto("vuelve igual la linea con acentos", conAcentos, d.leer(p1));
            verificarTexto("vuelve igual la linea multibyte", multibyte, d.leer(p2));
        }
    }

    private static void casoBusquedaSecuencial() throws IOException {
        titulo("CASO 4 - busquedaSecuencial y lo que cuesta");
        long[] posiciones = construir();

        try (ArchivoDatos d = ArchivoDatos.abrir(RUTA)) {
            d.reiniciarContadores();
            long p = d.busquedaSecuencial(1);
            verificar("la primera clave se encuentra leyendo 1 registro",
                    p == posiciones[0] && d.registrosLeidos() == 1,
                    p + " / " + d.registrosLeidos());

            d.reiniciarContadores();
            p = d.busquedaSecuencial(5);
            verificar("la ultima clave cuesta los 5 registros",
                    p == posiciones[4] && d.registrosLeidos() == 5,
                    p + " / " + d.registrosLeidos());

            d.reiniciarContadores();
            p = d.busquedaSecuencial(777);
            verificar("una clave inexistente da -1 y recorre el archivo entero",
                    p == -1 && d.registrosLeidos() == LINEAS.length,
                    p + " / " + d.registrosLeidos());

            d.reiniciarContadores();
            verificarTexto("la posicion que devuelve sirve para leer", LINEAS[2],
                    d.leer(d.busquedaSecuencial(3)));
        }
    }

    private static void casoReabrir() throws IOException {
        titulo("CASO 5 - cerrar y volver a abrir");
        long[] posiciones = construir();
        long tamano = new File(RUTA).length();

        try (ArchivoDatos d = ArchivoDatos.abrir(RUTA)) {
            verificar("tamano() se recupera del archivo", d.tamano() == tamano, d.tamano());
            for (int i = 0; i < LINEAS.length; i++) {
                if (!LINEAS[i].equals(d.leer(posiciones[i]))) {
                    verificar("el registro " + i + " sobrevive al cierre", false,
                            d.leer(posiciones[i]));
                    return;
                }
            }
            verificar("los " + LINEAS.length + " registros sobreviven al cierre", true,
                    LINEAS.length + "/" + LINEAS.length);
        }
    }

    private static void casoErrores() throws IOException {
        titulo("CASO 6 - errores");
        construir();

        boolean lanzo = false;
        try (ArchivoDatos d = ArchivoDatos.abrir(RUTA)) {
            d.agregar(99, "no deberia poder");
        } catch (IllegalStateException e) {
            lanzo = true;
        }
        verificar("agregar sobre un archivo abierto para leer lanza IllegalStateException",
                lanzo, lanzo);

        lanzo = false;
        try (ArchivoDatos d = ArchivoDatos.abrir(RUTA)) {
            d.leer(new File(RUTA).length() - 2);
        } catch (FormatoInvalidoException e) {
            lanzo = true;
        }
        verificar("leer una posicion que no entra en el archivo lanza FormatoInvalidoException",
                lanzo, lanzo);

        lanzo = false;
        try (ArchivoDatos d = ArchivoDatos.abrir("no-existe-datos.dat")) {
            d.tamano();
        } catch (FormatoInvalidoException e) {
            lanzo = true;
        }
        verificar("abrir un archivo inexistente lanza FormatoInvalidoException", lanzo, lanzo);
    }

    private static long[] construir() throws IOException {
        long[] posiciones = new long[LINEAS.length];
        try (ArchivoDatos d = ArchivoDatos.crear(RUTA)) {
            for (int i = 0; i < LINEAS.length; i++) {
                posiciones[i] = d.agregar(CLAVES[i], LINEAS[i]);
            }
        }
        return posiciones;
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println(texto);
    }

    private static void verificarTexto(String descripcion, String esperado, String obtenido) {
        casos++;
        if (esperado.equals(obtenido)) {
            System.out.println("  OK    " + descripcion);
        } else {
            fallas++;
            System.out.println("  FALLA " + descripcion);
            System.out.println("          esperado: " + esperado);
            System.out.println("          obtenido: " + obtenido);
        }
    }

    private static void verificar(String descripcion, boolean condicion, Object obtenido) {
        casos++;
        if (condicion) {
            System.out.println("  OK    " + descripcion);
        } else {
            fallas++;
            System.out.println("  FALLA " + descripcion + "  (obtenido: " + obtenido + ")");
        }
    }

}
