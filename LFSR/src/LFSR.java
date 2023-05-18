import java.util.Arrays;

public class LFSR {
    private int[] tapPositions; // Позиции отводов (задаются полиномом)
    private int[] register; // Значения регистра

    public LFSR(int[] initialRegister, int[] polynomial) {
        this.register = Arrays.copyOf(initialRegister, initialRegister.length);
        this.tapPositions = Arrays.copyOf(polynomial, polynomial.length);
    }

    public int generateNextBit() {
        // Сохраняем выходной бит
        int outputBit = register[register.length - 1];
        // System.out.println("reg: " + Arrays.toString(register) + " out: " + outputBit);

        // Если генерируемый бит == 0, то все биты в ячейках сдвигаются вправо без изменений
        // Сдвигаем все биты вправо
        System.arraycopy(register, 0, register, 1, register.length - 1);
        // Обновляем значение первого бита регистра
        register[0] = outputBit;
        
        // Если выходной бит == 1, то биты отвода меняют своё значение на противоположное, и все биты сдвигаются вправо
        if (outputBit == 1) {
            for (int i = 1; i < register.length; i++) {
                if (tapPositions[i] == 1) {
                    register[i] = register[i] ^ 1;
                }
            }
        }

        // Возвращаем полученный в начале выходной бит
        return outputBit;
    }
}
