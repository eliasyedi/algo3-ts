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
 *   Ejercicio 1 - TestArbolBMas: programa de prueba con P = 60 que imprime
 *   OK/FALLA por caso, la traza completa del «Ejemplo de traza» y
 *   verificarInvariantes() despues de cada insercion. Se completa a medida
 *   que avanzan las operaciones. Ver docs/.
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
import java.io.RandomAccessFile;

public class TestArbolBMas {

    private static final int P = 60;
    private static final int M_REF = 4;
    private static final String RUTA = "indice-test.dat";
    private static final String RUTA_REF = "indice-ref.dat";

    private static final long[] CLAVES_TRAZA =
            {50, 20, 80, 10, 30, 60, 90, 40, 70, 25, 35, 45, 15};

    private static final String ARBOL_FINAL =
            "1: #8(50)\n"
            + "2: #3(20,30,40) | #7(80)\n"
            + "3: #1[10,15] | #9[20,25] | #5[30,35] | #6[40,45] | #2[50,60,70] | #4[80,90]";

    private static int casos;
    private static int fallas;

    public static void main(String[] args) throws IOException {
        System.out.println("TestArbolBMas  -  P = " + P + ", posicion = 10 * clave");

        caso1ArbolVacio();
        Traza t = imprimirTraza();
        caso2SinDivision(t);
        caso3DivisionDeHoja(t);
        caso4DivisionDeInterno(t);
        caso5DivisionDeRaiz(t);
        caso6TresNiveles(t);
        caso7PrimeraYUltima();
        caso8Inexistente();
        caso9RangoEnUnaHoja();
        caso10RangoVariasHojas();
        caso11CerrarYReabrir();
        caso12BusquedasDespuesDeReabrir();

        pruebasAdicionales();

        System.out.println();
        System.out.println("casos: " + casos + "   fallas: " + fallas);
        new File(RUTA).delete();
        new File(RUTA_REF).delete();
        if (fallas > 0) {
            System.exit(1);
        }
    }


    private static class Traza {
        final long[] lecturas = new long[CLAVES_TRAZA.length];
        final long[] escrituras = new long[CLAVES_TRAZA.length];
        final int[] paginas = new int[CLAVES_TRAZA.length];
        final int[] niveles = new int[CLAVES_TRAZA.length];
        final String[] arbol = new String[CLAVES_TRAZA.length];
        final String[] cadena = new String[CLAVES_TRAZA.length];
        final String[] invariantes = new String[CLAVES_TRAZA.length];
    }

    private static Traza imprimirTraza() throws IOException {
        titulo("TRAZA COMPLETA  (volcarArbol, R, W, paginas e invariantes por insercion)");
        Traza t = new Traza();

        try (ArbolBMas a = ArbolBMas.crear(RUTA, P)) {
            System.out.println("  crear                      pags=" + a.cantidadPaginas()
                    + "  " + a.verificarInvariantes());
            System.out.println("    " + a.volcarArbol());

            for (int k = 0; k < CLAVES_TRAZA.length; k++) {
                long clave = CLAVES_TRAZA[k];
                a.reiniciarContadores();
                boolean nueva = a.insertar(clave, clave * 10);

                t.lecturas[k] = a.paginasLeidas();
                t.escrituras[k] = a.paginasEscritas();
                t.paginas[k] = a.cantidadPaginas();
                t.niveles[k] = a.niveles();
                t.arbol[k] = a.volcarArbol();
                t.cadena[k] = cadenaDeHojas(a);
                t.invariantes[k] = a.verificarInvariantes();

                System.out.printf("  insertar(%2d)  R=%d W=%d  pags=%2d  %s%n",
                        clave, t.lecturas[k], t.escrituras[k], t.paginas[k], t.invariantes[k]);
                for (String linea : t.arbol[k].split("\n")) {
                    System.out.println("    " + linea);
                }
                System.out.println("    cadena: " + t.cadena[k]);

                verificar("insertar(" + clave + ") devuelve true", nueva, nueva);
                verificar("insertar(" + clave + ") deja los invariantes en OK",
                        "OK".equals(t.invariantes[k]), t.invariantes[k]);
            }
        }
        return t;
    }


