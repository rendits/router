/* Copyright 2016 Albin Severinson
 * Vehicle representation used for tests.
 */

package com.rendits.testsuite;

import java.time.Instant;

public class Vehicle {
    private final int stationID;

    Vehicle(int stationID) {
        this.stationID = stationID;
    }

    public int getStationID() {
        return this.stationID;
    }

    /* Not necessarily compliant with ITS-G5 spec. in how leap seconds are
     * counted etc. But enough to test the router. */
    public static int getGenerationDeltaTime(){
        Instant instant = Instant.now();
        long generationDeltaTime = (instant.getEpochSecond()*1000 +
                                    instant.getNano()/1000000) % 65536;
        return (int) generationDeltaTime;
    }
}
