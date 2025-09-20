import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    // Fraction class for exact rational arithmetic
    static class Fraction {
        BigInteger num, den;

        Fraction(BigInteger n, BigInteger d) {
            if (d.equals(BigInteger.ZERO)) throw new ArithmeticException("Den=0");
            if (d.signum() < 0) {
                n = n.negate();
                d = d.negate();
            }
            BigInteger g = n.gcd(d);
            num = n.divide(g);
            den = d.divide(g);
        }

        Fraction add(Fraction o) {
            BigInteger n = num.multiply(o.den).add(o.num.multiply(den));
            BigInteger d = den.multiply(o.den);
            return new Fraction(n, d);
        }

        Fraction mul(Fraction o) {
            return new Fraction(num.multiply(o.num), den.multiply(o.den));
        }

        Fraction mul(BigInteger k) {
            return new Fraction(num.multiply(k), den);
        }

        boolean isInteger() {
            return den.equals(BigInteger.ONE);
        }
    }

    // Parse base-encoded string into BigInteger
    static BigInteger parseBaseValue(String value, int base) {
        return new BigInteger(value.toLowerCase(), base);
    }

    // Compute secret using Lagrange interpolation at x=0
    static BigInteger computeSecret(String json) {
        // Find k from keys
        Matcher mK = Pattern.compile("\"keys\"\\s*:\\s*\\{[^}]*\"k\"\\s*:\\s*(\\d+)").matcher(json);
        if (!mK.find()) throw new RuntimeException("k not found in JSON!");
        int k = Integer.parseInt(mK.group(1));

        // Find all entries like "1": {"base":"..","value":".."}
        Matcher m = Pattern.compile("\"(\\d+)\"\\s*:\\s*\\{[^}]*?\"base\"\\s*:\\s*\"(\\d+)\"[^}]*?\"value\"\\s*:\\s*\"([^\"]+)\"").matcher(json);

        List<Integer> xList = new ArrayList<>();
        List<BigInteger> yList = new ArrayList<>();

        while (m.find()) {
            int x = Integer.parseInt(m.group(1));
            int base = Integer.parseInt(m.group(2));
            String val = m.group(3).toLowerCase();
            BigInteger y = parseBaseValue(val, base);
            xList.add(x);
            yList.add(y);
        }

        // Sort by x and take the first k points
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) order.add(i);
        order.sort(Comparator.comparingInt(i -> xList.get(i)));

        List<BigInteger> xs = new ArrayList<>();
        List<BigInteger> ys = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            xs.add(BigInteger.valueOf(xList.get(order.get(i))));
            ys.add(yList.get(order.get(i)));
        }

        // Lagrange interpolation at x=0
        Fraction total = new Fraction(BigInteger.ZERO, BigInteger.ONE);
        for (int i = 0; i < k; i++) {
            Fraction term = new Fraction(BigInteger.ONE, BigInteger.ONE);
            BigInteger xi = xs.get(i);
            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                BigInteger xj = xs.get(j);
                Fraction frac = new Fraction(xj.negate(), xi.subtract(xj));
                term = term.mul(frac);
            }
            term = term.mul(ys.get(i));
            total = total.add(term);
        }

        if (!total.isInteger()) throw new RuntimeException("The result is not an integer!");

        // Ensure positive secret
        BigInteger secret = total.num;
        if (secret.signum() < 0) secret = secret.negate();
        return secret;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        StringBuilder sb = new StringBuilder();
        while (sc.hasNextLine()) {
            sb.append(sc.nextLine()).append("\n");
        }
        sc.close();

        String jsonInput = sb.toString();
        try {
            BigInteger secret = computeSecret(jsonInput);
            System.out.println(secret);
        } catch (RuntimeException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }
}
