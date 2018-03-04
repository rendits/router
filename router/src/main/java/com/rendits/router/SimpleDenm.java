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
import java.util.concurrent.atomic.AtomicInteger;
import net.gcdc.asn1.datatypes.IntRange;
import net.gcdc.camdenm.CoopIts.*;
import net.gcdc.camdenm.CoopIts.ItsPduHeader.MessageId;
import net.gcdc.camdenm.CoopIts.ItsPduHeader.ProtocolVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Rendits Simple DENM</h1>
 *
 * <p>This class is built to be a simpler representation of the ETSI ITS-G5 DENM message. It
 * provides methods for converting to/from proper DENM messages and to/from byte buffers.
 *
 * @author Albin Severinson (albin@rendits.com)
 * @version 1.0.0-SNAPSHOT
 */
public class SimpleDenm {
  private static final Logger logger = LoggerFactory.getLogger(Router.class);
  private static final int SIMPLE_DENM_LENGTH = 101;

  /* The sequence number of the DENM */
  private static AtomicInteger denmSequenceCounter = new AtomicInteger();
  private final int denmSequenceNumber;

  final byte messageId;
  final int stationId;
  final int generationDeltaTime;
  final byte containerMask;
  final byte managementMask;
  final int detectionTime;
  final int referenceTime;
  final int termination;
  final int latitude;
  final int longitude;
  final int semiMajorConfidence;
  final int semiMinorConfidence;
  final int semiMajorOrientation;
  final int altitude;
  final int relevanceDistance;
  final int relevanceTrafficDirection;
  final int validityDuration;
  final int transmissionInterval;
  final int stationType;
  final byte situationMask;
  final int informationQuality;
  final int causeCode;
  final int subCauseCode;
  final int linkedCauseCode;
  final int linkedSubCauseCode;
  final byte alacarteMask;
  final int lanePosition;
  final int temperature;
  final int positioningSolutionType;

  /** Create a simple DENM by supplying the values manually. */
  public SimpleDenm(
      int stationId,
      int generationDeltaTime,
      byte containerMask,
      byte managementMask,
      int detectionTime,
      int referenceTime,
      int termination,
      int latitude,
      int longitude,
      int semiMajorConfidence,
      int semiMinorConfidence,
      int semiMajorOrientation,
      int altitude,
      int relevanceDistance,
      int relevanceTrafficDirection,
      int validityDuration,
      int transmissionInterval,
      int stationType,
      byte situationMask,
      int informationQuality,
      int causeCode,
      int subCauseCode,
      int linkedCauseCode,
      int linkedSubCauseCode,
      byte alacarteMask,
      int lanePosition,
      int temperature,
      int positioningSolutionType) {

    this.denmSequenceNumber = denmSequenceCounter.getAndIncrement();
    this.messageId = MessageId.denm;
    this.stationId = stationId;
    this.generationDeltaTime = generationDeltaTime;
    this.containerMask = containerMask;
    this.managementMask = managementMask;
    this.detectionTime = detectionTime;
    this.referenceTime = referenceTime;
    this.termination = termination;
    this.latitude = latitude;
    this.longitude = longitude;
    this.semiMajorConfidence = semiMajorConfidence;
    this.semiMinorConfidence = semiMinorConfidence;
    this.semiMajorOrientation = semiMajorOrientation;
    this.altitude = altitude;
    this.relevanceDistance = relevanceDistance;
    this.relevanceTrafficDirection = relevanceTrafficDirection;
    this.validityDuration = validityDuration;
    this.transmissionInterval = transmissionInterval;
    this.stationType = stationType;
    this.situationMask = situationMask;
    this.informationQuality = informationQuality;
    this.causeCode = causeCode;
    this.subCauseCode = subCauseCode;
    this.linkedCauseCode = linkedCauseCode;
    this.linkedSubCauseCode = linkedSubCauseCode;
    this.alacarteMask = alacarteMask;
    this.lanePosition = lanePosition;
    this.temperature = temperature;
    this.positioningSolutionType = positioningSolutionType;
  }

