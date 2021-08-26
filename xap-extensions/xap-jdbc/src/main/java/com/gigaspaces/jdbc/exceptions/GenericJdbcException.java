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
package com.gigaspaces.jdbc.exceptions;


import java.sql.SQLException;

public abstract class GenericJdbcException extends RuntimeException {
    static final long serialVersionUID = 3118109450412077316L;
    public GenericJdbcException(String message, Throwable cause) {
        super(message, cause);
    }

    public GenericJdbcException(String message) {
        super(message);
    }

    public GenericJdbcException(SQLException e) {
        super(e);
    }
}
