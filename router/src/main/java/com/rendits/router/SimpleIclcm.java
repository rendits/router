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

package com.rendits.router;

import java.nio.ByteBuffer;
import net.gcdc.asn1.datatypes.IntRange;
import net.gcdc.camdenm.CoopIts.*;
import net.gcdc.camdenm.CoopIts.ItsPduHeader.MessageId;
import net.gcdc.camdenm.CoopIts.ItsPduHeader.ProtocolVersion;
import net.gcdc.camdenm.Iclcm;
import net.gcdc.camdenm.Iclcm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Rendits Simple iCLCM</h1>
 *
 * <p>This class is built to be a simpler representation of the ETSI ITS-G5 iCLCM message. It
 * provides methods for converting to/from proper iCLCM messages and to/from byte buffers.
 *
 * @author Albin Severinson (albin@rendits.com)
 * @version 1.0.0-SNAPSHOT
 */
public class SimpleIclcm {
  private static final Logger logger = LoggerFactory.getLogger(Router.class);
  private static final int SIMPLE_iCLCM_LENGTH = 111;

  final byte messageId;
  final int stationId;
  final byte containerMask;
  //HW Container
  final int rearAxleLocation;
  final int controllerType;
  final int responseTimeConstant;
  final int responseTimeDelay;
  final int targetLongAcc;
  final int timeHeadway;
  final int cruiseSpeed;
  //LF Container
  final byte lowFrequencyMask;
  final int participantsReady;
  final int startPlatoon;
  final int endOfScenario;
  //MIO Container
  final int mioId;
  final int mioRange;
  final int mioBearing;
  final int mioRangeRate;
  //Lane Container
  final int lane;
  //Pair ID Container
  final int forwardId;
  final int backwardId;
  //Merge Container
  final int mergeRequest;
  final int mergeSafeToMerge;
  final int mergeFlag;
  final int mergeFlagTail;
  final int mergeFlagHead;
  //Intersection Container
  final int platoonId;
  final int distanceTravelledCz;
  final int intention;
  final int counter;

  /** Create a simple iCLCM by supplying the values manually. */
  public SimpleIclcm(
      int stationId,
      byte containerMask,
      //HW Container
      int rearAxleLocation,
      int controllerType,
      int responseTimeConstant,
      int responseTimeDelay,
      int targetLongAcc,
      int timeHeadway,
      int cruiseSpeed,
      //LF Container
      byte lowFrequencyMask,
      int participantsReady,
      int startPlatoon,
      int endOfScenario,
      //MIO Container
      int mioId,
      int mioRange,
      int mioBearing,
      int mioRangeRate,
      //Lane Container
      int lane,
      //Pair ID Container
      int forwardId,
      int backwardId,
      //Merge Container
      int mergeRequest,
      int mergeSafeToMerge,
      int mergeFlag,
      int mergeFlagTail,
      int mergeFlagHead,
      //Intersection Container
      int platoonId,
      int distanceTravelledCz,
      int intention,
      int counter) {

    this.messageId = Iclcm.MessageID_iCLCM;
    this.stationId = stationId;
    this.containerMask = containerMask;
    //HW Container
    this.rearAxleLocation = rearAxleLocation;
    this.controllerType = controllerType;
    this.responseTimeConstant = responseTimeConstant;
    this.responseTimeDelay = responseTimeDelay;
    this.targetLongAcc = targetLongAcc;
    this.timeHeadway = timeHeadway;
    this.cruiseSpeed = cruiseSpeed;
    //LF Container
    this.lowFrequencyMask = lowFrequencyMask;
    this.participantsReady = participantsReady;
    this.startPlatoon = startPlatoon;
    this.endOfScenario = endOfScenario;
    //MIO Container
    this.mioId = mioId;
    this.mioRange = mioRange;
    this.mioBearing = mioBearing;
    this.mioRangeRate = mioRangeRate;
    //Lane Container
    this.lane = lane;
    //Pair ID Container
    this.forwardId = forwardId;
    this.backwardId = backwardId;
    //Merge Container
    this.mergeRequest = mergeRequest;
    this.mergeSafeToMerge = mergeSafeToMerge;
    this.mergeFlag = mergeFlag;
    this.mergeFlagTail = mergeFlagTail;
    this.mergeFlagHead = mergeFlagHead;
    //Intersection Container
    this.platoonId = platoonId;
    this.distanceTravelledCz = distanceTravelledCz;
    this.intention = intention;
    this.counter = counter;
  }