    private static void caso1ArbolVacio() throws IOException {
        titulo("CASO 1 - arbol inicialmente vacio");
        try (ArbolBMas a = ArbolBMas.crear(RUTA_REF, P)) {
            verificar("niveles() = 1", a.niveles() == 1, a.niveles());
            verificar("raiz() = 1", a.raiz() == 1, a.raiz());
            verificar("cantidadPaginas() = 2", a.cantidadPaginas() == 2, a.cantidadPaginas());
            verificar("cantidadClaves() = 0", a.cantidadClaves() == 0, a.cantidadClaves());
            verificar("los contadores arrancan en 0",
                    a.paginasLeidas() == 0 && a.paginasEscritas() == 0,
                    a.paginasLeidas() + "/" + a.paginasEscritas());
            verificar("el archivo mide 2 * P = 120 bytes",
                    new File(RUTA_REF).length() == 2L * P, new File(RUTA_REF).length());
            verificar("buscar(7) sobre el vacio = -1", a.buscar(7) == -1, a.buscar(7));
            verificarTexto("volcarArbol", "1: #1[]", a.volcarArbol());
            verificarTexto("volcarPagina(1)", "#1 H n=0 sig=-1 []", a.volcarPagina(1));
            verificarTexto("verificarInvariantes", "OK", a.verificarInvariantes());
        }
    }

    private static void caso2SinDivision(Traza t) {
        titulo("CASO 2 - inserciones sin division (50, 20, 80)");
        verificarTexto("despues de 50", "1: #1[50]", t.arbol[0]);
        verificarTexto("despues de 20", "1: #1[20,50]", t.arbol[1]);
        verificarTexto("despues de 80", "1: #1[20,50,80]", t.arbol[2]);
        for (int k = 0; k < 3; k++) {
            long clave = CLAVES_TRAZA[k];
            verificar("insertar(" + clave + ") lee 1 pagina", t.lecturas[k] == 1, t.lecturas[k]);
            verificar("insertar(" + clave + ") escribe 1 pagina (W = 2*0 + 1)",
                    t.escrituras[k] == 1, t.escrituras[k]);
            verificar("insertar(" + clave + ") no agrega paginas", t.paginas[k] == 2, t.paginas[k]);
            verificar("el arbol sigue de 1 nivel", t.niveles[k] == 1, t.niveles[k]);
        }
    }

    private static void caso3DivisionDeHoja(Traza t) {
        titulo("CASO 3 - division de hoja: la raiz hoja (10) y una hoja intermedia (40)");

        verificarTexto("insertar(10) parte la raiz hoja",
                "1: #3(50)\n2: #1[10,20] | #2[50,80]", t.arbol[3]);
        verificar("insertar(10) escribe 3 (W = 2*1 + 1)", t.escrituras[3] == 3, t.escrituras[3]);
        verificar("insertar(10) agrega 2 paginas: la hoja #2 y la raiz #3",
                t.paginas[3] == 4, t.paginas[3]);
        verificarTexto("la cadena arranca con dos hojas", "#1 -> #2", t.cadena[3]);

        verificarTexto("insertar(40) parte una hoja del medio",
                "1: #3(30,50,80)\n2: #1[10,20] | #5[30,40] | #2[50,60] | #4[80,90]", t.arbol[7]);
        verificar("insertar(40) escribe 3", t.escrituras[7] == 3, t.escrituras[7]);
        verificarTexto("la hoja nueva #5 se reenlaza en el medio",
                "#1 -> #5 -> #2 -> #4", t.cadena[7]);
        verificar("insertar(40) no cambia la altura", t.niveles[7] == 2, t.niveles[7]);
    }

