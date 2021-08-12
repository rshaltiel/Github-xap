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
package com.gigaspaces.sql.datagateway.netty.query;

import java.util.Collections;
import java.util.List;

public class RowDescription {
    public static final RowDescription EMPTY = new RowDescription(Collections.emptyList());

    private final List<ColumnDescription> columns;

    public RowDescription(List<ColumnDescription> columns) {
        this.columns = columns;
    }

    public int getColumnsCount() {
        return columns.size();
    }

    public List<ColumnDescription> getColumns() {
        return Collections.unmodifiableList(columns);
    }
}
