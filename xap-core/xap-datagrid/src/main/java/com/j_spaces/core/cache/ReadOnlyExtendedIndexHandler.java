package com.j_spaces.core.cache;

import com.gigaspaces.internal.server.storage.IEntryHolder;
import com.j_spaces.core.client.DuplicateIndexValueException;
import com.j_spaces.core.client.TemplateMatchCodes;
import com.j_spaces.kernel.IObjectInfo;
import com.j_spaces.kernel.IStoredList;
import com.j_spaces.kernel.StoredListFactory;
import com.j_spaces.kernel.list.IScanListIterator;
import com.j_spaces.kernel.list.MultiStoredList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;


@com.gigaspaces.api.InternalApi
public class ReadOnlyExtendedIndexHandler<K> //todo- readonly
        implements IExtendedEntriesIndex<K, IEntryCacheInfo> {
    private static final Logger _logger = LoggerFactory.getLogger(com.gigaspaces.logger.Constants.LOGGER_CACHE);
    private final TreeMap<Object, IStoredList<IEntryCacheInfo>> _orderedStore;
    private final TreeMap<Object, IEntryCacheInfo> _uniqueOrderedStore;
    private final TypeDataIndex _index;
    private final RecentExtendedIndexUpdates _recentExtendedIndexUpdates;

    private static final boolean FORCE_ORDERED_SCAN = true;

    public ReadOnlyExtendedIndexHandler(TypeDataIndex index) {
        _index = index;
        _orderedStore = new TreeMap<Object, IStoredList<IEntryCacheInfo>>();
        _uniqueOrderedStore = _index.isUniqueIndex() ? (TreeMap<Object, IEntryCacheInfo>) ((TreeMap) _orderedStore) : null;
        if (index.getCacheManager().getEngine().getLeaseManager().isSupportsRecentExtendedUpdates())
            _recentExtendedIndexUpdates = new RecentExtendedIndexUpdates(index.getCacheManager());
        else
            _recentExtendedIndexUpdates = null;
    }


    @Override
    public TreeMap<Object, IStoredList<IEntryCacheInfo>> getOrderedStore() {
        return _orderedStore;
    }


    @Override
    public NavigableMap<Object, IStoredList<IEntryCacheInfo>> getNonUniqueEntriesStore() {
        return _orderedStore;

    }

    @Override
    public NavigableMap<Object, IEntryCacheInfo> getUniqueEntriesStore() {//todo
        return _uniqueOrderedStore;
    }


    @Override
    public IStoredList<IEntryCacheInfo> getIndexEntries(K indexValue) {
        return _orderedStore.get(indexValue);
    }


//*
//     * insert a value for this key- if key already exist insert into the SL of same values
//     *
//     * @return the index backref


    @Override
    public IObjectInfo insertEntryIndexedField(IEntryCacheInfo pEntry, K fieldValue, TypeData pType, boolean alreadyCloned) {
        IObjectInfo oi = null;
        if (_index.isUniqueIndex()) {
            while (true) {
                IEntryCacheInfo other = getUniqueEntriesStore().putIfAbsent(fieldValue, pEntry);
                if (other == null) {
                    oi = pEntry;
                    break;
                }
                if (other.isRemovingOrRemoved() || other.isDeleted()) {//removing- help out
                    getUniqueEntriesStore().remove(fieldValue, other); //help remove
                } else {
                    DuplicateIndexValueException ex = new DuplicateIndexValueException(pEntry.getUID(), pEntry.getEntryHolder(_index.getCacheManager()).getClassName(), _index.getIndexDefinition().getName(), fieldValue, other.getUID());
                    if (_logger.isErrorEnabled())
                        _logger.error("Duplicate value encountered on unique index insertion ", ex);
                    //if case of failure we need to remove the partally inserted entry
                    //relevant for write + update + update under txn, done in the calling code
                    throw ex;
                }
            }
        } else {
            IStoredList<IEntryCacheInfo> newSL = null;
            IObjectInfo myoi = null, otheroi = null;
            IStoredList<IEntryCacheInfo> currentSL = null;
            boolean first = true;
            //ConcurrentMap<Object, IStoredList<IEntryCacheInfo>> store = getNonUniqueEntriesStore();
            Map<Object, IStoredList<IEntryCacheInfo>> store = getNonUniqueEntriesStore();
            while (true) {
                if (first) {
                    first = false;
                    if (!_index.assumeUniqueValue())
                        currentSL = store.get(fieldValue);
                }
                if (currentSL == null) {
                    if (!alreadyCloned && _index.considerValueClone()) {
                        fieldValue = (K) _index.cloneIndexValue(fieldValue, pEntry.getEntryHolder(_index.getCacheManager()));
                        alreadyCloned = true;
                    }
                    currentSL = store.putIfAbsent(fieldValue, pEntry);
                    if (currentSL == null) {
                        oi = pEntry;
                        break;
                    }
                }
                if (currentSL.isMultiObjectCollection()) {//a real SL
                    oi = currentSL.add(pEntry);
                    // may have been invalidated by PersistentGC
                    if (oi == null) {
                        //help remove entry for key only if currently mapped to given value
                        store.remove(fieldValue, currentSL);
                        currentSL = null;
                        continue;
                    } else
                        break; // OK - done.
                }
                //a single object is stored, create a SL and add it
                if (newSL == null)
                    newSL = StoredListFactory.createConcurrentList(pType.isAllowFifoIndexScans());

                otheroi = newSL.addUnlocked(currentSL.getObjectFromHead());
                myoi = newSL.addUnlocked(pEntry);

                if (!store.replace(fieldValue, currentSL, newSL)) {
                    newSL.removeUnlocked(otheroi);
                    newSL.removeUnlocked(myoi);
                    myoi = null;
                    currentSL = null;
                } else {
                    oi = myoi;
                    break; // OK - done.
                }
            }
        }

        return oi;

    }


//*
//     * remove entry indexed field from cache.
//

    @Override
    public void removeEntryIndexedField(IEntryHolder eh,
                                        Object fieldValue, IEntryCacheInfo pEntry, IObjectInfo oi) {
        if (_index.isUniqueIndex()
//&& oi == pEntry TBD open-up when unique index is a general feature
){
            if (!getUniqueEntriesStore().remove(fieldValue, pEntry)) ;
            {
                Object other = _index.considerValueClone() ? _index.cloneIndexValue(fieldValue, pEntry.getEntryHolder(_index.getCacheManager())) : fieldValue;
                if (other != fieldValue && (fieldValue.hashCode() != other.hashCode() || !fieldValue.equals(other) || ((Comparable) fieldValue).compareTo((Comparable) other) != 0))
                    throw new RuntimeException("Entry Class: " + pEntry.getClassName() +
                            " - Wrong hashCode() or equals() or Comparable.compareTo() implementation of " +
                            fieldValue.getClass() + " class field, or field value changed while entry stored in space.");
            }
        } else {
            removeNonUniqueIndexedField(eh,
                    fieldValue,
                    pEntry,
                    oi);
        }
    }

    private void removeNonUniqueIndexedField(IEntryHolder eh, Object fieldValue,
                                             IEntryCacheInfo pEntry, IObjectInfo oi) {
       // ConcurrentMap<Object, IStoredList<IEntryCacheInfo>> store = getNonUniqueEntriesStore();
        Map<Object, IStoredList<IEntryCacheInfo>> store = getNonUniqueEntriesStore();
        while (true) {
            IStoredList<IEntryCacheInfo> entries = store.get(fieldValue);
            if (entries == null)
                throw new RuntimeException("Entry Class: " + eh.getClassName() +
                        " - Wrong hashCode() or equals() or CompareTo implementation of " +
                        fieldValue.getClass() + " class field , or field value changed while entry stored in space.");

            if (entries.isMultiObjectCollection()) {//a true SL
                IObjectInfo myoi = oi;
                if (myoi == pEntry) {
                    myoi = entries.getHead();
                    if (myoi.getSubject() != pEntry)
                        throw new RuntimeException("Entry Class: " + eh.getClassName() +
                                " - Single-entry to multiple wrong OI ,  " +
                                fieldValue.getClass() + " class field.");
                }
                if (oi != null) {
                    entries.remove(myoi);
                } else {
                    boolean res = entries.removeByObject(pEntry);
                    if (!res)
                        throw new RuntimeException("Entry Class: " + eh.getClassName() +
                                " - removeByObject on SL returned false ,  " +
                                fieldValue.getClass() + " class field.");
                }
                if (entries.invalidate())
                    store.remove(fieldValue, entries);

                break;
            }
            if (entries != pEntry) {
                throw new RuntimeException("Entry Class: " + eh.getClassName() +
                        " - Single-entry Wrong hashCode() or equals() implementation of " +
                        fieldValue.getClass() + " class field , or field value changed while entry stored in space.");
            }
            //single value- remove me
            if (store.remove(fieldValue, pEntry))
                break;

        }
    }

    @Override
    public void onUpdate(IEntryCacheInfo eci) {
        if (_recentExtendedIndexUpdates != null)
            _recentExtendedIndexUpdates.onUpdate(eci);
    }

    @Override
    public void onUpdateEnd(IEntryCacheInfo eci) {
        if (_recentExtendedIndexUpdates != null)
            _recentExtendedIndexUpdates.onUpdateEnd(eci);
    }

    @Override
    public void onRemove(IEntryCacheInfo eci) {
        if (_recentExtendedIndexUpdates != null)
            _recentExtendedIndexUpdates.onRemove(eci);
    }

    @Override
    public int reapExpired() {
        if (_recentExtendedIndexUpdates != null)
            return _recentExtendedIndexUpdates.reapExpired();
        return 0;
    }




    @Override
    public IScanListIterator<IEntryCacheInfo> establishScan(K startPos, short relation, K endPos, boolean endPosInclusive) {
        return
                establishScan(startPos, relation, endPos, endPosInclusive, false /* ordered*/);
    }


    @Override
    public IScanListIterator<IEntryCacheInfo> establishScan(K startPos, short relation, K endPos, boolean endPosInclusive, boolean ordered) {
        ordered |= FORCE_ORDERED_SCAN; //should we force ordered scan always ?
        long startTime = _recentExtendedIndexUpdates != null ? System.currentTimeMillis() : 0;
        IScanListIterator<IEntryCacheInfo> res = ordered ?
                establishScanOrdered(startPos, relation, endPos, endPosInclusive) :
                establishScanUnOrdered(startPos, relation, endPos, endPosInclusive);

        if (_recentExtendedIndexUpdates != null && !_recentExtendedIndexUpdates.isEmpty()) {
            MultiStoredList<IEntryCacheInfo> msl = new MultiStoredList<IEntryCacheInfo>();
            msl.add(res);
            msl.add(_recentExtendedIndexUpdates.iterator(startTime,(ExtendedIndexIterator)res));
            return msl;
        } else
            return res;

    }


    private ExtendedIndexIterator<IEntryCacheInfo> establishScanUnOrdered(K startPos, short relation, K endPos, boolean endPosInclusive) {
        boolean reversedScan = (relation == TemplateMatchCodes.LT || relation == TemplateMatchCodes.LE);

        K start;
        K end;
        boolean endInclusive;
        boolean startinclusive;
        Object originalStart = startPos;
        Object originalEnd = endPos;
        short originalStartCondition = reversedScan ? 0 : relation;
        short originalEndCondition = !reversedScan ? 0 : relation;
        if (reversedScan) {
            start = endPos;
            startinclusive = endPosInclusive;
            end = startPos;
            endInclusive = relation == TemplateMatchCodes.LE;
        } else {
            startinclusive = relation == TemplateMatchCodes.GE;
            start = startPos;
            end = endPos;
            endInclusive = endPosInclusive;

        }

        NavigableMap baseMap = _orderedStore;
        NavigableMap mapToScan;
        if (end == null)
            mapToScan = start != null ? baseMap.tailMap(start, startinclusive) : baseMap;
        else
            mapToScan = start != null ? baseMap.subMap(start, startinclusive, end, endInclusive) : baseMap.headMap(end, endInclusive);
        return new ExtendedIndexIterator<IEntryCacheInfo>(mapToScan, _index,originalStart,originalStartCondition, originalEnd,originalEndCondition);
    }

    private ExtendedIndexIterator<IEntryCacheInfo> establishScanOrdered(K startPos, short relation, K endPos, boolean endPosInclusive)  {



        boolean reversedScan = (relation == TemplateMatchCodes.LT || relation == TemplateMatchCodes.LE);
        boolean startinclusive = (relation == TemplateMatchCodes.GE || relation == TemplateMatchCodes.LE);
        Object originalStart = startPos;
        Object originalEnd = endPos;
        short originalStartCondition = reversedScan ? 0 : relation;
        short originalEndCondition = !reversedScan ? 0 : relation;

        NavigableMap baseMap = reversedScan ? _orderedStore.descendingMap() : _orderedStore;
        NavigableMap mapToScan;
        if (endPos == null)
            mapToScan = startPos != null ? baseMap.tailMap(startPos, startinclusive) : baseMap;
        else
            mapToScan = startPos != null ? baseMap.subMap(startPos, startinclusive, endPos, endPosInclusive) : baseMap.headMap(endPos, endPosInclusive);
        return new ExtendedIndexIterator<IEntryCacheInfo>(mapToScan, _index,originalStart,originalStartCondition, originalEnd,originalEndCondition);
    }
}