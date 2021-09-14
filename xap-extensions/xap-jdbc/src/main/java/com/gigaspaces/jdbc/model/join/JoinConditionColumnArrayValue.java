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
package com.gigaspaces.jdbc.model.join;

import com.gigaspaces.jdbc.model.table.IQueryColumn;

import java.util.function.Function;

public class JoinConditionColumnArrayValue implements JoinCondition {
    private final IQueryColumn column;
    private final Function<IQueryColumn, Object> function;

    public JoinConditionColumnArrayValue(IQueryColumn column, Function<IQueryColumn, Object> function) {
        this.column = column;
        this.function = function;
    }

    @Override
    public Object getValue() {
        return function.apply(column);
    }

    @Override
    public boolean isOperator() {
        return false;
    }

    public IQueryColumn getColumn() {
        return this.column;
    }
}