    private static void caso4DivisionDeInterno(Traza t) {
        titulo("CASO 4 - division de un nodo interno (45)");
        verificarTexto("el interno #3 se parte en #3 y #7",
                "1: #8(50)\n"
                + "2: #3(30,40) | #7(80)\n"
                + "3: #1[10,20,25] | #5[30,35] | #6[40,45] | #2[50,60,70] | #4[80,90]",
                t.arbol[11]);
        verificar("insertar(45) escribe 5 (W = 2*2 + 1)", t.escrituras[11] == 5, t.escrituras[11]);
        verificar("insertar(45) lee niveles() = 2 paginas", t.lecturas[11] == 2, t.lecturas[11]);
        verificar("insertar(45) agrega 3 paginas: #6, #7 y #8",
                t.paginas[11] == 9, t.paginas[11]);
        String nivel2 = t.arbol[11].split("\n")[1];
        verificar("el 50 subio y DESAPARECIO del nivel 2 (en un interno la clave se mueve)",
                !nivel2.contains("50"), nivel2);
        verificar("el 30, que subio de una hoja, sigue estando en su hoja (ahi se copia)",
                t.arbol[11].contains("#5[30,35]"), t.arbol[11]);
        verificarTexto("la cadena sigue completa", "#1 -> #5 -> #6 -> #2 -> #4", t.cadena[11]);
    }

    private static void caso5DivisionDeRaiz(Traza t) {
        titulo("CASO 5 - division de la raiz y creacion de una raiz nueva (45)");
        verificar("la raiz paso a ser #8", t.arbol[11].startsWith("1: #8(50)"), t.arbol[11]);
        verificar("la raiz vieja #3 sigue existiendo, no se transformo",
                t.arbol[11].contains("#3(30,40)"), t.arbol[11]);
        verificar("la raiz nueva tiene 2 hijos",
                t.arbol[11].split("\n")[1].equals("2: #3(30,40) | #7(80)"),
                t.arbol[11].split("\n")[1]);
        verificar("las paginas se asignan de abajo hacia arriba: #6, #7, #8",
                t.paginas[10] == 6 && t.paginas[11] == 9, t.paginas[10] + " -> " + t.paginas[11]);
    }

    private static void caso6TresNiveles(Traza t) {
        titulo("CASO 6 - crecimiento del arbol a tres niveles");
        verificar("antes de insertar(45) el arbol tenia 2 niveles",
                t.niveles[10] == 2, t.niveles[10]);
        verificar("despues de insertar(45) tiene 3", t.niveles[11] == 3, t.niveles[11]);
        verificar("insertar(15) ya lee 3 paginas", t.lecturas[12] == 3, t.lecturas[12]);
        verificar("insertar(15) escribe 3", t.escrituras[12] == 3, t.escrituras[12]);
        verificar("el arbol final tiene 10 paginas", t.paginas[12] == 10, t.paginas[12]);
        verificarTexto("arbol final", ARBOL_FINAL, t.arbol[12]);
        verificarTexto("cadena final", "#1 -> #9 -> #5 -> #6 -> #2 -> #4", t.cadena[12]);
    }

    private static void caso7PrimeraYUltima() throws IOException {
        titulo("CASO 7 - primera y ultima clave, y caminoBusqueda");
        try (ArbolBMas a = ArbolBMas.abrir(RUTA)) {
            a.reiniciarContadores();
            long primera = a.buscar(10);
            verificar("buscar(10) = 100 leyendo niveles() = 3 paginas",
                    primera == 100 && a.paginasLeidas() == 3,
                    primera + " / " + a.paginasLeidas());

            a.reiniciarContadores();
            long ultima = a.buscar(90);
            verificar("buscar(90) = 900 leyendo 3 paginas",
                    ultima == 900 && a.paginasLeidas() == 3, ultima + " / " + a.paginasLeidas());

            verificar("buscar no escribe nunca", a.paginasEscritas() == 0, a.paginasEscritas());

            verificarTexto("caminoBusqueda(35)", "[8, 3, 5]", textoCamino(a.caminoBusqueda(35)));
            verificarTexto("caminoBusqueda(10)", "[8, 3, 1]", textoCamino(a.caminoBusqueda(10)));
            verificarTexto("caminoBusqueda(90)", "[8, 7, 4]", textoCamino(a.caminoBusqueda(90)));

            verificar("buscar(50): la clave igual al separador de la raiz va a la derecha",
                    a.buscar(50) == 500, a.buscar(50));
            verificar("buscar(20): igual al separador de #3", a.buscar(20) == 200, a.buscar(20));
            verificar("buscar(40): igual al separador de #3", a.buscar(40) == 400, a.buscar(40));
        }
    }

