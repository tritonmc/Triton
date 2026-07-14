package com.rexcantor64.triton.dependencies;

import com.rexcantor64.triton.test.TestParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DependencyTest {

    @Test
    public void testGradleDependencyVersionsMatchLibbyVersions() {

        assertEquals(TestParameters.ADVENTURE_VERSION, Dependency.ADVENTURE.getVersion());
        assertEquals(TestParameters.PACKET_EVENTS_VERSION, Dependency.PACKET_EVENTS_API.getVersion());
    }
}
