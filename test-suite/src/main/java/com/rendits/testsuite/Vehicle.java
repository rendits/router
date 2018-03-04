/* Copyright 2018 Rendits
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/* Dummy vehicle class used by the test suite.
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
