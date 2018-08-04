package models;

import com.google.gson.annotations.Expose;

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
    @Expose
    public int id;

    @Column(name = "label_value")
    @Expose
    public String labelValue;

    @OneToMany(mappedBy = "label")
    public List<ServingSize> servingSizes;

    public ServingLabel() {}
}
