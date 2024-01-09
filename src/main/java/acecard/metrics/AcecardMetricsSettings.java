package acecard.metrics;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;

public final class AcecardMetricsSettings {

    public static final String SETTINGS_NAMESPACE = "acecard.metrics";
    public static final String TYPE = "acecard";


    // Static Settings
    public static final Setting<Boolean> METRICS_ENABLED = Setting.boolSetting(SETTINGS_NAMESPACE + ".enabled",
            false, Property.NodeScope);
    
    // Dynamic Settings
    public static final Setting<Integer> METRICS_OUTPUT_SECONDS = Setting.intSetting(SETTINGS_NAMESPACE + ".output_seconds",
            10, Property.NodeScope, Property.Dynamic);
    public static final Setting<Integer> METRICS_RETAIN_HOURS = Setting.intSetting(SETTINGS_NAMESPACE + ".retain_hours",
            10, Property.NodeScope, Property.Dynamic);
    public static final Setting<List<String>> INDICIES_LIST = Setting.listSetting(SETTINGS_NAMESPACE + ".indices",            
    		Arrays.asList("signal"), Function.identity(), Property.NodeScope, Property.Dynamic);
    public static final Setting<List<String>> FIELDS_LIST = Setting.listSetting(SETTINGS_NAMESPACE + ".capturable_fields",            
    		Arrays.asList("processor", "signal_id"), Function.identity(), Property.NodeScope, Property.Dynamic);
}