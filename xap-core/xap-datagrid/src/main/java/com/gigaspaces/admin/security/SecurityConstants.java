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

package com.gigaspaces.admin.security;

/**
 * Created by evgeny on 4/24/16.
 *
 * @since 12.0
 */
public interface SecurityConstants {
    String KEY_USER = "user";
    String KEY_PASSWORD = "password";
    String KEY_USER_PROVIDER = "user-details-provider";
    String KEY_USER_PROPERTIES = "user-details-properties";

    String KEY_SSL_KEY_STORE_PATH = "ssl.keyStorePath";
    String KEY_SSL_KEY_STORE_PASSWORD = "ssl.keyStorePassword";
    String KEY_SSL_KEY_MANAGER_PASSWORD = "ssl.keyManagerPassword";
    String KEY_SSL_TRUST_STORE_PASSWORD = "ssl.trustStorePassword";
    String KEY_SSL_TRUST_STORE_PATH = "ssl.trustStorePath";
    String SSL_CUSTOM_PROPERTIES = "ssl.custom.properties";

    String PARAM_PREFIX = "-";

    String KEY_USER_FULL_PARAM = PARAM_PREFIX + KEY_USER;
    String KEY_PASSWORD_FULL_PARAM = PARAM_PREFIX + KEY_PASSWORD;
    String KEY_USER_PROVIDER_FULL_PARAM = PARAM_PREFIX + KEY_USER_PROVIDER;
    String KEY_USER_PROPERTIES_FULL_PARAM = PARAM_PREFIX + KEY_USER_PROPERTIES;
}
