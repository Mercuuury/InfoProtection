import java.math.BigInteger;

public class ECPoint {
    public BigInteger x;
    public BigInteger y;
    public boolean pointOfInfinity;
    public static final ECPoint INFINITY = infinity();

    public ECPoint(BigInteger x, BigInteger y) {
        this.x = x;
        this.y = y;
        this.pointOfInfinity = false;
    }

    public ECPoint(int x, int y) {
        this.x = BigInteger.valueOf(x);
        this.y = BigInteger.valueOf(y);
        this.pointOfInfinity = false;
    }

    public ECPoint(ECPoint point) {
        this.x = point.x;
        this.y = point.y;
        this.pointOfInfinity = point.pointOfInfinity;
    }

    private static ECPoint infinity() {
        ECPoint infinityPoint = new ECPoint(0, 0);
        infinityPoint.pointOfInfinity = true;
        return infinityPoint;
    }

    public boolean isPointOfInfinity() {
        return pointOfInfinity;
    }

    @Override
    public String toString() {
        if (isPointOfInfinity()) {
            return "INFINITY";
        } else {
            return "(" + x.toString() + ", " + y.toString() + ")";
        }
    }
}
