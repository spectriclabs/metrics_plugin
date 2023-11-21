package acecard.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.IndexableField;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.allocation.decider.AllocationDeciders;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.mapper.LuceneDocument;
import org.elasticsearch.index.shard.IndexingOperationListener;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.repositories.RepositoriesService;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.tracing.Tracer;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xcontent.NamedXContentRegistry;

import acecard.metrics.rest.actions.AcecardMetricsIndexInfoDisplay;

public class AcecardMetricsPlugin extends Plugin implements ActionPlugin {
	private static final Logger log = LogManager.getLogger(AcecardMetricsPlugin.class);

	private ClusterSettings clusterSettings = null;

	@Inject
	public AcecardMetricsPlugin(Settings settings) {
	}

	@Override
	public List<Setting<?>> getSettings() {
		List<Setting<?>> list = new ArrayList<>();
		list.addAll(Arrays.asList(AcecardMetricsSettings.METRICS_ENABLED, AcecardMetricsSettings.METRICS_OUTPUT_SECONDS, AcecardMetricsSettings.METRICS_RETAIN_HOURS,
				AcecardMetricsSettings.INDICIES_LIST));

		return list;
	}

	@Override
	public List<RestHandler> getRestHandlers(Settings settings, RestController restController,
			ClusterSettings clusterSettings, IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter,
			IndexNameExpressionResolver indexNameExpressionResolver, Supplier<DiscoveryNodes> nodesInCluster) {

		if (clusterSettings.get(AcecardMetricsSettings.METRICS_ENABLED)) {
			return Arrays.asList(new AcecardMetricsIndexInfoDisplay(this));
		}
		return Collections.emptyList();
	}

	@Override
	public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool,
			ResourceWatcherService resourceWatcherService, ScriptService scriptService,
			NamedXContentRegistry xContentRegistry, Environment environment, NodeEnvironment nodeEnvironment,
			NamedWriteableRegistry namedWriteableRegistry, IndexNameExpressionResolver indexNameExpressionResolver,
			Supplier<RepositoriesService> repositoriesServiceSupplier, Tracer tracer,
			AllocationDeciders allocationDeciders) {

		log.info("Creating Components");
		clusterSettings = clusterService.getClusterSettings();
		final List<Object> components = new ArrayList<>();

		return components;
	}

	@Override
	public void onIndexModule(IndexModule indexModule) {
		if (clusterSettings.get(AcecardMetricsSettings.METRICS_ENABLED)) {
			List<String> indicies = clusterSettings.get(AcecardMetricsSettings.INDICIES_LIST);			
			String indexName = indexModule.getIndex().getName();

 			if (checkIndicies(indexName, indicies)) {
				log.info("Creating Index Listener for Index:{}", indexName);
				indexModule.addIndexOperationListener(new IndexListener(indexName));
			}
		}
	}

	private boolean checkIndicies(String indexName, List<String> indicies) {
		//If the indicies list is empty then accept everything
		if (indicies == null || indicies.isEmpty()) {
			return true;
		}
	
		boolean match = false;
		for (String index: indicies) {
			if (indexName.startsWith(index)) {
				match = true;
				break;
			}
		}
	
		return match;
	}

	@Override
	public void close() throws IOException {
		super.close();
	}

	Map<String, AtomicInteger> metrics = new ConcurrentHashMap<String, AtomicInteger>();

	private void incrementMetric(String index, String processor, String signal) {
		StringBuilder builder = new StringBuilder();
		builder.append("ELASTICSEARCH_DOCUMENT_COUNT ").append('{').append("index=\"").append(index).append("\", ")
				.append("processor=\"").append(processor).append("\", ").append("signal_id=\"").append(signal)
				.append('\"').append('}');

		String key = builder.toString();

		synchronized (metrics) {
			AtomicInteger count = metrics.getOrDefault(key, new AtomicInteger(0));
			count.getAndIncrement();
			metrics.put(key, count);
		}
	}

	public String getMetrics() {
		StringBuilder builder = new StringBuilder();

		synchronized (metrics) {
			Date currentTime = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(currentTime);
			calendar.set(Calendar.MILLISECOND, 0);

			for (Entry<String, AtomicInteger> metric : metrics.entrySet()) {
				builder.append(metric.getKey()).append(' ').append(metric.getValue().get()).append(' ')
						.append(calendar.getTimeInMillis()).append('\n');
			}
			resetMetrics();
		}

		return builder.toString();
	}

	private void resetMetrics() {
		synchronized (metrics) {
			metrics.clear();
		}
	}

	public ClusterSettings getClusterSettings() {
		return clusterSettings;
	}

	// Listener Class for updating the documents
	private class IndexListener implements IndexingOperationListener {
		private String indexName;

		private IndexListener(String indexName) {
			this.indexName = indexName;
		}

		@Override
		public Engine.Index preIndex(ShardId shardId, Engine.Index index) {

			List<LuceneDocument> docs = index.docs();
			for (LuceneDocument d : docs) {
				String processorId = "UNDEFINED";
				String signalId = "UNDEFINED";

				IndexableField processor = d.getField("processor");
				IndexableField signal = d.getField("signal_id");

				if (processor != null) {
					if (processor.stringValue() != null) {
						processorId = processor.stringValue();
					} else if (processor.binaryValue() != null) {
						processorId = processor.binaryValue().utf8ToString();
					}
				}

				if (signal != null) {
					if (signal.stringValue() != null) {
						signalId = signal.stringValue();
					} else if (signal.binaryValue() != null) {
						signalId = signal.binaryValue().utf8ToString();
					}
				}

				incrementMetric(indexName, processorId, signalId);
			}

			return index;
		}

	}
}