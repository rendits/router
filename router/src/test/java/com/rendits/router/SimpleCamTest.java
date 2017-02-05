package com.rendits.router;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.gcdc.camdenm.CoopIts.Cam;

public class SimpleCamTest extends TestCase {

        public SimpleCamTest(String testName) {
                super(testName);
        }

        public void testCam() {
                SimpleCam simpleCam = new SimpleCam(100, //stationID
                                                    120, //generationDeltaTime
                                                    (byte) 128, //containerMask
                                                    5, //stationType
                                                    2, //latitude
                                                    48, //longitude
                                                    0, //semiMajorConfidence
                                                    0, //semiMinorConfidence
                                                    0, //semiMajorOrientation
                                                    400, //altitude
                                                    1, //heading value
                                                    1, //headingConfidence
                                                    0, //speedValue
                                                    1, //speedConfidence
                                                    40, //vehicleLength
                                                    20, //vehicleWidth
                                                    159, //longitudinalAcc
                                                    1, //longitudinalAccConf
                                                    2, //yawRateValue
                                                    1, //yawRateConfidence
                                                    0); //vehicleRole


                byte[] buffer = simpleCam.asByteArray();
                SimpleCam simpleCamFromArray = new SimpleCam(buffer);
                assertEquals(simpleCam, simpleCamFromArray);
                Cam cam = simpleCam.asCam();
                SimpleCam simpleCamFromCam = new SimpleCam(cam);
                assertEquals(simpleCam, simpleCamFromCam);
        }
}
