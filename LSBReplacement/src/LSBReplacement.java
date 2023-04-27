import java.io.*;
import java.util.Arrays;

public class LSBReplacement {
    public static void main(String[] args) {
        String originalImagePath = new File("LSBReplacement\\res\\28.bmp").getAbsolutePath();
        
        try {
            // Read the original image file
            FileInputStream inputStream = new FileInputStream(originalImagePath);
            byte[] header = new byte[122];
            inputStream.read(header);
            byte[] imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
            inputStream.close();

            // ----------------- Возможные преобразования байтов
            // hex представление байта
            String byte0hex = Integer.toString((imageBytes[0] & 0xff), 16);
            String byte1hex = Integer.toString((imageBytes[1] & 0xff), 16);
            String byte2hex = Integer.toString((imageBytes[2] & 0xff), 16);
            // binary представление байта
            String byte0binary = String.format("%8s", Integer.toBinaryString(imageBytes[0] & 0xFF)).replace(' ', '0');
            String byte1binary = String.format("%8s", Integer.toBinaryString(imageBytes[1] & 0xFF)).replace(' ', '0');
            String byte2binary = String.format("%8s", Integer.toBinaryString(imageBytes[2] & 0xFF)).replace(' ', '0');
            // binary string > char array
            char[] byte0binaryArray = byte0binary.toCharArray();
            char[] byte1binaryArray = byte1binary.toCharArray();
            char[] byte2binaryArray = byte2binary.toCharArray();
            // char array > binary string
            String byte0binary2 = new String(byte0binaryArray);
            String byte1binary2 = new String(byte1binaryArray);
            String byte2binary2 = new String(byte2binaryArray);
            // binary string > byte
            byte byte0 = (byte) Integer.parseInt(byte0binary, 2);
            byte byte1 = (byte) Integer.parseInt(byte1binary, 2);
            byte byte2 = (byte) Integer.parseInt(byte2binary, 2);
            // hex > binary
            System.out.println(byte0hex + " > " + byte0binary + " > " + Integer.toString((byte0 & 0xff), 16));
            System.out.println(byte1hex + " > " + byte1binary + " > " + Integer.toString((byte1 & 0xff), 16));
            System.out.println(byte2hex + " > " + byte2binary + " > " + Integer.toString((byte2 & 0xff), 16));
            // ----------------- Конец преобразований

            // Пробуем спрятать букву A в первые 8 байт файла
            // char > binary
            String binaryA = String.format("%8s", Integer.toBinaryString('A')).replace(' ', '0');
            char[] charBitsArray = binaryA.toCharArray();
            System.out.println("A > " + binaryA + " > " + Arrays.toString(charBitsArray));

            for (int i = 0; i < charBitsArray.length; i++) {
                String imageByteBinary = String.format("%8s", Integer.toBinaryString(imageBytes[i] & 0xFF)).replace(' ', '0');
                char[] imageByteBinaryArray = imageByteBinary.toCharArray();
                imageByteBinaryArray[7] = charBitsArray[i]; // Заменяем последний бит

                String embedImageByteBinary = new String(imageByteBinaryArray);
                System.out.println("Замена: " + imageByteBinary + " > " + embedImageByteBinary);

                byte newImageByte = (byte) Integer.parseInt(embedImageByteBinary, 2);
                imageBytes[i] = newImageByte;
            }

            // Пробуем достать букву A из первых 8 байт файла
            charBitsArray = new char[8];

            for (int i = 0; i < charBitsArray.length; i++) {
                String imageByteBinary = String.format("%8s", Integer.toBinaryString(imageBytes[i] & 0xFF)).replace(' ', '0');
                char[] imageByteBinaryArray = imageByteBinary.toCharArray();
                charBitsArray[i] = imageByteBinaryArray[7]; // Извлекаем последний бит
            }
            String extractedByteBinary = new String(charBitsArray);
            byte extractedByte = (byte) Integer.parseInt(extractedByteBinary, 2);
            System.out.println("Извлечено: " + extractedByte + " > " + (char) extractedByte);

            System.exit(0);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String charArrayToHexString(char[] b) {
        String result = "";
        for (int i=0; i < b.length; i++) {
          result +=
                Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }
}
