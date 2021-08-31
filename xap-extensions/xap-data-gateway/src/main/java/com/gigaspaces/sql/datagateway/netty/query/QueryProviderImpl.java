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

import com.fasterxml.jackson.databind.util.ArrayIterator;
import com.gigaspaces.jdbc.calcite.GSOptimizer;
import com.gigaspaces.jdbc.calcite.GSOptimizerValidationResult;
import com.gigaspaces.jdbc.calcite.GSRelNode;
import com.gigaspaces.jdbc.calcite.handlers.CalciteQueryHandler;
import com.gigaspaces.jdbc.calcite.sql.extension.SqlDeallocate;
import com.gigaspaces.jdbc.calcite.sql.extension.SqlShowOption;
import com.gigaspaces.jdbc.calcite.utils.CalciteUtils;
import com.gigaspaces.query.sql.functions.extended.LocalSession;
import com.gigaspaces.sql.datagateway.netty.exception.ExceptionUtil;
import com.gigaspaces.sql.datagateway.netty.exception.NonBreakingException;
import com.gigaspaces.sql.datagateway.netty.exception.ProtocolException;
import com.gigaspaces.sql.datagateway.netty.utils.*;
import com.google.common.collect.ImmutableList;
import com.j_spaces.jdbc.ResponsePacket;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.externalize.RelWriterImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.type.SqlTypeName;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;
import java.util.stream.Collectors;

import static com.gigaspaces.sql.datagateway.netty.utils.DateTimeUtils.convertTimeZone;
import static java.util.Collections.singletonList;

@SuppressWarnings({"unchecked", "rawtypes"})
public class QueryProviderImpl implements QueryProvider {

    private static final String SELECT_NULL_NULL_NULL = "SELECT NULL, NULL, NULL";

    private final CalciteQueryHandler handler;

    private final Map<String, Statement> statements = new HashMap<>();
    private final Map<String, Portal<?>> portals = new HashMap<>();
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(QueryProviderImpl.class);

    public QueryProviderImpl() {
        this.handler = new CalciteQueryHandler();
    }

    @Override
    public void prepare(Session session, String stmt, String qry, int[] paramTypes) throws ProtocolException {
        if (stmt.isEmpty())
            statements.remove(stmt);
        else if (statements.containsKey(stmt))
            throw new NonBreakingException(ErrorCodes.INVALID_STATEMENT_NAME, "Duplicate statement name");

        statements.put(stmt, prepareStatement(session, stmt, qry, paramTypes));
    }

    @Override
    public void bind(Session session, String portal, String stmt, Object[] params, int[] formatCodes) throws ProtocolException {
        if (!statements.containsKey(stmt))
            throw new NonBreakingException(ErrorCodes.INVALID_STATEMENT_NAME, "Invalid statement name");

        if (portal.isEmpty())
            portals.remove(stmt);
        else if (portals.containsKey(portal))
            throw new NonBreakingException(ErrorCodes.INVALID_CURSOR_NAME, "Duplicate cursor name");

        portals.put(portal, preparePortal(session, portal, statements.get(stmt), params, formatCodes));
    }

    @Override
    public StatementDescription describeS(String stmt) throws ProtocolException {
        if (!statements.containsKey(stmt))
            throw new NonBreakingException(ErrorCodes.INVALID_STATEMENT_NAME, "Invalid statement name");

        return statements.get(stmt).getDescription();
    }

    @Override
    public RowDescription describeP(String portal) throws ProtocolException {
        if (!portals.containsKey(portal))
            throw new NonBreakingException(ErrorCodes.INVALID_CURSOR_NAME, "Invalid cursor name");

        return portals.get(portal).getDescription();
    }

    @Override
    public Portal<?> execute(String portal) throws ProtocolException {
        if (!portals.containsKey(portal))
            throw new NonBreakingException(ErrorCodes.INVALID_CURSOR_NAME, "Invalid cursor name");

        Portal<?> res = portals.get(portal);
        res.execute();
        return res;
    }

