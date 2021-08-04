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

package com.gigaspaces.query.sql.functions;

/**
 * Built in string sql function to concatenate any number of given string arguments.
 *
 * @author Tamir Schwarz
 * @since 11.0.0
 */
@com.gigaspaces.api.InternalApi
public class ConcatSqlFunction extends SqlFunction {
    /**
     * @param context contains the string arguments to concat.
     * @return the string that results from concatenating all the arguments contained in context.
     */
    @Override
    public Object apply(SqlFunctionExecutionContext context) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < context.getNumberOfArguments(); i++) {
            if (context.getArgument(i) != null) {
                sb.append(context.getArgument(i));
            }
        }
        return sb.toString();
    }
}
