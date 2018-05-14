package util;

import models.Consumption;
import models.Goal;

import java.security.SecureRandom;
import java.util.Date;

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

    public static class DayTotals {
        public Date day;
        public int carbs;
        public int fat;
        public int protein;
        public int fiber;
        public int sugar;
        public int sodium;
        public int calories;

        public DayTotals(Date day, int carbs, int fat, int protein, int fiber, int sugar, int sodium, int calories) {
            this.day = day;
            this.carbs = carbs;
            this.fat = fat;
            this.protein = protein;
            this.fiber = fiber;
            this.sugar = sugar;
            this.sodium = sodium;
            this.calories = calories;
        }
    }
}
