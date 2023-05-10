import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;

public class ClientA {
    public static void main(String args[]) throws Exception {
        int votersCount = 10; // Количество избирателей
        String[] candidates = { "Петров", "Сидоров", "Смирнов", "Васильев" }; // Список кандидатов
        String publicKeyPath = new File("digitalSignature\\res\\publickey.pem").getAbsolutePath();
        String messageFilePath = new File("digitalSignature\\res\\message.txt").getAbsolutePath();
        String generateHash = "openssl dgst -sha1 " + messageFilePath;

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

        // Запускаем подключение к серверу B (подписывающая сторона) и серверу C (счетчик)
        try (Socket socketB = new Socket("localhost", 5000);
                BufferedReader inFromB = new BufferedReader(new InputStreamReader(socketB.getInputStream()));
                BufferedWriter outToB = new BufferedWriter(new OutputStreamWriter(socketB.getOutputStream()));
                Socket socketC = new Socket("localhost", 5005);
                BufferedReader inFromC = new BufferedReader(new InputStreamReader(socketC.getInputStream()));
                BufferedWriter outToC = new BufferedWriter(new OutputStreamWriter(socketC.getOutputStream()));) {
            System.out.println("Client connected to server B socket.");

            if (!socketB.isOutputShutdown()) {
                Random rand = new Random();
                for (int votes = 0; votes < votersCount; votes++) {
                    // Пользователь А выбирает кандидата (голосует)
                    int choice = rand.nextInt(candidates.length);
                    System.out.println("Выбор: " + candidates[choice]);

                    // Выбор записывается в файл
                    FileOutputStream fos = new FileOutputStream(messageFilePath);
                    fos.write(candidates[choice].getBytes());
                    fos.close();

                    // Вычисляется хеш-код файла
                    String hashCode = executeCommand(generateHash, false).split("= ")[1];

                    // Хеш-код файла переводится в BigInteger
                    BigInteger message = new BigInteger(1, hashCode.getBytes()).mod(modulus);

                    // Пользователь А генерирует случайное число (маскирующий множитель,
                    // взаимно простой с модулем)
                    BigInteger r = generateRandomNumber(modulus);

                    // Пользователь А маскирует сообщение, умножая его на маскирующий множитель
                    // в степени e (публичная экспонента)
                    BigInteger tmpBlindedMessage = blindMessage(message, r, publicExponent, modulus);
                    BigInteger blindedMessage = new BigInteger(1, tmpBlindedMessage.toByteArray());

                    // Пользователь A передает замаскированное сообщение на подпись пользователем B
                    outToB.write(blindedMessage.toString(16) + "\n");
                    outToB.flush();
                    System.out.println("Client sent blinded message to server B.");
                    Thread.sleep(3000);

                    // Пользователь A получает подписанное замаскированное сообщение
                    String blindedSignatureHexString = inFromB.readLine();
                    System.out.println("Client recieved signed blinded message from server B.");
                    byte[] blindedSignatureBytes = hexStringToByteArray(blindedSignatureHexString.replace("\n", ""));
                    BigInteger blindedSignature = new BigInteger(1, blindedSignatureBytes);

                    // Пользователь А снимает маскировку, умножая подписанное сообщение на число,
                    // обратное маскирующему множителю
                    BigInteger unblindedSignature = unblindSignature(blindedSignature, r, modulus);

                    // Исходное и подписанное сообщение передаются серверу C (счетчик)
                    outToC.write(message.toString(16) + "\n");
                    outToC.flush();
                    System.out.println("Client sent original message to server C.");
                    Thread.sleep(1000);

                    outToC.write(unblindedSignature.toString(16) + "\n");
                    outToC.flush();
                    System.out.println("Client sent signed message to server C.");
                    Thread.sleep(3000);
                    
                    System.out.println("READ from server C - " + inFromC.readLine());
                }
                outToB.write("stop" + "\n");
                outToB.flush();
                outToC.write("stop" + "\n");
                outToC.flush();
                System.out.println("Closing client connections & channels...");
            }
            inFromB.close();
            outToB.close();
            inFromC.close();
            outToC.close();
            socketB.close();
            socketC.close();
            System.out.println("Closing client connections & channels - DONE.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Генерация маскирующего множителя
    public static BigInteger generateRandomNumber(BigInteger n) {
        SecureRandom random = new SecureRandom();
        BigInteger r;
        do {
            r = new BigInteger(n.bitLength(), random);
        } while (r.compareTo(n) >= 0 || r.compareTo(BigInteger.ONE) < 0);
        return r;
    }

    // Маскировка сообщения умножением его на маскирующий множитель в степени e
    // (публичная экспонента)
    public static BigInteger blindMessage(BigInteger m, BigInteger r, BigInteger e, BigInteger n) {
        return m.multiply(r.modPow(e, n)).mod(n);
    }

    // Снятие маскировки умножением подписанного сообщения на число, обратное
    // маскирующему множителю
    public static BigInteger unblindSignature(BigInteger blindedSignature, BigInteger r, BigInteger n) {
        return blindedSignature.multiply(r.modInverse(n)).mod(n);
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
