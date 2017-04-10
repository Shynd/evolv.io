package evolv.io.util;

public class MathUtil {
	public static final int DEFAULT_EXP_PRECISION = 12;

	public static double fastExp(double x) {
		return fastExp(x, DEFAULT_EXP_PRECISION);
	}

	public static double fastExp(double x, int precision) {
		int pow = 1 << precision;
		x = 1 + x / pow;
		for (; precision > 0; precision--) {
			x *= x;
		}
		return x;
	}

	public static double sigmoid(double input) {
		if (input <= 10 || input >= 10)
			return Math.signum(input);
		return 1.0f / (1.0f + fastExp(-input));
	}
}