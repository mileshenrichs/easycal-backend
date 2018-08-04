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
                "WHERE c.id IN (SELECT MAX(c.id) FROM c GROUP BY c.foodItem)" +
                "AND c.user.id = :userId ORDER BY c.day DESC")
                .setParameter("userId", userId).setMaxResults(50).getResultList();
    }

    public static List<Consumption> getConsumptionsInRange(int userId, Date from, Date to) {
        return JPA.em().createQuery("SELECT c FROM Consumption c " +
                "WHERE c.user.id = :userId " +
                "AND c.day BETWEEN :dateFrom AND :dateTo ORDER BY c.day DESC")
                .setParameter("userId", userId).setParameter("dateFrom", from).setParameter("dateTo", to)
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

    public static List<FoodMealGroup> getUserFoodMealGroups(int userId) {
        User user = getUser(userId);
        return JPA.em().createQuery("SELECT fmg FROM FoodMealGroup fmg " +
                "WHERE fmg.user = :user").setParameter("user", user).getResultList();
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

    public static List<Exercise> getExercisesForDateRange(int userId, Date dayFrom, Date dayTo) {
        return JPA.em().createQuery("SELECT e FROM Exercise e WHERE e.user.id = :userId " +
                "AND e.day BETWEEN :dayFrom AND :dayTo ORDER BY e.day ASC").setParameter("userId", userId)
                .setParameter("dayFrom", dayFrom).setParameter("dayTo", dayTo).getResultList();
    }

    public static User getUserByEmailAddress(String emailAddress) {
        List<User> users = JPA.em().createQuery("SELECT u FROM User u WHERE u.emailAddress = :email")
                .setParameter("email", emailAddress).getResultList();
        if(users.size() > 0) {
            return users.get(0);
        }
        return null;
    }

    public static boolean userExists(String emailAddress) {
        List<User> users = JPA.em().createQuery("SELECT u FROM User u WHERE u.emailAddress = :email")
                .setParameter("email", emailAddress).getResultList();
        return users.size() > 0;
    }

    // Returns created user id (used to build JWT)
    public static User createUser(String emailAddress, String hashedPass) {
        User newUser = new User();
        newUser.emailAddress = emailAddress;
        newUser.password = hashedPass;
        JPA.em().persist(newUser);

        return newUser;
    }

}
