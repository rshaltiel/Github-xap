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

package com.gigaspaces.query.sql.functions.extended;

import com.gigaspaces.query.sql.functions.SqlFunction;
import com.gigaspaces.query.sql.functions.SqlFunctionExecutionContext;

/**
 * Built in mathematical sql function to perform log10 operation.
 *
 * @author Mishel Liberman
 * @since 16.0
 */
@com.gigaspaces.api.InternalApi
public class Log10SqlFunction extends SqlFunction {

    /**
     * @param context contains single argument of Number.
     * @return the result of log10 of context.getArgument(0).
     */
    @Override
    public Object apply(SqlFunctionExecutionContext context) {
        assertNumberOfArguments(1, context);

        Object logNumber = context.getArgument(0);
        if(logNumber == null){
            return null;
        }
        if (!(logNumber instanceof Number)) {
            throw new RuntimeException("Log10 function - wrong arguments types, arguments should be Number. First argument:[" + logNumber + "]");
        }
        return Math.log10(((Number) logNumber).doubleValue());
    }
}
