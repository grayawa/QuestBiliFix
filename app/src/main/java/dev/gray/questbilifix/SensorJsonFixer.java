package dev.gray.questbilifix;

/**
 * Repairs exactly one known malformed sensor-list entry from Meta Quest 3.
 * This class intentionally has no Android/Xposed dependencies to permit JVM tests.
 */
public final class SensorJsonFixer {
    private static final String BROKEN_NAME_PREFIX = "\"name\":\"{";
    private static final String SENSOR_MARKER = "\"aname\":\"Syncboss IMU Android Sensor\"";
    private static final String FIELD_SUFFIX =
            "\",\"vendor\":\"Oculus VR\",\"version\":\"2\",\"type\":\"65537\"";

    private SensorJsonFixer() {}

    public static String repair(String json) {
        if (json == null
                || json.indexOf(SENSOR_MARKER) < 0
                || json.indexOf(FIELD_SUFFIX) < 0) {
            return json;
        }

        int from = 0;
        while (true) {
            int beginning = json.indexOf(BROKEN_NAME_PREFIX, from);
            if (beginning < 0) {
                return json;
            }

            int marker = json.indexOf(SENSOR_MARKER, beginning + BROKEN_NAME_PREFIX.length());
            // The marker must appear at the beginning of this particular sensor's name.
            if (marker < 0 || marker - beginning > 128) {
                from = beginning + BROKEN_NAME_PREFIX.length();
                continue;
            }

            int end = json.indexOf(FIELD_SUFFIX, marker + SENSOR_MARKER.length());
            // Bound the search so a different JSON object cannot be accidentally replaced.
            if (end < 0 || end - marker > 65536) {
                from = beginning + BROKEN_NAME_PREFIX.length();
                continue;
            }

            // FIELD_SUFFIX begins with the closing quote of the outer name field.
            return json.substring(0, beginning)
                    + "\"name\":\"Syncboss IMU Android Sensor"
                    + json.substring(end);
        }
    }
}
