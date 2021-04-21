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
package com.gigaspaces.internal.query.explainplan;

import com.gigaspaces.internal.query.explainplan.model.ExplainPlanInfo;
import com.gigaspaces.internal.query.explainplan.model.IndexChoiceDetail;
import com.gigaspaces.internal.query.explainplan.model.IndexInfoDetail;
import com.gigaspaces.internal.query.explainplan.model.PartitionIndexInspectionDetail;
import com.j_spaces.core.client.SQLQuery;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ExplainPlan for the JDBC Driver V3
 * @author Mishel Liberman
 * @since 16.0
 */
public class ExplainPlanV3 extends ExplainPlanImpl {

    private final String tableName;
    private final String tableAlias;
    private final Map<String, String> visibleColumnsAndAliasMap;

    public ExplainPlanV3(String tableName, String tableAlias, Map<String, String> visibleColumnsAndAliasMap) {
        super(null);
        this.tableName = tableName;
        this.tableAlias = tableAlias;
        this.visibleColumnsAndAliasMap = visibleColumnsAndAliasMap;
    }


    @Override
    public String toString() {
        return getExplainPlanInfo().toString();
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableAlias() {
        return tableAlias;
    }

    public Map<String, String> getVisibleColumnsAndAliasMap() {
        return visibleColumnsAndAliasMap;
    }

    /**
     * @return JSON structured plan
     */
    public ExplainPlanInfo getExplainPlanInfo() {
        ExplainPlanInfo planInfo = new ExplainPlanInfo(this);
        if (!plans.isEmpty()) {
            appendScanDetails(planInfo);
        }
        return planInfo;
    }

    /**
     * Fill the planInfo with the criteria and the index inspections
     */
    private void appendScanDetails(ExplainPlanInfo planInfo) {
        indexInfoDescCache.clear();
        String queryFilterTree = getQueryFilterTree(plans.values().iterator().next().getRoot());
        planInfo.setCriteria(queryFilterTree);
        for (Map.Entry<String, SingleExplainPlan> entry : plans.entrySet()) {
            planInfo.addIndexInspection(getPartitionPlan(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * Gets the criteria from the QueryOperationNode root
     */
    private String getQueryFilterTree(QueryOperationNode root) {
        return root == null ? null : root.getPrettifiedString();
    }

    /**
     * Return index choices of single partition wrapped by IndexInspectionDetail
     */
    private PartitionIndexInspectionDetail getPartitionPlan(String partitionId, SingleExplainPlan singleExplainPlan) {
        final PartitionIndexInspectionDetail indexInspection = new PartitionIndexInspectionDetail();
        final Map<String, List<IndexChoiceNode>> indexesInfo = singleExplainPlan.getIndexesInfo();
        indexInspection.setUsedTiers(singleExplainPlan.getTiersInfo().values().stream().flatMap(List::stream).collect(Collectors.toList()));
        indexInspection.setPartition(partitionId);

        if (indexesInfo.size() == 1) {
            Map.Entry<String, List<IndexChoiceNode>> entry = indexesInfo.entrySet().iterator().next();
            List<IndexChoiceNode> indexChoices = entry.getValue();
            List<IndexChoiceDetail> indexInspections = getIndexInspectionPerTableType(indexChoices);
            indexInspection.setIndexes(indexInspections);
        } else if (indexesInfo.size() != 0) {
            throw new UnsupportedOperationException("Not supported with more than one type");
        }
        return indexInspection;
    }

    /**
     * Return index choices of single partition
     */
    private List<IndexChoiceDetail> getIndexInspectionPerTableType(List<IndexChoiceNode> indexChoices) {
        List<IndexChoiceDetail> indexChoiceDetailList = new ArrayList<>();
        for (IndexChoiceNode node : indexChoices) {
            final List<IndexInfoDetail> selected = getSelectedIndexesDescription(node.getChosen());
            final List<IndexInfoDetail> inspected = getInspectedIndexesDescription(node.getOptions());
            boolean isUnion = node.getChosen() instanceof UnionIndexInfo;
            final IndexChoiceDetail indexChoiceDetail = new IndexChoiceDetail(node.getName(), isUnion, inspected, selected);
            indexChoiceDetailList.add(indexChoiceDetail);
        }
        return indexChoiceDetailList;
    }

    /**
     * Gets Single index choice detail of the inspected indexes
     */
    private List<IndexInfoDetail> getInspectedIndexesDescription(List<IndexInfo> options) {
        final List<IndexInfoDetail> indexInfoDetails = new ArrayList<>();
        for (int i = options.size() - 1; i >= 0; i--) {
            final IndexInfo option = options.get(i);
            final IndexInfoDetail infoFormat = new IndexInfoDetail(getOptionDesc(option), option);
            indexInfoDetails.add(infoFormat);
        }
        return indexInfoDetails;
    }

    /**
     * Gets Single index choice detail of the selected indexes
     * Might return an array in case of a union choice
     */
    private List<IndexInfoDetail> getSelectedIndexesDescription(IndexInfo indexInfo) {
        final List<IndexInfoDetail> indexInfoDetails = new ArrayList<>();
        if (indexInfo == null) return indexInfoDetails;
        if (indexInfo instanceof UnionIndexInfo) {
            final List<IndexInfo> options = ((UnionIndexInfo) indexInfo).getOptions();
            if (options.size() == 0)
                return null;

            for (int i = options.size() - 1; i >= 0; i--) {
                final IndexInfo option = options.get(i);
                final IndexInfoDetail infoFormat = new IndexInfoDetail(getOptionDesc(option), option);
                indexInfoDetails.add(infoFormat);
            }
            return indexInfoDetails;
        }
        final IndexInfoDetail infoFormat = new IndexInfoDetail(getOptionDesc(indexInfo), indexInfo);
        return Collections.singletonList(infoFormat);
    }
}
