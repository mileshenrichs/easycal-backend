package models;

import javax.persistence.*;
import java.util.List;

/**
 * A unit of measure that represents how much of the food was consumed and its ratio to the standard 100 g measure
 */
@Entity
@Table(name = "serving_size")
public class ServingSize {

    @Id
    @GeneratedValue
    public int id;

    @JoinColumn(name = "food_item_id")
    @ManyToOne
    public FoodItem foodItem;

    @JoinColumn(name = "label_id")
    @ManyToOne
    public ServingLabel label;

    public double ratio;

    @OneToMany(mappedBy = "servingSize")
    public List<Consumption> consumptions;

    public ServingSize() {}
}
