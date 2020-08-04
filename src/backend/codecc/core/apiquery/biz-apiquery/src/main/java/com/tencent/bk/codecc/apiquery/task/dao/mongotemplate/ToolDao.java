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

package com.tencent.bk.codecc.apiquery.task.dao.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel;
import com.tencent.devops.common.api.pojo.Page;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工具配置持久层
 *
 * @version V2.0
 * @date 2020/4/26
 */
@Repository
public class ToolDao
{
    @Autowired
    @Qualifier("taskMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 分页获取工具配置信息(platform)
     *
     * @param toolName   工具名
     * @param platformIp Platform IP
     * @param taskId     任务ID
     * @param pageable   分页器
     * @return list
     */
    public Page<ToolConfigInfoModel> queryToolPlatformInfoPage(String toolName, String platformIp, Long taskId,
            Pageable pageable)
    {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        // 可选查询条件
        if (taskId != null && taskId != 0)
        {
            criteriaList.add(Criteria.where("task_id").is(taskId));
        }
        if (StringUtils.isNotEmpty(toolName))
        {
            criteriaList.add(Criteria.where("tool_name").is(toolName));
        }
        if (StringUtils.isNotEmpty(platformIp))
        {
            criteriaList.add(Criteria.where("platform_ip").is(platformIp));
        }

        if (CollectionUtils.isNotEmpty(criteriaList))
        {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // 获取满足条件的总数
        long totalCount = mongoTemplate.count(new Query(criteria), "t_tool_config");

        // 分页排序
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        SortOperation sort = Aggregation.sort(pageable.getSort());
        SkipOperation skip = Aggregation.skip(Long.valueOf(pageNumber * pageSize));
        LimitOperation limit = Aggregation.limit(pageSize);

        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria), sort, skip, limit);
        AggregationResults<ToolConfigInfoModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_tool_config", ToolConfigInfoModel.class);

        // 计算总页数
        int totalPageNum = 0;
        if (totalCount > 0)
        {
            totalPageNum = (Integer.parseInt(String.valueOf(totalCount)) + pageSize - 1) / pageSize;
        }

        // 页码加1返回
        return new Page<>(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults.getMappedResults());
    }


    /**
     * 按条件查询单个工具配置信息
     *
     * @param taskId   任务ID
     * @param toolName 工具名
     * @return model
     */
    public ToolConfigInfoModel findByTaskIdAndTool(Long taskId, String toolName)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId).and("tool_name").is(toolName));

        return mongoTemplate.findOne(query, ToolConfigInfoModel.class, "t_tool_config");
    }


    /**
     * 获取指定跟进状态的任务ID
     *
     * @param toolName     工具名
     * @param followStatus 跟进状态
     * @param isNot        是否取反查询(跟进状态)
     * @return taskIds
     */
    public List<Long> findTaskIdByToolAndStatus(String toolName, Integer followStatus, boolean isNot)
    {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();
        if (StringUtils.isNotEmpty(toolName))
        {
            criteriaList.add(Criteria.where("tool_name").is(toolName));
        }

        if (followStatus != null)
        {
            if (isNot)
            {
                criteriaList.add(Criteria.where("follow_status").ne(followStatus));
            }
            else
            {
                criteriaList.add(Criteria.where("follow_status").is(followStatus));
            }
        }
        if (CollectionUtils.isNotEmpty(criteriaList))
        {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        ProjectionOperation project = Aggregation.project("task_id");

        // 允许磁盘操作(支持较大数据集合的处理)
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(Aggregation.match(criteria), project).withOptions(options);


        List<ToolConfigInfoModel> configModels =
                mongoTemplate.aggregate(agg, "t_tool_config", ToolConfigInfoModel.class).getMappedResults();

        return configModels.stream().map(ToolConfigInfoModel::getTaskId).collect(Collectors.toList());
    }


    /**
     * 多条件查询工具配置信息
     *
     * @param taskIds      任务ID集合
     * @param toolName     工具名
     * @param followStatus 跟进状态
     * @param isNot        是否取反查询(跟进状态)
     * @return list
     */
    public List<ToolConfigInfoModel> findByToolAndFollowStatus(Set<Long> taskIds, String toolName, Integer followStatus,
            boolean isNot)
    {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        // 可选查询条件
        if (CollectionUtils.isNotEmpty(taskIds))
        {
            criteriaList.add(Criteria.where("task_id").in(taskIds));
        }
        if (StringUtils.isNotEmpty(toolName))
        {
            criteriaList.add(Criteria.where("tool_name").is(toolName));
        }
        if (followStatus != null)
        {
            if (isNot)
            {
                criteriaList.add(Criteria.where("follow_status").ne(followStatus));
            }
            else
            {
                criteriaList.add(Criteria.where("follow_status").is(followStatus));
            }
        }

        if (CollectionUtils.isNotEmpty(criteriaList))
        {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // 允许磁盘操作(支持较大数据集合的处理)
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();

        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria)).withOptions(options);
        AggregationResults<ToolConfigInfoModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_tool_config", ToolConfigInfoModel.class);

        return queryResults.getMappedResults();
    }

}
