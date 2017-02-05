package com.rendits.router;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.gcdc.camdenm.CoopIts.Denm;

public class SimpleDenmTest extends TestCase {

        public SimpleDenmTest(String testName) {
                super(testName);
        }

        public void testDenm() {
                SimpleDenm simpleDenm = new SimpleDenm(100, //stationID
                                                       1000, //generationDeltaTime
                                                       (byte) 160, //containerMask
                                                       (byte) 248, //managementMask
                                                       1, //detectionTime
                                                       2, //referenceTime
                                                       0, //termination
                                                       900000001, //latitude
                                                       1800000001, //longtitude
                                                       1, //semiMajorConfidence
                                                       2, //semiMinorConfidence
                                                       2, //semiMajorOrientation
                                                       3, //altitude
                                                       0, //relevanceDistance
                                                       0, //relevanceTrafficDirection
                                                       0, //validityDuration
                                                       1, //transmissionIntervall
                                                       5, //stationType
                                                       (byte) 128,    //situationMask
                                                       4, //informationQuality
                                                       2, //causeCode
                                                       2, //subCauseCode
                                                       0, //linkedCuaseCode
                                                       0, //linkedSubCauseCode
                                                       (byte) 8, //alacarteMask
                                                       0, //lanePosition
                                                       0, //temperature
                                                       5); //positioningSolutionType

                byte[] buffer = simpleDenm.asByteArray();
                SimpleDenm simpleDenmFromArray = new SimpleDenm(buffer);
                assertEquals(simpleDenm, simpleDenmFromArray);
                Denm denm = simpleDenm.asDenm();
                SimpleDenm simpleDenmFromDenm = new SimpleDenm(denm);
                assertEquals(simpleDenm, simpleDenmFromDenm);
        }
}
