import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Random;

public class Client {
    public static void main(String args[]) throws Exception {
        int votersCount = 5; // Количество избирателей
        String[] candidates = { "Петров", "Сидоров", "Смирнов", "Васильев" }; // Список кандидатов
        String privateKeyPath = new File("digitalSignature\\res\\privatekey.pem").getAbsolutePath();
        String publicKeyPath = new File("digitalSignature\\res\\publickey.pem").getAbsolutePath();
        String messageFilePath = new File("digitalSignature\\res\\message.txt").getAbsolutePath();

        String generatePrivateKey = "openssl genpkey -algorithm RSA -out " + privateKeyPath
        + " -pkeyopt rsa_keygen_bits:1024";
        String generatePublicKey = "openssl rsa -pubout -in " + privateKeyPath + " -out " + publicKeyPath;
        String generateHash = "openssl dgst -sha1 " + messageFilePath;

        // String encryptedMessageFilePath = new
        // File("digitalSignature\\res\\message.enc").getAbsolutePath();
        // String decryptedMessageFilePath = new
        // File("digitalSignature\\res\\message.dec").getAbsolutePath();
        // String signatureFilePath = new
        // File("digitalSignature\\res\\signature.bin").getAbsolutePath();
        // String printKeyStructure = "openssl rsa -text -noout -in " + privateKeyPath;
        // String encryptFile = "openssl rsautl -encrypt -inkey " + publicKeyPath + "
        // -pubin -in " + messageFilePath
        // + " -out " + encryptedMessageFilePath;
        // String decryptFile = "openssl rsautl -decrypt -inkey " + privateKeyPath + "
        // -in " + encryptedMessageFilePath
        // + " -out " + decryptedMessageFilePath;
        // String calculateSignature = "openssl dgst -sha256 -sign " + privateKeyPath +
        // " -out " + signatureFilePath + " "
        // + messageFilePath;
        // String verifySignature = "openssl dgst -sha256 -verify " + publicKeyPath + "
        // -signature " + signatureFilePath
        // + " " + messageFilePath;

        // Генерируем пару ключей
        openssl(generatePrivateKey, false);
        openssl(generatePublicKey, false);

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
        BigInteger publicExponent = rsaPrivateKey.getPublicExponent(); // Получаем публичную экспоненту, e
        BigInteger privateExponent = rsaPrivateKey.getPrivateExponent(); // Получаем приватную экспоненту, d

        try (Socket socket = new Socket("localhost", 5000);
                DataOutputStream oos = new DataOutputStream(socket.getOutputStream());) {
            System.out.println("Client connected to socket.");

            while (!socket.isOutputShutdown()) {
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
                    String hashCode = openssl(generateHash, false).split("= ")[1];

                    // Хеш-код файла переводится в BigInteger
                    BigInteger message = new BigInteger(1, hashCode.getBytes()).mod(modulus);

                    // Пользователь А генерирует случайное число (маскирующий множитель,
                    // взаимно простой с модулем)
                    BigInteger r = generateRandomNumber(modulus);

                    // Пользователь А маскирует сообщение, умножая его на маскирующий множитель
                    // в степени e (публичная экспонента)
                    BigInteger blindedMessage = blindMessage(message, r, publicExponent, modulus);

                    // Пользователь B подписывает замаскированное сообщение, возводя его
                    // в степень d (приватная экспонента)
                    BigInteger blindedSignature = signBlindedMessage(blindedMessage, privateExponent, modulus);

                    // Пользователь А снимает маскировку, умножая подписанное сообщение на число,
                    // обратное маскирующему множителю
                    BigInteger unblindedSignature = unblindSignature(blindedSignature, r, modulus);

                    // Исходное и подписанное сообщение передаются серверу
                    oos.writeUTF(message.toString(16));
                    System.out.println("Client sent original message to server.");
                    Thread.sleep(1000);

                    oos.writeUTF(unblindedSignature.toString(16));
                    System.out.println("Client sent signed message to server.");
                    Thread.sleep(1000);
                }
                oos.writeUTF("stop");
                break;
            }
            oos.close();
            socket.close();
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

    // Подпись замаскированного сообщения возведением его в степень d (приватная
    // экспонента)
    public static BigInteger signBlindedMessage(BigInteger blindedMessage, BigInteger d, BigInteger n) {
        return blindedMessage.modPow(d, n);
    }

    // Снятие маскировки умножением подписанного сообщения на число, обратное
    // маскирующему множителю
    public static BigInteger unblindSignature(BigInteger blindedSignature, BigInteger r, BigInteger n) {
        return blindedSignature.multiply(r.modInverse(n)).mod(n);
    }

    // Выполнение команды в командной строке Windows
    public static String openssl(String command, boolean print) {
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", command).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line, result = "";
            while ((line = reader.readLine()) != null) {
                result += line;
                if (print) System.out.println(line);
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
