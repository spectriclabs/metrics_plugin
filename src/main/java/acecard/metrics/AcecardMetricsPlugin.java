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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.IndexableField;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterStateListener;
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
	private AtomicBoolean isMasterNode = new AtomicBoolean(true);

	@Inject
	public AcecardMetricsPlugin(Settings settings) {
	}

	@Override
	public List<Setting<?>> getSettings() {
		List<Setting<?>> list = new ArrayList<>();
		list.addAll(Arrays.asList(AcecardMetricsSettings.METRICS_ENABLED, AcecardMetricsSettings.METRICS_OUTPUT_SECONDS,
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

		clusterService.addListener(new ClusterListener(this));
		return components;
	}

	@Override
	public void onIndexModule(IndexModule indexModule) {
		if (clusterSettings.get(AcecardMetricsSettings.METRICS_ENABLED)) {
			List<String> indicies = clusterSettings.get(AcecardMetricsSettings.INDICIES_LIST);
			log.info("Acecard Metrics Index List: {}", Arrays.toString(indicies.toArray()));
			
			String indexName = indexModule.getIndex().getName();

 			if (checkIndicies(indexName, indicies)) {
				log.info("Creating Index Listener for Index:{}", indexName);
				indexModule.addIndexOperationListener(new IndexListener(indexName, this));
			}
		}
	}

	private boolean checkIndicies(String indexName, List<String> indicies) {
		//If the indicies list is empty then accept everything
		if (indicies == null || indicies.isEmpty()) {
			log.info("Indicies is empty so match everything  - index: {}", indexName);
			return true;
		}
	
		boolean match = false;
		for (String index: indicies) {
			if (indexName.startsWith(index)) {
				log.info("Found match for index {} against indicies entry: {}", indexName, index);
				match = true;
				break;
			}
		}
	
		if (!match) {
			log.info("No match for index {}", indexName);
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

			if (count.get() == 1) {
				log.info("{} {}", key, count);
			}

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
		private AcecardMetricsPlugin plugin;

		private IndexListener(String indexName, AcecardMetricsPlugin plugin) {
			this.indexName = indexName;
			this.plugin = plugin;
		}

		@Override
		public Engine.Index preIndex(ShardId shardId, Engine.Index index) {
			if (plugin.isMasterNode.get()) {
				String processorId;
				String signalId;

				List<LuceneDocument> docs = index.docs();
				for (LuceneDocument d : docs) {
					IndexableField processor = d.getField("processor");
					IndexableField signal = d.getField("signal_id");

					processorId = processor == null ? "" : processor.stringValue();
					signalId = signal == null ? "" : signal.stringValue();

					incrementMetric(indexName, processorId, signalId);
				}
			}
			return index;
		}

	}

	private class ClusterListener implements ClusterStateListener {
		private AcecardMetricsPlugin plugin;

		private ClusterListener(AcecardMetricsPlugin plugin) {			
			this.plugin = plugin;		
		}

		@Override
		public void clusterChanged(ClusterChangedEvent event) {
			log.info("Received a cluster change event");
			log.info("local id: {} master id: {} isMaster: {}", event.state().nodes().getLocalNodeId(),event.state().nodes().getMasterNodeId(), event.localNodeMaster());
			// plugin.isMasterNode.set(event.localNodeMaster());
			//
			// if (!plugin.isMasterNode.get()) {
			// 	plugin.resetMetrics();
			// }
		}

	}
}