    @Override
    public void closeS(String name) {
        List<String> toClose = portals.values().stream()
                .filter(p -> p.getStatement().getName().equals(name))
                .map(Portal::name)
                .collect(Collectors.toList());

        for (String portal : toClose)
            closeP(portal);

        statements.remove(name);
    }

    @Override
    public void closeP(String name) {
        portals.remove(name);
    }

    @Override
    public void cancel(int pid, int secret) {
        // TODO implement query cancel protocol
    }

    @Override
    public List<Portal<?>> executeQueryMultiline(Session session, String query) throws ProtocolException {
        log.debug("Executing query: {}", query);
        if (query.equalsIgnoreCase(SELECT_NULL_NULL_NULL)) {
            List<ColumnDescription> columns = ImmutableList.of(
                    new ColumnDescription("column1", TypeUtils.PG_TYPE_UNKNOWN),
                    new ColumnDescription("column2", TypeUtils.PG_TYPE_UNKNOWN),
                    new ColumnDescription("column3", TypeUtils.PG_TYPE_UNKNOWN)
            );
            StatementDescription statementDescription = new StatementDescription(ParametersDescription.EMPTY, new RowDescription(columns));
            StatementImpl statement = new StatementImpl(this, Constants.EMPTY_STRING, null, null, statementDescription);
            QueryOp<Object[]> op = () -> singletonList(new Object[]{null, null, null}).iterator();
            return Collections.singletonList(new QueryPortal<>(this, Constants.EMPTY_STRING, statement, PortalCommand.SELECT, Constants.EMPTY_INT_ARRAY, op));
        }

        // TODO possibly it's worth to add SqlEmptyNode to sql parser
        if (query.trim().isEmpty()) {
            StatementImpl statement = new StatementImpl(this, Constants.EMPTY_STRING, null, null, StatementDescription.EMPTY);
            EmptyPortal<Object> portal = new EmptyPortal<>(this, Constants.EMPTY_STRING, statement);
            return Collections.singletonList(portal);
        }

        query = prepareQueryForCalcite(session, query);
        GSOptimizer optimizer = new GSOptimizer(session.getSpace());

        SqlNodeList nodes = parseMultiline(optimizer, query);

        List<Portal<?>> result = new ArrayList<>();
        for (SqlNode node : nodes) {
            StatementImpl statement = prepareStatement(session, Constants.EMPTY_STRING, optimizer, Constants.EMPTY_INT_ARRAY, node);
            result.add(preparePortal(session, Constants.EMPTY_STRING, statement, Constants.EMPTY_OBJECT_ARRAY, Constants.EMPTY_INT_ARRAY));
        }
        return result;
    }

    private StatementImpl prepareStatement(Session session, String name, String query, int[] paramTypes) throws ProtocolException {
        // TODO possibly it's worth to add SqlEmptyNode to sql parser
        if (query.trim().isEmpty()) {
            assert paramTypes.length == 0;
            return new StatementImpl(this, name, null, null, StatementDescription.EMPTY);
        }
        query = prepareQueryForCalcite(session, query);
        GSOptimizer optimizer = new GSOptimizer(session.getSpace());

        SqlNode ast = parse(optimizer, query);

        return prepareStatement(session, name, optimizer, paramTypes, ast);
    }

