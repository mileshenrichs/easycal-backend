package util;

import models.*;
import play.db.jpa.JPA;

import javax.persistence.EntityManager;
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

    public static boolean deleteConsumption(int consumptionId) {
        Consumption consumption = JPA.em().find(Consumption.class, consumptionId);
        if(consumption != null) {
            JPA.em().remove(consumption);
            return true;
        }
        return false;
    }

    public static boolean updateConsumption(int consumptionId, int servingSizeId, double quantity) {
        EntityManager em = JPA.em();
        Consumption consumption = em.find(Consumption.class, consumptionId);
        ServingSize servingSize = em.find(ServingSize.class, servingSizeId);
        if(consumption != null && servingSize != null) {
            consumption.servingSize = servingSize;
            consumption.servingQuantity = quantity;
            return true;
        }
        return false;
    }

    public static FoodItem getFoodItem(String foodItemId) {
        return JPA.em().find(FoodItem.class, foodItemId);
    }

    public static User getUser(int userId) {
        return JPA.em().find(User.class, userId);
    }

    public static ServingSize getServingSize(int servingSizeId) {
        return JPA.em().find(ServingSize.class, servingSizeId);
    }

    public static int findServingSizeId(FoodItem foodItem, String label) {
        List<Integer> ids = JPA.em()
                .createQuery("SELECT ss.id FROM ServingSize ss " +
                        "WHERE ss.foodItem.id = :foodItemId " +
                        "AND ss.label.labelValue = :labelValue", Integer.class)
                .setParameter("foodItemId", foodItem.id).setParameter("labelValue", label).getResultList();
        if(ids.size() > 0) {
            return ids.get(0);
        }
        return -1;
    }

}
