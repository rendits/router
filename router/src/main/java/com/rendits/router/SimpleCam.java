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

import java.lang.IllegalArgumentException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import net.gcdc.asn1.datatypes.IntRange;
import net.gcdc.camdenm.CoopIts.*;
import net.gcdc.camdenm.CoopIts.ItsPduHeader.MessageId;
import net.gcdc.camdenm.CoopIts.ItsPduHeader.ProtocolVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Rendits Simple CAM</h1>
 *
 * <p>This class is built to be a simpler representation of the ETSI ITS-G5 CAM message. It provides
 * methods for converting to/from proper CAM messages and to/from byte buffers.
 *
 * @author Albin Severinson (albin@rendits.com)
 * @version 1.0.0-SNAPSHOT
 */
public class SimpleCam {
  private static final Logger logger = LoggerFactory.getLogger(Router.class);
  private static final int SIMPLE_CAM_LENGTH = 82;

  final byte messageId;
  final int stationId;
  final int genDeltaTimeMillis;
  final byte containerMask;
  final int stationType;
  final int latitude;
  final int longitude;
  final int semiMajorAxisConfidence;
  final int semiMinorAxisConfidence;
  final int semiMajorOrientation;
  final int altitude;
  final int heading;
  final int headingConfidence;
  final int speed;
  final int speedConfidence;
  final int vehicleLength;
  final int vehicleWidth;
  final int longitudinalAcceleration;
  final int longitudinalAccelerationConfidence;
  final int yawRate;
  final int yawRateConfidence;
  final int vehicleRole;

  /** Create a simple CAM by supplying the values manually. */
  public SimpleCam(
      int stationId,
      int genDeltaTimeMillis,
      byte containerMask,
      int stationType,
      int latitude,
      int longitude,
      int semiMajorAxisConfidence,
      int semiMinorAxisConfidence,
      int semiMajorOrientation,
      int altitude,
      int heading,
      int headingConfidence,
      int speed,
      int speedConfidence,
      int vehicleLength,
      int vehicleWidth,
      int longitudinalAcceleration,
      int longitudinalAccelerationConfidence,
      int yawRate,
      int yawRateConfidence,
      int vehicleRole) {

    this.messageId = MessageId.cam;
    this.stationId = stationId;
    this.genDeltaTimeMillis = genDeltaTimeMillis;
    this.containerMask = containerMask;
    this.stationType = stationType;
    this.latitude = latitude;
    this.longitude = longitude;
    this.semiMajorAxisConfidence = semiMajorAxisConfidence;
    this.semiMinorAxisConfidence = semiMinorAxisConfidence;
    this.semiMajorOrientation = semiMajorOrientation;
    this.altitude = altitude;
    this.heading = heading;
    this.headingConfidence = headingConfidence;
    this.speed = speed;
    this.speedConfidence = speedConfidence;
    this.vehicleLength = vehicleLength;
    this.vehicleWidth = vehicleWidth;
    this.longitudinalAcceleration = longitudinalAcceleration;
    this.longitudinalAccelerationConfidence = longitudinalAccelerationConfidence;
    this.yawRate = yawRate;
    this.yawRateConfidence = yawRateConfidence;
    this.vehicleRole = vehicleRole;
  }

