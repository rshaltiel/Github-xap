package com.gigaspaces.jdbc;

import com.gigaspaces.jdbc.explainplan.JoinExplainPlan;
import com.gigaspaces.jdbc.model.QueryExecutionConfig;
import com.gigaspaces.jdbc.model.result.*;
import com.gigaspaces.jdbc.model.table.AggregationColumn;
import com.gigaspaces.jdbc.model.table.IQueryColumn;
import com.gigaspaces.jdbc.model.table.OrderColumn;
import com.gigaspaces.jdbc.model.table.TableContainer;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JoinQueryExecutor {
    private final List<TableContainer> tables;
    private final Set<IQueryColumn> invisibleColumns;
    private final List<IQueryColumn> visibleColumns;
    private final QueryExecutionConfig config;
    private final List<AggregationColumn> aggregationColumns;
    private final List<IQueryColumn> allQueryColumns;
    private final List<IQueryColumn> selectedQueryColumns;
    private final List<IQueryColumn> groupByColumns = new ArrayList<>();
    private final List<OrderColumn> orderColumns = new ArrayList<>();

    public JoinQueryExecutor(QueryExecutor queryExecutor) {
        this.tables = queryExecutor.getTables();
        tables.forEach(table -> this.groupByColumns.addAll(table.getGroupByColumns()));
        tables.forEach(table -> this.orderColumns.addAll(table.getOrderColumns()));
        this.invisibleColumns = queryExecutor.getInvisibleColumns();
        this.visibleColumns = (queryExecutor.getVisibleColumns().isEmpty() && queryExecutor.getAggregationColumns().isEmpty()) ? this.groupByColumns : queryExecutor.getVisibleColumns();
        this.config = queryExecutor.getConfig();
        this.config.setJoinUsed(true);
        this.aggregationColumns = queryExecutor.getAggregationColumns();
        this.allQueryColumns = Stream.concat(visibleColumns.stream(), invisibleColumns.stream()).collect(Collectors.toList());
        this.selectedQueryColumns = queryExecutor.getSelectedColumns().isEmpty() ? this.visibleColumns : queryExecutor.getSelectedColumns();
    }

    public QueryResult execute() {
        boolean isDistinct = false;
        for (TableContainer table : tables) {
            try {
                table.executeRead(config);
                isDistinct |= table.isDistinct();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new IllegalArgumentException(e);
            }
        }
        Collections.sort(orderColumns); //TODO see if necessary
        if (visibleColumns.isEmpty()) {
            visibleColumns.addAll(groupByColumns);
        }
        JoinTablesIterator joinTablesIterator = new JoinTablesIterator(tables);
        if (config.isExplainPlan()) {
            return explain(joinTablesIterator, orderColumns, groupByColumns, isDistinct);
        }
        QueryResult res = new JoinQueryResult(this.selectedQueryColumns);
        while (joinTablesIterator.hasNext()) {
            if (tables.stream().allMatch(TableContainer::checkJoinCondition))
                res.addRow(TableRowFactory.createTableRowFromSpecificColumns(this.allQueryColumns, orderColumns,
                        groupByColumns));
        }

        if (groupByColumns.isEmpty()) {
            if (!aggregationColumns.isEmpty()) {
                List<TableRow> aggregateRows = new ArrayList<>();
                TableRow aggregatedRow = config.isCalcite() ? TableRowUtils.aggregate(res.getRows(), selectedQueryColumns) : TableRowUtils.aggregate(res.getRows(), selectedQueryColumns, this.aggregationColumns, visibleColumns);
                aggregateRows.add(aggregatedRow);
                res.setRows(aggregateRows);
            }
        } else {
            res.groupBy(); //group by the results at the client
            if (!aggregationColumns.isEmpty()) {
                Map<TableRowGroupByKey, List<TableRow>> groupByRowsResult = res.getGroupByRowsResult();
                List<TableRow> totalAggregationsResultRowsList = new ArrayList<>();
                for (List<TableRow> rowsList : groupByRowsResult.values()) {
                    TableRow aggregatedRow = config.isCalcite() ? TableRowUtils.aggregate(rowsList, selectedQueryColumns) : TableRowUtils.aggregate(rowsList, selectedQueryColumns, this.aggregationColumns, visibleColumns);
                    totalAggregationsResultRowsList.add(aggregatedRow);
                }
                res.setRows(totalAggregationsResultRowsList);
            }
        }
        if (!orderColumns.isEmpty()) {
            res.sort(); //sort the results at the client
        }
        if (isDistinct) {
            res.distinct();
        }

        return res;
    }

    private QueryResult explain(JoinTablesIterator joinTablesIterator, List<OrderColumn> orderColumns,
                                List<IQueryColumn> groupByColumns, boolean isDistinct) {
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
        joinExplainPlan.setSelectColumns(visibleColumns.stream().map(IQueryColumn::toString).collect(Collectors.toList()));
        joinExplainPlan.setOrderColumns(orderColumns);
        joinExplainPlan.setGroupByColumns(groupByColumns);
        joinExplainPlan.setDistinct(isDistinct);
        return new ExplainPlanQueryResult(visibleColumns, joinExplainPlan, null);
    }
}
