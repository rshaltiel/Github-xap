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

import com.gigaspaces.internal.utils.ObjectConverter;
import com.gigaspaces.jdbc.calcite.pg.PgCalciteTable;
import com.gigaspaces.jdbc.exceptions.ColumnNotFoundException;
import com.gigaspaces.jdbc.exceptions.SQLExceptionWrapper;
import com.gigaspaces.jdbc.model.QueryExecutionConfig;
import com.gigaspaces.jdbc.model.result.QueryResult;
import com.gigaspaces.jdbc.model.result.TempQueryResult;
import com.j_spaces.core.IJSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.gigaspaces.jdbc.model.table.IQueryColumn.EMPTY_ORDINAL;

public class SchemaTableContainer extends TempTableContainer {
    private final PgCalciteTable table;
    private final IJSpace space;
    private static Logger _logger = LoggerFactory.getLogger(SchemaTableContainer.class);

    public SchemaTableContainer(PgCalciteTable table, String alias, IJSpace space) {
        super(alias);
        this.table = table;
        this.tableColumns.addAll(Arrays.stream(table.getSchemas()).map(x -> new ConcreteColumn(x.getPropertyName(), x.getJavaType(), null, true, this, EMPTY_ORDINAL)).collect(Collectors.toList()));
        this.space = space;

        allColumnNamesSorted.addAll(tableColumns.stream().map(IQueryColumn::getAlias).collect(Collectors.toList()));
    }

    @Override
    public QueryResult executeRead(QueryExecutionConfig config) throws SQLException {
        if (tableResult != null) return tableResult;
        tableResult = table.execute(this, space, tableColumns);
        if (queryTemplatePacket != null) {
            tableResult.filter(x -> queryTemplatePacket.eval(x));
        }
        return tableResult = new TempQueryResult(this);
    }

    @Override
    public IQueryColumn addQueryColumnWithColumnOrdinal(String columnName, String columnAlias, boolean isVisible, int columnOrdinal) {
        IQueryColumn queryColumn = tableColumns.stream()
                .filter(qc -> qc.getName().equalsIgnoreCase(columnName))
                .findFirst()
                .orElseThrow(() -> new ColumnNotFoundException("Could not find column with name [" + columnName + "]"));
        if (isVisible) visibleColumns.add(queryColumn);
        else invisibleColumns.add(queryColumn);
        return queryColumn;
    }


    @Override
    public String getTableNameOrAlias() {
        return alias == null ? table.getName() : alias;
    }

    @Override
    public Object getColumnValue(String columnName, Object value) {
        Optional<Class<?>> javaType = Arrays.stream(table.getSchemas()).filter(x -> x.getPropertyName().equalsIgnoreCase(columnName)).findFirst().map(PgCalciteTable.SchemaProperty::getJavaType);
        if (javaType.isPresent()) {
            Class<?> className = javaType.get();
            try {
                return ObjectConverter.convert(value, className);
            } catch (SQLException throwables) {
                _logger.warn("Couldn't convert column's value of type ["+ className.getName()+"] for column name ["+columnName+"]");

                throwables.printStackTrace();
                throw new SQLExceptionWrapper(throwables);
            }
        } else {
            _logger.warn("Couldn't find column / type for column ["+columnName+"], result type might be incorrect");
        }
        return value;
    }
}
