/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
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
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 圈复杂度持久代码
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Repository
public class CCNDefectDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    public List<CCNDefectEntity> findByTaskIdAndStatus(long taskId, int status) {
        return findByTaskIdAndAuthorAndStatusAndRelPaths(taskId, null, status, null);
    }

    public List<CCNDefectEntity> findByTaskIdAndAuthorAndRelPaths(long taskId, String author, Set<String> fileList)
    {
        return findByTaskIdAndAuthorAndStatusAndRelPaths(taskId, author, null, fileList);
    }

    /**
     * 根据任务ID，作者和路径列表查询
     *
     * @param taskId
     * @param author
     * @param status
     * @param fileList
     * @return
     */
    public List<CCNDefectEntity> findByTaskIdAndAuthorAndStatusAndRelPaths(long taskId, String author, Integer status, Set<String> fileList)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));

        //作者过滤
        if (StringUtils.isNotEmpty(author))
        {
            query.addCriteria(Criteria.where("author").is(author));
        }

        //路径过滤
        List<Criteria> criteriaList = new ArrayList<>();
        List<Criteria> orCriteriaList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fileList))
        {
            fileList.forEach(file ->
                    criteriaList.add(Criteria.where("rel_path").regex(file))
            );
            orCriteriaList.add(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
            query.addCriteria(new Criteria().andOperator(orCriteriaList.toArray(new Criteria[0])));
        }

        // 状态过滤
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        //查询总的数量，并且过滤计数
        return mongoTemplate.find(query, CCNDefectEntity.class);
    }

    /**
     * 根据工具ID和工具列表查询最近一次分析记录
     *
     * @param taskId
     * @param toolSet
     * @return
     */
    public List<CCNStatisticEntity> findFirstByTaskIdOrderByTime(long taskId, Set<String> toolSet)
    {
        //以taskid进行过滤
        MatchOperation match = Aggregation.match(Criteria.where("task_id").is(taskId).and("tool_name").in(toolSet));
        //根据开始时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "time");
        //以toolName进行分组，并且取第一个的endTime字段
        GroupOperation group = Aggregation.group("tool_name")
                .first("task_id").as("task_id")
                .first("tool_name").as("tool_name")
                .first("analysis_version").as("analysis_version")
                .first("time").as("time")
                .first("defect_count").as("defect_count")
                .first("defect_change").as("defect_change")
                .first("last_defect_count").as("last_defect_count")
                .first("average_ccn").as("average_ccn")
                .first("last_average_ccn").as("last_average_ccn")
                .first("average_ccn_change").as("average_ccn_change")
                .first("super_high_count").as("super_high_count")
                .first("high_count").as("high_count")
                .first("medium_count").as("medium_count")
                .first("low_count").as("low_count");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);

        AggregationResults<CCNStatisticEntity> queryResult = mongoTemplate.aggregate(agg, "t_ccn_statistic", CCNStatisticEntity.class);
        return queryResult.getMappedResults();
    }

    public void batchMarkDefect(long taskId, List<CCNDefectEntity> defectList, Integer markFlag)
    {
        if (CollectionUtils.isNotEmpty(defectList))
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CCNDefectEntity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity ->
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("mark", markFlag);
                update.set("mark_time", currTime);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    public void batchUpdateDefectAuthor(long taskId, List<CCNDefectEntity> defectList, String newAuthor)
    {
        if (CollectionUtils.isNotEmpty(defectList))
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CCNDefectEntity.class);
            defectList.forEach(defectEntity ->
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("author", newAuthor);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量获取最新分析统计数据
     *
     * @param taskIdSet 任务ID集合
     * @param toolName  工具名称
     * @return list
     */
    public List<CCNStatisticEntity> batchFindByTaskIdInAndTool(Collection<Long> taskIdSet, String toolName)
    {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName));
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "time");
        // 以taskId进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("tool_name").as("tool_name")
                .first("time").as("time")
                .first("average_ccn").as("average_ccn")
                .first("last_average_ccn").as("last_average_ccn")
                .first("average_ccn_change").as("average_ccn_change")
                .first("super_high_count").as("super_high_count")
                .first("high_count").as("high_count")
                .first("medium_count").as("medium_count");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);

        AggregationResults<CCNStatisticEntity> queryResult =
                mongoTemplate.aggregate(agg, "t_ccn_statistic", CCNStatisticEntity.class);
        return queryResult.getMappedResults();
    }
}
