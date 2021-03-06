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
 * Built in string sql function to replace matching subtring with other argument
 *
 * @author Alon Shoham
 * @since 15.8.0
 */
@com.gigaspaces.api.InternalApi
public class ReplaceSqlFunction extends SqlFunction {
    /**
     * @param context contains three argument of type string, CharSequence, CharSequence.
     * @return Returns the string context.getArgument(0) with all characters changed to lowercase.
     */
    @Override
    public Object apply(SqlFunctionExecutionContext context) {
        assertNumberOfArguments(3, context);
        Object arg = context.getArgument(0);
        if(arg == null){
            return null;
        }
        if (!(arg instanceof String))
            throw new RuntimeException("Replace function - wrong argument type: " + arg);
        Object subStringToReplace = context.getArgument(1);
        if (!(subStringToReplace instanceof CharSequence))
            throw new RuntimeException("Replace function - wrong subStringToReplace type: " + subStringToReplace);
        Object replaceBySubstring = context.getArgument(2);
        if (!(replaceBySubstring instanceof CharSequence))
            throw new RuntimeException("Replace function - wrong replaceBySubstring type: " + replaceBySubstring);
        return String.valueOf(arg).replace((CharSequence) subStringToReplace, (CharSequence) replaceBySubstring);
    }
}
