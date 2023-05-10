import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
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
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

public class ServerC {
    public static void main(String args[]) throws Exception {
        String publicKeyPath = new File("digitalSignature\\res\\publickey.pem").getAbsolutePath();
        String[] candidatesArray = { "Петров", "Сидоров", "Смирнов", "Васильев" }; // Список кандидатов
        HashMap<String, String> candidatesHashMap = new HashMap<String, String>();
        HashMap<String, Integer> votesHashMap = new HashMap<String, Integer>();
        for (String candidate : candidatesArray) {
            candidatesHashMap.put(generateHash(candidate), candidate);
            votesHashMap.put(candidate, 0);
        }

        // Получаем содержимое публичного ключа
        String pemContents = new String(Files.readAllBytes(Paths.get(publicKeyPath)), StandardCharsets.UTF_8);
        pemContents = pemContents.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] derBytes = Base64.getDecoder().decode(pemContents); // Переводим декодированое содержимое в массив байтов
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes); // Переводим приватный ключ в формат X509
        KeyFactory kf = KeyFactory.getInstance("RSA"); // Инициализируем ключевую фабрику для алгоритма RSA
        PublicKey publicKey = kf.generatePublic(keySpec); // Получаем объект PublicKey с помощью фабрики
        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey; // Преобразуем тип PublicKey к RSAPublicKey

        BigInteger modulus = rsaPublicKey.getModulus(); // Получаем модуль, N
        BigInteger publicExponent = rsaPublicKey.getPublicExponent(); // Получаем публичную экспоненту, e

        // Сервер получает исходное и подписанное сообщение (без маскировки).
        // Возведением подписанного сообщение в степень е (публичная экспонента) и
        // сравнения результата операции с исходным сообщением, сервер удостоверяется в
        // подлинности голоса.

        try (ServerSocket server = new ServerSocket(5005)) { // Запускаем сервер на порту 5000
            System.out.println("Server C is ready.");
            Socket client = server.accept(); // Становимся в ожидание подключения к клиентскому сокету
            System.out.print("Connection accepted.");
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream())); // Канал чтения из сокета
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream())); // Канал записи в сокет

            // Начинаем диалог с подключенным клиентом в цикле, пока сокет не закрыт
            System.out.println("Server reading from channel");
            boolean isOriginal = true;
            String originalMessage = "";
            String signedMessage = "";
            while (!client.isClosed()) {
                String entry;
                while ((entry = in.readLine()) != null) {
                    if (entry.equalsIgnoreCase("stop"))
                        break;

                    // Считываем данные после их получения
                    if (isOriginal) {
                        originalMessage = entry;
                        System.out.println("Server recieved original message");
                    } else {
                        signedMessage = entry;
                        System.out.println("Server recieved signed message");

                        byte[] originalMessageBytes = hexStringToByteArray(originalMessage.replace("\n", ""));
                        byte[] signedMessageBytes = hexStringToByteArray(signedMessage.replace("\n", ""));
                        boolean verified = verifySignature(new BigInteger(1, signedMessageBytes),
                                new BigInteger(1, originalMessageBytes), publicExponent, modulus);
                        System.out.println("Server verified the digital signature. Authenticity: " + verified);

                        if (verified) {
                            String messageHash = new String(originalMessageBytes);
                            votesHashMap.put(candidatesHashMap.get(messageHash),
                                    votesHashMap.get(candidatesHashMap.get(messageHash)) + 1);
                            out.write("Vote accepted." + "\n");
                        } else {
                            out.write("Vote not accepted." + "\n");
                        }
                        out.flush();
                    }
                    isOriginal = !isOriginal;
                }
                if (entry.equalsIgnoreCase("stop"))
                    break;
            }
            System.out.println("Client disconnected");
            System.out.println("Closing connections & channels...");
            in.close();
            client.close();
            server.close();
            System.out.println("Closing server connections & channels - DONE.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("Итоги голосования: ");
        votesHashMap.forEach((key, value) -> {
            System.out.println(key + " - " + value);
        });
    }

    // Проверка подлинности голоса возведением подписанного сообщения в степень е
    // (публичная экспонента) и сравнения результата операции с исходным сообщением
    public static boolean verifySignature(BigInteger signature, BigInteger m, BigInteger e, BigInteger n) {
        return signature.modPow(e, n).equals(m);
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
    public static String generateHash(String candidate) {
        try {
            String tempFilePath = new File("digitalSignature\\res\\hash.tmp").getAbsolutePath();
            FileOutputStream fos = new FileOutputStream(tempFilePath);
            fos.write(candidate.getBytes());
            fos.close();

            Process process = new ProcessBuilder("cmd.exe", "/c", "openssl dgst -sha1 " + tempFilePath).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line, result = "";
            while ((line = reader.readLine()) != null) {
                result += line;
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Error: " + exitCode);
            }
            return result.split("= ")[1];
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