  /**
   * Create a simple iCLCM from its byte buffer representation. This constructor is typically used
   * to create a simple iCLCM from the payload of a received byte buffer.
   *
   * @param receivedData Byte buffer representation of the message.
   */
  public SimpleIclcm(byte[] receivedData) {
    if (receivedData.length < SIMPLE_iCLCM_LENGTH) {
      logger.error(
          "Simple iCLCM is too short. Is: {} Should be: {}",
          receivedData.length,
          SIMPLE_iCLCM_LENGTH);
      throw new IllegalArgumentException();
    }

    /* Assign values, checking if they are valid. Invalid values
    are replaced with default values if possible. */
    ByteBuffer buffer = ByteBuffer.wrap(receivedData);
    this.messageId = buffer.get();
    if (messageId != Iclcm.MessageID_iCLCM) {
      logger.error("MessageID is: {} Should be: {}", messageId, Iclcm.MessageID_iCLCM);
      throw new IllegalArgumentException();
    }

    this.stationId = buffer.getInt();
    if (!checkInt(StationID.class, stationId, "StationID")) {
      throw new IllegalArgumentException();
    }

    this.containerMask = buffer.get();

    //HW Container
    this.rearAxleLocation = buffer.getInt();
    if (!checkInt(VehicleRearAxleLocation.class, rearAxleLocation, "RearAxleLocation")) {
      throw new IllegalArgumentException();
    }

    this.controllerType = buffer.getInt();
    if (!checkInt(ControllerType.class, controllerType, "ControllerType")) {
      throw new IllegalArgumentException();
    }

    int responseTimeConstant = buffer.getInt();
    if (checkInt(VehicleResponseTimeConstant.class, responseTimeConstant, "ResponseTimeConstant")) {
      this.responseTimeConstant = responseTimeConstant;
    } else {
      this.responseTimeConstant = VehicleResponseTimeConstant.unavailable;
    }

    int responseTimeDelay = buffer.getInt();
    if (checkInt(VehicleResponseTimeDelay.class, responseTimeDelay, "ResponseTimeDelay")) {
      this.responseTimeDelay = responseTimeDelay;
    } else {
      this.responseTimeDelay = VehicleResponseTimeDelay.unavailable;
    }

    int targetLongAcc = buffer.getInt();
    if (checkInt(
        TargetLongitudonalAcceleration.class, targetLongAcc, "TargetLongitudinalAcceleration")) {
      this.targetLongAcc = targetLongAcc;
    } else {
      this.targetLongAcc = TargetLongitudonalAcceleration.unavailable;
    }

    int timeHeadway = buffer.getInt();
    if (checkInt(TimeHeadway.class, timeHeadway, "TimeHeadway")) {
      this.timeHeadway = timeHeadway;
    } else {
      this.timeHeadway = TimeHeadway.unavailable;
    }

    int cruiseSpeed = buffer.getInt();
    if (checkInt(CruiseSpeed.class, cruiseSpeed, "CruiseSpeed")) {
      this.cruiseSpeed = cruiseSpeed;
    } else {
      this.cruiseSpeed = CruiseSpeed.unavailable;
    }

    //LF Container
    this.lowFrequencyMask = buffer.get();
    this.participantsReady = buffer.getInt();
    this.startPlatoon = buffer.getInt();
    this.endOfScenario = buffer.getInt();
    if (this.hasLowFrequencyContainer()) {
      if (this.hasParticipantsReady()) {
        if (!checkInt(ParticipantsReady.class, participantsReady, "ParticipantsReady")) {
          throw new IllegalArgumentException();
        }
      }

      if (this.hasStartPlatoon()) {
        if (!checkInt(StartPlatoon.class, startPlatoon, "StartPlatoon")) {
          throw new IllegalArgumentException();
        }
      }

      if (this.hasEndOfScenario()) {
        if (!checkInt(EndOfScenario.class, endOfScenario, "EndOfScenario")) {
          throw new IllegalArgumentException();
        }
      }
    }

    //MIO Container
    mioId = buffer.getInt();
    if (!checkInt(StationID.class, mioId, "MioID")) {
      throw new IllegalArgumentException();
    }

    int mioRange = buffer.getInt();
    if (checkInt(MioRange.class, mioRange, "MioRange")) {
      this.mioRange = mioRange;
    } else {
      this.mioRange = MioRange.unavailable;
    }

    int mioBearing = buffer.getInt();
    if (checkInt(MioBearing.class, mioBearing, "MioBearing")) {
      this.mioBearing = mioBearing;
    } else {
      this.mioBearing = MioBearing.unavailable;
    }

    int mioRangeRate = buffer.getInt();
    if (checkInt(MioRangeRate.class, mioRangeRate, "MioRangeRate")) {
      this.mioRangeRate = mioRangeRate;
    } else {
      this.mioRangeRate = MioRangeRate.unavailable;
    }

    //Lane container
    int lane = buffer.getInt();
    if (checkInt(Lane.class, lane, "Lane")) {
      this.lane = lane;
    } else {
      this.lane = Lane.unavailable;
    }

    //Pair ID container
    this.forwardId = buffer.getInt();
    if (!checkInt(StationID.class, forwardId, "ForwardID")) {
      throw new IllegalArgumentException();
    }

    this.backwardId = buffer.getInt();
    if (!checkInt(StationID.class, backwardId, "BackwardID")) {
      throw new IllegalArgumentException();
    }

    //Merge container
    this.mergeRequest = buffer.getInt();
    if (!checkInt(MergeRequest.class, mergeRequest, "MergeRequest")) {
      throw new IllegalArgumentException();
    }

    this.mergeSafeToMerge = buffer.getInt();
    if (!checkInt(MergeSafeToMerge.class, mergeSafeToMerge, "MergeSafeToMerge")) {
      throw new IllegalArgumentException();
    }

    this.mergeFlag = buffer.getInt();
    if (!checkInt(MergeFlag.class, mergeFlag, "MergeFlag")) {
      throw new IllegalArgumentException();
    }

    this.mergeFlagTail = buffer.getInt();
    if (!checkInt(MergeFlagTail.class, mergeFlagTail, "MergeFLagTail")) {
      throw new IllegalArgumentException();
    }

    this.mergeFlagHead = buffer.getInt();
    if (!checkInt(MergeFlagHead.class, mergeFlagHead, "MergeFlagHead")) {
      throw new IllegalArgumentException();
    }

    //Intersection Container
    this.platoonId = buffer.getInt();
    if (!checkInt(PlatoonID.class, platoonId, "PlatoonID")) {
      throw new IllegalArgumentException();
    }

    int distanceTravelledCz = buffer.getInt();
    if (checkInt(DistanceTravelledCZ.class, distanceTravelledCz, "DistanceTravelledCz")) {
      this.distanceTravelledCz = distanceTravelledCz;
    } else {
      this.distanceTravelledCz = 0;
    }

    this.intention = buffer.getInt();
    if (!checkInt(Intention.class, intention, "Intention")) {
      throw new IllegalArgumentException();
    }

    int counter = buffer.getInt();
    if (checkInt(Counter.class, counter, "Counter")) {
      this.counter = counter;
    } else {
      this.counter = 0;
    }
  }

