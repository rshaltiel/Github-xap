package com.gigaspaces.sql.datagateway.netty.utils;

import com.gigaspaces.jdbc.calcite.pg.PgTypeDescriptor;
import com.gigaspaces.sql.datagateway.netty.exception.ProtocolException;
import com.gigaspaces.sql.datagateway.netty.query.Session;
import io.netty.buffer.ByteBuf;

public class TypeInt8 extends PgType {
    public static final PgType INSTANCE = new TypeInt8();

    public TypeInt8() {
        super(PgTypeDescriptor.INT8);
    }

    @Override
    protected void asTextInternal(Session session, ByteBuf dst, Object value) throws ProtocolException {
        TypeUtils.checkType(value, Long.class);
        TypeUtils.writeText(session, dst, value.toString());
    }

    @Override
    protected <T> T fromTextInternal(Session session, ByteBuf src) {
        return (T) Long.valueOf(TypeUtils.readText(session, src));
    }

    @Override
    protected void asBinaryInternal(Session session, ByteBuf dst, Object value) throws ProtocolException {
        TypeUtils.checkType(value, Long.class);
        dst.writeInt(8).writeLong((Long) value);
    }

    @Override
    protected <T> T fromBinaryInternal(Session session, ByteBuf src) throws ProtocolException {
        TypeUtils.checkLen(src, 8);
        return (T) Long.valueOf(src.readLong());
    }
}
