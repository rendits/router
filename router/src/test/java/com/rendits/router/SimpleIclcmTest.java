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
                SimpleIclcm simpleIclcm = SampleMessages.getSampleIclcm();
                byte[] buffer = simpleIclcm.asByteArray();
                SimpleIclcm simpleIclcmFromArray = new SimpleIclcm(buffer);
                assertEquals(simpleIclcm, simpleIclcmFromArray);
                IgameCooperativeLaneChangeMessage iclcm = simpleIclcm.asIclcm();
                SimpleIclcm simpleIclcmFromIclcm = new SimpleIclcm(iclcm);
                assertEquals(simpleIclcm, simpleIclcmFromIclcm);
        }
}