  /**
   * Create a simple iCLCM from a proper iCLCM. This constructor is typically used after receiving a
   * proper iCLCM from another ITS station.
   *
   * @param proper A proper CAM message.
   */
  public SimpleIclcm(IgameCooperativeLaneChangeMessage proper) {
    IgameCooperativeLaneChangeMessageBody iclcm = proper.getIclm();
    ItsPduHeader header = proper.getHeader();
    messageId = (byte) header.getMessageID().value;
    stationId = (int) header.getStationID().value;
    IclmParameters iclmParameters = iclcm.getIclmParameters();
    byte containerMask = 0;

    if (messageId != Iclcm.MessageID_iCLCM) {
      logger.warn("Malformed message on BTP port 2010 from station with ID {}", stationId);
      throw new IllegalArgumentException("Malformed message on BTP port 2010");
    }

    /* VehicleContainerHighFrequency */
    VehicleContainerHighFrequency vehicleContainerHighFrequency =
        iclmParameters.getVehicleContainerHighFrequency();
    rearAxleLocation = (int) vehicleContainerHighFrequency.getVehicleRearAxleLocation().value;
    controllerType = (int) vehicleContainerHighFrequency.getControllerType().value;
    responseTimeConstant =
        (int)
            vehicleContainerHighFrequency
                .getVehicleResponseTime()
                .getVehicleResponseTimeConstant()
                .value;
    responseTimeDelay =
        (int)
            vehicleContainerHighFrequency
                .getVehicleResponseTime()
                .getVehicleResponseTimeDelay()
                .value;
    targetLongAcc = (int) vehicleContainerHighFrequency.getTargetLongitudinalAcceleration().value;
    timeHeadway = (int) vehicleContainerHighFrequency.getTimeHeadway().value;
    cruiseSpeed = (int) vehicleContainerHighFrequency.getCruisespeed().value;

    /* VehicleContainerLowFrequency */
    VehicleContainerLowFrequency lowFrequencyContainer = null;
    byte lowFrequencyMask = 0;
    if (iclmParameters.hasLowFrequencyContainer()) {
      containerMask += (1 << 7);
      lowFrequencyContainer = iclmParameters.getLowFrequencyContainer();

      if (lowFrequencyContainer.hasParticipantsReady()) {
        lowFrequencyMask += (1 << 7);
        participantsReady = (int) lowFrequencyContainer.getParticipantsReady().value;
      } else {
        participantsReady = 0;
      }

      if (lowFrequencyContainer.hasStartPlatoon()) {
        lowFrequencyMask += (1 << 6);
        startPlatoon = (int) lowFrequencyContainer.getStartPlatoon().value;
      } else {
        startPlatoon = 0;
      }

      if (lowFrequencyContainer.hasEndOfScenario()) {
        lowFrequencyMask += (1 << 5);
        endOfScenario = (int) lowFrequencyContainer.getEndOfScenario().value;
      } else {
        endOfScenario = 0;
      }
    } else {
      participantsReady = 0;
      startPlatoon = 0;
      endOfScenario = 0;
    }
    this.lowFrequencyMask = lowFrequencyMask;

    /* MostImportantObjectContainer */
    MostImportantObjectContainer mostImportantObjectContainer =
        iclmParameters.getMostImportantObjectContainer();
    mioId = (int) mostImportantObjectContainer.getMioID().value;
    mioRange = (int) mostImportantObjectContainer.getMioRange().value;
    mioBearing = (int) mostImportantObjectContainer.getMioBearing().value();
    mioRangeRate = (int) mostImportantObjectContainer.getMioRangeRate().value();

    /* LaneObject */
    LaneObject laneObject = iclmParameters.getLaneObject();
    lane = (int) laneObject.getLane().value();

    /* PairIdObject */
    PairIdObject pairIdObject = iclmParameters.getPairIdObject();
    forwardId = (int) pairIdObject.getForwardID().value;
    backwardId = (int) pairIdObject.getBackwardID().value;

    /* MergeObject */
    MergeObject mergeObject = iclmParameters.getMergeObject();
    mergeRequest = (int) mergeObject.getMergeRequest().value;
    mergeSafeToMerge = (int) mergeObject.getMergeSafeToMerge().value;
    mergeFlag = (int) mergeObject.getMergeFlag().value;
    mergeFlagTail = (int) mergeObject.getMergeFlagTail().value;
    mergeFlagHead = (int) mergeObject.getMergeFlagHead().value;

    /* ScenarioObject */
    ScenarioObject scenarioObject = iclmParameters.getScenarioObject();
    platoonId = (int) scenarioObject.getPlatoonID().value;
    distanceTravelledCz = (int) scenarioObject.getDistanceTravelledCZ().value;
    intention = (int) scenarioObject.getIntention().value;
    counter = (int) scenarioObject.getCounterIntersection().value;

    this.containerMask = containerMask;
  }