    private static void caso8Inexistente() throws IOException {
        titulo("CASO 8 - busqueda de una clave inexistente");
        try (ArbolBMas a = ArbolBMas.abrir(RUTA)) {
            a.reiniciarContadores();
            long r = a.buscar(33);
            verificar("buscar(33) = -1 y cuesta un camino completo: 3 paginas",
                    r == -1 && a.paginasLeidas() == 3, r + " / " + a.paginasLeidas());

            verificar("buscar(5) = -1 (antes de la primera)", a.buscar(5) == -1, a.buscar(5));
            verificar("buscar(95) = -1 (despues de la ultima)", a.buscar(95) == -1, a.buscar(95));

            a.reiniciarContadores();
            boolean repetida = a.insertar(35, 1);
            verificar("insertar(35, 1) sobre una clave que ya esta = false, R=3, W=0",
                    !repetida && a.paginasLeidas() == 3 && a.paginasEscritas() == 0,
                    repetida + " / " + a.paginasLeidas() + " / " + a.paginasEscritas());
            verificar("y no cambia la cantidad de claves", a.cantidadClaves() == 13,
                    a.cantidadClaves());
        }
    }

    private static void caso9RangoEnUnaHoja() throws IOException {
        titulo("CASO 9 - busqueda por rango dentro de una hoja");
        try (ArbolBMas a = ArbolBMas.abrir(RUTA)) {
            rango(a, 31, 35, 1, "35", 3);
            rango(a, 35, 35, 1, "35", 3);
            rango(a, 33, 34, 0, "", 3);
            rango(a, 91, 99, 0, "", 3);
        }
    }

    private static void caso10RangoVariasHojas() throws IOException {
        titulo("CASO 10 - rango que atraviesa varias hojas, con la regla de parada");
        try (ArbolBMas a = ArbolBMas.abrir(RUTA)) {
            rango(a, 22, 62, 7, "25 30 35 40 45 50 60", 6);
            rango(a, 20, 45, 6, "20 25 30 35 40 45", 5);
            rango(a, 10, 90, 13, "10 15 20 25 30 35 40 45 50 60 70 80 90", 8);
            rango(a, Long.MIN_VALUE, Long.MAX_VALUE, 13,
                    "10 15 20 25 30 35 40 45 50 60 70 80 90", 8);

            boolean lanzo = false;
            try {
                a.buscarRango(9, 5, new Recolector());
            } catch (RangoInvalidoException e) {
                lanzo = true;
            }
            verificar("buscarRango(9, 5) lanza RangoInvalidoException", lanzo, lanzo);
        }
    }

