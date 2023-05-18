import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class Main extends JFrame {
    private static final int WINDOW_WIDTH = 800; // Ширина окна
    private static final int WINDOW_HEIGHT = 400; // Высота окна
    private static final int CIRCLE_RADIUS = 5; // Радиус точки значения регистра
    private static LFSR generator; // Генератор битов РСЛОС
    private static int[] initialValue; // Начальное значение регистра
    private static int[] polynomial; // Образующий многочлен
    private static int numBits; // Максимальная длина (период) последовательности
    private static int[] sequence; // Последовательность максимальной длины
    String tux = new File("LFSR\\res\\tux.bmp").getAbsolutePath();
    String tuxEnc = new File("LFSR\\res\\tuxEncrypted.bmp").getAbsolutePath();
    String tuxDec = new File("LFSR\\res\\tuxDecrypted.bmp").getAbsolutePath();

    public Main() {
        Scanner in = new Scanner(System.in);
        System.out.println("Введите коэффициенты полинома (разделенные пробелами)");
        System.out.println("Например: 1 1 0 0 1 (для x^4 + x + 1)");
        System.out.print("> ");
        polynomial = toIntArray(in.nextLine());

        System.out.println("Введите начальное значение регистра сдвига (в двоичном формате)");
        System.out.println("Например: 1000");
        System.out.print("> ");
        initialValue = toIntArray(in.nextLine());
        in.close();

        // Максимальная длина (период) последовательности
        numBits = (int) Math.pow(2, Integer.SIZE - Integer.numberOfLeadingZeros(
                Integer.parseInt(
                        Arrays.toString(initialValue).replaceAll("\\[|\\]|,|\\s", ""),
                        2)))
                - 1;

        generator = new LFSR(initialValue, polynomial);
        sequence = new int[numBits]; // Последовательность бит регистра
        String seqStr = "";
        int[] bitFrequency = new int[2]; // Частота 0 и 1 для оценки критерием χ^2
        for (int i = 0; i < numBits; i++) {
            int bit = generator.generateNextBit();
            sequence[i] = bit;
            bitFrequency[bit]++;
            seqStr += bit;
        }

        // Вычисляем значение критерия χ^2
        double expectedFrequency = (double) numBits / 2;
        double chiSquare = 0;
        for (int i = 0; i < 2; i++) {
            chiSquare += Math.pow(bitFrequency[i] - expectedFrequency, 2) / expectedFrequency;
        }

        // Число степеней свободы = кол-во групп - 1 = 2 - 1
        // Примем уровень значимости = 0.05
        double criticalValue = 3.8; // Табличное критическое значение χ^2

        // Сравниваем наблюдаемое значение χ^2 с критическим
        String quality = "";
        if (chiSquare < criticalValue) {
            // Последовательность является более качественной
            quality = "Последовательность соответствует нормальному распределению на уровне значимости 0.05.";
        } else {
            // Последовательность имеет более низкое качество
            quality = "Последовательность значительно отклоняется от нормального распределения на уровне значимости 0.05.";
        }
        System.out.println("Последовательность: " + seqStr +
                "\nX^2 (набл.): " + chiSquare + " | X^2 (теор.): " + criticalValue + "\n" + quality);

        encryptImage(tux, tuxEnc);
        encryptImage(tuxEnc, tuxDec);

        setTitle("Точечная диаграмма РСЛОС");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);
        JLabel label1 = new JLabel("Последовательность: " + seqStr);
        JLabel label2 = new JLabel("X^2 (теор.): " + criticalValue);
        JLabel label3 = new JLabel("X^2 (набл.): " + Math.round(chiSquare * 1000.0) / 1000.0);
        JLabel label4 = new JLabel(quality);
        label1.setBounds(50, 25, 750, 30);
        label2.setBounds(50, 45, 300, 30);
        label3.setBounds(50, 65, 300, 30);
        label4.setBounds(50, 85, 750, 30);
        getContentPane().add(label1);
        getContentPane().add(label2);
        getContentPane().add(label3);
        getContentPane().add(label4);
    }

    public static void encryptImage(String inputFilePath, String outputFilePath) {
        try (FileInputStream fis = new FileInputStream(inputFilePath);
                FileOutputStream fos = new FileOutputStream(outputFilePath);) {
            byte[] header = new byte[122];
            fis.read(header);
            byte[] imageBytes = new byte[fis.available()];
            fis.read(imageBytes);
            fis.close();

            byte[] modifiedBytes = new byte[imageBytes.length];
            generator = new LFSR(initialValue, polynomial);
            for (int i = 0; i < imageBytes.length; i++) {
                byte imageByte = imageBytes[i];
                byte resultByte = 0;
                for (int j = 0; j < 8; j++) {
                    byte originalBit = (byte) ((imageByte >> j) & 0x01);
                    byte xorBit = (byte) generator.generateNextBit();
                    byte modifiedBit = (byte) (originalBit ^ xorBit);
                    resultByte |= (modifiedBit << j);
                }
                modifiedBytes[i] = resultByte;
            }

            fos.write(header);
            fos.write(modifiedBytes);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        int shiftPx = 25;
        g2d.drawLine(0, WINDOW_HEIGHT - shiftPx, WINDOW_WIDTH - shiftPx, WINDOW_HEIGHT - shiftPx); // X
        g2d.drawLine(CIRCLE_RADIUS + shiftPx, 2 * shiftPx, CIRCLE_RADIUS + shiftPx, WINDOW_HEIGHT); // Y

        for (int i = 0, x = 2 * shiftPx; i < numBits; i++) {
            int bit = sequence[i];
            if (bit == 0) {
                g2d.setColor(Color.BLUE);
            } else {
                g2d.setColor(Color.RED);
            }
            int y = WINDOW_HEIGHT - CIRCLE_RADIUS - (bit * shiftPx) - shiftPx;
            Shape circle = new Ellipse2D.Double(x, y, CIRCLE_RADIUS, CIRCLE_RADIUS);
            g2d.fill(circle);
            x += ((WINDOW_WIDTH - 2 * shiftPx) / numBits);
        }
    }

    public static int[] toIntArray(String string) {
        char[] charArray = string.replaceAll(" ", "").toCharArray();
        int[] intArray = new int[charArray.length];
        for (int i = 0; i < charArray.length; i++) {
            intArray[i] = Integer.parseInt(charArray[i] + "");
        }

        return intArray;
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.setVisible(true);
    }
}