  /** Return true if the simple iCLCM has a low frequency container. */
  boolean hasLowFrequencyContainer() {
    return (containerMask & (1 << 7)) != 0;
  }

  /** Return true if the simple iCLCM has a participants ready flag. */
  boolean hasParticipantsReady() {
    return (lowFrequencyMask & (1 << 7)) != 0;
  }

  /** Return true if the simple iCLCM has a start platoon flag. */
  boolean hasStartPlatoon() {
    return (lowFrequencyMask & (1 << 6)) != 0;
  }

  /** Return true if the simple iCLCM has a end of scenario flag. */
  boolean hasEndOfScenario() {
    return (lowFrequencyMask & (1 << 5)) != 0;
  }

  /** Return the IntRange min and max value as a nice string. */
  String getIntRangeString(IntRange intRange) {
    String string = "minValue=" + intRange.minValue() + ", maxValue=" + intRange.maxValue();
    return string;
  }

  /** Return true if given value is within the IntRange, and false otherwise. */
  boolean compareIntRange(int value, IntRange intRange) {
    return value <= intRange.maxValue() && value >= intRange.minValue();
  }

  /** Check if an integer is within its range of allowed values. Print a warning if it isn't. */
  public boolean checkInt(Class<?> classOfT, int value, String name) {
    IntRange intRange = (IntRange) classOfT.getAnnotation(IntRange.class);
    if (intRange == null) {
      logger.error("{} does not have an IntRange!", classOfT);
      return false;
    }
    if (!compareIntRange(value, intRange)) {
      logger.warn("{} is outside of range. Value={}, {}", name, value, getIntRangeString(intRange));
      return false;
    } else {
      return true;
    }
  }

