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

    public static List<Consumption> getRecentConsumptions(int userId) {
        return JPA.em().createQuery("SELECT c FROM Consumption c " +
                "WHERE c.id IN (SELECT MIN(c.id) FROM c GROUP BY c.foodItem)" +
                "AND c.user.id = :userId ORDER BY c.day DESC")
                .setParameter("userId", userId).getResultList();
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

    public static ServingLabel getServingLabelByValue(String label) {
        List<ServingLabel> servingLabels = JPA.em()
                .createQuery("SELECT sl FROM ServingLabel sl " +
                        "WHERE sl.labelValue = :labelValue")
                .setParameter("labelValue", label).getResultList();
        if(servingLabels.size() > 0) {
            return servingLabels.get(0);
        }
        return null;
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

    public static List<FoodItem> getUserFoodItems(int userId) {
        User user = JPA.em().find(User.class, userId);
        return user.createdFoods;
    }

    public static boolean deleteUserFoodItem(String foodItemId) {
        FoodItem item = JPA.em().find(FoodItem.class, foodItemId);
        if(item != null) {
            JPA.em().remove(item);
            return true;
        }
        return false;
    }

    public static Exercise getExerciseForDay(int userId, Date day) {
        User user = JPA.em().find(User.class, userId);
        List<Exercise> exercises = JPA.em().createQuery("SELECT e FROM Exercise e " +
                "WHERE e.user = :user AND e.day = :day").setParameter("day", day).setParameter("user", user).getResultList();
        if(exercises.size() > 0) {
            return exercises.get(0);
        }
        return null;
    }

}
