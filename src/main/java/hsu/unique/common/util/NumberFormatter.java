package hsu.unique.common.util;

public final class NumberFormatter {

    private NumberFormatter() {
    }

    public static String toFourDigits(int number) {
        return String.format("%04d", number);
    }
}