  /** Object hash. */
  @Override
  public int hashCode() {
    assert false : "hashCode not designed";
    return 42; // any arbitrary constant will do
  }

  /** Object equals. */
  @Override
  public boolean equals(Object object) {
    // self check
    if (this == object) {
      return true;
    }

    // null check
    if (object == null) {
      return false;
    }

    // type check and cast
    if (getClass() != object.getClass()) {
      return false;
    }

    SimpleIclcm simpleIclcm = (SimpleIclcm) object;

    // field comparison
    return messageId == simpleIclcm.messageId
        && stationId == simpleIclcm.stationId
        && containerMask == simpleIclcm.containerMask
        && rearAxleLocation == simpleIclcm.rearAxleLocation
        && controllerType == simpleIclcm.controllerType
        && responseTimeConstant == simpleIclcm.responseTimeConstant
        && responseTimeDelay == simpleIclcm.responseTimeDelay
        && targetLongAcc == simpleIclcm.targetLongAcc
        && timeHeadway == simpleIclcm.timeHeadway
        && cruiseSpeed == simpleIclcm.cruiseSpeed
        && lowFrequencyMask == simpleIclcm.lowFrequencyMask
        && participantsReady == simpleIclcm.participantsReady
        && startPlatoon == simpleIclcm.startPlatoon
        && endOfScenario == simpleIclcm.endOfScenario
        && mioId == simpleIclcm.mioId
        && mioRange == simpleIclcm.mioRange
        && mioBearing == simpleIclcm.mioBearing
        && mioRangeRate == simpleIclcm.mioRangeRate
        && lane == simpleIclcm.lane
        && forwardId == simpleIclcm.forwardId
        && backwardId == simpleIclcm.backwardId
        && mergeRequest == simpleIclcm.mergeRequest
        && mergeSafeToMerge == simpleIclcm.mergeSafeToMerge
        && mergeFlag == simpleIclcm.mergeFlag
        && mergeFlagTail == simpleIclcm.mergeFlagTail
        && mergeFlagHead == simpleIclcm.mergeFlagHead
        && platoonId == simpleIclcm.platoonId
        && distanceTravelledCz == simpleIclcm.distanceTravelledCz
        && intention == simpleIclcm.intention
        && counter == simpleIclcm.counter;
  }

