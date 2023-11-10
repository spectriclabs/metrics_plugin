/* 
 * UNCLASSIFIED//FOUO
 */
package acecard.metrics.rest.actions;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.common.Table;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.cat.AbstractCatAction;

import acecard.metrics.AcecardMetricsPlugin;
import acecard.metrics.AcecardMetricsSettings;

public class AcecardMetricsIndexInfoDisplay extends AbstractCatAction {

	private AcecardMetricsPlugin plugin;
	private Timer timer;

	private StringBuilder response;
	private ZonedDateTime metricsLastRequested = ZonedDateTime.now();

	public AcecardMetricsIndexInfoDisplay(AcecardMetricsPlugin acecardMetricsPlugin) {
		this.plugin = acecardMetricsPlugin;

		// set the seconds based off of the setting value
		int outputSeconds = this.plugin.getClusterSettings().get(AcecardMetricsSettings.METRICS_OUTPUT_SECONDS);
		
		// create timer to build metrics on a per x second basis
		LocalDateTime timerStartTime = LocalDateTime.now();
		int seconds = timerStartTime.getSecond();
		int mod = seconds % outputSeconds;
		timerStartTime = timerStartTime.plusSeconds(outputSeconds - mod);

		Date timerStartDate = Date.from(timerStartTime.atZone(ZoneId.systemDefault()).toInstant());

		this.timer = new Timer();
		this.timer.schedule(new MetricsTimerTask(), timerStartDate, (outputSeconds * 1000));

		this.response = new StringBuilder();
	}

	@Override
	protected void finalize() throws Throwable {
		this.timer.cancel();
	}

	private void resetResponse() {
		synchronized (response) {
			response.setLength(0);
			response.append("# HELP ELASTICSEARCH_DOCUMENT_COUNT number of documents injected into elasticsearch broken down by index, processor, signal_id\n");
			response.append("# TYPE ELASTICSEARCH_DOCUMENT_COUNT gauge\n");
		}
	}

	@Override
	protected RestChannelConsumer doCatRequest(RestRequest restRequest, NodeClient nodeClient) {
		String metricResponse;
		synchronized (response) {
			metricsLastRequested = ZonedDateTime.now();
			metricResponse = response.toString();
			
			resetResponse();
		}

				return channel -> {
					channel.sendResponse(new RestResponse(RestStatus.OK, metricResponse));
		};
	}

	@Override
	protected void documentation(StringBuilder stringBuilder) {
		stringBuilder.append(documentation());
	}

	public static String documentation() {
		return "/_acecard/metrics\n";
	}

	@Override
	protected Table getTableWithHeader(RestRequest restRequest) {
		final Table table = new Table();
		table.startHeaders();
		table.addCell("test", "desc:test");
		table.endHeaders();
		return table;
	}

	@Override
	public String getName() {
		return "rest_handler_acecard_metrics_index_info";
	}

	@Override
	// Declare all the routes here
	public List<Route> routes() {
		return new ArrayList<>(Arrays.asList(new Route(RestRequest.Method.GET, "/_acecard/metrics"),
				new Route(RestRequest.Method.POST, "/_acecard/metrics")));
	}

	private class MetricsTimerTask extends TimerTask {
		@Override
		public void run() {
			synchronized (response) {
				ZonedDateTime currentTime = ZonedDateTime.now();

				// if no requests in the last hour reset the response
				if (metricsLastRequested.isBefore(currentTime.minusHours(1))) {
					resetResponse();
				}

				response.append(plugin.getMetrics());
			}
		}

	}
}
/*
 * UNCLASSIFIED//FOUO
 */