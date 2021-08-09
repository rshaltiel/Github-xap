package com.gigaspaces.sql.datagateway.netty.utils;

import com.gigaspaces.jdbc.calcite.pg.PgTypeDescriptor;

public class TypeAny extends PgType {
    public static final PgType INSTANCE = new TypeAny();

    public TypeAny() {
        super(PgTypeDescriptor.ANY);
    }
}
