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
                SimpleCam simpleCam = SampleMessages.getSampleCam();
                byte[] buffer = simpleCam.asByteArray();
                SimpleCam simpleCamFromArray = new SimpleCam(buffer);
                assertEquals(simpleCam, simpleCamFromArray);
                Cam cam = simpleCam.asCam();
                SimpleCam simpleCamFromCam = new SimpleCam(cam);
                assertEquals(simpleCam, simpleCamFromCam);
        }
}
