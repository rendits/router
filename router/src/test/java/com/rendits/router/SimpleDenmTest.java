package com.rendits.router;

import static org.junit.Assert.assertEquals;

import net.gcdc.camdenm.CoopIts.Denm;
import org.junit.Test;

public class SimpleDenmTest {

  @Test
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
