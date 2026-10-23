package dev.gray.questbilifix;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class SensorJsonFixerTest {
    @Test
    public void repairsKnownQuestImuSensorOnly() {
        // Synthetic/redacted sample; not a real device's calibration or serial info.
        String malformed = "[{\"name\":\"Double Tap\"}, "
                + "{\"name\":\"{\"aname\":\"Syncboss IMU Android Sensor\","
                + "\"calibration\":{\"factory\":\"redacted\"}}\","
                + "\"vendor\":\"Oculus VR\",\"version\":\"2\",\"type\":\"65537\","
                + "\"power\":\"0.001\"}, {\"name\":\"IAD Sensor\"}]";
        String expected = "[{\"name\":\"Double Tap\"}, "
                + "{\"name\":\"Syncboss IMU Android Sensor\","
                + "\"vendor\":\"Oculus VR\",\"version\":\"2\",\"type\":\"65537\","
                + "\"power\":\"0.001\"}, {\"name\":\"IAD Sensor\"}]";

        String result = SensorJsonFixer.repair(malformed);
        assertEquals(expected, result);
        assertTrue(result.contains("\"name\":\"IAD Sensor\""));
    }

    @Test
    public void normalJsonUnchanged() {
        String input = "[{\"name\":\"Syncboss IMU Android Sensor\","
                + "\"vendor\":\"Oculus VR\",\"version\":\"2\",\"type\":\"65537\"}]";
        assertSame(input, SensorJsonFixer.repair(input));
    }

    @Test
    public void otherSensorUnchanged() {
        String input = "[{\"name\":\"{\"aname\":\"Other Sensor\"}\","
                + "\"vendor\":\"Oculus VR\",\"version\":\"2\",\"type\":\"65537\"}]";
        assertSame(input, SensorJsonFixer.repair(input));
    }

    @Test
    public void nullAndEmptyInput() {
        assertEquals(null, SensorJsonFixer.repair(null));
        assertEquals("", SensorJsonFixer.repair(""));
    }
}
