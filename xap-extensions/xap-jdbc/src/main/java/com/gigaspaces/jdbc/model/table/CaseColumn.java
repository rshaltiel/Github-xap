package com.gigaspaces.jdbc.model.table;

import com.gigaspaces.internal.transport.IEntryPacket;
import com.gigaspaces.internal.utils.ObjectConverter;
import com.gigaspaces.jdbc.model.result.TableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CaseColumn implements IQueryColumn{
    private final String columnName;
    private final Class<?> returnType;
    private final int columnOrdinal;
    private final List<ICaseCondition> caseConditions;
    private static Logger _logger = LoggerFactory.getLogger(CaseColumn.class);

    public CaseColumn(String columnName, Class<?> type, int columnOrdinal) {
        this.columnName = columnName;
        this.returnType = type;
        this.columnOrdinal = columnOrdinal;
        this.caseConditions = new ArrayList<>();
    }

    @Override
    public int getColumnOrdinal() {
        return columnOrdinal;
    }

    @Override
    public String getName() {
        return columnName;
    }

    @Override
    public String getAlias() {
        return columnName;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public boolean isUUID() {
        return false;
    }

    @Override
    public TableContainer getTableContainer() {
        return null;
    }

    @Override
    public Object getCurrentValue() {
        return null;
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public IQueryColumn create(String columnName, String columnAlias, boolean isVisible, int columnOrdinal) {
        return null;
    }

    @Override
    public Object getValue(IEntryPacket entryPacket) {
        throw new UnsupportedOperationException("Unsupported yet");
    }

    @Override
    public int compareTo(IQueryColumn other) {
        return Integer.compare(this.getColumnOrdinal(), other.getColumnOrdinal());
    }

    public void addCaseCondition(ICaseCondition caseCondition) {
        this.caseConditions.add(caseCondition);
    }

    public Object getValue(TableRow tableRow) {
        for (ICaseCondition caseCondition : caseConditions) {
            if(caseCondition.check(tableRow)) {
                Object result = caseCondition.getResult();
                if(result instanceof CaseColumn) { //nested case
                    return ((CaseColumn) result).getValue(tableRow);
                } else if (result instanceof ConcreteColumn) { //column value
                    return tableRow.getPropertyValue(((ConcreteColumn) result).getAlias());
                }
                try {
                    return ObjectConverter.convert(result, this.returnType);
                } catch (SQLException e) {
                    _logger.warn("Failed to convert CASE column value to " + this.returnType.getName(), e);
                    return result;
                }
            }
        }
        throw new IllegalStateException("should not arrive here");
    }

    @Override
    public String toString() {
        return getAlias();
    }

    @Override
    public boolean isCaseColumn() {
        return true;
    }
}
