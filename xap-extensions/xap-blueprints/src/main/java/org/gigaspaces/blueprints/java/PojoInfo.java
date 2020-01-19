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
package org.gigaspaces.blueprints.java;

import org.gigaspaces.blueprints.TemplateUtils;

import java.io.IOException;
import java.util.*;

public class PojoInfo {
    private final String className;
    private final String packageName;
    private final Set<String> imports = new LinkedHashSet<>();
    private final Set<String> warnings = new LinkedHashSet<>();
    private final Set<String> annotations = new LinkedHashSet<>();
    private final List<PropertyInfo> properties = new ArrayList<>();

    public PojoInfo(String className, String packageName) {
        this.className = className;
        this.packageName = packageName;
    }

    public String generate() throws IOException {
        return TemplateUtils.evaluateResource("templates/pojo.template", this);
    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    public Set<String> getImports() {
        return imports;
    }

    public Set<String> getWarnings() {
        return warnings;
    }

    public Set<String> getAnnotations() {
        return annotations;
    }

    public PojoInfo annotate(String annotation) {
        this.annotations.add(annotation);
        return this;
    }

    public List<PropertyInfo> getProperties() {
        return properties;
    }

    public PojoInfo addProperty(String name, Class<?> type) {
        addPropertyImpl(name, type);
        return this;
    }

    private PropertyInfo addPropertyImpl(String name, Class<?> type) {
        PropertyInfo propertyInfo = new PropertyInfo(name, type);
        properties.add(propertyInfo);
        Package typePackage = type.getPackage();
        if (typePackage != null && !typePackage.getName().equals("java.lang"))
            imports.add(typePackage.getName() + ".*");
        return propertyInfo;

    }

    public PojoInfo addPropertyWithAutoGenerate(String name, Class<?> type) {
        PropertyInfo propertyInfo = addPropertyImpl(name, type);
        propertyInfo.annotations.add("@SpaceId(autoGenerate=true)");
        return this;
    }

    public static class PropertyInfo {
        private final String name;
        private final String camelCaseName;
        private final Class<?> type;
        private final Set<String> annotations = new LinkedHashSet<>();

        public PropertyInfo(String name, Class<?> type) {
            this.name = name;
            this.camelCaseName = toCamelCase(name);
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getFieldName() {
            return camelCaseName;
        }

        public String getTypeName() {
            return type.getSimpleName();
        }

        public Set<String> getAnnotations() {
            return annotations;
        }

        public PropertyInfo annotate(String annotation) {
            annotations.add(annotation);
            return this;
        }
    }

    private static String toCamelCase(String s) {
        if (s.toUpperCase().equals(s))
            return s.toLowerCase();
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }
}