  /**
   * Create a simple DENM from its byte buffer representation. This constructor is typically used to
   * create a simple DENM from the payload of a received byte buffer.
   *
   * @param receivedData Byte buffer representation of the message.
   */
  public SimpleDenm(byte[] receivedData) {
    if (receivedData.length < SIMPLE_DENM_LENGTH) {
      logger.error(
          "Simple DENM is too short. Is: {} Should be: {}",
          receivedData.length,
          SIMPLE_DENM_LENGTH);
      throw new IllegalArgumentException();
    }

    this.denmSequenceNumber = denmSequenceCounter.getAndIncrement();

    /* Assign values, checking if they are valid. Invalid values
    are replaced with default values if possible. */
    ByteBuffer buffer = ByteBuffer.wrap(receivedData);
    messageId = buffer.get();
    if (messageId != MessageId.denm) {
      logger.error("Simple DENM has incorrect id. Id: {} Should be: {}", messageId, MessageId.denm);
      throw new IllegalArgumentException();
    }

    stationId = buffer.getInt();
    if (!checkInt(StationID.class, stationId, "StationID")) {
      throw new IllegalArgumentException();
    }

    generationDeltaTime = buffer.getInt();
    if (!checkInt(GenerationDeltaTime.class, generationDeltaTime, "GenerationDeltaTime")) {
      throw new IllegalArgumentException();
    }

    containerMask = buffer.get();
    managementMask = buffer.get();

    detectionTime = buffer.getInt();
    referenceTime = buffer.getInt();

    /* These timestamps are handled differently that what the
     * spec. states. As a workaround to Matlab not supporting long
     * these values are sent as number of increments of 65536ms.
     * We get the true timestamps by multiplying with 65536 and
     * adding the generationDeltaTime.
     */
    if (!checkInt(
        TimestampIts.class, detectionTime * 65536 + generationDeltaTime, "DetectionTime")) {
      throw new IllegalArgumentException();
    }
    if (!checkInt(
        TimestampIts.class, referenceTime * 65536 + generationDeltaTime, "ReferenceTime")) {
      throw new IllegalArgumentException();
    }

    int termination = buffer.getInt();
    if (Termination.isMember(termination)) {
      this.termination = termination;
    } else {
      logger.warn("Termination is not valid. Value={}", termination);
      this.termination = (int) Termination.defaultValue().value();
    }

    int latitude = buffer.getInt();
    if (checkInt(Latitude.class, latitude, "Latitude")) {
      this.latitude = latitude;
    } else {
      this.latitude = Latitude.unavailable;
    }

    int longitude = buffer.getInt();
    if (checkInt(Longitude.class, longitude, "Longitude")) {
      this.longitude = longitude;
    } else {
      this.longitude = Longitude.unavailable;
    }

    int semiMajorConfidence = buffer.getInt();
    if (checkInt(SemiAxisLength.class, semiMajorConfidence, "SemiMajorConfidence")) {
      this.semiMajorConfidence = semiMajorConfidence;
    } else {
      this.semiMajorConfidence = SemiAxisLength.unavailable;
    }

    int semiMinorConfidence = buffer.getInt();
    if (checkInt(SemiAxisLength.class, semiMinorConfidence, "SemiMinorConfidence")) {
      this.semiMinorConfidence = semiMinorConfidence;
    } else {
      this.semiMinorConfidence = SemiAxisLength.unavailable;
    }

    int semiMajorOrientation = buffer.getInt();
    if (checkInt(HeadingValue.class, semiMajorOrientation, "SemiMajorOrientation")) {
      this.semiMajorOrientation = semiMajorOrientation;
    } else {
      this.semiMajorOrientation = HeadingValue.unavailable;
    }

    int altitude = buffer.getInt();
    if (checkInt(AltitudeValue.class, altitude, "Altitude")) {
      this.altitude = altitude;
    } else {
      this.altitude = AltitudeValue.unavailable;
    }

    int relevanceDistance = buffer.getInt();
    if (RelevanceDistance.isMember(relevanceDistance)) {
      this.relevanceDistance = relevanceDistance;
    } else {
      logger.warn("RelevanceDistance is not valid. Value={}", relevanceDistance);
      this.relevanceDistance = (int) RelevanceDistance.defaultValue().value();
    }

    int relevanceTrafficDirection = buffer.getInt();
    if (RelevanceTrafficDirection.isMember(relevanceTrafficDirection)) {
      this.relevanceTrafficDirection = relevanceTrafficDirection;
    } else {
      logger.error("RelevanceTrafficDirection is not valid. Value={}", relevanceTrafficDirection);
      this.relevanceTrafficDirection = RelevanceTrafficDirection.defaultValue().value();
    }

    int validityDuration = buffer.getInt();
    if (checkInt(ValidityDuration.class, validityDuration, "ValidityDuration")) {
      this.validityDuration = validityDuration;
    } else {
      this.validityDuration = (int) net.gcdc.camdenm.CoopIts.defaultValidity.value;
    }

    int transmissionInterval = buffer.getInt();
    if (checkInt(TransmissionInterval.class, transmissionInterval, "TransmissionInterval")) {
      this.transmissionInterval = transmissionInterval;
    } else {
      this.transmissionInterval = TransmissionInterval.oneMilliSecond * 100;
    }

    int stationType = buffer.getInt();
    if (checkInt(StationType.class, stationType, "StationType")) {
      this.stationType = stationType;
    } else {
      this.stationType = StationType.unknown;
    }

    this.situationMask = buffer.get();
    int informationQuality = buffer.getInt();
    if (checkInt(InformationQuality.class, informationQuality, "InformationQuality")) {
      this.informationQuality = informationQuality;
    } else {
      this.informationQuality = InformationQuality.unavailable;
    }

    this.causeCode = buffer.getInt();
    if (!checkInt(CauseCodeType.class, causeCode, "CauseCode")) {
      throw new IllegalArgumentException();
    }

    this.subCauseCode = buffer.getInt();
    if (!checkInt(SubCauseCodeType.class, subCauseCode, "SubCauseCode")) {
      throw new IllegalArgumentException();
    }

    this.linkedCauseCode = buffer.getInt();
    if (!checkInt(CauseCodeType.class, linkedCauseCode, "LinkedCauseCode")) {
      throw new IllegalArgumentException();
    }

    this.linkedSubCauseCode = buffer.getInt();
    if (!checkInt(SubCauseCodeType.class, linkedSubCauseCode, "LinkedSubCauseCode")) {
      throw new IllegalArgumentException();
    }

    this.alacarteMask = buffer.get();
    this.lanePosition = buffer.getInt();
    if (!checkInt(LanePosition.class, lanePosition, "LanePosition")) {
      throw new IllegalArgumentException();
    }

    int temperature = buffer.getInt();
    if (checkInt(Temperature.class, temperature, "Temperature")) {
      this.temperature = temperature;
    } else {
      this.temperature = 0;
    }

    this.positioningSolutionType = buffer.getInt();
    if (!PositioningSolutionType.isMember(positioningSolutionType)) {
      logger.warn(
          "PositioningSolutionType is not valid. Value={ } else { }", positioningSolutionType);
      throw new IllegalArgumentException();
    }
  }

