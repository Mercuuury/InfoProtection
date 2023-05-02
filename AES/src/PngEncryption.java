import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class PngEncryption {
    public static void main(String[] args) {
        String inputFilePath = new File("AES\\res\\tux.png").getAbsolutePath();
        String tuxECBFilePath = new File("AES\\res\\tuxECB.png").getAbsolutePath();
        String key = generateKey();

        try {
            FileInputStream inputStream = new FileInputStream(inputFilePath);
            byte[] header = new byte[95]; // Заголовок
            inputStream.read(header);
            byte[] imageBytes = new byte[inputStream.available() - 8]; // Байты пикселей
            inputStream.read(imageBytes);
            byte[] footer = new byte[8]; // EoF
            inputStream.read(footer);
            inputStream.close();

            byte[] idat = Arrays.copyOfRange(imageBytes, 0, 4);
            int[] idatIndexes = indexOf(imageBytes, idat);
            idatIndexes[idatIndexes.length - 1] = imageBytes.length;
            System.out.println(Arrays.toString(idatIndexes));

            FileOutputStream fos2 = new FileOutputStream(tuxECBFilePath);
            fos2.write(header);
            
            for (int i = 0; i < idatIndexes.length - 1; i++) {
                byte[] pixels = Arrays.copyOfRange(imageBytes, idatIndexes[i] + idat.length, idatIndexes[i + 1]);

                String tmpBytesFilePath = new File("AES\\res\\imageBytes"+i+".tmp").getAbsolutePath();
                String tmpBytesEncryptedFilePath = new File("AES\\res\\imageBytesEncrypted"+i+".tmp").getAbsolutePath();
                FileOutputStream fos = new FileOutputStream(tmpBytesFilePath);
                fos.write(pixels);
                fos.close();

                encrypt(tmpBytesFilePath, tmpBytesEncryptedFilePath, "ecb", key, false);

                FileInputStream fis = new FileInputStream(tmpBytesEncryptedFilePath);
                byte[] encryptedImageBytes = new byte[fis.available()];
                fis.read(encryptedImageBytes);
                fis.close();

                fos2.write(idat);
                fos2.write(encryptedImageBytes);
                // fos2.write(Arrays.copyOfRange(encryptedImageBytes, 8, encryptedImageBytes.length)); // Без Salted__
            }

            fos2.write(footer);
            fos2.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int[] indexOf(byte[] outerArray, byte[] smallerArray) {
        int[] indexes = new int[4];
        int idx = 0;
        for(int i = 0; i < outerArray.length - smallerArray.length+1; ++i) {
            boolean found = true;
            for(int j = 0; j < smallerArray.length; ++j) {
               if (outerArray[i+j] != smallerArray[j]) {
                   found = false;
                   break;
               }
            }
            if (found) indexes[idx++] = i;
         }
       return indexes;  
    }  

    private static void encrypt(String inputFilePath, String outputFilePath, String method, String key,
            boolean decrypt) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c",
                    "openssl enc " + (decrypt ? "-d" : "") + " -aes-256-" + method +
                            " -in " + inputFilePath + " -out " + outputFilePath + " -pass pass:" + key);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line, result = "";
            while ((line = reader.readLine()) != null) {
                result = line;
                System.out.println(result);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Error: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String generateKey() {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "openssl rand -hex 16");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line, result = "";
            while ((line = reader.readLine()) != null) {
                result = line;
                System.out.println(result);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Error: " + exitCode);
            }

            return result;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return e.toString();
        }
    }
}
