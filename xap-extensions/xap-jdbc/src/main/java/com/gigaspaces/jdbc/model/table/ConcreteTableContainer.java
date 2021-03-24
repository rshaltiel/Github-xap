package com.gigaspaces.jdbc.model.table;

import com.gigaspaces.internal.client.QueryResultTypeInternal;
import com.gigaspaces.internal.metadata.ITypeDesc;
import com.gigaspaces.internal.query.explainplan.ExplainPlanV3;
import com.gigaspaces.internal.transport.IEntryPacket;
import com.gigaspaces.internal.transport.ProjectionTemplate;
import com.gigaspaces.jdbc.exceptions.ColumnNotFoundException;
import com.gigaspaces.jdbc.exceptions.TypeNotFoundException;
import com.gigaspaces.jdbc.model.QueryExecutionConfig;
import com.gigaspaces.jdbc.model.result.ExplainPlanResult;
import com.gigaspaces.jdbc.model.result.QueryResult;
import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.Modifiers;
import com.j_spaces.core.client.ReadModifiers;
import com.j_spaces.jdbc.SQLUtil;
import com.j_spaces.jdbc.builder.QueryTemplatePacket;
import com.j_spaces.jdbc.builder.range.Range;
import com.j_spaces.jdbc.query.IQueryResultSet;
import com.j_spaces.jdbc.query.QueryTableData;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ConcreteTableContainer extends TableContainer {
    private final IJSpace space;
    private QueryTemplatePacket queryTemplatePacket;
    private final ITypeDesc typeDesc;
    private final List<String> allColumnNamesSorted;
    private final List<QueryColumn> visibleColumns = new ArrayList<>();
    private final String name;
    private final String alias;
    private Integer limit = Integer.MAX_VALUE;

    public ConcreteTableContainer(String name, String alias, IJSpace space) {
        this.space = space;
        this.name = name;
        this.alias = alias;

        try {
            typeDesc = SQLUtil.checkTableExistence(name, space);
        } catch (SQLException e) {
            throw new TypeNotFoundException("Unknown table [" + name + "]", e);
        }

        allColumnNamesSorted = Arrays.asList(typeDesc.getPropertiesNames());
        allColumnNamesSorted.sort(String::compareTo);
    }

    private QueryTemplatePacket createEmptyQueryTemplatePacket(String tableName) {
        QueryTableData queryTableData = new QueryTableData(tableName, null, 0);
        queryTableData.setTypeDesc(typeDesc);
        return new QueryTemplatePacket(queryTableData, QueryResultTypeInternal.NOT_SET);
    }

    private QueryTemplatePacket createQueryTemplatePacketWithRange(String tableName, Range range) {
        QueryTableData queryTableData = new QueryTableData(tableName, null, 0);
        queryTableData.setTypeDesc(typeDesc);
        return new QueryTemplatePacket(queryTableData, QueryResultTypeInternal.NOT_SET, range.getPath(), range);
    }

    @Override
    public QueryResult executeRead(QueryExecutionConfig config) throws SQLException {
        String[] projectionC = visibleColumns.stream().map(QueryColumn::getName).toArray(String[]::new);

        try {
            ProjectionTemplate _projectionTemplate = ProjectionTemplate.create(projectionC, typeDesc);

            if (queryTemplatePacket == null) {
                queryTemplatePacket = createEmptyQueryTemplatePacket(name);
            }
            queryTemplatePacket.setProjectionTemplate(_projectionTemplate);

            int modifiers = ReadModifiers.REPEATABLE_READ;
            ExplainPlanV3 explainPlanImpl = null;
            if (config.isExplainPlan()) {
                // Using LinkedHashMap to keep insertion order from the ArrayList
                final Map<String, String> visibleColumnsAndAliasMap = visibleColumns.stream().collect(Collectors.toMap
                        (QueryColumn::getName, queryColumn -> queryColumn.getAlias() == null ? "" : queryColumn.getAlias()
                                , (oldValue, newValue) -> newValue, LinkedHashMap::new));

                explainPlanImpl = new ExplainPlanV3(name, alias, visibleColumnsAndAliasMap);
                queryTemplatePacket.setExplainPlan(explainPlanImpl);
                modifiers = Modifiers.add(modifiers, Modifiers.EXPLAIN_PLAN);
                modifiers = Modifiers.add(modifiers, Modifiers.DRY_RUN);
            }

            queryTemplatePacket.prepareForSpace(typeDesc);
            IQueryResultSet<IEntryPacket> res = queryTemplatePacket.readMultiple(space.getDirectProxy(), null, limit, modifiers);
            if (explainPlanImpl != null) {
                return new ExplainPlanResult(explainPlanImpl.getExplainPlanInfo().toString(config.isExplainPlanVerbose()));
            } else {
                return new QueryResult(res, visibleColumns);
            }
        } catch (Exception e) {
            throw new SQLException("Failed to get results from space", e);
        }
    }

    @Override
    public QueryColumn addQueryColumn(String columnName, String alias) {
        if (!columnName.equalsIgnoreCase(QueryColumn.UUID_COLUMN) && typeDesc.getFixedPropertyPositionIgnoreCase(columnName) == -1) {
            throw new ColumnNotFoundException("Could not find column with name [" + columnName + "]");
        }
        QueryColumn qc = new QueryColumn(columnName, alias, true);
        this.visibleColumns.add(qc);
        return qc;
    }

    public List<QueryColumn> getVisibleColumns() {
        return visibleColumns;
    }

    @Override
    public List<String> getAllColumnNames() {
        return allColumnNamesSorted;
    }

    @Override
    public String getTableNameOrAlias() {
        return alias == null ? name : alias;
    }

    @Override
    public void addRange(Range range) {
        if (queryTemplatePacket == null) {
            queryTemplatePacket = createQueryTemplatePacketWithRange(this.name, range);
        } else {
            QueryTemplatePacket queryTemplatePacketNew = createQueryTemplatePacketWithRange(this.name, range);
            queryTemplatePacket = queryTemplatePacket.and(queryTemplatePacketNew);
        }
    }

    @Override
    public void setLimit(Integer value) {
        if (this.limit != Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Already set!");
        }
        this.limit = value;
    }
}