import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class ServerB {
    public static void main(String[] args) throws Exception {
        String privateKeyPath = new File("digitalSignature\\res\\privatekey.pem").getAbsolutePath();
        String publicKeyPath = new File("digitalSignature\\res\\publickey.pem").getAbsolutePath();
        String generatePrivateKey = "openssl genpkey -algorithm RSA -out " + privateKeyPath
                + " -pkeyopt rsa_keygen_bits:1024";
        String generatePublicKey = "openssl rsa -pubout -in " + privateKeyPath + " -out " + publicKeyPath;

        // Генерируем пару ключей
        executeCommand(generatePrivateKey, false);
        executeCommand(generatePublicKey, false);

        // Получаем содержимое приватного ключа
        String pemContents = new String(Files.readAllBytes(Paths.get(privateKeyPath)), StandardCharsets.UTF_8);
        pemContents = pemContents.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] derBytes = Base64.getDecoder().decode(pemContents); // Переводим декодированое содержимое в массив байтов
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(derBytes); // Переводим приватный ключ в формат PKCS#8
        KeyFactory kf = KeyFactory.getInstance("RSA"); // Инициализируем ключевую фабрику для алгоритма RSA
        PrivateKey privateKey = kf.generatePrivate(keySpec); // Получаем объект PrivateKey с помощью фабрики
        RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) privateKey; // Преобразуем тип PrivateKey к RSAPrivateCrtKey

        BigInteger modulus = rsaPrivateKey.getModulus(); // Получаем модуль, N
        BigInteger privateExponent = rsaPrivateKey.getPrivateExponent(); // Получаем приватную экспоненту, d

        try (ServerSocket server = new ServerSocket(5000)) { // Запускаем сервер на порту 5000
            System.out.println("Server B is ready.");
            Socket client = server.accept(); // Становимся в ожидание подключения к клиентскому сокету
            System.out.println("Connection accepted.");
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream())); // Канал чтения из сокета
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream())); // Канал записи в сокет

            // Начинаем диалог с подключенным клиентом в цикле, пока сокет не закрыт
            System.out.println("Server reading from channel");
            while (!client.isClosed()) {
                String entry;
                while ((entry = in.readLine()) != null) {
                    if (entry.equalsIgnoreCase("stop"))
                        break;

                    // Считываем данные после их получения. Получаем замаскированное сообщение
                    String blindedMessageHexString = entry;
                    byte[] blindedMessageBytes = hexStringToByteArray(blindedMessageHexString.replace("\n", ""));
                    BigInteger blindedMessage = new BigInteger(1, blindedMessageBytes);
                    System.out.println("Server B recieved blinded message from client A.");

                    // Пользователь B подписывает замаскированное сообщение, возводя его
                    // в степень d (приватная экспонента)
                    BigInteger tmpBlindedSignature = signBlindedMessage(blindedMessage, privateExponent, modulus);
                    BigInteger blindedSignature = new BigInteger(1, tmpBlindedSignature.toByteArray());

                    out.write(blindedSignature.toString(16) + "\n");
                    out.flush();
                    System.out.println("Server B sent signed blinded message to client A.");
                }
                if (entry.equalsIgnoreCase("stop"))
                    break;
            }
            System.out.println("Client disconnected");
            System.out.println("Closing connections & channels...");
            in.close();
            out.close();
            client.close();
            server.close();
            System.out.println("Closing server connections & channels - DONE.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Подпись замаскированного сообщения возведением его в степень d (приватная экспонента)
    public static BigInteger signBlindedMessage(BigInteger blindedMessage, BigInteger d, BigInteger n) {
        return blindedMessage.modPow(d, n);
    }

    // Перевод строки, записанной в 16сс, в массив байтов
    public static byte[] hexStringToByteArray(String hexString) {
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString;
        }
        int len = hexString.length();
        byte[] byteArray = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return byteArray;
    }

    // Выполнение команды в командной строке Windows
    public static String executeCommand(String command, boolean print) {
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", command).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line, result = "";
            while ((line = reader.readLine()) != null) {
                result += line;
                if (print)
                    System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Error: " + exitCode);
            }
            return result;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
