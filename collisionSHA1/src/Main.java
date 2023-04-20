import java.io.*;
import java.nio.charset.Charset;

public class Main {
    public static void main(String[] args) {
        int numDocuments = 10; // Число генерируемых документов за цикл
        int maxIterations = 50; // Максимальное число итераций цикла
        String inputFile = new java.io.File("collisionSHA1\\res\\leasing.txt").getAbsolutePath();
        String inputSHA = generateSHA1(inputFile, true);

        for (int iter = 0; iter < maxIterations; iter++) {
            try (BufferedReader br = new BufferedReader(new FileReader(inputFile, Charset.forName("UTF-8")))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) { // Построчно считываем входной файл
                    sb.append(line).append("\n");
                }
                String inputText = sb.toString();

                // Генерируем эквивалентные документы с послед-тями управляющими символов
                for (int i = 1; i <= numDocuments; i++) {
                    String documentText = inputText.replaceAll("\s", "\s\s\b"); // Заменяем s на s-s-b

                    int numReplacements = (int) (Math.random() * 10) + 1; // Количество замен s-s-b на s-b-s
                    // Заменяем случайные послед-ти s-s-b в случайных местах на послед-ти s-b-s
                    for (int j = 1; j <= numReplacements; j++) {
                        int index = documentText.indexOf("\s\s\b",
                                (int) (Math.random() * documentText.length()) + 1); // Индекс последовательности s-s-b
                                                                                    // начиная со случайного места в
                                                                                    // документе
                        if (index != -1) {
                            documentText = documentText.substring(0, index) + "\s\b\s"
                                    + documentText.substring(index + 3);
                        }
                    }

                    // Записываем результат в файл
                    String outputFile = new java.io.File("collisionSHA1\\res\\leasing_" + i + ".txt").getAbsolutePath();
                    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, Charset.forName("UTF-8")));
                    writer.write(documentText);
                    writer.close();

                    // System.out.println("Сгенерирован " + i + "-й документ: " + outputFile);
                    System.out.print(iter + "" + i + " ");
                    if (inputSHA.equals(generateSHA1(outputFile, false))) {
                        System.out.println("Обнаружена коллизия с " + i + "-м документом!\n");
                        generateSHA1(inputFile, true);
                        generateSHA1(outputFile, true);
                        maxIterations = 0;
                        break;
                    } // else System.out.println("Коллизий не обнаружено.\n");
                }

                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static String generateSHA1(String filePath, boolean print) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "openssl dgst -sha1 " + filePath);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line, result = "";
            while ((line = reader.readLine()) != null) {
                if (print)
                    System.out.println(line);
                result = line.split("= ")[1];
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