package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {
    /**
     * Performs a GET request to a specified URL and returns the response as a string.
     * @param requestUrl The URL to send a GET request to.
     * @return A string, likely consisting of a JSON object representing an API response.
     */
    public static String get(URL requestUrl) throws IOException {
        HttpURLConnection con = (HttpURLConnection) requestUrl.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder res = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            res.append(inputLine);
        }
        in.close();
        return res.toString();
    }
}