  /**
   * Create a simple DENM from a proper DENM. This constructor is typically used after receiving a
   * proper DENM from another ITS station.
   *
   * @param denmPacket A proper DENM message.
   */
  public SimpleDenm(Denm denmPacket) {
    DecentralizedEnvironmentalNotificationMessage denm = denmPacket.getDenm();
    ItsPduHeader header = denmPacket.getHeader();
    ManagementContainer managementContainer = denm.getManagement();
    messageId = (byte) header.getMessageID().value;
    stationId = (int) header.getStationID().value;
    generationDeltaTime = (int) managementContainer.getReferenceTime().value % 65536;
    byte containerMask = 0;

    if (messageId != MessageId.denm) {
      logger.warn("Malformed message on BTP port 2002 from station with ID {}", stationId);
      throw new IllegalArgumentException("Malformed message on BTP port 2002");
    }

    /* ManagementContainer */
    byte managementMask = 0;

    denmSequenceNumber = (int) managementContainer.getActionID().getSequenceNumber().value;
    detectionTime = (int) managementContainer.getDetectionTime().value / 65536;
    referenceTime = (int) managementContainer.getReferenceTime().value / 65536;

    if (managementContainer.hasTermination()) {
      managementMask += (1 << 7);
      termination = (int) managementContainer.getTermination().value();
    } else {
      termination = (int) Termination.defaultValue().value();
    }

    latitude = (int) managementContainer.getEventPosition().getLatitude().value;
    longitude = (int) managementContainer.getEventPosition().getLongitude().value;
    semiMajorConfidence =
        (int)
            managementContainer
                .getEventPosition()
                .getPositionConfidenceEllipse()
                .getSemiMajorConfidence()
                .value;
    semiMinorConfidence =
        (int)
            managementContainer
                .getEventPosition()
                .getPositionConfidenceEllipse()
                .getSemiMinorConfidence()
                .value;
    semiMajorOrientation =
        (int)
            managementContainer
                .getEventPosition()
                .getPositionConfidenceEllipse()
                .getSemiMajorOrientation()
                .value;
    altitude = (int) managementContainer.getEventPosition().getAltitude().getAltitudeValue().value;

    if (managementContainer.hasRelevanceDistance()) {
      managementMask += (1 << 6);
      relevanceDistance = (int) managementContainer.getRelevanceDistance().value();
    } else {
      relevanceDistance = (int) RelevanceDistance.defaultValue().value();
    }

    if (managementContainer.hasRelevanceTrafficDirection()) {
      managementMask += (1 << 5);
      relevanceTrafficDirection = (int) managementContainer.getRelevanceTrafficDirection().value();
    } else {
      relevanceTrafficDirection = RelevanceTrafficDirection.defaultValue().value();
    }

    if (managementContainer.hasValidityDuration()) {
      managementMask += (1 << 4);
      validityDuration = (int) managementContainer.getValidityDuration().value;
    } else {
      validityDuration = (int) net.gcdc.camdenm.CoopIts.defaultValidity.value;
    }

    if (managementContainer.hasTransmissionInterval()) {
      managementMask += (1 << 3);
      transmissionInterval = (int) managementContainer.getTransmissionInterval().value;
    } else {
      transmissionInterval = TransmissionInterval.oneMilliSecond * 100;
    }

    stationType = (int) managementContainer.getStationType().value;
    this.managementMask = managementMask;

    /* SituationContainer */
    SituationContainer situationContainer = null;
    byte situationMask = 0;
    if (denm.hasSituation()) {
      containerMask += (1 << 7);
      situationContainer = denm.getSituation();

      informationQuality = (int) situationContainer.getInformationQuality().value;
      causeCode = (int) situationContainer.getEventType().getCauseCode().value;
      subCauseCode = (int) situationContainer.getEventType().getSubCauseCode().value;

      if (situationContainer.hasLinkedCause()) {
        situationMask += (1 << 7);
        linkedCauseCode = (int) situationContainer.getLinkedCause().getCauseCode().value;
        linkedSubCauseCode = (int) situationContainer.getLinkedCause().getSubCauseCode().value;
      } else {
        linkedCauseCode = 0;
        linkedSubCauseCode = 0;
      }
    } else {
      informationQuality = InformationQuality.unavailable;
      causeCode = 0;
      subCauseCode = 0;
      linkedCauseCode = 0;
      linkedSubCauseCode = 0;
    }
    this.situationMask = situationMask;

    /* Not used for GCDC16 */
    /* LocationContainer */
    /*
      LocationContainer locationContainer = null;
      if(denm.hasLocation()){
      containerMask += (1<<6);
      locationContainer = denm.getLocation();
      byte locationMask = 0;
      buffer.put(locationMask);

      if(locationContainer.hasEventSpeed()){
      locationMask += (1<<7);
      buffer.putInt((int) locationContainer.getEventSpeed().getSpeedValue().value;
      buffer.putInt((int) locationContainer.getEventSpeed().getSpeedConfidence().value;
      }else buffer.put(new byte[2*4]);

      if(locationContainer.hasEventSpeed()){
      locationMask += (1<<6);
      buffer.putInt((int) locationContainer.getEventPositionHeading().getHeadingValue().value;
      buffer.putInt((int) locationContainer.getEventPositionHeading().getHeadingConfidence().value;
      }else buffer.put(new byte[2*4]);

      if(locationContainer.hasEventSpeed()){
      locationMask += (1<<5);
      buffer.putInt((int) locationContainer.getRoadType().value();
      }else buffer.putInt(0);

      //Need to update the mask since it has been changed
      buffer.put(headerLength + managementLength + situationLength, locationMask);
      }else buffer.put(new byte[locationLength]);
    */

    /* AlacarteContainer */
    AlacarteContainer alacarteContainer = null;

    byte alacarteMask = 0;
    if (denm.hasAlacarte()) {
      containerMask += (1 << 5);
      alacarteContainer = denm.getAlacarte();

      if (alacarteContainer.hasLanePosition()) {
        alacarteMask += (1 << 7);
        lanePosition = (int) alacarteContainer.getLanePosition().value;
      } else {
        lanePosition = 0;
      }

      if (alacarteContainer.hasExternalTemperature()) {
        alacarteMask += (1 << 5);
        temperature = (int) alacarteContainer.getExternalTemperature().value;
      } else {
        temperature = 0;
      }

      if (alacarteContainer.hasPositioningSolution()) {
        alacarteMask += (1 << 3);
        positioningSolutionType = (int) alacarteContainer.getPositioningSolution().value();
      } else {
        positioningSolutionType = 0;
      }
    } else {
      lanePosition = 0;
      temperature = 0;
      positioningSolutionType = 0;
    }
    this.alacarteMask = alacarteMask;
    this.containerMask = containerMask;
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

    SimpleDenm simpleDenm = (SimpleDenm) object;

    // field comparison
    return messageId == simpleDenm.messageId
        && stationId == simpleDenm.stationId
        && generationDeltaTime == simpleDenm.generationDeltaTime
        && containerMask == simpleDenm.containerMask
        && managementMask == simpleDenm.managementMask
        && detectionTime == simpleDenm.detectionTime
        && referenceTime == simpleDenm.referenceTime
        && termination == simpleDenm.termination
        && latitude == simpleDenm.latitude
        && longitude == simpleDenm.longitude
        && semiMajorConfidence == simpleDenm.semiMajorConfidence
        && semiMinorConfidence == simpleDenm.semiMinorConfidence
        && semiMajorOrientation == simpleDenm.semiMajorOrientation
        && altitude == simpleDenm.altitude
        && relevanceDistance == simpleDenm.relevanceDistance
        && relevanceTrafficDirection == simpleDenm.relevanceTrafficDirection
        && validityDuration == simpleDenm.validityDuration
        && transmissionInterval == simpleDenm.transmissionInterval
        && stationType == simpleDenm.stationType
        && situationMask == simpleDenm.situationMask
        && informationQuality == simpleDenm.informationQuality
        && causeCode == simpleDenm.causeCode
        && subCauseCode == simpleDenm.subCauseCode
        && linkedCauseCode == simpleDenm.linkedCauseCode
        && linkedSubCauseCode == simpleDenm.linkedSubCauseCode
        && alacarteMask == simpleDenm.alacarteMask
        && lanePosition == simpleDenm.lanePosition
        && temperature == simpleDenm.temperature
        && positioningSolutionType == simpleDenm.positioningSolutionType;
  }

