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
package com.gigaspaces.jdbc.model.table;

import com.gigaspaces.internal.transport.IEntryPacket;

public interface IQueryColumn extends Comparable<IQueryColumn> {
    int EMPTY_ORDINAL = -1;

    String UUID_COLUMN = "UID";

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

    default boolean isAggregate(){
        return false;
    }

    default boolean isCaseColumn(){
        return false;
    }

    default boolean isFunction() { return false;}

    default boolean isLiteral() { return false;}
}
