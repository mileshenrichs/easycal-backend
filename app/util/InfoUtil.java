package util;

/**
 * General util class for methods helpful when pulling info from external API
 */
public class InfoUtil {
    public static boolean containsNegative(double[] numbers) {
        for(double n : numbers) {
            if(n < 0) {
                return true;
            }
        }
        return false;
    }
}