  /** Check if the simple DENM is valid by checking if all values fall within their range. */
  boolean isValid() {
    boolean valid = true;

    if (!checkInt(StationID.class, stationId, "StationID")) {
      valid = false;
    }

    if (!checkInt(GenerationDeltaTime.class, generationDeltaTime, "GenerationDeltaTime")) {
      valid = false;
    }

    /* These timestamps are handled differently that what the
     * spec. states. As a workaround to Matlab not supporting long
     * these values are sent as number of increments of 65536ms.
     * We get the true timestamps by multiplying with 65536 and
     * adding the generationDeltaTime.
     */
    if (!checkInt(
        TimestampIts.class, detectionTime * 65536 + generationDeltaTime, "DetectionTime")) {
      valid = false;
    }

    if (!checkInt(
        TimestampIts.class, referenceTime * 65536 + generationDeltaTime, "ReferenceTime")) {
      valid = false;
    }

    if (!Termination.isMember(termination)) {
      logger.error("Termination is not valid. Value={}", termination);
      valid = false;
    }
    if (!checkInt(Latitude.class, latitude, "Latitude")) {
      valid = false;
    }

    if (!checkInt(Longitude.class, longitude, "Longitude")) {
      valid = false;
    }

    if (!checkInt(SemiAxisLength.class, semiMajorConfidence, "SemiMajorConfidence")) {
      valid = false;
    }

    if (!checkInt(SemiAxisLength.class, semiMinorConfidence, "SemiMinorConfidence")) {
      valid = false;
    }

    if (!checkInt(HeadingValue.class, semiMajorOrientation, "SemiMajorOrientation")) {
      valid = false;
    }

    if (!checkInt(AltitudeValue.class, altitude, "Altitude")) {
      valid = false;
    }

    if (!RelevanceDistance.isMember(relevanceDistance)) {
      logger.error("RelevanceDistance is not valid. Value={}", relevanceDistance);
      valid = false;
    }

    if (!RelevanceTrafficDirection.isMember(relevanceTrafficDirection)) {
      logger.error("RelevanceTrafficDirection is not valid. Value={}", relevanceTrafficDirection);
      valid = false;
    }

    if (!checkInt(ValidityDuration.class, validityDuration, "ValidityDuration")) {
      valid = false;
    }

    if (!checkInt(TransmissionInterval.class, transmissionInterval, "TransmissionInterval")) {
      valid = false;
    }

    if (!checkInt(StationType.class, stationType, "StationType")) {
      valid = false;
    }

    if (!checkInt(InformationQuality.class, informationQuality, "InformationQuality")) {
      valid = false;
    }

    if (!checkInt(CauseCodeType.class, causeCode, "CauseCode")) {
      valid = false;
    }

    if (!checkInt(SubCauseCodeType.class, subCauseCode, "SubCauseCode")) {
      valid = false;
    }

    if (!checkInt(CauseCodeType.class, linkedCauseCode, "LinkedCauseCode")) {
      valid = false;
    }
    if (!checkInt(SubCauseCodeType.class, linkedSubCauseCode, "LinkedSubCauseCode")) {
      valid = false;
    }

    if (!checkInt(LanePosition.class, lanePosition, "LanePosition")) {
      valid = false;
    }

    if (!checkInt(Temperature.class, temperature, "Temperature")) {
      valid = false;
    }

    if (!PositioningSolutionType.isMember(positioningSolutionType)) {
      logger.error("PositioningSolutionType is not valid. Value={}", positioningSolutionType);
      valid = false;
    }

    return valid;
  }

