package com.gigaspaces.jdbc;

import com.gigaspaces.jdbc.explainplan.JoinExplainPlan;
import com.gigaspaces.jdbc.model.QueryExecutionConfig;
import com.gigaspaces.jdbc.model.result.*;
import com.gigaspaces.jdbc.model.table.AggregationColumn;
import com.gigaspaces.jdbc.model.table.IQueryColumn;
import com.gigaspaces.jdbc.model.table.OrderColumn;
import com.gigaspaces.jdbc.model.table.TableContainer;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JoinQueryExecutor {
    private final List<TableContainer> tables;
    private final List<IQueryColumn> visibleColumns;
    private final Set<IQueryColumn> invisibleColumns;
    private final QueryExecutionConfig config;
    private final List<ProcessLayer> processLayers;

    public JoinQueryExecutor(QueryExecutor queryExecutor) {
        this.tables = queryExecutor.getTables();
        this.invisibleColumns = queryExecutor.getInvisibleColumns();
        this.visibleColumns = queryExecutor.getVisibleColumns();
        this.config = queryExecutor.getConfig();
        this.config.setJoinUsed(true);
        this.processLayers = queryExecutor.getProcessLayers();
    }

    public QueryResult execute() {
        boolean isDistinct = false;
        for (TableContainer table : tables) {
            try {
                table.executeRead(config);
                isDistinct |= table.isDistinct();
            } catch (SQLException e) {
                throw new IllegalArgumentException(e);
            }
        }
        QueryResult res = null;
        for (int i = 0; i < processLayers.size(); i++) {
            ProcessLayer processLayer = processLayers.get(i);
            Collections.sort(processLayer.getOrderColumns()); //TODO see if necessary
            if(i == 0){
                if (visibleColumns.isEmpty()) {
                    visibleColumns.addAll(processLayer.getGroupByColumns());
                }
                List<IQueryColumn> allColumns = Stream.concat(visibleColumns.stream(), invisibleColumns.stream()).collect(Collectors.toList());
                JoinTablesIterator joinTablesIterator = new JoinTablesIterator(tables);
                if (config.isExplainPlan()) {
                    List<IQueryColumn> projectedColumns = processLayers.get(processLayers.size()-1).getProjectedColumns();
                    return explain(joinTablesIterator, projectedColumns, processLayer.getOrderColumns(), processLayer.getGroupByColumns(), processLayer.getAggregationColumns(), isDistinct);
                }
                res = new JoinQueryResult(allColumns);
                outer: while (joinTablesIterator.hasNext()) {
                    for (TableContainer table : tables) {
                        if(!table.checkJoinCondition()){
                            continue outer;
                        }
                    }
                    res.addRow(TableRowFactory.createTableRowFromSpecificColumns(allColumns, Collections.emptyList(), Collections.emptyList()));
                }
            }
            res = processLayer.process(res);
        }
        //TODO limit
        return res;
    }

    private QueryResult explain(JoinTablesIterator joinTablesIterator, List<IQueryColumn> projectedColumns, List<OrderColumn> orderColumns,
                                List<IQueryColumn> groupByColumns, List<AggregationColumn> aggregationColumns, boolean isDistinct) {
        Stack<TableContainer> stack = new Stack<>();
        TableContainer current = joinTablesIterator.getStartingPoint();
        stack.push(current);
        while (current.getJoinedTable() != null) {
            current = current.getJoinedTable();
            stack.push(current);
        }
        TableContainer first = stack.pop();
        TableContainer second = stack.pop();
        JoinExplainPlan joinExplainPlan = new JoinExplainPlan(first.getJoinInfo(), ((ExplainPlanQueryResult) first.getQueryResult()).getExplainPlanInfo(), ((ExplainPlanQueryResult) second.getQueryResult()).getExplainPlanInfo());
        TableContainer last = second;
        while (!stack.empty()) {
            TableContainer curr = stack.pop();
            joinExplainPlan = new JoinExplainPlan(last.getJoinInfo(), joinExplainPlan, ((ExplainPlanQueryResult) curr.getQueryResult()).getExplainPlanInfo());
            last = curr;
        }
        joinExplainPlan.setSelectColumns(projectedColumns.stream().map(IQueryColumn::toString).collect(Collectors.toList()));
        joinExplainPlan.setOrderColumns(orderColumns);
        joinExplainPlan.setGroupByColumns(groupByColumns);
        joinExplainPlan.setDistinct(isDistinct);
        joinExplainPlan.setAggregationColumns(aggregationColumns);
        return new ExplainPlanQueryResult(visibleColumns, joinExplainPlan, null);
    }
}