  /** Check if the simple iCLCM is valid by checking if all values fall within their range. */
  boolean isValid() {
    boolean valid = true;

    if (!checkInt(StationID.class, stationId, "StationID")) {
      valid = false;
    }

    if (!checkInt(VehicleRearAxleLocation.class, rearAxleLocation, "RearAxleLocation")) {
      valid = false;
    }

    if (!checkInt(ControllerType.class, controllerType, "ControllerType")) {
      valid = false;
    }

    if (!checkInt(
        VehicleResponseTimeConstant.class, responseTimeConstant, "ResponseTimeConstant")) {
      valid = false;
    }

    if (!checkInt(VehicleResponseTimeDelay.class, responseTimeDelay, "ResponseTimeDelay")) {
      valid = false;
    }

    if (!checkInt(
        TargetLongitudonalAcceleration.class, targetLongAcc, "TargetLongitudinalAcceleration")) {
      valid = false;
    }

    if (!checkInt(TimeHeadway.class, timeHeadway, "TimeHeadway")) {
      valid = false;
    }

    if (!checkInt(CruiseSpeed.class, cruiseSpeed, "CruiseSpeed")) {
      valid = false;
    }

    if (!checkInt(ParticipantsReady.class, participantsReady, "ParticipantsReady")) {
      valid = false;
    }

    if (!checkInt(StartPlatoon.class, startPlatoon, "StartPlatoon")) {
      valid = false;
    }

    if (!checkInt(EndOfScenario.class, endOfScenario, "EndOfScenario")) {
      valid = false;
    }
    if (!checkInt(StationID.class, mioId, "MioID")) {
      valid = false;
    }

    if (!checkInt(MioRange.class, mioRange, "MioRange")) {
      valid = false;
    }

    if (!checkInt(MioBearing.class, mioBearing, "MioBearing")) {
      valid = false;
    }

    if (!checkInt(MioRangeRate.class, mioRangeRate, "MioRangeRate")) {
      valid = false;
    }

    if (!checkInt(Lane.class, lane, "Lane")) {
      valid = false;
    }

    if (!checkInt(StationID.class, forwardId, "ForwardID")) {
      valid = false;
    }

    if (!checkInt(StationID.class, backwardId, "BackwardID")) {
      valid = false;
    }

    if (!checkInt(MergeRequest.class, mergeRequest, "MergeRequest")) {
      valid = false;
    }

    if (!checkInt(MergeSafeToMerge.class, mergeSafeToMerge, "MergeSafeToMerge")) {
      valid = false;
    }

    if (!checkInt(MergeFlag.class, mergeFlag, "MergeFlag")) {
      valid = false;
    }

    if (!checkInt(MergeFlagTail.class, mergeFlagTail, "MergeFLagTail")) {
      valid = false;
    }

    if (!checkInt(MergeFlagHead.class, mergeFlagHead, "MergeFlagHead")) {
      valid = false;
    }

    if (!checkInt(PlatoonID.class, platoonId, "PlatoonID")) {
      valid = false;
    }

    if (!checkInt(DistanceTravelledCZ.class, distanceTravelledCz, "DistanceTravelledCz")) {
      valid = false;
    }

    if (!checkInt(Intention.class, intention, "Intention")) {
      valid = false;
    }

    if (!checkInt(Counter.class, counter, "Counter")) {
      valid = false;
    }
    return valid;
  }