    private StatementImpl prepareStatement(Session session, String name, GSOptimizer optimizer, int[] paramTypes, SqlNode ast) throws ProtocolException {
        if (SqlUtil.isCallTo(ast, SqlShowOption.OPERATOR)) {
            StatementDescription description = describeShow((SqlShowOption) ast);
            return new StatementImpl(this, name, ast, optimizer, description);
        } else if (SqlUtil.isCallTo(ast, SqlDeallocate.OPERATOR)) {
            // all parameters should be literals
            return new StatementImpl(this, name, ast, optimizer, StatementDescription.EMPTY);
        } else if (ast.getKind() == SqlKind.SET_OPTION) {
            // all parameters should be literals
            return new StatementImpl(this, name, ast, optimizer, StatementDescription.EMPTY);
        } else if (ast.getKind() == SqlKind.EXPLAIN) {
            SqlExplain explain = (SqlExplain) ast;
            SqlNode explicandum = explain.getExplicandum();
            if (explicandum.isA(SqlKind.QUERY)) {
                SqlExplain.Depth depth = explain.getDepth();
                SqlExplainLevel detailLevel = explain.getDetailLevel();
                SqlExplainFormat format = explain.getFormat();

                GSOptimizerValidationResult validated = validate(optimizer, explicandum);
                explicandum = validated.getValidatedAst();
                RelDataType rowType = validated.getRowType();
                ParametersDescription paramDesc = getParametersDescription(paramTypes, validated.getParameterRowType());

                return new ExplainStatement(this, name, depth, detailLevel, format, rowType, paramDesc, explicandum, optimizer);
            }
            throw new NonBreakingException(ErrorCodes.UNSUPPORTED_FEATURE, "cannot explain non-query statement. query: " + ast);
        } else {
            GSOptimizerValidationResult validated = validate(optimizer, ast);

            ParametersDescription paramDesc = getParametersDescription(paramTypes, validated.getParameterRowType());

            RelDataType rowType = validated.getRowType();
            List<ColumnDescription> columns = new ArrayList<>(rowType.getFieldCount());
            for (RelDataTypeField field : rowType.getFieldList()) {
                columns.add(new ColumnDescription(field.getName(), TypeUtils.fromInternal(field.getType())));
            }
            RowDescription rowDesc = new RowDescription(columns);

            StatementDescription description = new StatementDescription(paramDesc, rowDesc);
            return new StatementImpl(this, name, validated.getValidatedAst(), optimizer, description);
        }
    }

    private ParametersDescription getParametersDescription(int[] requestedTypes, RelDataType inferredTypes) {
        ParametersDescription paramDesc;
        List<RelDataTypeField> fieldList = inferredTypes.getFieldList();
        if (fieldList.isEmpty()) {
            paramDesc = ParametersDescription.EMPTY;
        } else {
            List<ParameterDescription> params = new ArrayList<>(inferredTypes.getFieldCount());
            for (int i = 0; i < fieldList.size(); i++) {
                if (requestedTypes.length <= i || requestedTypes[i] == 0) {
                    params.add(new ParameterDescription(TypeUtils.fromInternal(fieldList.get(i).getType())));
                } else {
                    params.add(new ParameterDescription(TypeUtils.getType(requestedTypes[i])));
                }
            }
            paramDesc = new ParametersDescription(params);
        }
        return paramDesc;
    }

    private StatementDescription describeShow(SqlShowOption node) {
        PgType type;

        String name = node.getName().toString();
        switch (name.toLowerCase(Locale.ROOT)) {
            case "transaction_isolation":
            case "client_encoding":
            case "datestyle": {
                type = TypeVarchar.INSTANCE;
                break;
            }
            case "max_identifier_length":
            case "statement_timeout":
            case "extra_float_digits": {
                type = TypeInt4.INSTANCE;
                break;
            }

            default: {
                type = TypeUnknown.INSTANCE;
                break;
            }
        }

        return new StatementDescription(ParametersDescription.EMPTY,
                new RowDescription(singletonList(new ColumnDescription(name, type))));
    }

