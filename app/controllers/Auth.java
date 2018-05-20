package controllers;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import models.User;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import play.Play;
import play.mvc.Controller;
import util.DatabaseUtil;
import util.Secret;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Henrichs on 5/20/2018.
 * Handles user accounts and authentication
 */
public class Auth extends Controller {

    public static void checkAuth() {
        JsonObjectBuilder resObj = Json.createObjectBuilder();
        String reqBody = "";
        try {
            reqBody = IOUtils.toString(request.body, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject reqObj = new JSONObject(reqBody);
        String token = reqObj.getString("token");

        try {
            Jwts.parser().setSigningKey(Secret.appSecret()).parseClaimsJws(token);

            // JWT is valid
            renderJSON(resObj.add("auth", true).build().toString());
        } catch (SignatureException e) {
            // invalid JWT
            renderJSON(resObj.add("auth", false).build().toString());
        }
    }

    public static void registerUser() {
        JsonObjectBuilder resObj = Json.createObjectBuilder();

        String reqBody = "";
        try {
            reqBody = IOUtils.toString(request.body, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject reqObj = new JSONObject(reqBody);
        String email = reqObj.getString("email");
        String password = reqObj.getString("password");

        // make sure user with given email doesn't already exist
        if (!DatabaseUtil.userExists(email)) {
            if(email.length() > 0 && password.length() > 0) {
                String hash = BCrypt.hashpw(password, BCrypt.gensalt());

                User newUser = DatabaseUtil.createUser(email, hash);

                // build JSON web token
                String token = buildJWT(newUser);

                // add token to response
                resObj.add("token", token);
            } else { // either username or password is not specified
                resObj.add("error", true).add("message", "missing field");
            }
        } else { // user already exists
            resObj.add("error", true).add("message", "user exists");
        }

        renderJSON(resObj.build().toString());
    }

    public static void logInUser() {
        JsonObjectBuilder resObj = Json.createObjectBuilder();

        String reqBody = "";
        try {
            reqBody = IOUtils.toString(request.body, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject reqObj = new JSONObject(reqBody);
        String email = reqObj.getString("email");
        String password = reqObj.getString("password");

        if(email.length() > 0 && password.length() > 0) {
            User user = DatabaseUtil.getUserByEmailAddress(email);
            if(user != null) {
                // check if password is correct
                if(BCrypt.checkpw(password, user.password)) {
                    // build JSON web token
                    String token = buildJWT(user);

                    // add token to response
                    resObj.add("token", token);
                } else {
                    resObj.add("error", true).add("message", "incorrect pass");
                }
            } else {
                resObj.add("error", true).add("message", "user doesn't exist");
            }
        } else {
            resObj.add("error", true).add("message", "missing field");
        }

        renderJSON(resObj.build().toString());
    }

    private static String buildJWT(User user) {
        // keep user logged in for a week
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        Date weekFromNow = calendar.getTime();

        return Jwts.builder()
                .claim("userId", user.id)
                .claim("emailAddress", user.emailAddress)
                .setExpiration(weekFromNow)
                .signWith(SignatureAlgorithm.HS256, Secret.appSecret())
                .compact();
    }

}
