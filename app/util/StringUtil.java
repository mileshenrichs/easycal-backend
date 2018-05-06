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
        int cutoffIndex = label.contains(",") ? label.indexOf(",") : label.length();
        label = label.substring(0, cutoffIndex);
        return label;
    }
}
