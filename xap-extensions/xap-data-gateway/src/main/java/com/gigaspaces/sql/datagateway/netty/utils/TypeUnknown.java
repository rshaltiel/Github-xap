package com.gigaspaces.sql.datagateway.netty.utils;

import com.gigaspaces.jdbc.calcite.pg.PgTypeDescriptor;

public class TypeUnknown extends PgType {
    public static final PgType INSTANCE = new TypeUnknown();

    public TypeUnknown() {
        super(PgTypeDescriptor.UNKNOWN);
    }
}
