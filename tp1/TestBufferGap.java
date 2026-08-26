import java.util.Random;

public class TestBufferGap {

    //1
    public static void testTablas() throws BufferVacioException {

        BufferGap<Character> bufferGap = new BufferGap<>();

        bufferGap.insertar('H');
        System.out.println(bufferGap);
        bufferGap.insertar('O');
        System.out.println(bufferGap);
        bufferGap.insertar('L');
        System.out.println(bufferGap);
        bufferGap.insertar('A');
        System.out.println(bufferGap);

        bufferGap.moverCursor(-2);
        System.out.println(bufferGap);

        bufferGap.insertar('X');
        System.out.println(bufferGap);
        bufferGap.get(4);
        System.out.println(bufferGap);
        bufferGap.borrar();
        System.out.println(bufferGap);

    }

    //2
    public static void testRamdomCharacters() {

        BufferGap<Character> buffer = new BufferGap<>();
        Random random = new Random();
        Character[] chars = new Character[100000];

        for (int i = 0; i < 100000; i++) {
            chars[i] = (char) ('a' + random.nextInt(26));
        }

        for (Character c : chars) {
            buffer.insertar(c);
        }

        //assertions
//        assert chars.length == buffer.size();
        System.out.println("longitud the input -> " + chars.length + " longitud de bufferGap -> " + buffer.size());

        int i = 0;
        for (Character c : chars) {
//            assert chars[i] == c;
            if (chars[i] != c) {
                System.out.println(chars[i] + " != " + c);
            }
            i++;
        }


    }

    public static void testDesplazamientos() {
        System.out.println("\n--- Conteo de desplazamientos en el medio ---");

        final int CANTIDAD_INSERTAR = 10000;

        for (int n = 100000; n <= 1000000; n += 100000) {

            BufferGap<Character> buffer = new BufferGap<>();

            for (int i = 0; i < n; i++) {
                buffer.insertar((char) ('a' + (i % 26)));
            }

            buffer.moverCursor(-(n / 2));

            buffer.reiniciarDesplazamientos();

            for (int i = 0; i < CANTIDAD_INSERTAR; i++) {
                buffer.insertar((char) ('a' + (i % 26)));
            }

            System.out.println(
                    "n = " + n +
                            " | BufferGap desplazamientos = " +
                            buffer.desplazamientos()
            );
        }
    }





    public static void testDesplazamientoIngenuo(){
        final int CANTIDAD_INSERTAR = 10000;

        System.out.println("\n--- Comparación de desplazamientos ---");

        for (int n = 100000; n <= 1000000; n += 100000) {

            // =========================
            // BufferGap
            // =========================

            BufferGap<Character> bufferGap = new BufferGap<>();

            for (int i = 0; i < n; i++) {
                bufferGap.insertar((char) ('a' + (i % 26)));
            }

            bufferGap.moverCursor(-(n / 2));
            bufferGap.reiniciarDesplazamientos();

            for (int i = 0; i < CANTIDAD_INSERTAR; i++) {
                bufferGap.insertar((char) ('a' + (i % 26)));
            }

            long desplazamientosGap = bufferGap.desplazamientos();

            BufferIngenuo<Character> ingenuo = new BufferIngenuo<>();

            for (int i = 0; i < n; i++) {
                ingenuo.insertar((char) ('a' + (i % 26)));
            }

            ingenuo.moverCursor(-(n / 2));
            ingenuo.reiniciarDesplazamientos();

            for (int i = 0; i < CANTIDAD_INSERTAR; i++) {
                ingenuo.insertar((char) ('a' + (i % 26)));
            }

            long desplazamientosIngenuo = ingenuo.desplazamientos();

            System.out.println(
                    "n = " + n + "\t" +
                           "BufferGap = "+ desplazamientosGap + "\t\t" +
                            "BufferGapIngenuo = "+  desplazamientosIngenuo
            );
        }
    }


    public static void main(String[] args) throws BufferVacioException {
        testRamdomCharacters();

        testTablas();

        testDesplazamientos();

        testDesplazamientoIngenuo();

    }


}