  /**
   * Create a simple CAM from its byte buffer representation. This constructor is typically used to
   * create a simple CAM from the payload of a received byte buffer.
   *
   * @param receivedData Byte buffer representation of the message.
   */
  public SimpleCam(byte[] receivedData) {
    if (receivedData.length < SIMPLE_CAM_LENGTH) {
      logger.error(
          "Simple CAM is too short. Is: {} Should be: {}", receivedData.length, SIMPLE_CAM_LENGTH);
      throw new IllegalArgumentException();
    }

    /* Assign values, checking if they are valid. Invalid values
    are replaced with default values if possible. */
    ByteBuffer buffer = ByteBuffer.wrap(receivedData);
    this.messageId = buffer.get();
    if (messageId != MessageId.cam) {
      logger.error("MessageID is: {} Should be: {}", messageId, MessageId.cam);
      throw new IllegalArgumentException();
    }

    this.stationId = buffer.getInt();
    if (!checkInt(StationID.class, stationId, "StationID")) {
      throw new IllegalArgumentException();
    }

    this.genDeltaTimeMillis = buffer.getInt();
    if (!checkInt(GenerationDeltaTime.class, genDeltaTimeMillis, "GenerationDeltaTime")) {
      throw new IllegalArgumentException();
    }

    this.containerMask = buffer.get();
    int stationType = buffer.getInt();
    if (checkInt(StationType.class, stationType, "StationType")) {
      this.stationType = stationType;
    } else {
      this.stationType = StationType.unknown;
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

    int semiMajorAxisConfidence = buffer.getInt();
    if (checkInt(SemiAxisLength.class, semiMajorAxisConfidence, "SemiMajorConfidence")) {
      this.semiMajorAxisConfidence = semiMajorAxisConfidence;
    } else {
      this.semiMajorAxisConfidence = SemiAxisLength.unavailable;
    }

    int semiMinorAxisConfidence = buffer.getInt();
    if (checkInt(SemiAxisLength.class, semiMinorAxisConfidence, "SemiMinorConfidence")) {
      this.semiMinorAxisConfidence = semiMinorAxisConfidence;
    } else {
      this.semiMinorAxisConfidence = SemiAxisLength.unavailable;
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

    int heading = buffer.getInt();
    if (checkInt(HeadingValue.class, heading, "Heading")) {
      this.heading = heading;
    } else {
      this.heading = HeadingValue.unavailable;
    }

    int headingConfidence = buffer.getInt();
    if (checkInt(HeadingConfidence.class, headingConfidence, "HeadingConfidence")) {
      this.headingConfidence = headingConfidence;
    } else {
      this.headingConfidence = HeadingConfidence.unavailable;
    }

    int speed = buffer.getInt();
    if (checkInt(SpeedValue.class, speed, "Speed")) {
      this.speed = speed;
    } else {
      this.speed = SpeedValue.unavailable;
    }

    int speedConfidence = buffer.getInt();
    if (checkInt(SpeedConfidence.class, speedConfidence, "SpeedConfidence")) {
      this.speedConfidence = speedConfidence;
    } else {
      this.speedConfidence = SpeedConfidence.unavailable;
    }

    int vehicleLength = buffer.getInt();
    if (checkInt(VehicleLengthValue.class, vehicleLength, "VehicleLength")) {
      this.vehicleLength = vehicleLength;
    } else {
      this.vehicleLength = VehicleLengthValue.unavailable;
    }

    int vehicleWidth = buffer.getInt();
    if (checkInt(VehicleWidth.class, vehicleWidth, "VehicleWidth")) {
      this.vehicleWidth = vehicleWidth;
    } else {
      this.vehicleWidth = VehicleWidth.unavailable;
    }

    int longitudinalAcceleration = buffer.getInt();
    if (checkInt(
        LongitudinalAccelerationValue.class,
        longitudinalAcceleration,
        "LongitudinalAcceleration")) {
      this.longitudinalAcceleration = longitudinalAcceleration;
    } else {
      this.longitudinalAcceleration = LongitudinalAccelerationValue.unavailable;
    }

    int longitudinalAccelerationConfidence = buffer.getInt();
    if (checkInt(
        AccelerationConfidence.class,
        longitudinalAccelerationConfidence,
        "LongitudinalAccelerationConfidence")) {
      this.longitudinalAccelerationConfidence = longitudinalAccelerationConfidence;
    } else {
      this.longitudinalAccelerationConfidence = AccelerationConfidence.unavailable;
    }

    int yawRate = buffer.getInt();
    if (checkInt(YawRateValue.class, yawRate, "YawRate")) {
      this.yawRate = yawRate;
    } else {
      this.yawRate = YawRateValue.unavailable;
    }

    int yawRateConfidence = buffer.getInt();
    /* TODO: Find a cleaner way to check enums. Also, this
     * approach is not very informative.*/
    if (YawRateConfidence.isMember(yawRateConfidence)) {
      this.yawRateConfidence = yawRateConfidence;
    } else {
      logger.warn("YawRateConfidence is not valid. Value={}", yawRateConfidence);
      this.yawRateConfidence = (int) YawRateConfidence.unavailable.value();
    }

    int vehicleRole = buffer.getInt();
    if (this.hasLowFrequencyContainer()) {
      if (VehicleRole.isMember(vehicleRole)) {
        this.vehicleRole = vehicleRole;
      } else {
        logger.warn("VehicleRole is not valid. Value={}", vehicleRole);
        this.vehicleRole = (int) VehicleRole.default_.value();
      }
    } else {
      this.vehicleRole = (int) VehicleRole.default_.value();
    }
  }

  /**
   * Create a simple CAM from a proper CAM. This constructor is typically used after receiving a
   * proper CAM from another ITS station.
   *
   * @param camPacket A proper CAM message.
   */
  public SimpleCam(Cam camPacket) {
    CoopAwareness cam = camPacket.getCam();
    ItsPduHeader header = camPacket.getHeader();
    GenerationDeltaTime generationDeltaTime = cam.getGenerationDeltaTime();

    messageId = (byte) header.getMessageID().value;
    stationId = (int) header.getStationID().value;
    genDeltaTimeMillis = (int) generationDeltaTime.value;
    byte containerMask = 0;

    if (messageId != MessageId.cam) {
      logger.warn("Malformed message on BTP port 2001 from station with ID {}", stationId);
      throw new IllegalArgumentException("Malformed message on BTP port 2001");
    }

    CamParameters camParameters = cam.getCamParameters();

    /* BasicContainer */
    BasicContainer basicContainer = camParameters.getBasicContainer();
    stationType = (int) basicContainer.getStationType().value;
    latitude = (int) basicContainer.getReferencePosition().getLatitude().value;
    longitude = (int) basicContainer.getReferencePosition().getLongitude().value;
    semiMajorAxisConfidence =
        (int)
            basicContainer
                .getReferencePosition()
                .getPositionConfidenceEllipse()
                .getSemiMajorConfidence()
                .value;
    semiMinorAxisConfidence =
        (int)
            basicContainer
                .getReferencePosition()
                .getPositionConfidenceEllipse()
                .getSemiMinorConfidence()
                .value;
    semiMajorOrientation =
        (int)
            basicContainer
                .getReferencePosition()
                .getPositionConfidenceEllipse()
                .getSemiMajorOrientation()
                .value;
    altitude = (int) basicContainer.getReferencePosition().getAltitude().getAltitudeValue().value;

    /* HighFrequencyContainer */
    HighFrequencyContainer highFrequencyContainer = camParameters.getHighFrequencyContainer();
    BasicVehicleContainerHighFrequency basicVehicleContainerHighFrequency =
        highFrequencyContainer.getBasicVehicleContainerHighFrequency();

    heading = (int) basicVehicleContainerHighFrequency.getHeading().getHeadingValue().value;
    headingConfidence =
        (int) basicVehicleContainerHighFrequency.getHeading().getHeadingConfidence().value;
    speed = (int) basicVehicleContainerHighFrequency.getSpeed().getSpeedValue().value;
    speedConfidence =
        (int) basicVehicleContainerHighFrequency.getSpeed().getSpeedConfidence().value;
    vehicleLength =
        (int) basicVehicleContainerHighFrequency.getVehicleLength().getVehicleLengthValue().value;
    vehicleWidth = (int) basicVehicleContainerHighFrequency.getVehicleWidth().value;
    longitudinalAcceleration =
        (int)
            basicVehicleContainerHighFrequency
                .getLongitudinalAcceleration()
                .getLongitudinalAccelerationValue()
                .value();
    longitudinalAccelerationConfidence =
        (int)
            basicVehicleContainerHighFrequency
                .getLongitudinalAcceleration()
                .getLongitudinalAccelerationConfidence()
                .value();
    yawRate = (int) basicVehicleContainerHighFrequency.getYawRate().getYawRateValue().value;
    yawRateConfidence =
        (int) basicVehicleContainerHighFrequency.getYawRate().getYawRateConfidence().value();

    /* LowFrequencyContainer */
    LowFrequencyContainer lowFrequencyContainer = null;
    if (camParameters.hasLowFrequencyContainer()) {
      containerMask += (1 << 7);
      lowFrequencyContainer = camParameters.getLowFrequencyContainer();
      BasicVehicleContainerLowFrequency basicVehicleContainerLowFrequency =
          lowFrequencyContainer.getBasicVehicleContainerLowFrequency();
      vehicleRole = (int) basicVehicleContainerLowFrequency.getVehicleRole().value();
    } else {
      this.vehicleRole = (int) VehicleRole.default_.value();
    }

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

  /*
    public boolean checkEnum(Enum classOfT, int value, String name){
    boolean valid = classOfT.isMember(value);
    if(!valid) logger.error("{} is not valid. Value={}",
    name, value);
    return valid;
    }
  */

  public int getLatitude() {
    return this.latitude;
  }

  public int getLongitude() {
    return this.longitude;
  }

  public int getGenerationDeltaTime() {
    return this.genDeltaTimeMillis;
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

    SimpleCam simpleCam = (SimpleCam) object;

    // field comparison
    return messageId == simpleCam.messageId
        && stationId == simpleCam.stationId
        && genDeltaTimeMillis == simpleCam.genDeltaTimeMillis
        && containerMask == simpleCam.containerMask
        && stationType == simpleCam.stationType
        && latitude == simpleCam.latitude
        && longitude == simpleCam.longitude
        && semiMajorAxisConfidence == simpleCam.semiMajorAxisConfidence
        && semiMinorAxisConfidence == simpleCam.semiMinorAxisConfidence
        && semiMajorOrientation == simpleCam.semiMajorOrientation
        && altitude == simpleCam.altitude
        && heading == simpleCam.heading
        && headingConfidence == simpleCam.headingConfidence
        && speed == simpleCam.speed
        && speedConfidence == simpleCam.speedConfidence
        && vehicleLength == simpleCam.vehicleLength
        && vehicleWidth == simpleCam.vehicleWidth
        && longitudinalAcceleration == simpleCam.longitudinalAcceleration
        && longitudinalAccelerationConfidence == simpleCam.longitudinalAccelerationConfidence
        && yawRate == simpleCam.yawRate
        && yawRateConfidence == simpleCam.yawRateConfidence
        && vehicleRole == simpleCam.vehicleRole;
  }

  /** Check if the simple CAM is valid by checking if all values fall within their range. */
  public boolean isValid() {
    boolean valid = true;

    if (messageId != MessageId.cam) {
      logger.error("MessageID is: {} Should be: {}", messageId, MessageId.cam);
      valid = false;
    }

    if (!checkInt(StationID.class, stationId, "StationID")) {
      valid = false;
    }

    if (!checkInt(GenerationDeltaTime.class, genDeltaTimeMillis, "GenerationDeltaTime")) {
      valid = false;
    }

    if (!checkInt(StationType.class, stationType, "StationType")) {
      valid = false;
    }

    if (!checkInt(Latitude.class, latitude, "Latitude")) {
      valid = false;
    }

    if (!checkInt(Longitude.class, longitude, "Longitude")) {
      valid = false;
    }

    if (!checkInt(SemiAxisLength.class, semiMajorAxisConfidence, "SemiMajorConfidence")) {
      valid = false;
    }

    if (!checkInt(SemiAxisLength.class, semiMinorAxisConfidence, "SemiMinorConfidence")) {
      valid = false;
    }

    if (!checkInt(HeadingValue.class, semiMajorOrientation, "SemiMajorOrientation")) {
      valid = false;
    }

    if (!checkInt(AltitudeValue.class, altitude, "Altitude")) {
      valid = false;
    }

    if (!checkInt(HeadingValue.class, heading, "Heading")) {
      valid = false;
    }

    if (!checkInt(HeadingConfidence.class, headingConfidence, "HeadingConfidence")) {
      valid = false;
    }

    if (!checkInt(SpeedValue.class, speed, "Speed")) {
      valid = false;
    }

    if (!checkInt(SpeedConfidence.class, speedConfidence, "SpeedConfidence")) {
      valid = false;
    }

    if (!checkInt(VehicleLengthValue.class, vehicleLength, "VehicleLength")) {
      valid = false;
    }

    if (!checkInt(VehicleWidth.class, vehicleWidth, "VehicleWidth")) {
      valid = false;
    }

    if (!checkInt(
        LongitudinalAccelerationValue.class,
        longitudinalAcceleration,
        "LongitudinalAcceleration")) {
      valid = false;
    }

    if (!checkInt(
        AccelerationConfidence.class,
        longitudinalAccelerationConfidence,
        "LongitudinalAccelerationConfidence")) {
      valid = false;
    }

    if (!checkInt(YawRateValue.class, yawRate, "YawRate")) {
      valid = false;
    }

    /* TODO: Find a cleaner way to check enums. Also, this
     * approach is not very informative.*/
    if (!YawRateConfidence.isMember(yawRateConfidence)) {
      logger.error("YawRateConfidence is not valid. Value={}", yawRateConfidence);
      valid = false;
    }

    if (this.hasLowFrequencyContainer() && !VehicleRole.isMember(vehicleRole)) {
      logger.error("VehicleRole is not valid. Value={}", vehicleRole);
      valid = false;
    }

    return valid;
  }

  /** Return true if this simple CAM has a low frequency container. */
  public boolean hasLowFrequencyContainer() {
    return (containerMask & (1 << 7)) != 0;
  }

  /**
   * Return the byte buffer representation of the message. Typically used when transmitting the
   * message over UDP.
   */
  public byte[] asByteArray() {
    byte[] packetBuffer = new byte[SIMPLE_CAM_LENGTH];
    ByteBuffer buffer = ByteBuffer.wrap(packetBuffer);
    try {
      buffer.put(messageId);
      buffer.putInt(stationId);
      buffer.putInt(genDeltaTimeMillis);
      buffer.put(containerMask);
      buffer.putInt(stationType);
      buffer.putInt(latitude);
      buffer.putInt(longitude);
      buffer.putInt(semiMajorAxisConfidence);
      buffer.putInt(semiMinorAxisConfidence);
      buffer.putInt(semiMajorOrientation);
      buffer.putInt(altitude);
      buffer.putInt(heading);
      buffer.putInt(headingConfidence);
      buffer.putInt(speed);
      buffer.putInt(speedConfidence);
      buffer.putInt(vehicleLength);
      buffer.putInt(vehicleWidth);
      buffer.putInt(longitudinalAcceleration);
      buffer.putInt(longitudinalAccelerationConfidence);
      buffer.putInt(yawRate);
      buffer.putInt(yawRateConfidence);
      buffer.putInt(vehicleRole);
    } catch (BufferOverflowException e) {
      logger.error("Error converting simple CAM to byte array.", e);
      /* Return an empty byte array as the vehicle control
       * system has to deal with those anyway.
       */
      return new byte[SIMPLE_CAM_LENGTH];
    }
    return packetBuffer;
  }

  /**
   * Return the proper CAM representation of the message. Typically used when transmitting the
   * message to another ITS station.
   */
  public Cam asCam() {
    LowFrequencyContainer lowFrequencyContainer =
        (containerMask & (1 << 7)) != 0
            ? new LowFrequencyContainer(
                new BasicVehicleContainerLowFrequency(
                    VehicleRole.fromCode(vehicleRole),
                    ExteriorLights.builder()
                        .set(false, false, false, false, false, false, false, false)
                        .create(),
                    new PathHistory()))
            : null;

    //Not used for participating vehicles
    SpecialVehicleContainer specialVehicleContainer = null;

    BasicContainer basicContainer =
        new BasicContainer(
            new StationType(stationType),
            new ReferencePosition(
                new Latitude(latitude),
                new Longitude(longitude),
                new PosConfidenceEllipse(
                    new SemiAxisLength(semiMajorAxisConfidence),
                    new SemiAxisLength(semiMinorAxisConfidence),
                    new HeadingValue(semiMajorOrientation)),
                new Altitude(new AltitudeValue(altitude), AltitudeConfidence.unavailable)));
    HighFrequencyContainer highFrequencyContainer =
        new HighFrequencyContainer(
            BasicVehicleContainerHighFrequency.builder()
                .heading(
                    new Heading(
                        new HeadingValue(heading), new HeadingConfidence(headingConfidence)))
                .speed(new Speed(new SpeedValue(speed), new SpeedConfidence(speedConfidence)))
                //DriveDirection isn't part of the GCDC spec. Set to unavailable.
                .driveDirection(DriveDirection.values()[2])
                .vehicleLength(
                    new VehicleLength(
                        new VehicleLengthValue(vehicleLength),
                        VehicleLengthConfidenceIndication.unavailable))
                .vehicleWidth(new VehicleWidth(vehicleWidth))
                .longitudinalAcceleration(
                    new LongitudinalAcceleration(
                        new LongitudinalAccelerationValue(longitudinalAcceleration),
                        new AccelerationConfidence(longitudinalAccelerationConfidence)))
                //Curvature and CurvatureCalculationMode isn't part of the GCDC spec. Set to unavailable.
                .curvature(new Curvature())
                .curvatureCalculationMode(CurvatureCalculationMode.values()[2])
                .yawRate(
                    new YawRate(
                        new YawRateValue(yawRate),
                        //TODO: This code is slow. Cache YawRateConfidence.values() if it's a problem.
                        YawRateConfidence.values()[yawRateConfidence]))
                .create());

    return new Cam(
        new ItsPduHeader(
            new ProtocolVersion(1), new MessageId(MessageId.cam), new StationID(stationId)),
        new CoopAwareness(
            new GenerationDeltaTime(genDeltaTimeMillis * GenerationDeltaTime.oneMilliSec),
            new CamParameters(
                basicContainer,
                highFrequencyContainer,
                lowFrequencyContainer,
                specialVehicleContainer)));
  }
}
