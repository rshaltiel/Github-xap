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
package com.gigaspaces.jdbc.calcite;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.logical.LogicalAggregate;

public class GSAggregateRule extends ConverterRule {
    static final Config DEFAULT_CONFIG = Config.INSTANCE
            .withConversion(LogicalAggregate.class, Convention.NONE,
                    GSConvention.INSTANCE, "GSAggregateRule")
            .withRuleFactory(GSAggregateRule::new);

    static final GSAggregateRule INSTANCE = new GSAggregateRule(DEFAULT_CONFIG);

    private GSAggregateRule(Config config) {
        super(config);
    }

    @Override
    public RelNode convert(RelNode rel) {
        final Aggregate agg = (Aggregate) rel;
        final RelTraitSet traitSet = rel.getCluster()
                .traitSet().replace(GSConvention.INSTANCE);
        return new GSAggregate(
                rel.getCluster(),
                traitSet,
                convert(agg.getInput(), traitSet),
                agg.getGroupSet(),
                agg.getGroupSets(),
                agg.getAggCallList());
    }
}