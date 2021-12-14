package com.gigaspaces.internal.server.space.tiered_storage;

import com.gigaspaces.internal.metadata.ITypeDesc;
import com.gigaspaces.internal.server.space.SpaceEngine;
import com.gigaspaces.internal.server.space.metadata.SpaceTypeManager;
import com.gigaspaces.internal.server.storage.IEntryHolder;
import com.gigaspaces.internal.server.storage.ITemplateHolder;
import com.gigaspaces.metrics.LongCounter;
import com.gigaspaces.time.SystemTime;
import com.j_spaces.core.cache.CacheManager;
import com.j_spaces.core.cache.InitialLoadInfo;
import com.j_spaces.core.cache.context.Context;
import com.j_spaces.core.cache.context.TieredState;
import com.j_spaces.core.sadapter.ISAdapterIterator;
import com.j_spaces.core.sadapter.SAException;

import java.io.IOException;

public class InternalRDBMSManager {

    InternalRDBMS internalRDBMS;
    private final LongCounter readDisk = new LongCounter();
    private final LongCounter writeDisk = new LongCounter();
    private final TypesMetaData metaData = new TypesMetaData();
    private long diskReadAvg = 0;
    private long diskWriteAvg = 0;

    public InternalRDBMSManager(InternalRDBMS internalRDBMS) {
        this.internalRDBMS = internalRDBMS;
    }

    public boolean initialize(String spaceName, String fullMemberName, SpaceTypeManager typeManager, boolean isBackup) throws SAException{
        return internalRDBMS.initialize(spaceName, fullMemberName, typeManager, isBackup);
    }

    public long getDiskSize() throws SAException, IOException{
        return internalRDBMS.getDiskSize();
    }

//    public LongCounter getDiskSizeMetric() throws SAException, IOException{
//        LongAdder longAdder = new LongAdder();
//        longAdder.add(getDiskSize());
//        return new LongCounter(longAdder);
//    }

    public long getFreeSpaceSize() throws SAException, IOException{
        return internalRDBMS.getFreeSpaceSize();
    }

    public void createTable(ITypeDesc typeDesc) throws SAException{
        internalRDBMS.createTable(typeDesc);
    }

    public TypesMetaData getMetaData(){
        return metaData;
    }

    /**
     * Inserts a new entry to the internalDiskStorage
     *
     * @param entryHolder entry to insert
     * @param initialLoadOrigin
     */
    public void insertEntry(Context context, IEntryHolder entryHolder, CacheManager.InitialLoadOrigin initialLoadOrigin) throws SAException{
        if(initialLoadOrigin != CacheManager.InitialLoadOrigin.FROM_TIERED_STORAGE && context.isDiskEntry() && entryHolder.getXidOriginatedTransaction() == null) {
            long startTime = SystemTime.timeMillis();
            internalRDBMS.insertEntry(context, entryHolder);
            long duration = SystemTime.timeMillis() - startTime;
            diskWriteAvg = (diskWriteAvg * writeDisk.getCount()) + duration;
            writeDisk.inc();
            diskWriteAvg = diskWriteAvg / writeDisk.getCount();
        }
        String type = entryHolder.getServerTypeDesc().getTypeName();
        metaData.increaseCounterMap(type);
        if(context.isRAMEntry()){
            metaData.increaseRamCounterMap(type);
        }
    }

    /**
     * updates an entry.
     *
     * @param updatedEntry new content, same UID and class
     */
    public void updateEntry(Context context, IEntryHolder updatedEntry) throws SAException{
        internalRDBMS.updateEntry(context, updatedEntry);

    }

    /**
     * Removes an entry from the  internalDiskStorage
     *
     * @param entryHolder entry to remove
     */
    public boolean removeEntry(Context context, IEntryHolder entryHolder) throws SAException{
        boolean removed = false;
        if(context.getEntryTieredState() != TieredState.TIERED_HOT) {
            removed = internalRDBMS.removeEntry(context, entryHolder);
        }
        String type = entryHolder.getServerTypeDesc().getTypeName();
        if(removed || context.getEntryTieredState() == TieredState.TIERED_HOT){
            metaData.decreaseCounterMap(type);
            if(context.isRAMEntry()){
                metaData.decreaseRamCounterMap(type);
            }
        }
        return removed;
    }



    public void updateRamCounterAfterUpdate(String type, boolean isUpdatedEntryHot, boolean isOriginEntryHot){
        if(isOriginEntryHot != isUpdatedEntryHot){
            if(isUpdatedEntryHot){
                metaData.increaseRamCounterMap(type);
            }
            else{
                metaData.decreaseRamCounterMap(type);
            }
        }
    }

    public IEntryHolder getEntryById(Context context, String typeName, Object id, ITemplateHolder templateHolder) throws SAException{
        long startTime = SystemTime.timeMillis();
        IEntryHolder entryById = internalRDBMS.getEntryById(context, typeName, id);
        long duration = SystemTime.timeMillis() - startTime;
        diskReadAvg = (diskReadAvg * readDisk.getCount()) + duration;
        if (templateHolder != null && templateHolder.isReadOperation()){
            readDisk.inc();
        }
        diskReadAvg = diskReadAvg / readDisk.getCount();

        return entryById;
    }

    public IEntryHolder getEntryByUID(Context context, String typeName, String uid, ITemplateHolder templateHolder) throws SAException{
        long startTime = SystemTime.timeMillis();
        IEntryHolder entryByUID = internalRDBMS.getEntryByUID(context, typeName, uid);
        long duration = SystemTime.timeMillis() - startTime;
        diskReadAvg = (diskReadAvg * readDisk.getCount()) + duration;
        if (templateHolder != null && templateHolder.isReadOperation()){
            readDisk.inc();
        }
        diskReadAvg = diskReadAvg / readDisk.getCount();
        return entryByUID;
    }

    public ISAdapterIterator<IEntryHolder> makeEntriesIter(Context context, String typeName, ITemplateHolder templateHolder) throws SAException{
        long startTime = SystemTime.timeMillis();
        ISAdapterIterator<IEntryHolder> iEntryHolderISAdapterIterator = internalRDBMS.makeEntriesIter(context, typeName, templateHolder);
        long duration = SystemTime.timeMillis() - startTime;
        diskReadAvg = (diskReadAvg * readDisk.getCount()) + duration;
        if (templateHolder != null && templateHolder.isReadOperation() && !context.isDisableTieredStorageMetric()){
            readDisk.inc();
        }
        diskReadAvg = diskReadAvg / readDisk.getCount();

        return iEntryHolderISAdapterIterator;
    }

    public boolean isKnownType(String name){
        return internalRDBMS.isKnownType(name);
    }

    public void shutDown(){
        internalRDBMS.shutDown();
    }

    public LongCounter getReadDisk() {
        return readDisk;
    }

    public LongCounter getWriteDisk() {
        return writeDisk;
    }

    public long getDiskReadAvg() {
        return diskReadAvg;
    }

    public long getDiskWriteAvg() {
        return diskWriteAvg;
    }

    public void deleteData() throws SAException {
        internalRDBMS.deleteData();
    }

    public void persistType(ITypeDesc typeDesc) throws SAException {
        internalRDBMS.persistType(typeDesc);
    }

    public void initialLoad(Context context, SpaceEngine engine, InitialLoadInfo initialLoadInfo) throws SAException {
        internalRDBMS.initialLoad(context, engine, initialLoadInfo);
    }

    public SpaceTypeManager getTypeManager() {
        return internalRDBMS.getTypeManager();
    }
}

