package com.gigaspaces.query.sql.functions;

import com.gigaspaces.internal.utils.ObjectConverter;
import com.gigaspaces.internal.utils.math.MutableNumber;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Returns the the addition of two objects
 *
 * @author Tomer Shapira
 * @since 16.0.0
 */
public class PlusSqlFunction extends SqlFunction {

    final static HashSet<String> monthIntervals = new HashSet<>(Arrays.asList("INTERVAL_YEAR", "INTERVAL_MONTH", "INTERVAL_YEAR_MONTH"));
    final static HashSet<String> nanoIntervals = new HashSet<>(Arrays.asList("INTERVAL_DAY", "INTERVAL_DAY_HOUR", "INTERVAL_DAY_MINUTE", "INTERVAL_DAY_SECOND", "INTERVAL_HOUR", "INTERVAL_HOUR_MINUTE", "INTERVAL_MINUTE",
            "INTERVAL_SECOND", "INTERVAL_HOUR_SECOND", "INTERVAL_MINUTE_SECOND"));

    /**
     * @param context which contains two arguments that have addition relations between them. also contains the type of the second object to support intervals
     * @return Returns the addition of the two objects.
     */
    @Override
    public Object apply(SqlFunctionExecutionContext context) {
        assertNumberOfArguments(2, context);
        Object left = context.getArgument(0);
        Object right = context.getArgument(1);
        Object res = null;
        Class originalClass = null;
        if(left == null || right == null){
            return null;
        }
        try {
            if (left instanceof Time) {
                originalClass = left.getClass();
                left = ObjectConverter.convert(left, LocalTime.class);
            }
            if (left instanceof Date || left instanceof java.util.Date) {
                originalClass = left.getClass();
                left = ObjectConverter.convert(left, LocalDate.class);
            }
            if (left instanceof Timestamp) {
                originalClass = left.getClass();
                left = ObjectConverter.convert(left, LocalDateTime.class);
            }
        }
        catch (Exception e){
            throw new RuntimeException("adding " + left + " to " + right + " is not supported");
        }

        if (left instanceof LocalTime) {
            if (right instanceof BigDecimal) {
                if (nanoIntervals.contains(context.getType())) {
                    long milliToNanoFactor = 1000 * 1000;
                    res = ((LocalTime) left).plusNanos(((BigDecimal) right).longValue() * milliToNanoFactor);
                } else {
                    throw new RuntimeException("cannot add " + context.getType() + " to time");
                }
            }

        }
        if (left instanceof LocalDate) {
            if (right instanceof BigDecimal) {
                if (monthIntervals.contains(context.getType())) {
                    res = ((LocalDate) left).plusMonths(((BigDecimal) right).longValue());
                } else if (context.getType().equals("INTERVAL_DAY")) {
                    long milliToDaysFactor = 1000 * 60 * 60 * 24;
                    res =  ((LocalDate) left).plusDays(((BigDecimal) right).longValue() / milliToDaysFactor);
                }
                else {
                    throw new RuntimeException("cannot add " + context.getType() + " to date");
                }
            }
        }
        if (left instanceof LocalDateTime) {
            if (right instanceof BigDecimal) {
                if (monthIntervals.contains(context.getType())) {
                    res = ((LocalDateTime) left).plusMonths(((BigDecimal) right).longValue());
                } else if (nanoIntervals.contains(context.getType())){
                    long milliToNanoFactor = 1000 * 1000;
                    res = ((LocalDateTime) left).plusNanos(((BigDecimal) right).longValue() * milliToNanoFactor);
                }
                else {
                    throw new RuntimeException("cannot add " + context.getType() + " to date");
                }
            }
        }
        else if(left instanceof Number && right instanceof Number){
            MutableNumber mutableNumber = MutableNumber.fromClass(left.getClass(), false);
            mutableNumber.add((Number) left);
            mutableNumber.add((Number) right);
            res = mutableNumber.toNumber();
        }
        if (res != null) {
            if (originalClass == null){
                return res;
            }
            else{
                try {
                    return ObjectConverter.convert(res, originalClass);
                } catch (SQLException throwables) {
                    throw new RuntimeException("adding " + left + " to " + right + " is not supported");
                }
            }
        }
        throw new RuntimeException("adding " + left + " to " + right + " is not supported");
    }
}