    private Portal<?> preparePortal(Session session, String name, Statement statement, Object[] params, int[] formatCodes) throws ProtocolException {
        SqlNode query = statement.getQuery();

        if (query == null)
            return new EmptyPortal<>(this, name, statement);

        if (query.getKind() == SqlKind.SET_OPTION) {
            return prepareSetOption(session, name, statement, (SqlSetOption) query);
        }

        if (SqlUtil.isCallTo(query, SqlShowOption.OPERATOR)) {
            return prepareShowOption(session, name, statement, (SqlShowOption) query);
        }

        if (SqlUtil.isCallTo(query, SqlDeallocate.OPERATOR)) {
            return prepareDeallocate(session, name, statement, (SqlDeallocate) query);
        }

        if (statement instanceof ExplainStatement) {
            return prepareExplain(session, name, (ExplainStatement) statement, params, formatCodes, query);
        }

        if (query.isA(SqlKind.QUERY)) {
            return prepareQuery(session, name, statement, params, formatCodes, query);
        }

        throw new NonBreakingException(ErrorCodes.UNSUPPORTED_FEATURE, "Unsupported query kind: " + query.getKind());
    }

    private Portal<Object[]> prepareExplain(Session session, String name, ExplainStatement statement, Object[] params, int[] formatCodes, SqlNode query) {
            QueryOp<Object[]> op = () -> {
                SqlExplain.Depth depth = statement.getDepth();
                if (depth == SqlExplain.Depth.LOGICAL)
                    throw new NonBreakingException(ErrorCodes.UNSUPPORTED_FEATURE, "Logical explanation is unsupported.");

                if (depth == SqlExplain.Depth.TYPE) {
                    String dump = RelOptUtil.dumpType(statement.getRowType());
                    return singletonList(new Object[]{dump}).iterator();
                }

                GSOptimizer optimizer = statement.getOptimizer();
                RelRoot relRoot = optimizer.optimizeLogical(query);
                GSRelNode physicalPlan = optimizer.optimizePhysical(relRoot);
                LocalSession localSession = new LocalSession(session.getUsername());
                ResponsePacket packet = handler.executeExplain(session.getSpace(), physicalPlan, params, localSession);
                return new ArrayIterator<>(packet.getResultEntry().getFieldValues());
            };
            return new QueryPortal<>(this, name, statement, PortalCommand.SELECT, formatCodes, op);
    }

    private Portal<Object[]> prepareShowOption(Session session, String name, Statement statement, SqlShowOption show) {
        String var = show.getName().toString();
        switch (var.toLowerCase(Locale.ROOT)) {
            case "transaction_isolation":
                return new QueryPortal(this, name, statement, PortalCommand.SHOW, Constants.EMPTY_INT_ARRAY, () -> singletonList(new Object[]{"READ_COMMITTED"}).iterator());
            case "client_encoding":
                return new QueryPortal(this, name, statement, PortalCommand.SHOW, Constants.EMPTY_INT_ARRAY, () -> singletonList(new Object[]{session.getCharset().name()}).iterator());
            case "datestyle":
                return new QueryPortal(this, name, statement, PortalCommand.SHOW, Constants.EMPTY_INT_ARRAY, () -> singletonList(new Object[]{session.getDateStyle()}).iterator());
            case "statement_timeout":
                return new QueryPortal(this, name, statement, PortalCommand.SHOW, Constants.EMPTY_INT_ARRAY, () -> singletonList(new Object[]{0}).iterator());
            case "extra_float_digits":
                return new QueryPortal(this, name, statement, PortalCommand.SHOW, Constants.EMPTY_INT_ARRAY, () -> singletonList(new Object[]{2}).iterator());
            case "max_identifier_length":
                return new QueryPortal(this, name, statement, PortalCommand.SHOW, Constants.EMPTY_INT_ARRAY, () -> singletonList(new Object[]{63}).iterator());
            default:
                return new QueryPortal(this, name, statement, PortalCommand.SHOW, Constants.EMPTY_INT_ARRAY, Collections::emptyIterator);
        }
    }

    private Portal<?> prepareDeallocate(Session session, String name, Statement statement, SqlDeallocate query) {
        String resource = query.getResourceName().toString();
        return new CommandPortal(this, name, statement, PortalCommand.DEALLOCATE, () -> closeS(resource));
    }

