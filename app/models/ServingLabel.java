package models;

/**
 * Label of a serving size (i.e. cup, slice, oz)
 */
public class ServingLabel {
    private int id;
    private String labelValue;

    public ServingLabel(int id, String labelValue) {
        this.id = id;
        this.labelValue = labelValue;
    }
}