    private static void caso11CerrarYReabrir() throws IOException {
        titulo("CASO 11 - cerrar y volver a abrir (cabecera, tamanho, archivo invalido)");
        verificar("el archivo mide cantidadPaginas * P = 10 * 60 = 600 bytes",
                new File(RUTA).length() == 600, new File(RUTA).length());

        try (ArbolBMas a = ArbolBMas.abrir(RUTA)) {
            verificar("raiz() = 8", a.raiz() == 8, a.raiz());
            verificar("niveles() = 3", a.niveles() == 3, a.niveles());
            verificar("cantidadPaginas() = 10", a.cantidadPaginas() == 10, a.cantidadPaginas());
            verificar("cantidadClaves() = 13", a.cantidadClaves() == 13, a.cantidadClaves());
            verificar("tamPagina() = 60", a.tamPagina() == 60, a.tamPagina());
            verificar("ordenInterno() = 4 y capacidadHoja() = 3",
                    a.ordenInterno() == 4 && a.capacidadHoja() == 3,
                    a.ordenInterno() + "/" + a.capacidadHoja());
            verificar("abrir no lee ninguna pagina de nodo", a.paginasLeidas() == 0,
                    a.paginasLeidas());
            verificarTexto("el arbol reabierto es el mismo", ARBOL_FINAL, a.volcarArbol());
            verificarTexto("y sus invariantes siguen en OK", "OK", a.verificarInvariantes());
        }

        verificar("abrir un archivo inexistente lanza FormatoInvalidoException",
                lanzaFormato("no-existe.dat"), "");
        verificar("MAGIC roto lanza FormatoInvalidoException", lanzaFormato(conMagicRoto()), "");
        verificar("archivo truncado lanza FormatoInvalidoException",
                lanzaFormato(truncado()), "");
        verificar("M que no corresponde a P lanza FormatoInvalidoException",
                lanzaFormato(conMMentiroso()), "");
    }

    private static void caso12BusquedasDespuesDeReabrir() throws IOException {
        titulo("CASO 12 - repetir busquedas despues de reabrir");
        try (ArbolBMas a = ArbolBMas.abrir(RUTA)) {
            a.reiniciarContadores();
            long r45 = a.buscar(45);
            long r15 = a.buscar(15);
            verificar("buscar(45) = 450 y buscar(15) = 150 con 6 paginas entre las dos",
                    r45 == 450 && r15 == 150 && a.paginasLeidas() == 6,
                    r45 + " / " + r15 + " / " + a.paginasLeidas());

            verificarTexto("caminoBusqueda(35) sigue dando lo mismo", "[8, 3, 5]",
                    textoCamino(a.caminoBusqueda(35)));
            rango(a, 22, 62, 7, "25 30 35 40 45 50 60", 6);

            for (int k = 0; k < CLAVES_TRAZA.length; k++) {
                long clave = CLAVES_TRAZA[k];
                if (a.buscar(clave) != clave * 10) {
                    verificar("buscar(" + clave + ") despues de reabrir", false, a.buscar(clave));
                    return;
                }
            }
            verificar("las 13 claves se encuentran despues de reabrir", true, "13/13");
        }
    }


    private static void pruebasAdicionales() throws IOException {
        titulo("PRUEBAS ADICIONALES (no estan entre los 12 casos)");

        try (ArbolBMas a = ArbolBMas.crear(RUTA_REF, 60)) {
            verificar("P=60 da L=3 y M=4",
                    a.capacidadHoja() == 3 && a.ordenInterno() == 4,
                    a.capacidadHoja() + "/" + a.ordenInterno());
        }
        try (ArbolBMas a = ArbolBMas.crear(RUTA_REF, 4096)) {
            verificar("P=4096 da L=255 y M=341",
                    a.capacidadHoja() == 255 && a.ordenInterno() == 341,
                    a.capacidadHoja() + "/" + a.ordenInterno());
        }
        verificar("P=20 lanza TamPaginaInvalidoException", lanzaTamPagina(20), "");
        verificar("P=43 lanza TamPaginaInvalidoException (daria L=1)", lanzaTamPagina(43), "");
        verificar("P=44 se acepta (L=2, M=3)", !lanzaTamPagina(44), "");

        construirArbolDeReferencia();
        try (ArbolBMas a = ArbolBMas.abrir(RUTA_REF)) {
            verificarTexto("volcarPagina de una hoja",
                    "#5 H n=2 sig=6 [30:300 35:350]", a.volcarPagina(5));
            verificarTexto("volcarPagina de un interno lleno",
                    "#3 I n=3 [#1 20 #9 30 #5 40 #6]", a.volcarPagina(3));
            verificarTexto("volcarPagina de la ultima hoja",
                    "#4 H n=2 sig=-1 [80:800 90:900]", a.volcarPagina(4));
            verificar("el diagnostico no toca los contadores",
                    a.paginasLeidas() == 0 && a.paginasEscritas() == 0,
                    a.paginasLeidas() + "/" + a.paginasEscritas());
        }

        verificar("el arbol construido con insertar es identico byte a byte al armado a mano",
                primerByteDistinto(RUTA, RUTA_REF) < 0,
                "primer byte distinto en el offset " + primerByteDistinto(RUTA, RUTA_REF));

        romperCadena();
        verificarDeteccion("una cadena de hojas rota");
        romperSeparador();
        verificarDeteccion("un separador que miente");
        romperCantidadClaves();
        verificarDeteccion("un total de claves que no coincide");

        try (ArbolBMas a = ArbolBMas.abrir(RUTA)) {
            EstadisticasBMas e = a.estadisticas();
            verificar("estadisticas: 6 hojas y 3 internos",
                    e.hojas == 6 && e.internos == 3, e.hojas + "/" + e.internos);
            verificar("ocupacionHojas = 13 / (6*3)",
                    Math.abs(e.ocupacionHojas - 13.0 / 18.0) < 1e-9, e.ocupacionHojas);
            verificar("ocupacionInternos = 8 / (3*4), la raiz tiene solo 2 hijos",
                    Math.abs(e.ocupacionInternos - 8.0 / 12.0) < 1e-9, e.ocupacionInternos);
            System.out.println("          " + e);
        }
    }

