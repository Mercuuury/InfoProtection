import java.math.BigInteger;
import java.util.Scanner;

public class EllipticCurve {
    public BigInteger a; // Коэффициент a
    public BigInteger b; // Коэффициент b
    public BigInteger p; // Модуль p
    public ECPoint g; // Генерирующая точка g
    
    public EllipticCurve(BigInteger a, BigInteger b, BigInteger p, ECPoint g) {
        this.a = a;
        this.b = b;
        this.p = p;
        this.g = g;
    }

    public EllipticCurve(int a, int b, int p, ECPoint g) {
        this.a = BigInteger.valueOf(a);
        this.b = BigInteger.valueOf(b);
        this.p = BigInteger.valueOf(p);
        this.g = g;
    }

    public ECPoint[] solve() {
        ECPoint[] solutions = new ECPoint[0];
        // y^2 = x^3 + ax + b
        for (BigInteger x = BigInteger.ZERO; x.compareTo(p) < 0; x = x.add(BigInteger.ONE)) {
            for (BigInteger y = BigInteger.ZERO; y.compareTo(p) < 0; y = y.add(BigInteger.ONE)) {
                if (((x.pow(3).add(a.multiply(x)).add(b).subtract(y.pow(2))).mod(p)).equals(BigInteger.ZERO)) {
                    solutions = appendToArray(solutions, new ECPoint(x, y));
                }
            }
        }
        
        return solutions;
    }

    public ECPoint add(ECPoint p1, ECPoint p2) {
        // Если одна из точек является точкой O (находится в бесконечности),
        // то ее суммированием мы получим вторую точку, т.к. точка О является
        // единичным элементом
        if (p1.isPointOfInfinity()) {
            return new ECPoint(p2);
        }
        if (p2.isPointOfInfinity()) {
            return new ECPoint(p1);
        }
        // Если координаты x точек равны, а координаты у симметричны, то
        // результатом будет являться точка в бесконечности (О), т.к.
        // линия, соединяющая p1 и p2, вертикальна и пересекает кривую 
        // в одной точке, которая затем отражается от оси x и дает O. 
        if (p1.x == p2.x && p1.y == p2.y.negate()) {
            return ECPoint.INFINITY;
        }
        
        BigInteger m; // Наклон прямой
        if (p1.x.subtract(p2.x).mod(p).compareTo(BigInteger.ZERO) == 0) {
            if (p1.y.subtract(p2.y).mod(p).compareTo(BigInteger.ZERO) == 0) {
                // Если p1 == p2, то проходящая через них прямая имеет наклон
                // m = (3 * (x_p1)^2 + a) / (2 * y_p1)
                BigInteger nom = p1.x.multiply(p1.x).multiply(BigInteger.valueOf(3)).add(a); // Числитель
                BigInteger den = p1.y.add(p1.y); // Знаменатель
                if (den.equals(BigInteger.ZERO)) {
                    m = nom.multiply(BigInteger.ZERO);
                } else {
                    m = nom.multiply(den.modInverse(p));
                }
            } else {
                return ECPoint.INFINITY;
            }
        } else {
            // Если p1 и q1 не совпадают (x_p1 != x_p2), 
            // то проходящая через них прямая имеет наклон
            // m = (y_p2 - y_p1) / (x_p2 - x_p1)
            BigInteger nom = p2.y.subtract(p1.y); // Числитель
            BigInteger den = p2.x.subtract(p1.x); // Знаменатель
            m = nom.multiply(den.modInverse(p));
        }

        // x_p3 = m^2 - x_p1 - x_p2
        // y_p3 = m * (x_p1 - x_p3) - y_p1
        BigInteger xr = m.multiply(m).subtract(p1.x).subtract(p2.x).mod(p);
        BigInteger yr = m.multiply(p1.x.subtract(xr)).subtract(p1.y).mod(p);
        return new ECPoint(xr, yr);
    }

    public ECPoint multiply(ECPoint p1, BigInteger n) {
        if (p1.isPointOfInfinity()) {
            return ECPoint.INFINITY;
        }
        
        // Алгоритм удвоения сложения
        ECPoint result = ECPoint.INFINITY;
        // Проходимся по двоичному представлению n
        int bitLength = n.bitLength();
        for (int i = bitLength - 1; i >= 0; --i) {
            result = add(result, result); // Удвоение
            if (n.testBit(i)) {
                result = add(result, p1); // Сложение
            }
        }
        
        return result;
    }

    private static ECPoint[] appendToArray(ECPoint[] array, ECPoint element) {
        ECPoint[] newArray = new ECPoint[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = element;
        return newArray;
    }

    public static void main(String[] args) {
        EllipticCurve curve = new EllipticCurve(0, 7, 17, new ECPoint(15, 13));
        ECPoint[] solutions = curve.solve();
        // for (ECPoint ecPoint : solutions) {
        //     System.out.println(ecPoint.toString()); 
        // }
        Grid frame = new Grid(17, solutions);

        ECPoint g2 = curve.multiply(curve.g, BigInteger.valueOf(2));
        ECPoint g3 = curve.multiply(curve.g, BigInteger.valueOf(3));
        ECPoint g4 = curve.multiply(curve.g, BigInteger.valueOf(4));
        ECPoint g5 = curve.multiply(curve.g, BigInteger.valueOf(5));
        ECPoint g6 = curve.multiply(curve.g, BigInteger.valueOf(6));
        ECPoint g7 = curve.multiply(curve.g, BigInteger.valueOf(7));
        ECPoint g8 = curve.multiply(curve.g, BigInteger.valueOf(8));
        System.out.println("G:" + curve.g.toString());
        System.out.println("2G:" + g2.toString());
        System.out.println("3G:" + g3.toString());
        System.out.println("4G:" + g4.toString());
        System.out.println("5G:" + g5.toString());
        System.out.println("6G:" + g6.toString());
        System.out.println("7G:" + g7.toString());
        System.out.println("8G:" + g8.toString());
        frame.setPostOpPoint(curve.g, "G");
        frame.setPostOpPoint(g2, "2G");
        frame.setPostOpPoint(g3, "3G");
        frame.setPostOpPoint(g4, "4G");
        frame.setPostOpPoint(g5, "5G");
        frame.setPostOpPoint(g6, "6G");
        frame.setPostOpPoint(g7, "7G");
        frame.setPostOpPoint(g8, "8G");


        System.out.print("Для продолжения введите что-нибудь в консоль > ");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        frame.clearPostOpPoints();

        ECPoint p = new ECPoint(2, 10);
        ECPoint q = new ECPoint(8, 14);
        ECPoint r = curve.add(p, q);
        System.out.println("\nP:" + p.toString());
        System.out.println("Q:" + q.toString());
        System.out.println("R:" + r.toString());
        frame.setPostOpPoint(p, "P");
        frame.setPostOpPoint(q, "Q");
        frame.setPostOpPoint(r, "R");
    }
}
