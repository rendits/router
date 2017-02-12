package com.rendits.router;

import static org.junit.Assert.assertEquals;

import net.gcdc.camdenm.Iclcm.IgameCooperativeLaneChangeMessage;
import org.junit.Test;

public class SimpleIclcmTest {

  @Test
  public void testIclcm() {
    SimpleIclcm simpleIclcm = SampleMessages.getSampleIclcm();
    byte[] buffer = simpleIclcm.asByteArray();
    SimpleIclcm simpleIclcmFromArray = new SimpleIclcm(buffer);
    assertEquals(simpleIclcm, simpleIclcmFromArray);
    IgameCooperativeLaneChangeMessage iclcm = simpleIclcm.asIclcm();
    SimpleIclcm simpleIclcmFromIclcm = new SimpleIclcm(iclcm);
    assertEquals(simpleIclcm, simpleIclcmFromIclcm);
  }
}
