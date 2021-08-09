package com.gigaspaces.sql.datagateway.netty.utils;

import com.gigaspaces.jdbc.calcite.pg.PgTypeDescriptor;
import com.gigaspaces.sql.datagateway.netty.exception.ProtocolException;
import com.gigaspaces.sql.datagateway.netty.query.Session;
import io.netty.buffer.ByteBuf;

public class TypeVarchar extends PgType {
    public static final PgType INSTANCE = new TypeVarchar();

    public TypeVarchar() {
        super(PgTypeDescriptor.VARCHAR);
    }

    @Override
    protected void asTextInternal(Session session, ByteBuf dst, Object value) throws ProtocolException {
        TypeUtils.checkType(value, String.class);
        TypeUtils.writeText(session, dst, value.toString());
    }

    @Override
    protected <T> T fromTextInternal(Session session, ByteBuf src) {
        return (T) TypeUtils.readText(session, src);
    }

    @Override
    protected void asBinaryInternal(Session session, ByteBuf dst, Object value) throws ProtocolException {
        TypeUtils.checkType(value, String.class);
        TypeUtils.writeText(session, dst, value.toString());
    }

    @Override
    protected <T> T fromBinaryInternal(Session session, ByteBuf src) {
        return (T) TypeUtils.readText(session, src);
    }
}
