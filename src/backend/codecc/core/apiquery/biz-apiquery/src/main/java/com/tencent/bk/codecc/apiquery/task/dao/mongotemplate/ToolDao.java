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
import com.tencent.bk.codecc.apiquery.defect.model.DefectStatModel;
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel;
import com.tencent.bk.codecc.apiquery.task.model.ToolsRegisterStatisticsModel;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.FOLLOW_STATUS;
import com.tencent.devops.common.util.DateTimeUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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
     * @param taskIds      任务ID集合
     * @param tools        工具名
     * @param followStatus 跟进状态
     * @param isNot        是否取反查询(跟进状态)
     * @return taskIds
     */
    public List<Long> findTaskIdByToolAndStatus(List<Long> taskIds, Collection<String> tools, Integer followStatus,
            boolean isNot) {
        // 组装基本筛选条件
        Criteria criteria = getCriteriaByCondition(taskIds, tools, followStatus, isNot);

        GroupOperation group = Aggregation.group("task_id").first("task_id").as("task_id");

        // 允许磁盘操作(支持较大数据集合的处理)
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(Aggregation.match(criteria), group).withOptions(options);


        List<ToolConfigInfoModel> configModels =
                mongoTemplate.aggregate(agg, "t_tool_config", ToolConfigInfoModel.class).getMappedResults();

        return configModels.stream().map(ToolConfigInfoModel::getTaskId).collect(Collectors.toList());
    }


    /**
     * 多条件查询工具配置信息
     *
     * @param taskIds      任务ID集合
     * @param tools        工具名
     * @param followStatus 跟进状态
     * @param isNot        是否取反查询(跟进状态)
     * @return list
     */
    public List<ToolConfigInfoModel> findByToolAndFollowStatus(Set<Long> taskIds, Collection<String> tools,
            Integer followStatus, boolean isNot) {
        // 组装基本筛选条件
        Criteria criteria = getCriteriaByCondition(taskIds, tools, followStatus, isNot);

        // 允许磁盘操作(支持较大数据集合的处理)
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();

        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria)).withOptions(options);
        AggregationResults<ToolConfigInfoModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_tool_config", ToolConfigInfoModel.class);

        return queryResults.getMappedResults();
    }

    /**
     *  组装基本筛选条件
     * @return criteria
     */
    @NotNull
    private Criteria getCriteriaByCondition(Collection<Long> taskIds, Collection<String> tools, Integer followStatus,
            boolean isNot) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        // 可选查询条件
        if (CollectionUtils.isNotEmpty(taskIds)) {
            criteriaList.add(Criteria.where("task_id").in(taskIds));
        }
        if (CollectionUtils.isNotEmpty(tools)) {
            criteriaList.add(Criteria.where("tool_name").in(tools));
        }
        if (followStatus != null) {
            // 下架取反 = 非下架, 有效的
            if (isNot && FOLLOW_STATUS.WITHDRAW.value() == followStatus) {
                List<Integer> effectiveStatus = FOLLOW_STATUS.getEffectiveStatus();
                criteriaList.add(Criteria.where("follow_status").in(effectiveStatus));
            } else {
                criteriaList.add(Criteria.where("follow_status").is(followStatus));
            }
        }

        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        return criteria;
    }

    /**
     * 按任务id分组统计工具数
     *
     * @param taskIds      任务ID集合
     * @param followStatus 排除下架状态
     * @return list
     */
    public List<DefectStatModel> countToolsByTaskIds(Set<Long> taskIds, Integer followStatus) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        if (CollectionUtils.isNotEmpty(taskIds)) {
            criteriaList.add(Criteria.where("task_id").in(taskIds));
        }
        criteriaList.add(Criteria.where("follow_status").ne(followStatus));

        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // 允许磁盘操作(支持较大数据集合的处理)
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        // 按任务id分组统计工具数
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id").count().as("count");
        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria), group).withOptions(options);
        AggregationResults<DefectStatModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_tool_config", DefectStatModel.class);

        return queryResults.getMappedResults();
    }

    /**
     * 根据tool_name follow_status 分组查询follow_status不同状态下的数量
     * @param taskIds
     * @param startTime
     * @param endTime
     * @return
     */
    public List<ToolsRegisterStatisticsModel> getToolRegisterCount(List<Long> taskIds, long startTime, long endTime) {
        MatchOperation match = Aggregation
                .match(Criteria.where("task_id").in(taskIds).and("create_date").gte(startTime).lte(endTime));

        GroupOperation group = Aggregation.group("tool_name", "follow_status")
                .first("tool_name").as("tool_name")
                .first("follow_status").as("follow_status")
                .count().as("register_count");
        Aggregation aggregation = Aggregation.newAggregation(match, group);
        AggregationResults<ToolsRegisterStatisticsModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_tool_config", ToolsRegisterStatisticsModel.class);
        return queryResults.getMappedResults();
    }

    /**
     * 多条件分页查询工具注册明细信息
     */
    public Page<ToolConfigInfoModel> findToolInfoPage(TaskToolInfoReqVO reqVO, Pageable pageable) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();
        // 指定taskId集合
        Collection<Long> taskIdSet = reqVO.getTaskIds();
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            criteriaList.add(Criteria.where("task_id").in(taskIdSet));
        }

        String toolName = reqVO.getToolName();
        if (StringUtils.isNotEmpty(toolName)) {
            criteriaList.add(Criteria.where("tool_name").is(toolName));
        }
        Integer followStatus = reqVO.getFollowStatus();
        if (followStatus != null) {
            // 除了 已接入和下架 其他都是未跟进
            if (followStatus == 0) {
                List<Integer> status = Lists.newArrayList(FOLLOW_STATUS.NOT_FOLLOW_UP_0.value(),
                        FOLLOW_STATUS.NOT_FOLLOW_UP_1.value(),
                        FOLLOW_STATUS.EXPERIENCE.value(), FOLLOW_STATUS.ACCESSING.value(),
                        FOLLOW_STATUS.HANG_UP.value());
                criteriaList.add(Criteria.where("follow_status").in(status));
            } else if (followStatus < 0) {
                // 非下架
                List<Integer> status =
                        Lists.newArrayList(FOLLOW_STATUS.NOT_FOLLOW_UP_0.value(), FOLLOW_STATUS.NOT_FOLLOW_UP_1.value(),
                                FOLLOW_STATUS.EXPERIENCE.value(), FOLLOW_STATUS.ACCESSING.value(),
                                FOLLOW_STATUS.HANG_UP.value(), FOLLOW_STATUS.ACCESSED.value());
                criteriaList.add(Criteria.where("follow_status").in(status));
            } else {
                criteriaList.add(Criteria.where("follow_status").is(followStatus));
            }
        }

        String startTime = reqVO.getStartTime();
        if (StringUtils.isNotEmpty(startTime)) {
            criteriaList.add(Criteria.where("create_date").gte(DateTimeUtils.getTimeStamp(startTime)));
        }

        String endTime = reqVO.getEndTime();
        if (StringUtils.isNotEmpty(endTime)) {
            criteriaList.add(Criteria.where("create_date").lte(DateTimeUtils.getTimeStamp(endTime)));
        }

        if (CollectionUtils.isNotEmpty(criteriaList)) {
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
        // 指定查询字段
        ProjectionOperation project = Aggregation.project("task_id", "tool_name", "follow_status", "create_date");
        // 允许磁盘操作(支持较大数据集合的处理)
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria), sort, skip, limit, project)
                .withOptions(options);
        AggregationResults<ToolConfigInfoModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_tool_config", ToolConfigInfoModel.class);
        // 计算总页数
        int totalPageNum = 0;
        if (totalCount > 0) {
            totalPageNum = (Integer.parseInt(String.valueOf(totalCount)) + pageSize - 1) / pageSize;
        }
        // 页码加1返回
        return new Page<>(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults.getMappedResults());
    }

    /**
     * 根据工具查询有效工具数量
     *
     * @param taskIdList taskId集合
     * @param toolName   工具名
     * @return list
     */
    public Long findToolCountNow(List<Long> taskIdList, String toolName, List<Integer> followStatus) {
        Query query = new Query();

        // taskId
        if (CollectionUtils.isNotEmpty(taskIdList)) {
            query.addCriteria(Criteria.where("task_id").in(taskIdList));
        }
        // 工具筛选
        query.addCriteria(Criteria.where("tool_name").is(toolName));

        if (CollectionUtils.isNotEmpty(followStatus)) {
            query.addCriteria(Criteria.where("follow_status").in(followStatus));
        }

        return mongoTemplate.count(query, "t_tool_config");
    }

    /**
     * 按条件查询单个工具配置信息
     *
     * @param taskIdList 任务ID集合
     * @param toolName   工具名
     * @return model
     */
    public List<ToolConfigInfoModel> findByTaskIdsAndToolName(Set<Long> taskIdList, String toolName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").in(taskIdList).and("tool_name").is(toolName));
        return mongoTemplate.find(query, ToolConfigInfoModel.class, "t_tool_config");
    }
}
