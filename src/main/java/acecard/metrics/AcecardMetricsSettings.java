package acecard.metrics;

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;

public final class AcecardMetricsSettings {

    public static final String SETTINGS_NAMESPACE = "acecard.metrics";
    public static final String TYPE = "acecard";


    // Static Settings
    public static final Setting<Boolean> METRICS_ENABLED = Setting.boolSetting(SETTINGS_NAMESPACE + ".enabled",
            true, Property.NodeScope);
    public static final Setting<Integer> METRICS_OUTPUT_SECONDS = Setting.intSetting(SETTINGS_NAMESPACE + ".output_seconds",
            10, Property.NodeScope);

}