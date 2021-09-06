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
package com.gigaspaces.jdbc.calcite;

import com.gigaspaces.jdbc.calcite.pg.PgTypeDescriptor;
import com.gigaspaces.jdbc.calcite.pg.PgTypeUtils;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.Table;

import java.util.Collections;
import java.util.Set;

public class GSRootSchema extends GSAbstractSchema {
    @Override
    public Table getTable(String name) {
        return null;
    }

    @Override
    public Set<String> getTableNames() {
        return Collections.emptySet();
    }

    @Override
    public RelProtoDataType getType(String name) {
        PgTypeDescriptor type = PgTypeUtils.getTypeByName(name);
        if (type == PgTypeDescriptor.UNKNOWN)
            return null;
        return PgTypeUtils.toRelProtoDataType(type);
    }

    @Override
    public Set<String> getTypeNames() {
        return PgTypeUtils.getTypeNames();
    }
}