  /**
   * Return the byte buffer representation of the message. Typically used when transmitting the
   * message over UDP.
   */
  byte[] asByteArray() {
    byte[] packetBuffer = new byte[SIMPLE_DENM_LENGTH];
    ByteBuffer buffer = ByteBuffer.wrap(packetBuffer);
    buffer.put(messageId);
    buffer.putInt(stationId);
    buffer.putInt(generationDeltaTime);
    buffer.put(containerMask);
    buffer.put(managementMask);
    buffer.putInt(detectionTime);
    buffer.putInt(referenceTime);
    buffer.putInt(termination);
    buffer.putInt(latitude);
    buffer.putInt(longitude);
    buffer.putInt(semiMajorConfidence);
    buffer.putInt(semiMinorConfidence);
    buffer.putInt(semiMajorOrientation);
    buffer.putInt(altitude);
    buffer.putInt(relevanceDistance);
    buffer.putInt(relevanceTrafficDirection);
    buffer.putInt(validityDuration);
    buffer.putInt(transmissionInterval);
    buffer.putInt(stationType);
    buffer.put(situationMask);
    buffer.putInt(informationQuality);
    buffer.putInt(causeCode);
    buffer.putInt(subCauseCode);
    buffer.putInt(linkedCauseCode);
    buffer.putInt(linkedSubCauseCode);
    buffer.put(alacarteMask);
    buffer.putInt(lanePosition);
    buffer.putInt(temperature);
    buffer.putInt(positioningSolutionType);
    return packetBuffer;
  }

