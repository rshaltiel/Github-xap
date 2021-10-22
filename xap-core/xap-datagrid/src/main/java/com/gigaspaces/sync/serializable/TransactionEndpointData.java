/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gigaspaces.sync.serializable;

import com.gigaspaces.api.InternalApi;
import com.gigaspaces.sync.DataSyncOperation;
import com.gigaspaces.sync.SynchronizationSourceDetails;
import com.gigaspaces.sync.TransactionData;
import com.gigaspaces.transaction.ConsolidatedDistributedTransactionMetaData;
import com.gigaspaces.transaction.TransactionParticipantMetaData;

import static com.gigaspaces.sync.serializable.ExternalizableDataSyncOperation.convertDataSyncOperations;

@InternalApi
public class TransactionEndpointData implements TransactionData, EndpointData {

    private static final long serialVersionUID = 258825999388261637L;

    private final DataSyncOperation[] dataSyncOperations;
    private final SpaceSyncEndpointMethod spaceSyncEndpointMethod;
    private final SynchronizationSourceDetails synchronizationSourceDetails;

    public TransactionEndpointData(com.gigaspaces.sync.TransactionData transactionData, SpaceSyncEndpointMethod spaceSyncEndpointMethod) {
        this.dataSyncOperations = convertDataSyncOperations(transactionData.getTransactionParticipantDataItems());
        this.spaceSyncEndpointMethod = spaceSyncEndpointMethod;
        synchronizationSourceDetails = new SerializableSynchronizationSourceDetails(transactionData.getSourceDetails());
    }


    @Override
    public boolean isConsolidated() {
        return false;
    }

    @Override
    public TransactionParticipantMetaData getTransactionParticipantMetaData() {
        return null;
    }

    @Override
    public ConsolidatedDistributedTransactionMetaData getConsolidatedDistributedTransactionMetaData() {
        return null;
    }

    @Override
    public DataSyncOperation[] getTransactionParticipantDataItems() {
        return dataSyncOperations;
    }

    @Override
    public SynchronizationSourceDetails getSourceDetails() {
        return synchronizationSourceDetails;
    }

    @Override
    public SpaceSyncEndpointMethod getSyncEndpointMethod() {
        return spaceSyncEndpointMethod;
    }
}
