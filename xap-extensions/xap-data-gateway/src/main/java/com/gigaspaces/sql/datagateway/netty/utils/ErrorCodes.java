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
package com.gigaspaces.sql.datagateway.netty.utils;

public class ErrorCodes {
    public static final String BAD_DATETIME_FORMAT = "22007";
    public static final String INVALID_PARAMETER_VALUE = "22023";
    public static final String PROTOCOL_VIOLATION = "08P01";
    public static final String INTERNAL_ERROR = "XX000";
    public static final String INVALID_CREDENTIALS = "28P01";
    public static final String INVALID_STATEMENT_NAME = "26000";
    public static final String SYNTAX_ERROR = "42601";
    public static final String INVALID_CURSOR_NAME = "34000";
    public static final String UNSUPPORTED_FEATURE = "0A000";
}
