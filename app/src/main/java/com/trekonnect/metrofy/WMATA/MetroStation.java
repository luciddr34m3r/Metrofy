package com.trekonnect.metrofy.WMATA;

import java.util.List;

/**
 * Created by cbarnard on 12/30/14.
 */
public class MetroStation {

    private String stationCode;
    private String name;
    private List<MetroTrain> trains;

    public MetroStation(String name, String stationCode) {
        this.stationCode = stationCode;
        this.name = name;


    }

    public String getStationName() {
        return name;
    }

    public String getStationCode() {
        return stationCode;
    }

    public List<MetroTrain> getTrains() {
        return trains;
    }

    @Override
    public String toString() {
        return getStationName();
    }
}
