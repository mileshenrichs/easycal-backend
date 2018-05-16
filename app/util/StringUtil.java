package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Util class for manipulating strings
 */
public class StringUtil {
    public static String formatFoodItemName(String name) {
        // capitalize only first letter
        name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        // remove UPC number/GTIN code
        Pattern upcPattern = Pattern.compile(", (upc|gtin): \\d+");
        Matcher upcMatcher = upcPattern.matcher(name);
        if(upcMatcher.find()) {
            name = name.substring(0, upcMatcher.start());
        }
        return name;
    }

    public static String formatServingSizeLabel(String label) {
        // convert to lower case
        label = label.toLowerCase();
        // remove any qualifiers
        int cutoffIndex = label.length();
        if(label.contains("|")) {
            cutoffIndex = label.indexOf("|") - 1;
        } else if(label.contains(",")) {
            cutoffIndex = label.indexOf(",");
        }
        label = label.substring(0, cutoffIndex);
        return label;
    }

    public static double processNutrientString(String nutrient) {
        Pattern gramsPattern = Pattern.compile("m*g");
        Matcher gramsMatcher = gramsPattern.matcher(nutrient);
        if(gramsMatcher.find()) {
            return Double.valueOf(nutrient.substring(0, gramsMatcher.start()));
        } else {
            return Double.valueOf(nutrient);
        }
    }

    public static class UserServingSize {
        public double amount;
        public String label;

        private UserServingSize(double amount, String label) {
            this.amount = amount;
            this.label = label;
        }
    }

    public static UserServingSize processServingSizeString(String servingSize) {
        double amount = Double.valueOf(servingSize.substring(0, servingSize.indexOf(" ")));
        String label = servingSize.substring(servingSize.indexOf(" ") + 1);
        return new UserServingSize(amount, label);
    }
}
