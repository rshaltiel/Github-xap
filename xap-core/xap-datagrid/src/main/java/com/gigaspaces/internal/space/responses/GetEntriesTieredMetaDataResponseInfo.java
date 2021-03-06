package com.gigaspaces.internal.space.responses;

import com.gigaspaces.internal.io.IOUtils;
import com.gigaspaces.internal.server.storage.EntryTieredMetaData;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

public class GetEntriesTieredMetaDataResponseInfo extends AbstractSpaceResponseInfo {
    static final long serialVersionUID = 663660777455511033L;
    private Map<Integer, Exception> exceptionMap = new HashMap<>();
    private Map<Object, EntryTieredMetaData> entryMetaDataMap = new HashMap<>();

    public GetEntriesTieredMetaDataResponseInfo() {
    }

    public Map<Object, EntryTieredMetaData> getEntryMetaDataMap() {
        return entryMetaDataMap;
    }

    public void setEntryMetaDataMap(Map<Object, EntryTieredMetaData> entryMetaDataMap) {
        this.entryMetaDataMap = entryMetaDataMap;
    }

    public Map<Integer, Exception> getExceptionMap() {
        return exceptionMap;
    }

    public void setExceptionMap(Map<Integer, Exception> exceptionMap) {
        this.exceptionMap = exceptionMap;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        IOUtils.writeObject(out, entryMetaDataMap);
        IOUtils.writeObject(out, exceptionMap);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        entryMetaDataMap = IOUtils.readObject(in);
        exceptionMap = IOUtils.readObject(in);
    }

    public void addException(Integer partitionId, Exception e){
        exceptionMap.put(partitionId, e);
    }

    public void addResultToEntryMetaDataMap(Map<Object, EntryTieredMetaData> resultEntriesTieredMetaDataMap){
        if(resultEntriesTieredMetaDataMap != null){
            for(Map.Entry<Object, EntryTieredMetaData> entry : resultEntriesTieredMetaDataMap.entrySet()){
                if(!entryMetaDataMap.containsKey(entry.getKey()) || entry.getValue().getTieredState() != null){
                    entryMetaDataMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}

