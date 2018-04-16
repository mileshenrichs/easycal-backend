package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class EasyCal extends Controller {

    public static void test() {
        renderJSON("{\n" +
                "  \"test\": \"this is a test\",\n" +
                "  \"truetest\": {\n" +
                "    \"demo\": true\n" +
                "  }\n" +
                "}");
    }

}