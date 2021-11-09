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

package com.gigaspaces.internal.cluster.node.impl;

import com.gigaspaces.internal.cluster.node.IReplicationInContext;
import com.gigaspaces.internal.cluster.node.handlers.IReplicationInFacade;
import com.gigaspaces.internal.cluster.node.impl.packets.data.ReplicationPacketDataMediator;
import com.gigaspaces.internal.cluster.node.impl.packets.data.operations.AbstractReplicationPacketSingleEntryData;
import com.gigaspaces.internal.io.IOUtils;
import com.gigaspaces.internal.metadata.ITypeDesc;
import com.gigaspaces.internal.server.space.metadata.SpaceTypeManager;
import com.gigaspaces.internal.server.storage.IEntryData;
import com.j_spaces.core.cluster.IReplicationFilterEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@com.gigaspaces.api.InternalApi
public class DataTypeDropPacketData
        extends AbstractReplicationPacketSingleEntryData {
    Logger logger = LoggerFactory.getLogger("MY_LOGGER");
    private static final long serialVersionUID = 1L;
    private String className;

    public DataTypeDropPacketData() {
    }

    public DataTypeDropPacketData(String className, boolean fromGateway) {
        super(fromGateway);
        this.className = className;
    }

    public ReplicationSingleOperationType getOperationType() {
        return ReplicationSingleOperationType.DATA_TYPE_DROP;
    }

    public void execute(IReplicationInContext context,
                        IReplicationInFacade inReplicationHandler, ReplicationPacketDataMediator dataMediator) throws Exception {
        inReplicationHandler.inDataTypeDrop(context, className);
    }

    public boolean beforeDelayedReplication() {
        return true;
    }

    public boolean supportsReplicationFilter() {
        return false;
    }

    public boolean requiresRecoveryDuplicationProtection() {
        return false;
    }

    @Override
    public IEntryData getMainEntryData() {
        return null;
    }

    @Override
    public IEntryData getSecondaryEntryData() {
        return null;
    }

    @Override
    public String getMainTypeName() {
        return className;
    }

    @Override
    protected IReplicationFilterEntry toFilterEntry(
            SpaceTypeManager spaceTypeManager) {
        throw new UnsupportedOperationException();
    }

    public String getUid() {
        return className;
    }

    public int getOrderCode() {
        return 0;
    }

    public boolean isTransient() {
        return false;
    }


    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        IOUtils.writeString(out, className);
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        super.readExternal(in);
        className = IOUtils.readString(in);
    }

    @Override
    public void writeToSwap(ObjectOutput out) throws IOException {
        super.writeToSwap(out);
        IOUtils.writeString(out, className);
    }

    @Override
    public void readFromSwap(ObjectInput in) throws IOException,
            ClassNotFoundException {
        super.readFromSwap(in);
        className = IOUtils.readString(in);
    }

    @Override
    public String toString() {
        return "DATA TYPE DROP: " + className;
    }

    @Override
    public boolean filterIfNotPresentInReplicaState() {
        return false;
    }

    @Override
    public boolean containsFullEntryData() {
        return true;
    }

}
