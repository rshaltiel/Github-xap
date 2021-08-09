package com.gigaspaces.sql.datagateway.netty.utils;

import com.gigaspaces.jdbc.calcite.pg.PgTypeDescriptor;
import com.gigaspaces.sql.datagateway.netty.exception.ProtocolException;
import com.gigaspaces.sql.datagateway.netty.query.Session;
import io.netty.buffer.ByteBuf;

public class TypeFloat4 extends PgType {
    public static final PgType INSTANCE = new TypeFloat4();

    public TypeFloat4() {
        super(PgTypeDescriptor.FLOAT4);
    }

    @Override
    protected void asTextInternal(Session session, ByteBuf dst, Object value) throws ProtocolException {
        TypeUtils.checkType(value, Float.class);
        TypeUtils.writeText(session, dst, value.toString());
    }

    @Override
    protected <T> T fromTextInternal(Session session, ByteBuf src) {
        return (T) Float.valueOf(TypeUtils.readText(session, src));
    }

    @Override
    protected void asBinaryInternal(Session session, ByteBuf dst, Object value) throws ProtocolException {
        TypeUtils.checkType(value, Float.class);
        dst.writeInt(4).writeFloat((Float) value);
    }

    @Override
    protected <T> T fromBinaryInternal(Session session, ByteBuf src) throws ProtocolException {
        TypeUtils.checkLen(src, 4);
        return (T) Float.valueOf(src.readFloat());
    }
}
