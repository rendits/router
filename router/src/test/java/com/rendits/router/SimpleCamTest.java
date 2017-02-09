package com.rendits.router;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import net.gcdc.camdenm.CoopIts.Cam;

public class SimpleCamTest {

        @Test
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
