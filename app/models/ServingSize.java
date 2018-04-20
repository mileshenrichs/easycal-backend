package models;

/**
 * Created by Henrichs on 4/20/2018.
 */
public class ServingSize {
    private int id;
    private FoodItem foodItem;
    private ServingLabel label;
    private double ratio;

    public ServingSize(int id, FoodItem foodItem, ServingLabel label, double ratio) {
        this.id = id;
        this.foodItem = foodItem;
        this.label = label;
        this.ratio = ratio;
    }
}
