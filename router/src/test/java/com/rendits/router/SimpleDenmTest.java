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
                SimpleDenm simpleDenm = SampleMessages.getSampleDenm();
                byte[] buffer = simpleDenm.asByteArray();
                SimpleDenm simpleDenmFromArray = new SimpleDenm(buffer);
                assertEquals(simpleDenm, simpleDenmFromArray);
                Denm denm = simpleDenm.asDenm();
                SimpleDenm simpleDenmFromDenm = new SimpleDenm(denm);
                assertEquals(simpleDenm, simpleDenmFromDenm);
        }
}
