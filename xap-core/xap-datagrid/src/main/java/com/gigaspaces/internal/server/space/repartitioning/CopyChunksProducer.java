package com.gigaspaces.internal.server.space.repartitioning;

import com.gigaspaces.internal.cluster.ClusterTopology;
import com.gigaspaces.internal.remoting.routing.partitioned.PartitionedClusterUtils;
import com.gigaspaces.internal.transport.IEntryPacket;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.query.aggregators.SpaceEntriesAggregator;
import com.gigaspaces.query.aggregators.SpaceEntriesAggregatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.BlockingQueue;

public class CopyChunksProducer extends SpaceEntriesAggregator<CopyChunksResponseInfo> {
    private static final long serialVersionUID = 2759306184206709756L;
    public static Logger logger = LoggerFactory.getLogger(CopyChunksProducer.class);
    private final static Integer BROADCAST_KEY = -1;
    private ClusterTopology newMap;
    private Map<Integer, List<IEntryPacket>> batchMap;
    private BlockingQueue<Batch> queue;
    private int batchSize;
    private ScaleType scaleType;
    private Set<Integer> newPartitionIds;

    CopyChunksProducer(ClusterTopology newMap, BlockingQueue<Batch> queue, int batchSize, ScaleType scaleType, Set<Integer> newPartitionIds) {
        this.newMap = newMap;
        this.batchSize = batchSize;
        this.queue = queue;
        this.newPartitionIds = newPartitionIds;
        this.batchMap = new HashMap<>();
        this.scaleType = scaleType;
    }

    @Override
    public String getDefaultAlias() {
        return CopyChunksRequestInfo.class.getName();
    }

    @Override
    public void aggregate(SpaceEntriesAggregatorContext context) {
        SpaceTypeDescriptor typeDescriptor = context.getTypeDescriptor();
        if(typeDescriptor.isBroadcast()){
            if(scaleType.equals(ScaleType.IN))
                return;
            if(context.getPartitionId() != 0)
                return;
            List<IEntryPacket> entries = insertBatchToMap(BROADCAST_KEY, (IEntryPacket) context.getRawEntry());
            if (entries.size() == batchSize) {
                addBroadcastBatch(entries);
                batchMap.remove(BROADCAST_KEY);
            }
        }
        else {
            Object routingValue = context.getPathValue(typeDescriptor.getRoutingPropertyName());
            int newPartitionId = PartitionedClusterUtils.getPartitionId(routingValue, newMap) + 1;
            if (newPartitionId != context.getPartitionId() + 1) {
                if (isAutoGeneratedRouting(context)) {
                    throw new AutoGeneratedIdNotSupportedException("Failed to scale space with type ["
                            + typeDescriptor.getTypeName() + "] , type with auto-generated routing property does not support horizontal scale");
                }
                List<IEntryPacket> entries = insertBatchToMap(newPartitionId, (IEntryPacket) context.getRawEntry());
                if (entries.size() == batchSize) {
                    insertBatchToQueue(newPartitionId, entries);
                    batchMap.remove(newPartitionId);
                }
            }
        }
    }

    private List<IEntryPacket> insertBatchToMap(int newPartitionId, IEntryPacket rawEntry) {
        boolean containsKey = batchMap.containsKey(newPartitionId);
        List<IEntryPacket> entries = containsKey ? batchMap.get(newPartitionId) : new ArrayList<>(batchSize);
        entries.add(rawEntry);
        if(!containsKey)
            batchMap.put(newPartitionId, entries);
        return entries;
    }

    private boolean isAutoGeneratedRouting(SpaceEntriesAggregatorContext context) {
        SpaceTypeDescriptor typeDescriptor = context.getTypeDescriptor();
        return typeDescriptor.isAutoGenerateId() && typeDescriptor.getRoutingPropertyName().equalsIgnoreCase(typeDescriptor.getIdPropertyName());
    }


    @Override
    public CopyChunksResponseInfo getIntermediateResult() {
        for (Map.Entry<Integer, List<IEntryPacket>> entry : batchMap.entrySet()) {
            if(entry.getKey().equals(BROADCAST_KEY))
                addBroadcastBatch(entry.getValue());
            else
                insertBatchToQueue(entry.getKey(), entry.getValue());
        }
        return null;
    }

    private void addBroadcastBatch(List<IEntryPacket> entries){
        for(Integer id: newPartitionIds){
            insertBatchToQueue(id, entries);
        }
    }

    private void insertBatchToQueue(Integer id, List<IEntryPacket> entries) {
        try {
            queue.put(new WriteBatch(id, entries));
        } catch (Exception e) {
            logger.error("Exception in aggregator while trying to put batch in queue", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void aggregateIntermediateResult(CopyChunksResponseInfo partitionResult) {

    }
}