    private Portal<?> prepareSetOption(Session session, String name, Statement statement, SqlSetOption query) throws NonBreakingException {
        String var = query.getName().toString();

        if (!SqlUtil.isLiteral(query.getValue()))
            throw new NonBreakingException(ErrorCodes.INVALID_PARAMETER_VALUE,
                    query.getValue().getParserPosition(), "Literal value is expected.");

        SqlLiteral literal = (SqlLiteral) query.getValue();
        switch (var.toLowerCase(Locale.ROOT)) {
            case "client_encoding": {
                String val = asString(literal);
                CommandOp op = () -> {
                    try {
                        session.setCharset(Charset.forName(val));
                    } catch (UnsupportedCharsetException e) {
                        throw new NonBreakingException(ErrorCodes.INVALID_PARAMETER_VALUE, literal.getParserPosition(), "Unknown charset");
                    }
                };

                return new CommandPortal(this, name, statement, PortalCommand.SET, op);
            }

            case "datestyle": {
                String val = asString(literal);
                CommandOp op = () -> session.setDateStyle(val.indexOf(',') < 0 ? val + ", MDY" : val);
                return new CommandPortal(this, name, statement, PortalCommand.SET, op);
            }

            case "timezone" : {
                String val = asString(literal);
                CommandOp op = () -> session.setTimeZone(TimeZone.getTimeZone(convertTimeZone(val)));
                return new CommandPortal(this, name, statement, PortalCommand.SET, op);
            }

            default:
                // TODO support missing variables
                return new CommandPortal(this, name, statement, PortalCommand.SET, () -> {});
        }
    }

    private Portal<Object[]> prepareQuery(Session session, String name, Statement statement, Object[] params, int[] formatCodes, SqlNode query) {
        QueryOp<Object[]> op = () -> {
            GSRelNode physicalPlan = statement.getOptimizer().optimize(query);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            RelWriterImpl writer = new RelWriterImpl(pw, SqlExplainLevel.EXPPLAN_ATTRIBUTES, false);
            physicalPlan.explain(writer);
            log.info("Query physical plan: \n" + query + "\n-\n" + sw);

            LocalSession localSession = new LocalSession(session.getUsername());
            ResponsePacket packet = handler.executeStatement(session.getSpace(), physicalPlan, params, localSession);
            return new ArrayIterator<>(packet.getResultEntry().getFieldValues());
        };
        return new QueryPortal<>(this, name, statement, PortalCommand.SELECT, formatCodes, op);
    }

    private String asString(SqlLiteral literal) throws NonBreakingException {
        if (!SqlLiteral.valueMatchesType(literal.getValue(), SqlTypeName.CHAR))
            throw new NonBreakingException(ErrorCodes.INVALID_PARAMETER_VALUE,
                    literal.getParserPosition(), "String literal is expected.");
        return literal.getValueAs(String.class);
    }

    private SqlNode parse(GSOptimizer optimizer, String query) throws ProtocolException {
        try {
            return optimizer.parse(query);
        } catch (Exception e) {
            throw ExceptionUtil.wrapException("Failed to prepare query", e);
        }
    }

    private SqlNodeList parseMultiline(GSOptimizer optimizer, String query) throws ProtocolException {
        try {
            return optimizer.parseMultiline(query);
        } catch (Exception e) {
            throw ExceptionUtil.wrapException("Failed to prepare query", e);
        }
    }

    private GSOptimizerValidationResult validate(GSOptimizer optimizer, SqlNode ast) throws ProtocolException {
        try {
            return optimizer.validate(ast);
        } catch (Exception e) {
            throw ExceptionUtil.wrapException("Failed to prepare query", e);
        }
    }

    private static String prepareQueryForCalcite(Session session, String query) {
        Properties customProperties = session.getSpace().getURL().getCustomProperties();
        query = CalciteUtils.prepareQueryForCalcite(query, customProperties);
        return query;
    }
}
