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
package com.gigaspaces.sql.datagateway.netty.query;

public class StatementDescription {
    public static final StatementDescription EMPTY =
            new StatementDescription(ParametersDescription.EMPTY, RowDescription.EMPTY);

    private final ParametersDescription parametersDescription;
    private final RowDescription rowDescription;

    public StatementDescription(ParametersDescription parametersDescription, RowDescription rowDescription) {
        this.parametersDescription = parametersDescription;
        this.rowDescription = rowDescription;
    }

    public ParametersDescription getParametersDescription() {
        return this.parametersDescription;
    }

    public RowDescription getRowDescription() {
        return this.rowDescription;
    }
}
