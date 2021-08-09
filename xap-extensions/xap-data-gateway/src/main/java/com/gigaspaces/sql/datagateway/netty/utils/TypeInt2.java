package com.gigaspaces.sql.datagateway.netty.utils;

import com.gigaspaces.jdbc.calcite.pg.PgTypeDescriptor;
import com.gigaspaces.sql.datagateway.netty.exception.ProtocolException;
import com.gigaspaces.sql.datagateway.netty.query.Session;
import io.netty.buffer.ByteBuf;

public class TypeInt2 extends PgType {
    public static final PgType INSTANCE = new TypeInt2();

    public TypeInt2() {
        super(PgTypeDescriptor.INT2);
    }

    @Override
    protected void asTextInternal(Session session, ByteBuf dst, Object value) throws ProtocolException {
        TypeUtils.checkType(value, Short.class);
        TypeUtils.writeText(session, dst, value.toString());
    }

    @Override
    protected <T> T fromTextInternal(Session session, ByteBuf src) {
        return (T) Short.valueOf(TypeUtils.readText(session, src));
    }

    @Override
    protected void asBinaryInternal(Session session, ByteBuf dst, Object value) throws ProtocolException {
        TypeUtils.checkType(value, Short.class);
        dst.writeInt(2).writeShort((Short) value);
    }

    @Override
    protected <T> T fromBinaryInternal(Session session, ByteBuf src) throws ProtocolException {
        TypeUtils.checkLen(src, 2);
        return (T) Short.valueOf(src.readShort());
    }
}
