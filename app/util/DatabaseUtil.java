package util;

import models.Consumption;
import play.db.jpa.JPA;

import java.util.*;

/**
 * Utility class for all database communication and queries
 */
@SuppressWarnings("unchecked")
public class DatabaseUtil {

    public static List<Consumption> getConsumptionsForDay(int userId, Date day) {
        return JPA.em().createQuery("SELECT c FROM Consumption c " +
                "WHERE c.user.id = :userId AND c.day = :day")
                .setParameter("userId", userId)
                .setParameter("day", day)
                .getResultList();
    }

}