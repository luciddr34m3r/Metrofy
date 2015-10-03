package com.trekonnect.metrofy.WMATA;

/**
 * Created by cbarnard on 12/30/14.
 */
public class MetroLine {

    private String displayName;
    private String lineCode;
    //private List<MetroStation> stations;
    private Boolean hasAlert;

    public MetroLine(String displayName, String lineCode) {
        this.displayName = displayName;
        this.lineCode = lineCode;


    }

    public Boolean getHasAlert() {
        return hasAlert;
    }

    public String getLineCode() {
        return lineCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
