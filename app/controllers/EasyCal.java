package controllers;

import play.mvc.*;

import java.util.*;

import models.*;
import util.DatabaseUtil;

public class EasyCal extends Controller {

    public static void test() {
        List<Consumption> consumptions = DatabaseUtil.getConsumptionsForDay(1, new GregorianCalendar(2018, Calendar.APRIL, 20).getTime());
        renderJSON("{\n" +
                "  \"test\": \"this is a test\",\n" +
                "  \"truetest\": {\n" +
                "    \"demo\": true\n" +
                "  }\n" +
                "}");
    }

}