package com.gigaspaces.jdbc.model.result;

import com.gigaspaces.jdbc.model.join.JoinInfo;
import com.gigaspaces.jdbc.model.table.CaseColumn;
import com.gigaspaces.jdbc.model.table.IQueryColumn;
import com.gigaspaces.jdbc.model.table.TableContainer;
import com.j_spaces.jdbc.ResultEntry;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.util.Pair;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class QueryResult {
    private final List<IQueryColumn> selectedColumns;
    private Cursor<TableRow> cursor;
    private Map<TableRowGroupByKey, List<TableRow>> groupByRows = new HashMap<>();

    public QueryResult(List<IQueryColumn> selectedColumns) {
        this.selectedColumns = selectedColumns;
    }

    public List<IQueryColumn> getSelectedColumns() {
        return selectedColumns;
    }

    public TableContainer getTableContainer() {
        return null;
    }

    public int size() {
        return 0;
    }

    public Map<TableRowGroupByKey, List<TableRow>> getGroupByRowsResult() {
        return groupByRows;
    }

    private void setGroupByRowsResult(Map<TableRowGroupByKey, List<TableRow>> groupByRows) {
        this.groupByRows = groupByRows;
    }

    public void addRow(TableRow tableRow) {
    }

    public List<TableRow> getRows() {
        return null;
    }

    public void setRows(List<TableRow> rows) {
    }

    public void filter(Predicate<TableRow> predicate) {
        setRows(getRows().stream().filter(predicate).collect(Collectors.toList()));
    }

    public void sort() {
        Collections.sort(getRows());
    }

    public boolean next() {
        if (getTableContainer() == null || getTableContainer().getJoinedTable() == null) {
            return getCursor().next();
        }
        QueryResult joinedResult = getTableContainer().getJoinedTable().getQueryResult();
        if (joinedResult == null) {
            return getCursor().next();
        }
        while (hasNext()) {
            JoinInfo joinInfo = getTableContainer().getJoinedTable().getJoinInfo();
            if (joinInfo != null && joinInfo.getJoinType().equals(JoinInfo.JoinType.SEMI) && joinInfo.isHasMatch()) {
                if (getCursor().next()) {
                    joinedResult.reset();
                    joinInfo.resetHasMatch();
                } else {
                    return false;
                }
            }
            if (joinedResult.next()) {
                return true;
            }
            if (getCursor().next()) {
                if (joinInfo != null)
                    joinInfo.resetHasMatch();
                joinedResult.reset();
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean hasNext() {
        if (getCursor().isBeforeFirst())
            return getCursor().next();
        return true;
    }

    public TableRow getCurrent() {
        return getCursor().getCurrent();
    }

    public void reset() {
        getCursor().reset();
        if (getTableContainer() == null || getTableContainer().getJoinedTable() == null) {
            return;
        }
        QueryResult joinedResult = getTableContainer().getJoinedTable().getQueryResult();
        if (joinedResult != null) {
            joinedResult.reset();
        }
    }

    public Cursor<TableRow> getCursor() {
        if (cursor == null) {
            cursor = getCursorType().equals(Cursor.Type.SCAN) ? new RowScanCursor(getRows()) :
                    new HashedRowCursor(getTableContainer().getJoinInfo(), getRows());
        }
        return cursor;
    }

    public Cursor.Type getCursorType() {
        if (getTableContainer() != null && getTableContainer().getJoinInfo() != null) {
            JoinInfo joinInfo = getTableContainer().getJoinInfo();
            if(joinInfo.joinConditionsContainsOnlyEqualAndAndOperators() || joinInfo.isEquiJoin()) {
                return Cursor.Type.HASH;
            }
        }
        return Cursor.Type.SCAN;
    }

    public ResultEntry convertEntriesToResultArrays(RelRoot logicalRel) {
        // Column (field) names and labels (aliases)
        int columns = getSelectedColumns().size();

        String[] fieldNames = getSelectedColumns().stream().map(IQueryColumn::getName).toArray(String[]::new);
        String[] columnLabels = getSelectedColumns().stream().map(qC -> qC.getAlias() == null ? qC.getName() : qC.getAlias()).toArray(String[]::new);

        // use logical rel to extract final projected columns and alias
        if (logicalRel !=null && logicalRel.fields != null && logicalRel.fields.size() <= columnLabels.length) {
            for (Pair<Integer, String> field : logicalRel.fields) {
                String columnLabel = columnLabels[field.getKey()];
                String newLabel = field.getValue();
                //e.g. when newLabel == id0 and columnLabel == id then skip, otherwise replace
                if (!(newLabel.length() > columnLabel.length() && newLabel.startsWith(columnLabel))) {
                    columnLabels[field.getKey()] = newLabel;
                }
            }
        }

        //the field values for the result
        Object[][] fieldValues = new Object[size()][columns];


        int row = 0;

        while (next()) {
            TableRow entry = getCurrent();
            int column = 0;
            for (int i = 0; i < columns; i++) {
                fieldValues[row][column++] = entry.getPropertyValue(getSelectedColumns().get(i));
            }

            row++;
        }

        return new ResultEntry(
                fieldNames,
                columnLabels,
                null, //TODO
                fieldValues);
    }

    public void groupBy() {

        Map<TableRowGroupByKey, TableRow> tableRows = new HashMap<>();
        Map<TableRowGroupByKey, List<TableRow>> groupByTableRows = new HashMap<>();

        for (TableRow tableRow : getRows()) {
            Object[] groupByValues = tableRow.getGroupByValues();
            if (groupByValues.length > 0) {
                TableRowGroupByKey key = new TableRowGroupByKey(groupByValues);
                tableRows.putIfAbsent(key, tableRow);

                List<TableRow> tableRowsList = groupByTableRows.computeIfAbsent(key, k -> new ArrayList<>());
                tableRowsList.add(tableRow);
            }
        }
        if (!tableRows.isEmpty()) {
            setGroupByRowsResult(groupByTableRows);
            setRows(new ArrayList<>(tableRows.values()));
        }
    }

    public void distinct() {
        Map<TableRowGroupByKey, TableRow> tableRows = new HashMap<>();
        for (TableRow tableRow : getRows()) {
            Object[] distinctValues = tableRow.getDistinctValues();
            if (distinctValues.length > 0) {
                tableRows.putIfAbsent(new TableRowGroupByKey(distinctValues), tableRow);
            }
        }
        if (!tableRows.isEmpty()) {
            setRows(new ArrayList<>(tableRows.values()));
        }

    }

    public void limit(Integer limit) {
        if (getRows() != null) {
            List<TableRow> limitedRows = getRows().subList(0, Math.min(limit, size()));
            setRows(limitedRows);
        }
    }

    public void applyCaseColumnsOnResult() {
        if (this instanceof ExplainPlanQueryResult) return;
        List<CaseColumn> caseColumns =
                getSelectedColumns().stream().filter(qc -> qc instanceof CaseColumn).map(qc -> ((CaseColumn) qc)).collect(Collectors.toList());
        if (caseColumns.isEmpty()) return;
        List<TableRow> newRows = new ArrayList<>();
        for (TableRow row : getRows()) {
            newRows.add(TableRowFactory.applyCaseColumnToTableRow(row, caseColumns));
        }
        setRows(newRows);
    }
}