    private static void verificarDeteccion(String que) throws IOException {
        try (ArbolBMas a = ArbolBMas.abrir(RUTA_REF)) {
            String r = a.verificarInvariantes();
            verificar("verificarInvariantes detecta " + que, !"OK".equals(r), r);
            System.out.println("          " + r);
        }
    }


    private static class Recolector implements ConsumidorEntrada {
        private final StringBuilder claves = new StringBuilder();

        @Override
        public void aceptar(long clave, long posicion) {
            if (claves.length() > 0) {
                claves.append(' ');
            }
            claves.append(clave);
        }

        @Override
        public String toString() {
            return claves.toString();
        }
    }

    private static void rango(ArbolBMas a, long desde, long hasta, int entregadosEsperados,
                              String clavesEsperadas, int paginasEsperadas) throws IOException {
        Recolector r = new Recolector();
        a.reiniciarContadores();
        int entregados = a.buscarRango(desde, hasta, r);
        long paginas = a.paginasLeidas();

        String etiqueta = "buscarRango(" + desde + ", " + hasta + ")";
        verificarTexto(etiqueta + " entrega", clavesEsperadas, r.toString());
        verificar(etiqueta + " devuelve " + entregadosEsperados,
                entregados == entregadosEsperados, entregados);
        verificar(etiqueta + " lee " + paginasEsperadas + " paginas",
                paginas == paginasEsperadas, paginas);
    }

