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

import com.gigaspaces.dih.consumer.*;
import com.gigaspaces.dih.consumer.cr8.CR8MessageConverter;
import com.gigaspaces.dih.consumer.web.GSConsumerHandler;
import com.gigaspaces.dih.type_converter.ConversionMap;
import com.gigaspaces.internal.client.spaceproxy.ISpaceProxy;
import org.openspaces.core.GigaSpace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.annotation.Resource;
import java.io.IOException;

/**
 * This class was auto-generated by GigaSpaces
 */
@Configuration
public class ServiceConfig {
    @Resource
    private GigaSpace gigaspace;

    private String pipelineName;
    private String spaceName;
    private String webPort;
    private String kafkaBootstrapServers;
    private String kafkaTopic;
    private String kafkaMessageCommandClass;
    private String kafkaMessageValidateClass;

    public ServiceConfig() {
    }

    public ServiceConfig(String pipelineName, String spaceName, String webPort, String kafkaBootstrapServers, String kafkaTopic, String kafkaMessageCommandClass, String kafkaMessageValidateClass) {
        this.pipelineName = pipelineName;
        this.spaceName = spaceName;
        this.webPort = webPort;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.kafkaTopic = kafkaTopic;
        this.kafkaMessageCommandClass = kafkaMessageCommandClass;
        this.kafkaMessageValidateClass = kafkaMessageValidateClass;
    }

    @Bean(name = "GSConsumerHandler")
    public GSConsumerHandler myBean() throws IOException {
        return new GSConsumerHandler(pipelineName, Integer.parseInt(webPort), initRunnable());
    }

    public GSKafkaConsumerThread initRunnable() throws IOException {
        return new GSKafkaConsumerThread(pipelineName, spaceName, kafkaBootstrapServers, kafkaTopic,
                kafkaMessageCommandClass, kafkaMessageValidateClass, initConverter(), initCleanser(), initAuditor());
    }

    private GSAuditor initAuditor() {
        return new DefaultAuditor();
    }

    private GSMessageConverter initConverter() throws IOException {
        return new CR8MessageConverter(new ConversionMap());
    }


    public GSMessageCleanser initCleanser() {
        return new DefaultMessageCleanser((ISpaceProxy) gigaspace.getSpace());
    }
}
