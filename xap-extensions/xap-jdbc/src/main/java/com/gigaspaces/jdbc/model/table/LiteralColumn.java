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

public class LiteralColumn implements IQueryColumn{
    private final Object value;

    public LiteralColumn(Object value) {
        this.value = value;
    }

    @Override
    public int getColumnOrdinal() {
        throw new UnsupportedOperationException("Unsupported method getColumnOrdinal");
    }

    @Override
    public void setColumnOrdinal(int ordinal) {
        throw new UnsupportedOperationException("Unsupported method setColumnOrdinal");
    }

    @Override
    public String getName() {
        return "\'" + value + "\'";
    }

    @Override
    public String getAlias() {
        throw new UnsupportedOperationException("Unsupported method getAlias");
    }

    @Override
    public boolean isVisible() {
        throw new UnsupportedOperationException("Unsupported method isVisible");
    }

    @Override
    public boolean isUUID() {
        throw new UnsupportedOperationException("Unsupported method isUUID");
    }

    @Override
    public TableContainer getTableContainer() {
        throw new UnsupportedOperationException("Unsupported method getName");
    }

    @Override
    public Object getCurrentValue() {
        return value;
    }

    @Override
    public Class<?> getReturnType() {
        return value.getClass();
    }

    @Override
    public IQueryColumn create(String columnName, String columnAlias, boolean isVisible, int columnOrdinal) {
        throw new UnsupportedOperationException("Unsupported method create");
    }

    @Override
    public int compareTo(IQueryColumn o) {
        throw new UnsupportedOperationException("Unsupported method compareTo");
    }

    @Override
    public Object getValue(IEntryPacket entryPacket) {
        return value;
    }
}
