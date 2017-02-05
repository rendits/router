/* Class with sample messages for use in tests. */

package com.rendits.router;

public class SampleMessages {

        public static SimpleDenm getSampleDenm() {
                return new SimpleDenm(100, //stationID
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
        }

        public static SimpleCam getSampleCam() {
                return new SimpleCam(100, //stationID
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
        }

        public static SimpleIclcm getSampleIclcm() {
                return new SimpleIclcm(100, //stationID
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
        }

}
