package com.gigaspaces.internal.server.space;

import com.gigaspaces.attribute_store.AttributeStore;
import com.gigaspaces.internal.cluster.ClusterTopology;
import com.gigaspaces.internal.cluster.ClusterTopologyState;
import com.gigaspaces.internal.zookeeper.ZNodePathFactory;
import com.gigaspaces.logger.Constants;
import com.j_spaces.core.admin.SpaceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZookeeperTopologyHandler implements Closeable {


    private String puName;
    private final AttributeStore attributeStore;
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    public ZookeeperTopologyHandler(String puName, AttributeStore attributeStore) {
        this.puName = puName;
        this.attributeStore = attributeStore;
    }

    private static String getZkTopologyPath(String puName) {
        return ZNodePathFactory.processingUnit(puName, "topology");
    }

    public static String getZkTopologyStatePath(String puName) {
        return getZkTopologyPath(puName)+"/state";
    }

    public static String getHeapReportStatePath2(String puName) {
        return ZNodePathFactory.processingUnit(puName, "reports");
    }
    public static String getHeapReportStatePath(String puName) {
        return getHeapReportStatePath2(puName)+"/heapreport";
    }

    public static String getPipelineStatus(String pipelineName) {
        return ZNodePathFactory.pipeline(pipelineName, "status");
    }

    public static String getPipeline(String pipelineName) {
        return ZNodePathFactory.pipeline(pipelineName, "pipeline");
    }


    public static String pipelines() {
        return ZNodePathFactory.pipelines();
    }


    public static String getZkTopologyGenerationPath(String puName, int generation) {
        return getZkTopologyPath(puName)+"/"+generation;
    }

    public void addListener(ZookeeperClient zookeeperClient, SpaceConfig spaceConfig, int partitionId) {
        zookeeperClient.addConnectionStateListener(new ReconnectTask(spaceConfig, partitionId), executorService);
    }

    public ClusterTopologyState getClusterTopologyState() throws IOException {
        return attributeStore.getObject(getZkTopologyStatePath(puName));
    }

    public ClusterTopology getClusterTopology(int generation) throws IOException {
        return attributeStore.getObject(getZkTopologyGenerationPath(puName,generation));
    }

    @Override
    public void close() throws IOException {
        executorService.shutdownNow();
    }

    public class ReconnectTask implements Runnable {

        private final SpaceConfig spaceConfig;
        private final int partitionId;
        private final Logger logger = LoggerFactory.getLogger(Constants.LOGGER_ZOOKEEPER);

        ReconnectTask(SpaceConfig spaceConfig, int partitionId) {
            this.spaceConfig = spaceConfig;
            this.partitionId = partitionId;
        }

        @Override
        public void run() {
            try {
                ClusterTopologyState topologyState = getClusterTopologyState();
                int generation = topologyState.getGenerationForPartition(partitionId);
                ClusterTopology topology = getClusterTopology(generation);
                int currentGeneration = spaceConfig.getClusterInfo().getTopology().getGeneration();
                if (topology.getGeneration() > currentGeneration) {
                    logger.warn(spaceConfig.getContainerName() + " is at chunks map generation " + currentGeneration + " but current generation in Zookeeper is " + topology.getGeneration());
                    spaceConfig.getClusterInfo().setTopology(topology);
                }
            } catch (IOException e) {
                logger.warn("Failed to get chunks routing mapping", e);
                throw new UncheckedIOException("Failed to get chunks routing mapping", e);
            }
        }
    }
}
