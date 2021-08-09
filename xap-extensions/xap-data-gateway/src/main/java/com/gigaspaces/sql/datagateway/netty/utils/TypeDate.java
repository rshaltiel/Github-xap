package com.gigaspaces.sql.datagateway.netty.utils;

import com.gigaspaces.jdbc.calcite.pg.PgTypeDescriptor;
import com.gigaspaces.sql.datagateway.netty.exception.ProtocolException;
import com.gigaspaces.sql.datagateway.netty.query.Session;
import io.netty.buffer.ByteBuf;

// TODO implement type encoder/decoder
public class TypeDate extends PgType {
    public static final PgType INSTANCE = new TypeDate();

    public TypeDate() {
        super(PgTypeDescriptor.DATE);
    }

    @Override
    protected void asTextInternal(Session session, ByteBuf dst, Object value) throws ProtocolException {
        TypeUtils.writeText(session, dst, session.getDateTimeUtils().toString(value, false));
    }
}
