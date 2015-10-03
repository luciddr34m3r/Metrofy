package com.trekonnect.metrofy.WMATA;

/**
 * Created by cbarnard on 12/30/14.
 */
public class MetroTrain {

    private String cars;
    private String destination;
    private String destinationCode;
    private String time;

    public MetroTrain(String destination, String destinationCode, String cars, String time) {
        this.destination = destination;
        this.destinationCode = destinationCode;
        this.cars = cars;
        this.time = time;
    }

    public String getCars() {
        return cars;
    }

    public String getDestination() {
        return destination;
    }

    public String getDestinationCode() {
        return destinationCode;
    }

    public String getTime() {
        return time;
    }
}
