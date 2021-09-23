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

package org.openspaces.launcher;

import com.gigaspaces.admin.security.SecurityConstants;
import com.gigaspaces.internal.utils.GsEnv;
import com.gigaspaces.start.SystemLocations;

import java.io.*;
import java.util.Properties;

/**
 * @author Niv Ingberg
 * @since 10.0.0
 */
public class WebLauncherConfig {

    private final static String PORT = "port";
    private final static String BIND_ADDRESS = "bind-address";
    private final static int DEFAULT_PORT = 8099;

    private final String webuiHome;
    private final String name;
    private final String loggerName;
    private final int port;
    /**
     * @since 10.1 GS-12102
     */
    private final String hostAddress;
    private String warFilePath;
    private final String tempDirPath;

    /**
    * @since 12.1
    */
    private final String sslKeyStorePath;
    /**
     * @since 12.1
     */
    private final String sslKeyStorePassword;
    /**
     * @since 12.1
     */
    private String sslKeyManagerPassword;
    /**
     * @since 12.1
     */
    private final String sslTrustStorePassword;
    /**
     * @since 12.1
     */
    private final String sslTrustStorePath;

    private final boolean sslEnabled;

    public WebLauncherConfig(Properties props) throws IOException {
        this.webuiHome = System.getProperty("com.gigaspaces.webui.path", SystemLocations.singleton().tools("gs-webui").toString());
        this.name = props.getProperty("name", System.getProperty("org.openspaces.launcher.name", "GS Web UI"));
        this.loggerName = props.getProperty("logger", System.getProperty("org.openspaces.launcher.logger", "org.openspaces.launcher"));
        this.port = Integer.parseInt( props.getProperty( PORT, GsEnv.property("org.openspaces.launcher.port", "WEBUI_PORT").get( String.valueOf( DEFAULT_PORT ) ) ) );
        this.hostAddress = props.getProperty( BIND_ADDRESS, GsEnv.property("org.openspaces.launcher.bind-address", "BIND_ADDRESS").get("0.0.0.0") );
        this.warFilePath = props.getProperty("path", System.getProperty("org.openspaces.launcher.path", webuiHome));
        this.tempDirPath = props.getProperty("work", System.getProperty("org.openspaces.launcher.work", webuiHome + "/work"));

        if (props.containsKey(SecurityConstants.KEY_USER_PROVIDER))
            System.setProperty(SecurityConstants.KEY_USER_PROVIDER, props.getProperty(SecurityConstants.KEY_USER_PROVIDER));
        if (props.containsKey(SecurityConstants.KEY_USER_PROPERTIES))
            System.setProperty(SecurityConstants.KEY_USER_PROPERTIES, props.getProperty(SecurityConstants.KEY_USER_PROPERTIES));

        String sslCustomPropertiesFile = props.getProperty(SecurityConstants.SSL_CUSTOM_PROPERTIES);
        if( sslCustomPropertiesFile != null ){
            Properties sslProperties = new Properties();
            sslProperties.load( new FileInputStream( sslCustomPropertiesFile ) );
            props.putAll(sslProperties);
        }

        this.sslKeyManagerPassword = props.getProperty(SecurityConstants.KEY_SSL_KEY_MANAGER_PASSWORD);
        this.sslKeyStorePassword = props.getProperty(SecurityConstants.KEY_SSL_KEY_STORE_PASSWORD);
        this.sslKeyStorePath = props.getProperty(SecurityConstants.KEY_SSL_KEY_STORE_PATH);
        this.sslTrustStorePath = props.getProperty(SecurityConstants.KEY_SSL_TRUST_STORE_PATH);
        this.sslTrustStorePassword = props.getProperty(SecurityConstants.KEY_SSL_TRUST_STORE_PASSWORD);

        if( sslKeyManagerPassword == null && sslKeyStorePassword != null ){
            sslKeyManagerPassword = sslKeyStorePassword;
        }

        this.sslEnabled =
                sslKeyStorePassword != null ||
                sslKeyStorePath != null ||
                sslTrustStorePath != null ||
                sslTrustStorePassword != null;
    }

    public int getPort() {
        return port;
    }

    public String getTempDirPath() {
        return tempDirPath;
    }

    public String getWarFilePath() {
        return warFilePath;
    }

    public void setWarFilePath(String warFilePath) {
        this.warFilePath = warFilePath;
    }

    /**
     * `   `* @since 10.1
     *
     * @author evgenyf
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * @since 12.1
     */
     public String getSslKeyStorePath() {
        return sslKeyStorePath;
    }

    /**
     * @since 12.1
     */
    public String getSslKeyStorePassword() {
        return sslKeyStorePassword;
    }

    /**
     * @since 12.1
     */
    public String getSslKeyManagerPassword() {
        return sslKeyManagerPassword;
    }

    /**
     * @since 12.1
     */
    public String getSslTrustStorePassword() {
        return sslTrustStorePassword;
    }

    /**
     * @since 12.1
     */
    public String getSslTrustStorePath() {
        return sslTrustStorePath;
    }

    /**
     * @since 12.1
     */
    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public String getName() {
        return name;
    }

    public String getLoggerName() {
        return loggerName;
    }
}