  /**
   * Return the proper DENM representation of the message. Typically used when transmitting the
   * message to another ITS station.
   */
  Denm asDenm() {
    /* Management container */
    ManagementContainer managementContainer =
        ManagementContainer.builder()
            .actionID(
                new ActionID(
                    new StationID(stationId), new SequenceNumber(denmSequenceNumber % 65535)))

            /* Referencetime is sent in increments of 65536ms. So to
             * get the current time we need to multiply with that and
             * add the generationDeltaTime. This is a workaround for
             * Simulink not supporting longs.
             */
            .detectionTime(new TimestampIts((long) detectionTime * 65536 + generationDeltaTime))
            .referenceTime(new TimestampIts((long) referenceTime * 65536 + generationDeltaTime))
            .termination(
                (managementMask & (1 << 7)) != 0 ? Termination.values()[termination] : null)
            .eventPosition(
                new ReferencePosition(
                    new Latitude(latitude),
                    new Longitude(longitude),
                    new PosConfidenceEllipse(
                        new SemiAxisLength(semiMajorConfidence),
                        new SemiAxisLength(semiMinorConfidence),
                        new HeadingValue(semiMajorOrientation)),
                    new Altitude(
                        new AltitudeValue(altitude),
                        //TODO: Should altitudeconfidence be added?
                        AltitudeConfidence.unavailable)))
            .relevanceDistance(
                (managementMask & (1 << 6)) != 0
                    ? RelevanceDistance.values()[relevanceDistance]
                    : null)
            .relevanceTrafficDirection(
                (managementMask & (1 << 5)) != 0
                    ? RelevanceTrafficDirection.values()[relevanceTrafficDirection]
                    : null)
            .validityDuration(
                (managementMask & (1 << 4)) != 0 ? new ValidityDuration(validityDuration) : null)
            .transmissionInterval(
                (managementMask & (1 << 3)) != 0
                    ? new TransmissionInterval(transmissionInterval)
                    : null)
            .stationType(new StationType(stationType))
            .create();

    /* Situation container */
    SituationContainer situationContainer =
        (containerMask & (1 << 7)) != 0
            ? new SituationContainer(
                new InformationQuality(informationQuality),
                new CauseCode(new CauseCodeType(causeCode), new SubCauseCodeType(subCauseCode)),
                (situationMask & (1 << 7)) != 0
                    ? new CauseCode(
                        new CauseCodeType(linkedCauseCode),
                        new SubCauseCodeType(linkedSubCauseCode))
                    : null,
                //TODO: Add EventHistory to SituationContainer
                null)
            : null;

    /* Location container */
    LocationContainer locationContainer =
        (containerMask & (1 << 6)) != 0 ? new LocationContainer() : null;

    /* Alacarte container */
    AlacarteContainer alacarteContainer =
        (containerMask & (1 << 5)) != 0
            ? new AlacarteContainer(
                (alacarteMask & (1 << 7)) != 0 ? new LanePosition(lanePosition) : null,
                //TODO: Currently no plans of implementing.
                (alacarteMask & (1 << 6)) != 0 ? new ImpactReductionContainer() : null,
                (alacarteMask & (1 << 5)) != 0 ? new Temperature(temperature) : null,
                //TODO: Currently no plans of implementing.
                (alacarteMask & (1 << 4)) != 0 ? new RoadWorksContainerExtended() : null,
                (alacarteMask & (1 << 3)) != 0
                    ? PositioningSolutionType.values()[positioningSolutionType]
                    : null,
                //TODO: Currently no plans of implementing.
                (alacarteMask & (1 << 2)) != 0 ? new StationaryVehicleContainer() : null)
            : null;

    DecentralizedEnvironmentalNotificationMessage decentralizedEnvironmentalNotificationMessage =
        new DecentralizedEnvironmentalNotificationMessage(
            managementContainer, situationContainer, locationContainer, alacarteContainer);

    return new Denm(
        new ItsPduHeader(
            new ProtocolVersion(1), new MessageId(MessageId.denm), new StationID(stationId)),
        decentralizedEnvironmentalNotificationMessage);
  }
}
