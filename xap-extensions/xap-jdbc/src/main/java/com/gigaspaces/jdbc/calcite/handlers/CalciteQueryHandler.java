package com.gigaspaces.jdbc.calcite.handlers;

import com.gigaspaces.jdbc.QueryExecutor;
import com.gigaspaces.jdbc.calcite.GSOptimizer;
import com.gigaspaces.jdbc.calcite.GSOptimizerValidationResult;
import com.gigaspaces.jdbc.calcite.GSRelNode;
import com.gigaspaces.jdbc.calcite.SelectHandler;
import com.gigaspaces.jdbc.model.QueryExecutionConfig;
import com.gigaspaces.jdbc.model.result.ExplainPlanQueryResult;
import com.gigaspaces.jdbc.model.result.QueryResult;
import com.gigaspaces.query.sql.functions.extended.LocalSession;
import com.j_spaces.core.IJSpace;
import com.j_spaces.jdbc.ResponsePacket;
import org.apache.calcite.rel.externalize.RelWriterImpl;
import org.apache.calcite.runtime.CalciteException;
import org.apache.calcite.sql.SqlExplain;
import org.apache.calcite.sql.SqlExplainLevel;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.validate.SqlValidatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Properties;

import static com.gigaspaces.jdbc.calcite.utils.CalciteUtils.prepareQueryForCalcite;

public class CalciteQueryHandler {
    private static final Logger logger = LoggerFactory.getLogger("com.gigaspaces.jdbc.v3");
    private boolean explainPlan;


    public ResponsePacket handle(String query, IJSpace space, Object[] preparedValues) throws SQLException {
        Properties customProperties = space.getURL().getCustomProperties();
        GSRelNode calcitePlan = optimizeWithCalcite(query, space, customProperties);
        return executeStatement(space, calcitePlan, preparedValues, null);
    }

    public ResponsePacket executeStatement(IJSpace space, GSRelNode relNode, Object[] preparedValues, LocalSession session) throws SQLException {
        ResponsePacket packet = new ResponsePacket();
        QueryExecutionConfig queryExecutionConfig;
        if (explainPlan) {
            queryExecutionConfig = new QueryExecutionConfig(true, false);
        } else {
            queryExecutionConfig = new QueryExecutionConfig();
        }
        QueryExecutor qE = new QueryExecutor(space, queryExecutionConfig, preparedValues);
        SelectHandler selectHandler = new SelectHandler(qE, session);
        relNode.accept(selectHandler);
        QueryResult queryResult = qE.execute();
        if (explainPlan) {
            packet.setResultEntry(((ExplainPlanQueryResult) queryResult).convertEntriesToResultArrays(queryExecutionConfig));
        } else {
            packet.setResultEntry(queryResult.convertEntriesToResultArrays());
        }
        return packet;
    }

    public ResponsePacket executeExplain(IJSpace space, GSRelNode relNode, Object[] preparedValues, LocalSession session) throws SQLException {
        ResponsePacket packet = new ResponsePacket();
        QueryExecutionConfig queryExecutionConfig;
        queryExecutionConfig = new QueryExecutionConfig(true, false);
        QueryExecutor qE = new QueryExecutor(space, queryExecutionConfig, preparedValues);
        SelectHandler selectHandler = new SelectHandler(qE, session);
        relNode.accept(selectHandler);
        QueryResult queryResult = qE.execute();
        packet.setResultEntry(((ExplainPlanQueryResult) queryResult).convertEntriesToResultArrays(queryExecutionConfig));
        return packet;
    }

    private GSRelNode optimizeWithCalcite(String query, IJSpace space, Properties properties) throws SQLException {
        try {
            query = prepareQueryForCalcite(query, properties);
            GSOptimizer optimizer = new GSOptimizer(space);
            SqlNode ast = optimizer.parse(query);
            if (ast instanceof SqlExplain) {
                ast = ((SqlExplain) ast).getExplicandum();
                explainPlan = true;
            }
            GSOptimizerValidationResult validated = optimizer.validate(ast);
            GSRelNode physicalPlan = optimizer.optimize(validated.getValidatedAst());
            if (explainPlan || logger.isDebugEnabled()) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                RelWriterImpl writer = new RelWriterImpl(pw, SqlExplainLevel.EXPPLAN_ATTRIBUTES, false);
                physicalPlan.explain(writer);
                logger.info("Physical Plan: \n" + sw.toString());
            }

            return physicalPlan;
        } catch (CalciteException calciteException) {
            Throwable cause = calciteException.getCause();
            if (cause != null) {
                if (cause instanceof SqlValidatorException) {
                    throw new SQLException("Query validation failed.", cause);
                }
            }
            throw calciteException; //runtime
        } catch (SqlParseException sqlParseException) {
            throw new SQLException("Query parsing failed.", sqlParseException);
        }
    }

}