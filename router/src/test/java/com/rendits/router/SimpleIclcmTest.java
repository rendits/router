package com.rendits.router;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.gcdc.camdenm.Iclcm.IgameCooperativeLaneChangeMessage;

public class SimpleIclcmTest extends TestCase {

        public SimpleIclcmTest(String testName) {
                super(testName);
        }

        public void testIclcm() {
                SimpleIclcm simpleIclcm = new SimpleIclcm(100, //stationID
                                          (byte) 128, //containerMask (1000 0000)
                                          100, //rearAxleLocation
                                          0, //controllerType
                                          1001, //responseTimeConstant
                                          1001, //responseTimeDelay
                                          10, //targetLongAcc
                                          1, //timeHeadway
                                          3, //cruiseSpeed
                                          (byte) 224, //lowFrequencyMask (1110 0000)
                                          1, //participantsReady
                                          0, //startPlatoon
                                          1, //endOfScenario
                                          255, //mioID
                                          10, //mioRange
                                          11, //mioBearing
                                          12, //mioRangeRate
                                          3, //lane
                                          0, //forwardID
                                          0, //backwardID
                                          0, //mergeRequest
                                          0, //safeToMerge
                                          1, //flag
                                          0, //flagTail
                                          1, //flagHead
                                          254, //platoonID
                                          100, //distanceTravelledCz
                                          2, //intention
                                          2); //counter

                byte[] buffer = simpleIclcm.asByteArray();
                SimpleIclcm simpleIclcmFromArray = new SimpleIclcm(buffer);
                assertEquals(simpleIclcm, simpleIclcmFromArray);
                IgameCooperativeLaneChangeMessage iclcm = simpleIclcm.asIclcm();
                SimpleIclcm simpleIclcmFromIclcm = new SimpleIclcm(iclcm);
                assertEquals(simpleIclcm, simpleIclcmFromIclcm);
        }
}