  /**
   * Return the byte buffer representation of the message. Typically used when transmitting the
   * message over UDP.
   */
  public byte[] asByteArray() {
    byte[] packetBuffer = new byte[SIMPLE_iCLCM_LENGTH];
    ByteBuffer buffer = ByteBuffer.wrap(packetBuffer);
    buffer.put(messageId);
    buffer.putInt(stationId);
    buffer.put(containerMask);
    //HW Container
    buffer.putInt(rearAxleLocation);
    buffer.putInt(controllerType);
    buffer.putInt(responseTimeConstant);
    buffer.putInt(responseTimeDelay);
    buffer.putInt(targetLongAcc);
    buffer.putInt(timeHeadway);
    buffer.putInt(cruiseSpeed);
    //LF Container
    buffer.put(lowFrequencyMask);
    buffer.putInt(participantsReady);
    buffer.putInt(startPlatoon);
    buffer.putInt(endOfScenario);
    //MIO Container
    buffer.putInt(mioId);
    buffer.putInt(mioRange);
    buffer.putInt(mioBearing);
    buffer.putInt(mioRangeRate);
    //Lane container
    buffer.putInt(lane);
    //Pair ID container
    buffer.putInt(forwardId);
    buffer.putInt(backwardId);
    //Merge container
    buffer.putInt(mergeRequest);
    buffer.putInt(mergeSafeToMerge);
    buffer.putInt(mergeFlag);
    buffer.putInt(mergeFlagTail);
    buffer.putInt(mergeFlagHead);
    //Intersection Container
    buffer.putInt(platoonId);
    buffer.putInt(distanceTravelledCz);
    buffer.putInt(intention);
    buffer.putInt(counter);
    return packetBuffer;
  }

  /**
   * Return the proper iCLCM representation of the message. Typically used when transmitting the
   * message to another ITS station.
   */
  public IgameCooperativeLaneChangeMessage asIclcm() {
    VehicleContainerHighFrequency vehicleContainerHighFrequency =
        new VehicleContainerHighFrequency(
            new VehicleRearAxleLocation(rearAxleLocation),
            new ControllerType(controllerType),
            new VehicleResponseTime(
                new VehicleResponseTimeConstant(responseTimeConstant),
                new VehicleResponseTimeDelay(responseTimeDelay)),
            new TargetLongitudonalAcceleration(targetLongAcc),
            new TimeHeadway(timeHeadway),
            new CruiseSpeed(cruiseSpeed));

    VehicleContainerLowFrequency vehicleContainerLowFrequency =
        (containerMask & (1 << 7)) != 0
            ? VehicleContainerLowFrequency.builder()
                .participantsReady(
                    (lowFrequencyMask & (1 << 7)) != 0
                        ? new ParticipantsReady(participantsReady)
                        : null)
                .startPlatoon(
                    (lowFrequencyMask & (1 << 6)) != 0 ? new StartPlatoon(startPlatoon) : null)
                .endOfScenario(
                    (lowFrequencyMask & (1 << 5)) != 0 ? new EndOfScenario(endOfScenario) : null)
                .create()
            : null;

    MostImportantObjectContainer mostImportantObjectContainer =
        new MostImportantObjectContainer(
            new StationID(mioId),
            new MioRange(mioRange),
            new MioBearing(mioBearing),
            new MioRangeRate(mioRangeRate));

    LaneObject laneObject = new LaneObject(new Lane(lane));

    PairIdObject pairIdObject =
        new PairIdObject(
            new StationID(forwardId), new StationID(backwardId), new AcknowledgeFlag());

    MergeObject mergeObject =
        new MergeObject(
            new MergeRequest(mergeRequest),
            new MergeSafeToMerge(mergeSafeToMerge),
            new MergeFlag(mergeFlag),
            new MergeFlagTail(mergeFlagTail),
            new MergeFlagHead(mergeFlagHead));

    ScenarioObject scenarioObject =
        new ScenarioObject(
            new PlatoonID(platoonId),
            new DistanceTravelledCZ(distanceTravelledCz),
            new Intention(intention),
            new Counter(counter));

    IclmParameters iclmParameters =
        new IclmParameters(
            vehicleContainerHighFrequency,
            vehicleContainerLowFrequency,
            mostImportantObjectContainer,
            laneObject,
            pairIdObject,
            mergeObject,
            scenarioObject);

    //TODO: GenerationDeltaTime isn't part of the iCLCM spec in D3.2
    IgameCooperativeLaneChangeMessageBody igameCooperativeLaneChangeMessageBody =
        new IgameCooperativeLaneChangeMessageBody(new GenerationDeltaTime(), iclmParameters);

    return new IgameCooperativeLaneChangeMessage(
        new ItsPduHeader(
            new ProtocolVersion(1), new MessageId(Iclcm.MessageID_iCLCM), new StationID(stationId)),
        igameCooperativeLaneChangeMessageBody);
  }
}
