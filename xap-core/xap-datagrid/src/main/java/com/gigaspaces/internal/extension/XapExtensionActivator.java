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

package com.gigaspaces.internal.extension;

import com.gigaspaces.logger.LogUtils;
import org.jini.rio.boot.LoggableClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@com.gigaspaces.api.InternalApi
public class XapExtensionActivator {

    private static final Logger logger = LoggerFactory.getLogger(XapExtensionActivator.class.getName());

    public static synchronized void scanAndActivate(ClassLoader classLoader, String key) {
        if (logger.isDebugEnabled())
            logger.debug("Scanning for " + key + " in class loader " + toString(classLoader));
        final long startTime = System.currentTimeMillis();
        final Map<URL, Properties> xapProperties = loadManifestsEntryAttributes(classLoader, "xap");
        final Set<String> extensions = loadSet(xapProperties, key, ",");
        activate(classLoader, extensions);
        if (logger.isDebugEnabled())
            logger.debug(LogUtils.formatDuration(startTime, "Finished scanning for extensions in class loader " + toString(classLoader)));
    }

    private static void activate(ClassLoader classLoader, Set<String> extensions) {
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            if (logger.isDebugEnabled())
                logger.debug("Activating " + extensions.size() + " extensions...");
            for (String extensionName : extensions) {
                activate(classLoader, extensionName);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private static void activate(ClassLoader classLoader, String extensionName) {
        if (logger.isDebugEnabled())
            logger.debug("Activating extension " + extensionName);
        try {
            final Class<?> extensionClass = classLoader.loadClass(extensionName);
            final Object instance = extensionClass.newInstance();
            XapExtension extension = (XapExtension) instance;
            extension.activate();
            if (logger.isDebugEnabled())
                logger.debug("Activated extension " + extensionName);
        } catch (NoClassDefFoundError e) {
            logger.warn("Failed to activate extension " + extensionName + " - " + e);
            throw new RuntimeException("Failed to activate extension " + extensionName, e);
        } catch (ClassNotFoundException e) {
            logger.warn("Failed to activate extension " + extensionName + " - " + e);
            throw new RuntimeException("Failed to activate extension " + extensionName, e);
        } catch (InstantiationException e) {
            logger.warn("Failed to activate extension " + extensionName + " - " + e);
            throw new RuntimeException("Failed to activate extension " + extensionName, e);
        } catch (IllegalAccessException e) {
            logger.warn("Failed to activate extension " + extensionName + " - " + e);
            throw new RuntimeException("Failed to activate extension " + extensionName, e);
        }
    }

    private static Set<String> loadSet(Map<URL, Properties> xapProperties, String name, String separator) {
        Set<String> result = new HashSet<String>();
        for (Properties properties : xapProperties.values()) {
            final String value = properties.getProperty(name);
            if (value != null) {
                String[] tokens = value.split(separator);
                for (String token : tokens) {
                    result.add(token.trim());
                }
            }
        }
        return result;
    }

    private static Map<URL, Properties> loadManifestsEntryAttributes(ClassLoader classLoader, String entryName) {
        try {
            Map<URL, Properties> result = new HashMap<URL, Properties>();
            final Enumeration<URL> resources = classLoader.getResources(JarFile.MANIFEST_NAME);
            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                Properties xapProperties = loadManifestEntryAttributes(url, entryName);
                if (xapProperties != null) {
                    result.put(url, xapProperties);
                }
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load manifest information", e);
        }
    }

    private static Properties loadManifestEntryAttributes(URL url, String entryName) throws IOException {
        Properties result = null;
        InputStream inputStream = null;
        try {
            if (logger.isTraceEnabled())
                logger.trace("Loading manifest from " + url);
            inputStream = url.openStream();
            Manifest manifest = new Manifest(inputStream);
            final Attributes attributes = manifest.getAttributes(entryName);
            if (attributes != null) {
                result = new Properties();
                if (logger.isDebugEnabled())
                    logger.debug("Found " + attributes.size() + " attributes for entry '" + entryName + "'");
                for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
                    result.setProperty(entry.getKey().toString(), entry.getValue().toString());
                    if (logger.isTraceEnabled())
                        logger.trace("Found entry " + entry.getKey() + " => " + entry.getValue());
                }
            }
        } catch(FileNotFoundException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("File returned by classloader resource does not exist anymore", e);
            }
        } finally {
            if (inputStream != null)
                inputStream.close();
        }
        return result;
    }

    private static String toString(ClassLoader classLoader) {
        return classLoader instanceof LoggableClassLoader
                ? ((LoggableClassLoader) classLoader).getLogName()
                : classLoader.toString();
    }
}
