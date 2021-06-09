import java.nio.ByteBuffer;

public class Caster {
    public static byte[] CharsToBytes (char[] chars) {
        ByteBuffer buffer = ByteBuffer.allocate(chars.length * Character.BYTES);
        buffer.asCharBuffer().put(chars);
        byte[] bytes = new byte[chars.length * Character.BYTES];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = buffer.get(i);
        }
        return bytes;
    }
    public static byte[] ShortsToBytes (short[] shorts) {
        ByteBuffer buffer = ByteBuffer.allocate(shorts.length * Short.BYTES);
        buffer.asShortBuffer().put(shorts);
        byte[] bytes = new byte[shorts.length * Short.BYTES];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = buffer.get(i);
        }
        return bytes;
    }
    public static short[] bytesToShorts (byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        short[] shorts = new short[bytes.length / Short.BYTES];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = buffer.asShortBuffer().get(i);
        }
        return shorts;
    }
    public static char[] bytesToChars (byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        char[] chars = new char[bytes.length / Character.BYTES];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = buffer.asCharBuffer().get(i);
        }
        return chars;
    }
}
