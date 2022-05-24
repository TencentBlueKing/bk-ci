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

package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.task.model.ToolCheckerSetEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.model.ToolCountScriptEntity;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代码检查任务持久层代码
 *
 * @version V1.0
 * @date 2019/4/26
 */
@Repository
public class ToolDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    public void updateToolStepStatusByTaskIdAndToolName(ToolConfigBaseVO toolConfigBaseVO)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(toolConfigBaseVO.getTaskId())).
                addCriteria(Criteria.where("tool_name").is(toolConfigBaseVO.getToolName()));
        Update update = new Update();
        update.set("cur_step", toolConfigBaseVO.getCurStep());
        update.set("step_status", toolConfigBaseVO.getStepStatus());
        update.set("current_build_id", toolConfigBaseVO.getCurrentBuildId());
        update.set("updatedBy", ComConstants.SYSTEM_USER);
        update.set("updatedDate", System.currentTimeMillis());
        mongoTemplate.updateMulti(query, update, ToolConfigInfoEntity.class);
    }

    public void updateParamJson(ToolConfigInfoEntity toolConfigInfoEntity, String userName)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(toolConfigInfoEntity.getTaskId())).
                addCriteria(Criteria.where("tool_name").is(toolConfigInfoEntity.getToolName()));
        Update update = new Update();
        update.set("param_json", toolConfigInfoEntity.getParamJson());
        update.set("updated_by", userName);
        update.set("updated_date", System.currentTimeMillis());
        mongoTemplate.updateMulti(query, update, ToolConfigInfoEntity.class);
    }

    public void clearCheckerSet(long taskId, List<String> toolNames)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId)).
                addCriteria(Criteria.where("tool_name").in(toolNames));
        Update update = new Update();
        update.set("checker_set", null);
        mongoTemplate.updateMulti(query, update, ToolConfigInfoEntity.class);
    }

    public void setCheckerSet(long taskId, List<ToolCheckerSetEntity> toolCheckerSets)
    {
        if (CollectionUtils.isNotEmpty(toolCheckerSets))
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ToolConfigInfoEntity.class);
            for (ToolCheckerSetEntity toolCheckerSetVO : toolCheckerSets)
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(taskId)).
                        addCriteria(Criteria.where("tool_name").is(toolCheckerSetVO.getToolName()));
                Update update = new Update();
                update.set("checker_set", toolCheckerSetVO);
                ops.updateOne(query, update);
            }
            ops.execute();
        }
    }


    /**
     * 更新工具配置
     *
     * @param taskId     任务ID
     * @param toolName   工具名
     * @param specConfig 特殊配置
     * @param userName   更改人
     */
    public Boolean updateToolConfigInfo(Long taskId, String toolName, String userName, String specConfig,
            String platformIp)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId)).addCriteria(Criteria.where("tool_name").is(toolName));

        Update update = new Update();
        if (specConfig != null)
        {
            update.set("spec_config", specConfig);
        }

        if (StringUtils.isNotBlank(platformIp))
        {
            update.set("platform_ip", platformIp);
        }

        update.set("updated_date", System.currentTimeMillis());
        update.set("updated_by", userName);
        return mongoTemplate.updateMulti(query, update, ToolConfigInfoEntity.class).getModifiedCount() > 0;
    }

    /**
     * 更新最近一次扫描使用的工具镜像版本
     * @param taskId
     * @param toolName
     * @param toolImageRevision
     */
    public void updateToolImageRevision(long taskId, String toolName, String toolImageRevision)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId)).
                addCriteria(Criteria.where("tool_name").is(toolName));
        Update update = new Update();
        update.set("tool_image_revision", toolImageRevision);
        mongoTemplate.updateMulti(query, update, ToolConfigInfoEntity.class);
    }


    /**
     * 按任务ID查询指定状态的且有构建ID的工具信息
     *
     * @param taskIds      有效任务ID
     * @param followStatus 跟进状态
     * @param pageable     分页
     * @return list
     */
    public List<ToolConfigInfoEntity> getTaskIdsAndFollowStatusPage(Collection<Long> taskIds,
            List<Integer> followStatus, Pageable pageable)
    {
        Query query = new Query();
        if (CollectionUtils.isNotEmpty(taskIds))
        {
            query.addCriteria(Criteria.where("task_id").in(taskIds));
        }
        if (CollectionUtils.isNotEmpty(followStatus))
        {
            query.addCriteria(Criteria.where("follow_status").in(followStatus));
        }

        // 当前构建ID字段存在且不为空
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("current_build_id").exists(true));
        criteria.add(Criteria.where("current_build_id").ne(""));
        query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));

        if (pageable != null)
        {
            query.with(pageable);
        }

        return mongoTemplate.find(query, ToolConfigInfoEntity.class);
    }


    /**
     * 批量更新工具跟进状态
     *
     * @param toolConfigInfoList 工具配置信息列表
     * @param followStatus       跟进状态
     */
    public void batchUpdateToolFollowStatus(List<ToolConfigInfoEntity> toolConfigInfoList,
            ComConstants.FOLLOW_STATUS followStatus)
    {
        if (CollectionUtils.isNotEmpty(toolConfigInfoList))
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ToolConfigInfoEntity.class);
            toolConfigInfoList.forEach(entity ->
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(entity.getEntityId())).and("task_id")
                        .is(entity.getTaskId()));

                Update update = new Update();
                update.set("last_follow_status", entity.getFollowStatus());
                update.set("follow_status", followStatus.value());
                update.set("updated_date", System.currentTimeMillis());

                ops.updateOne(query, update);
            });
            ops.execute();
        }

    }


    /**
     * 根据工具名分组 查询工具数量
     *
     * @param taskIdList taskId集合
     * @param endTime    结束时间
     * @return list
     */
    public List<ToolCountScriptEntity> findDailyToolCount(List<Long> taskIdList, long endTime) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        // taskId
        if (CollectionUtils.isNotEmpty(taskIdList)) {
            criteriaList.add(Criteria.where("task_id").in(taskIdList));
        }
        // 有效状态
        List<Integer> effectiveStatus = ComConstants.FOLLOW_STATUS.getEffectiveStatus();
        criteriaList.add(Criteria.where("follow_status").in(effectiveStatus));
        // 时间
        criteriaList.add(Criteria.where("create_date").lte(endTime));

        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // 按工具名分组统计工具数
        GroupOperation group = Aggregation.group("tool_name").first("tool_name").as("tool_name").count().as("count");
        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria), group);
        AggregationResults<ToolCountScriptEntity> queryResults =
                mongoTemplate.aggregate(aggregation, "t_tool_config", ToolCountScriptEntity.class);

        return queryResults.getMappedResults();
    }

    public void removeByIds(Collection<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return;
        }

        Query query = new Query();
        List<ObjectId> objectIdList = idList.stream().map(id -> new ObjectId(id)).collect(Collectors.toList());
        query.addCriteria(Criteria.where("_id").in(objectIdList));
        mongoTemplate.remove(query, ToolConfigInfoEntity.class);
    }
}
