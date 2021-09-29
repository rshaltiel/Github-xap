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
package com.gigaspaces.jdbc.model.result;

import com.gigaspaces.internal.transport.IEntryPacket;
import com.gigaspaces.jdbc.model.table.ConcreteTableContainer;
import com.gigaspaces.jdbc.model.table.IQueryColumn;
import com.gigaspaces.jdbc.model.table.TableContainer;
import com.j_spaces.jdbc.query.IQueryResultSet;

import java.util.List;
import java.util.stream.Collectors;

public class ConcreteQueryResult extends QueryResult{
    private final TableContainer tableContainer;
    private List<TableRow> rows;

    public ConcreteQueryResult(IQueryResultSet<IEntryPacket> res, ConcreteTableContainer tableContainer) {
        super(tableContainer.getSelectedColumns());
        this.tableContainer = tableContainer;
        this.rows = res.stream().map(x -> TableRowFactory.createTableRowFromIEntryPacket(x, tableContainer)).collect(Collectors.toList());
    }

    public ConcreteQueryResult(List<IQueryColumn> selectedColumns, List<TableRow> rows){
        super(selectedColumns);
        this.tableContainer = null;
        this.rows = rows;
    }

    @Override
    public TableContainer getTableContainer() {
        return this.tableContainer;
    }

    @Override
    public int size() {
        return this.rows.size();
    }

    @Override
    public void addRow(TableRow tableRow) {
        this.rows.add(tableRow);
    }

    @Override
    public List<TableRow> getRows() {
        return this.rows;
    }

    @Override
    public void setRows(List<TableRow> rows) {
        this.rows = rows;
    }
}
