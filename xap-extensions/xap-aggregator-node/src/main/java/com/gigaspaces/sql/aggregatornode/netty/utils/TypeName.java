package com.gigaspaces.sql.aggregatornode.netty.utils;

import com.gigaspaces.sql.aggregatornode.netty.exception.ProtocolException;
import com.gigaspaces.sql.aggregatornode.netty.query.Session;
import io.netty.buffer.ByteBuf;

public class TypeName extends PgType {
    public static final PgType INSTANCE = new TypeName();

    public TypeName() {
        super(19, "name", 63, 1003, 0);
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
