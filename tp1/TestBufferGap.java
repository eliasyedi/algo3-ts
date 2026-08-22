import java.util.Random;

public class TestBufferGap {

    //1
    public static void testTablas()throws BufferVacioException{

        BufferGap<Character> bufferGap = new BufferGap<>();

        bufferGap.insertar('H');
        System.out.println(bufferGap);
        bufferGap.insertar('O');
        System.out.println(bufferGap);
        bufferGap.insertar('L');
        System.out.println(bufferGap);
        bufferGap.insertar('A');
        bufferGap.printRawArray();
        System.out.println(bufferGap);

        bufferGap.printRawArray();
        bufferGap.moverCursor(-2);
        bufferGap.printRawArray();
        System.out.println(bufferGap);

        bufferGap.insertar('X');
        bufferGap.printRawArray();
        System.out.println(bufferGap);
        bufferGap.get(4);
        System.out.println(bufferGap);
        bufferGap.borrar();
        System.out.println(bufferGap);

    }

    //2
    public static void testRamdomCharacters(){

        BufferGap<Character> buffer = new BufferGap<>();
        Random random = new Random();
        Character[] chars = new Character[100000];

        for (int i = 0; i < 100000; i++) {
            chars[i] = (char) ('a' + random.nextInt(26));
        }

        for(Character c : chars){
            buffer.insertar(c);
        }

        //assertions
        assert chars.length == buffer.size();

        int i = 0;
        for(Character c : chars){
            assert chars[i] == c;
            i++;
        }



    }


    public static void main (String[] args) throws BufferVacioException {
        testRamdomCharacters();

        testTablas();

    }


}