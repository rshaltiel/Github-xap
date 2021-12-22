package org.openspaces.persistency.space;

import com.gigaspaces.datasource.SpaceTypeSchemaAdapter;
import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.internal.metadata.TypeDescriptorUtils;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.query.IdQuery;
import com.gigaspaces.sync.*;
import org.openspaces.core.GigaSpace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GigaSpaceSynchronizationEndpoint extends SpaceSynchronizationEndpoint {
    private final GigaSpace targetSpace;
    private final Map<String, SpaceTypeSchemaAdapter> spaceTypeSchemaAdapters;

    public GigaSpaceSynchronizationEndpoint(GigaSpace targetSpace, Map<String, SpaceTypeSchemaAdapter> spaceTypeSchemaAdapters) {
        this.targetSpace = targetSpace;
        this.spaceTypeSchemaAdapters = spaceTypeSchemaAdapters;
    }

    @Override
    public void onTransactionSynchronization(TransactionData transactionData) {
        processEndpointData(transactionData.getTransactionParticipantDataItems());
    }

    @Override
    public void onOperationsBatchSynchronization(OperationsBatchData batchData) {
        processEndpointData(batchData.getBatchDataItems());
    }

    @Override
    public void onAddIndex(AddIndexData addIndexData) {
        targetSpace.getTypeManager().asyncAddIndexes(addIndexData.getTypeName(), addIndexData.getIndexes(), null);
    }

    @Override
    public void onIntroduceType(IntroduceTypeData introduceTypeData) {
        targetSpace.getTypeManager().registerTypeDescriptor(adaptSpaceTypeDescriptor(introduceTypeData));
    }

    private void processEndpointData(DataSyncOperation[] dataSyncOperations){
        List<SpaceDocument> adapted = new ArrayList<>();
        for (int i = 0; i < dataSyncOperations.length; i++) {
            DataSyncOperation operation = dataSyncOperations[i];
            if(isWriteOperation(operation)){
                //Accumulate write operations until encountering a remove operation
                adapted.add(adaptSingleDataSyncOperation(operation));
                continue;
            }
            //Encountered a remove operation, flush the write operations
            if (!adapted.isEmpty()) {
                targetSpace.writeMultiple(adapted.toArray());
                adapted.clear();
            }
            processRemoveOperation(operation);
        }
        //In case all operations were write
        if (!adapted.isEmpty())
            targetSpace.writeMultiple(adapted.toArray());
    }

    private void processRemoveOperation(DataSyncOperation operation){
        SpaceTypeDescriptor typeDesc = operation.getTypeDescriptor();
        SpaceDocument entry = operation.getDataAsDocument();
        Object id = TypeDescriptorUtils.toSpaceId(typeDesc.getIdPropertiesNames(), entry::getProperty);
        Object routing = entry.getProperty(typeDesc.getRoutingPropertyName());
        IdQuery<SpaceDocument> idQuery = new IdQuery<>(typeDesc.getTypeName(), id, routing);
        targetSpace.takeById(idQuery);
    }

    private boolean isWriteOperation(DataSyncOperation dataSyncOperation){
        switch (dataSyncOperation.getDataSyncOperationType()){
            case WRITE:
            case UPDATE:
            case PARTIAL_UPDATE:
            case CHANGE:
                return true;
            case REMOVE:
            case REMOVE_BY_UID:
                return false;
        }
        return false;
    }

    private SpaceDocument adaptSingleDataSyncOperation(DataSyncOperation dataSyncOperation) {
        if(!dataSyncOperation.supportsDataAsDocument())
            throw new UnsupportedOperationException("Only Space Document is supported");
        String typeName = dataSyncOperation.getTypeDescriptor().getTypeName();
        if(spaceTypeSchemaAdapters.containsKey(typeName))
            return spaceTypeSchemaAdapters.get(typeName).adaptEntry(dataSyncOperation.getDataAsDocument());
        return dataSyncOperation.getDataAsDocument();
    }

    private SpaceTypeDescriptor adaptSpaceTypeDescriptor(IntroduceTypeData introduceTypeData){
        String typeName = introduceTypeData.getTypeDescriptor().getTypeName();
        if(spaceTypeSchemaAdapters.containsKey(typeName))
            return spaceTypeSchemaAdapters.get(typeName).adaptTypeDescriptor(introduceTypeData.getTypeDescriptor());
        return introduceTypeData.getTypeDescriptor();
    }
}
