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

import com.gigaspaces.sql.datagateway.netty.utils.TypeUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

public class ParametersDescription {
    public static ParametersDescription EMPTY = new ParametersDescription(Collections.emptyList());

    private final List<ParameterDescription> parameters;

    public ParametersDescription(List<ParameterDescription> parameters) {
        this.parameters = parameters;
    }

    public ParametersDescription(int[] types) {
        this.parameters = Arrays.stream(types)
                .mapToObj(TypeUtils::getType)
                .map(ParameterDescription::new)
                .collect(Collectors.toList());
    }

    public int getParametersCount() {
        return parameters.size();
    }
    public List<ParameterDescription> getParameters() {
        return unmodifiableList(parameters);
    }
}