    private static String cadenaDeHojas(ArbolBMas a) throws IOException {
        int nro = a.caminoBusqueda(Long.MIN_VALUE)[a.niveles() - 1];
        StringBuilder s = new StringBuilder();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA, "r")) {
            while (nro != -1) {
                if (s.length() > 0) {
                    s.append(" -> ");
                }
                s.append('#').append(nro);
                raf.seek((long) nro * P + 8);
                nro = raf.readInt();
            }
        }
        return s.toString();
    }

    private static String textoCamino(int[] camino) {
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < camino.length; i++) {
            if (i > 0) {
                s.append(", ");
            }
            s.append(camino[i]);
        }
        return s.append(']').toString();
    }

    private static void construirArbolDeReferencia() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_REF, "rw")) {
            raf.setLength(10L * P);

            raf.seek(0);
            raf.writeInt(0x41454433);
            raf.writeInt(1);
            raf.writeInt(P);
            raf.writeInt(M_REF);
            raf.writeInt(3);
            raf.writeInt(8);
            raf.writeInt(10);
            raf.writeInt(3);
            raf.writeLong(13);

            escribirHoja(raf, 1, 9, new long[] {10, 15});
            escribirHoja(raf, 9, 5, new long[] {20, 25});
            escribirHoja(raf, 5, 6, new long[] {30, 35});
            escribirHoja(raf, 6, 2, new long[] {40, 45});
            escribirHoja(raf, 2, 4, new long[] {50, 60, 70});
            escribirHoja(raf, 4, -1, new long[] {80, 90});

            escribirInterno(raf, 3, new int[] {1, 9, 5, 6}, new long[] {20, 30, 40});
            escribirInterno(raf, 7, new int[] {2, 4}, new long[] {80});
            escribirInterno(raf, 8, new int[] {3, 7}, new long[] {50});
        }
    }

    private static void escribirHoja(RandomAccessFile raf, int nro, int sig, long[] claves)
            throws IOException {
        long base = (long) nro * P;
        raf.seek(base);
        raf.writeInt(0);
        raf.writeInt(claves.length);
        raf.writeInt(sig);
        for (int i = 0; i < claves.length; i++) {
            raf.seek(base + 12 + 16L * i);
            raf.writeLong(claves[i]);
            raf.writeLong(claves[i] * 10);
        }
    }

    private static void escribirInterno(RandomAccessFile raf, int nro, int[] hijos, long[] claves)
            throws IOException {
        long base = (long) nro * P;
        raf.seek(base);
        raf.writeInt(1);
        raf.writeInt(claves.length);
        raf.writeInt(-1);
        for (int i = 0; i < hijos.length; i++) {
            raf.seek(base + 12 + 4L * i);
            raf.writeInt(hijos[i]);
        }
        for (int i = 0; i < claves.length; i++) {
            raf.seek(base + 12 + 4L * M_REF + 8L * i);
            raf.writeLong(claves[i]);
        }
    }

    private static void romperCadena() throws IOException {
        construirArbolDeReferencia();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_REF, "rw")) {
            raf.seek(5L * P + 8);
            raf.writeInt(2);
        }
    }

    private static void romperSeparador() throws IOException {
        construirArbolDeReferencia();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_REF, "rw")) {
            raf.seek(3L * P + 12 + 4L * M_REF);
            raf.writeLong(12);
        }
    }

    private static void romperCantidadClaves() throws IOException {
        construirArbolDeReferencia();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_REF, "rw")) {
            raf.seek(32);
            raf.writeLong(99);
        }
    }

    private static String conMagicRoto() throws IOException {
        construirArbolDeReferencia();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_REF, "rw")) {
            raf.seek(0);
            raf.writeInt(0);
        }
        return RUTA_REF;
    }

    private static String truncado() throws IOException {
        construirArbolDeReferencia();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_REF, "rw")) {
            raf.setLength(10L * P - 1);
        }
        return RUTA_REF;
    }

    private static String conMMentiroso() throws IOException {
        construirArbolDeReferencia();
        try (RandomAccessFile raf = new RandomAccessFile(RUTA_REF, "rw")) {
            raf.seek(12);
            raf.writeInt(7);
        }
        return RUTA_REF;
    }

    private static boolean lanzaTamPagina(int tamPagina) {
        try (ArbolBMas a = ArbolBMas.crear(RUTA_REF, tamPagina)) {
            return false;
        } catch (TamPaginaInvalidoException e) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean lanzaFormato(String ruta) {
        try (ArbolBMas a = ArbolBMas.abrir(ruta)) {
            return false;
        } catch (FormatoInvalidoException e) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static int primerByteDistinto(String rutaA, String rutaB) throws IOException {
        try (RandomAccessFile a = new RandomAccessFile(rutaA, "r");
             RandomAccessFile b = new RandomAccessFile(rutaB, "r")) {
            long largo = a.length() < b.length() ? a.length() : b.length();
            for (int i = 0; i < largo; i++) {
                a.seek(i);
                b.seek(i);
                if (a.readByte() != b.readByte()) {
                    return i;
                }
            }
            return a.length() == b.length() ? -1 : (int) largo;
        }
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
