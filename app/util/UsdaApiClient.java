package util;

import org.json.JSONObject;
import play.Play;

import java.io.IOException;
import java.net.URL;

/** A client which facilitates requests made to the FoodData (formerly USDA Food Composition) database API. */
public class UsdaApiClient {
    // Filters out "Foundation" and "Experimental" food types described here: https://fdc.nal.usda.gov/faq.html
    private static final String DATA_TYPE_LIST = "Branded,SR%20Legacy,Survey%20(FNDDS)";

    public static JSONObject getFoodSearchResponse(String query) throws IOException {
        URL endpoint = new URL(
                String.format("https://api.nal.usda.gov/fdc/v1/foods/search?query=%s&dataType=%s&api_key=%s",
                        query, DATA_TYPE_LIST, Play.configuration.getProperty("fooddata.apikey")));
        return new JSONObject(HttpUtil.get(endpoint));
    }

    public static JSONObject getFoodDetailsResponse(int fdcId) throws IOException {
        URL endpoint = new URL(String.format("https://api.nal.usda.gov/fdc/v1/food/%d?api_key=%s",
                fdcId, Play.configuration.getProperty("fooddata.apikey")));
        return new JSONObject(HttpUtil.get(endpoint));
    }
}
