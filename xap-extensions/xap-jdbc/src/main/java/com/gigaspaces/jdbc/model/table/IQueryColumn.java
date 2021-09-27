package com.gigaspaces.jdbc.model.table;

import com.gigaspaces.internal.transport.IEntryPacket;

public interface IQueryColumn extends Comparable<IQueryColumn> {
    int EMPTY_ORDINAL = -1;

    String UUID_COLUMN = "UID";

    default int getRefOrdinal() { return -1;}
    default void setRefOrdinal(int refOrdinal) {};

    int getColumnOrdinal();

    String getName();

    String getAlias();

    boolean isVisible();

    boolean isUUID();

    TableContainer getTableContainer();

    Object getCurrentValue();

    Class<?> getReturnType();

    IQueryColumn create(String columnName, String columnAlias, boolean isVisible, int columnOrdinal);

    Object getValue(IEntryPacket entryPacket);

    default IQueryColumn copy(){
        throw new UnsupportedOperationException("Copy is unsupported!");
    }

    default boolean isConcrete() {return false;}

    default boolean isAggregate(){
        return false;
    }

    default boolean isCaseColumn(){
        return false;
    }

    default boolean isFunction() { return false;}

    default boolean isLiteral() { return false;}
}
