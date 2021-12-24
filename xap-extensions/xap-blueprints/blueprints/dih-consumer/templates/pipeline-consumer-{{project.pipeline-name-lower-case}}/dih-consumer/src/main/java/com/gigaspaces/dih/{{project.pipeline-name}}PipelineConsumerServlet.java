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
package com.gigaspaces.dih;

import com.gigaspaces.dih.consumer.web.PipelineConsumerServlet;
import com.gigaspaces.dih.model.PipelineTypeRegistrar;
import com.gigaspaces.internal.client.spaceproxy.ISpaceProxy;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import javax.servlet.ServletContext;
import org.openspaces.core.cluster.ClusterInfo;

/**
 * This class was auto-generated by GigaSpaces
 */
public class {{project.pipeline-name}}PipelineConsumerServlet extends PipelineConsumerServlet {

    private static final long serialVersionUID = 8216313915439061676L;

    @Override
    public void registerTypes(ISpaceProxy spaceProxy) {
        PipelineTypeRegistrar.registerTypes(new GigaSpaceConfigurer(spaceProxy).create());
    }

    @Override
    public String getPuName() {
        return ((ClusterInfo) getServletContext().getAttribute("clusterInfo")).getName();
    }

    @Override
    protected ISpaceProxy getSpace(ServletContext servletContext) {
        GigaSpace gigaSpace = (GigaSpace) servletContext.getAttribute("gigaSpace");
        return (ISpaceProxy) gigaSpace.getSpace();
    }
}