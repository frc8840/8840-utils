package frc.team_8840_lib.utils.buffer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ByteConversions {

    public static byte[] possibleNegDoubleTo9ByteArray(final double i) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeDouble(i);
        dos.flush();

        byte[] isNegative = new byte[1];
        //if it's negative, set the first byte to fully 1s
        if (i < 0) {
            isNegative[0] = (byte) 0xFF;
        } else {
            isNegative[0] = (byte) 0x00;
        }

        return combineByteArrays(isNegative, bos.toByteArray());
    } 

    public static byte[] doubleToByteArray(final double i) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeDouble(Math.abs(i));
        dos.flush();
        return bos.toByteArray();
    }

    public static byte[] stringToByteArray(final String i) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeUTF(i);
        dos.flush();
        return bos.toByteArray();
    }

    public static byte[] intToByteArray(final int i) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(i);
        dos.flush();
        return bos.toByteArray();
    }

    public static byte[] combineByteArrays(byte[] ...arrs) {
        int length = 0;
        for (byte[] arr : arrs) {
            length += arr.length;
        }

        byte[] result = new byte[length];
        int index = 0;
        for (byte[] arr : arrs) {
            for (byte b : arr) {
                result[index++] = b;
            }
        }

        return result;
    }

    public static byte[] combineByteArrays(byte inbetween, byte[] ...ars) {
        int length = 0;
        for (byte[] arr : ars) {
            length += arr.length;
        }

        byte[] result = new byte[length + ars.length - 1];
        int index = 0;
        for (byte[] arr : ars) {
            for (byte b : arr) {
                result[index++] = b;
            }

            if (index < result.length) {
                result[index++] = inbetween;
            }
        }

        return result;
    }

}
