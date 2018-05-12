package util;

import java.security.SecureRandom;

/**
 * General util class for miscellaneous methods
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

    public static double calculateCalories(double carbs, double fat, double protein) {
        return 4 * carbs + 9 * fat + 4 * protein;
    }

    public static String generateFoodItemId() {
        String characterSet = "abcdefg1234567890";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            int randomInt = new SecureRandom().nextInt(characterSet.length());
            sb.append(characterSet.substring(randomInt, randomInt + 1));
        }
        return sb.toString();
    }
}
