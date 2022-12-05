/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.mongodb.bulk.BulkWriteResult;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.web.aop.annotation.ActiveStatistic;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * cloc统计持久类
 *
 * @date 2020/4/9
 * @version V1.0
 */
@Repository
public class CLOCStatisticsDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    public void upsertCLOCStatistic(CLOCStatisticEntity clocStatisticEntity)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(clocStatisticEntity.getTaskId()))
                .addCriteria(Criteria.where("tool_name").is(clocStatisticEntity.getToolName()))
                .addCriteria(Criteria.where("language").is(clocStatisticEntity.getLanguage()));

        Update update = new Update();
        update.set("task_id", clocStatisticEntity.getTaskId())
                .set("stream_name", clocStatisticEntity.getStreamName())
                .set("tool_name", clocStatisticEntity.getToolName())
                .set("sum_code", clocStatisticEntity.getSumCode())
                .set("sum_blank", clocStatisticEntity.getSumBlank())
                .set("sum_comment", clocStatisticEntity.getSumComment());
        mongoTemplate.upsert(query, update, CLOCStatisticEntity.class);
    }

    /**
     * 批量失效cloc统计数据
     * @param taskId
     */
    public void batchDisableClocStatistic(Long taskId, String toolName)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId).and("tool_name").is(toolName));
        Update update = new Update();
        update.set("sum_code", 0)
                .set("sum_blank", 0)
                .set("sum_comment", 0);
        mongoTemplate.updateMulti(query, update, CLOCStatisticEntity.class);
    }

    /**
     * 按任务ID集合 批量统计代码行总数
     *
     * @param taskIdSet 任务ID集合
     * @param langList  代码语言类型列表
     * @return taskId code
     */
    public List<CLOCStatisticEntity> batchQueryCodeLineSum(Collection<Long> taskIdSet, List<String> langList)
    {
        // 以taskId language进行过滤
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIdSet)
                .and("tool_name").is(Tool.CLOC.name())
                .and("language").in(langList));

        // 以taskId, language进行分组，并统计所有语言类型的代码行总数
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .sum("sum_code").as("sum_code");

        Aggregation agg = Aggregation.newAggregation(match, group);

        AggregationResults<CLOCStatisticEntity> queryResult =
                mongoTemplate.aggregate(agg, "t_cloc_statistic", CLOCStatisticEntity.class);
        return queryResult.getMappedResults();
    }

    public List<CLOCStatisticEntity> batchQueryClocStaticByTaskId(Collection<Long> taskIdSet) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(Tool.CLOC.name()));
        return mongoTemplate.find(query, CLOCStatisticEntity.class);
    }

    @ActiveStatistic
    public BulkWriteResult batchUpsertCLOCStatistic(Collection<CLOCStatisticEntity> clocStatisticEntity)
    {
        if (CollectionUtils.isNotEmpty(clocStatisticEntity)) {
            List<Pair<Query, Update>> upsertCondition = new ArrayList<>();
            for (CLOCStatisticEntity entity : clocStatisticEntity) {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(entity.getTaskId()))
                        .addCriteria(Criteria.where("tool_name").is(entity.getToolName()))
                        .addCriteria(Criteria.where("language").is(entity.getLanguage()))
                        .addCriteria(Criteria.where("build_id").is(entity.getBuildId()));

                Update update = new Update();
                update.set("task_id", entity.getTaskId())
                        .set("build_id", entity.getBuildId())
                        .set("stream_name", entity.getStreamName())
                        .set("tool_name", entity.getToolName())
                        .set("sum_code", entity.getSumCode())
                        .set("sum_blank", entity.getSumBlank())
                        .set("sum_comment", entity.getSumComment())
                        .set("sum_efficient_comment", entity.getSumEfficientComment())
                        .set("blank_change", entity.getBlankChange())
                        .set("code_change", entity.getCodeChange())
                        .set("comment_change", entity.getCommentChange())
                        .set("efficient_comment_change", entity.getEfficientCommentChange())
                        .set("language", entity.getLanguage())
                        .set("file_num", entity.getFileNum())
                        .set("file_num_change", entity.getFileNumChange())
                        .set("created_date", entity.getCreatedDate())
                        .set("updated_date", entity.getUpdatedDate());

                upsertCondition.add(Pair.of(query, update));
            }
            return mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CLOCStatisticEntity.class)
                    .upsert(upsertCondition)
                    .execute();
        }

        return null;
    }

    /**
     * 按任务ID批量获取最新构建ID
     * @param taskIds 任务ID集合
     * @return build id
     */
    public List<CLOCStatisticEntity> queryLastBuildIdByTaskIds(Collection<Long> taskIds) {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIds)
                .and("tool_name").is(Tool.CLOC.name()));

        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "_id");

        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("build_id").as("build_id");

        Aggregation agg = Aggregation.newAggregation(match, sort, group);
        AggregationResults<CLOCStatisticEntity> queryResult =
                mongoTemplate.aggregate(agg, "t_cloc_statistic", CLOCStatisticEntity.class);
        return queryResult.getMappedResults();
    }

    /**
     * 按任务ID和最新构建ID统计总代码数
     *
     * @param taskIds      任务ID集合
     * @param lastBuildIds 最新构建ID集合
     * @return list
     */
    public List<CLOCStatisticEntity> batchStatClocStatisticByTaskId(Collection<Long> taskIds,
            List<String> lastBuildIds) {
        MatchOperation match =
                Aggregation.match(Criteria.where("task_id").in(taskIds)
                        .and("build_id").in(lastBuildIds)
                        .and("tool_name").is(Tool.CLOC.name()));

        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .sum("sum_code").as("sum_code")
                .sum("sum_blank").as("sum_blank")
                .sum("sum_comment").as("sum_comment");

        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<CLOCStatisticEntity> queryResult =
                mongoTemplate.aggregate(agg, "t_cloc_statistic", CLOCStatisticEntity.class);
        return queryResult.getMappedResults();
    }

}
