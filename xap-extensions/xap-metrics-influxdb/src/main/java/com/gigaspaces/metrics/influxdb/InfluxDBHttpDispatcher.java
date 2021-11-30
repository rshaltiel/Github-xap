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

package com.gigaspaces.metrics.influxdb;

import com.gigaspaces.internal.utils.StringUtils;
import com.gigaspaces.metrics.HttpUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Niv Ingberg
 * @since 10.2.1
 */
public class InfluxDBHttpDispatcher extends InfluxDBDispatcher {
    private static final Logger logger = Logger.getLogger(InfluxDBHttpDispatcher.class.getName());
    private static final String CONTENT_TYPE = System.getProperty("com.gigaspaces.metrics.influxdb.http.content_type", "text/plain");
    private static final int TIMEOUT = Integer.getInteger("com.gigaspaces.metrics.influxdb.http.timeout", 30000);
    private final URL url;

    public InfluxDBHttpDispatcher(InfluxDBReporterFactory factory) {
        this.url = toUrl(factory);
        if (logger.isLoggable(Level.CONFIG))
            logger.log(Level.CONFIG, "InfluxDBHttpDispatcher created [url=" + url + "]");
    }

    public URL getUrl() {
        return url;
    }

    @Override
    protected void doSend(String content) throws IOException {
        int httpCode = HttpUtils.post(url, content, CONTENT_TYPE, TIMEOUT);
        if (httpCode != 204)
            throw new IOException("Failed to post [HTTP Code=" + httpCode + ", url=" + url.toString() + "]");
    }

    private static URL toUrl(InfluxDBReporterFactory factory) {
        // See https://influxdb.com/docs/v0.9/guides/writing_data.html
        // "http://localhost:8086/write?db=db1");
        try {
            if (!StringUtils.hasLength(factory.getHost()))
                throw new IllegalArgumentException("Mandatory property not provided - host");
            if (!StringUtils.hasLength(factory.getDatabase()))
                throw new IllegalArgumentException("Mandatory property not provided - database");
            String suffix = "/write?db=" + factory.getDatabase();
            suffix = append(suffix, "rp", factory.getRetentionPolicy());
            suffix = append(suffix, "u", factory.getUsername());
            suffix = append(suffix, "p", factory.getPassword());
            suffix = append(suffix, "precision", toString(factory.getTimePrecision()));
            suffix = append(suffix, "consistency", factory.getConsistency());
            return new URL(factory.getProtocol(), factory.getHost(), factory.getPort(), suffix);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to create InfluxDB HTTP url", e);
        }
    }

    private static String toString(TimeUnit timeUnit) {
        // https://influxdb.com/docs/v0.9/write_protocols/write_syntax.html#http
        if (timeUnit == null)
            return null;
        if (timeUnit == TimeUnit.NANOSECONDS)
            return "n";
        if (timeUnit == TimeUnit.MICROSECONDS)
            return "u";
        if (timeUnit == TimeUnit.MILLISECONDS)
            return "ms";
        if (timeUnit == TimeUnit.SECONDS)
            return "s";
        if (timeUnit == TimeUnit.MINUTES)
            return "m";
        if (timeUnit == TimeUnit.HOURS)
            return "h";
        throw new IllegalArgumentException("Unsupported time precision: " + timeUnit);
    }

    private static String append(String prefix, String name, String value) {
        return StringUtils.hasLength(value) ? prefix + '&' + name + '=' + value : prefix;
    }
}
