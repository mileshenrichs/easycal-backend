package models;

import javax.persistence.*;
import java.util.List;

/**
 * Label of a serving size (i.e. cup, slice, oz)
 */
@Entity
@Table(name = "serving_labels")
public class ServingLabel {

    @Id
    @GeneratedValue
    public int id;

    @Column(name = "label_value")
    public String labelValue;

    @OneToMany(mappedBy = "label")
    public List<ServingSize> servingSizes;

    public ServingLabel(int id, String labelValue) {
        this.id = id;
        this.labelValue = labelValue;
    }

    public ServingLabel() {}
}
