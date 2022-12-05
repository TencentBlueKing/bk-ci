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

import com.tencent.bk.codecc.defect.model.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 告警持久类
 *
 * @version V1.0
 * @date 2019/9/29
 */
@Slf4j
@Repository
public class DefectDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 批量更新告警状态的fixed位
     *
     * @param taskId
     * @param defectList
     */
    public void batchUpdateDefectStatusFixedBit(long taskId, List<DefectEntity> defectList) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DefectEntity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId()))
                        .and("task_id").is(taskId));
                Update update = new Update();
                update.set("status", defectEntity.getStatus());
                update.set("fixed_time", defectEntity.getFixedTime());
                update.set("fixed_build_number", defectEntity.getFixedBuildNumber());
                update.set("exclude_time", defectEntity.getExcludeTime());
                update.set("file_path_name", defectEntity.getFilePathname());
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量更新告警状态的ignore位
     *
     * @param taskId
     * @param defectList
     * @param ignoreReasonType
     * @param ignoreReason
     * @param ignoreAuthor
     */
    public void batchUpdateDefectStatusIgnoreBit(long taskId, List<DefectEntity> defectList, int ignoreReasonType,
                                                 String ignoreReason, String ignoreAuthor) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DefectEntity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId()))
                        .and("task_id").is(taskId));
                Update update = new Update();
                update.set("status", defectEntity.getStatus());
                update.set("ignore_time", currTime);
                update.set("ignore_reason_type", ignoreReasonType);
                update.set("ignore_reason", ignoreReason);
                update.set("ignore_author", ignoreAuthor);
                ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量标志告警
     *
     * @param taskId
     * @param defectList
     * @param markFlag
     */
    public void batchMarkDefect(long taskId, List<DefectEntity> defectList, Integer markFlag) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DefectEntity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId()))
                        .and("task_id").is(taskId));
                Update update = new Update();
                update.set("mark", markFlag);
                update.set("mark_time", currTime);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量更新告警作者
     *
     * @param taskId
     * @param defectList
     * @param authorList
     */
    public void batchUpdateDefectAuthor(long taskId, List<DefectEntity> defectList, Set<String> authorList) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DefectEntity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId()))
                        .and("task_id").is(taskId));
                Update update = new Update();
                update.set("author_list", authorList);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 多条件批量获取告警信息
     *
     * @param toolName       工具名称
     * @param taskIdSet      任务ID集合
     * @param checkerNameSet 规则名称集合
     * @param status         告警状态（非待修复状态需要加1）
     * @return defect list
     */
    public List<DefectEntity> batchQueryDefect(String toolName, Collection<Long> taskIdSet,
                                               Set<String> checkerNameSet, Integer status) {
        Document fieldsObj = new Document();
        fieldsObj.put("stream_name", false);
        fieldsObj.put("defect_instances", false);
        fieldsObj.put("ext_bug_id", false);
        fieldsObj.put("platform_build_id", false);
        fieldsObj.put("platform_project_id", false);

        Query query = new BasicQuery(new Document(), fieldsObj);
        if (StringUtils.isNotBlank(toolName)) {
            query.addCriteria(Criteria.where("tool_name").is(toolName));
        }
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            query.addCriteria(Criteria.where("task_id").in(taskIdSet));
        }
        if (CollectionUtils.isNotEmpty(checkerNameSet)) {
            query.addCriteria(Criteria.where("checker_name").in(checkerNameSet));
        }
        if (status != null && status != 0) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        return mongoTemplate.find(query, DefectEntity.class);
    }

    /**
     * 批量查询cloc告警
     *
     * @param toolName
     * @param taskIdSet
     * @return
     */
    public List<CLOCDefectEntity> batchQueryClocDefect(String toolName, Collection<Long> taskIdSet) {
        Query query = new BasicQuery(new Document());
        if (StringUtils.isNotBlank(toolName)) {
            query.addCriteria(Criteria.where("tool_name").is(toolName));
        }
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            query.addCriteria(Criteria.where("task_id").in(taskIdSet));
        }

        return mongoTemplate.find(query, CLOCDefectEntity.class);
    }

    /**
     * 批量更新告警详情
     *
     * @param taskId
     * @param toolName
     * @param defectList
     */
    public void batchUpdateDefectDetail(Long taskId, String toolName, List<DefectEntity> defectList) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DefectEntity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(taskId)
                        .and("tool_name").is(toolName)
                        .and("id").is(defectEntity.getId()));
                Update update = new Update();
                update.set("line_number", defectEntity.getLineNumber());
                update.set("severity", defectEntity.getSeverity());
                update.set("display_type", defectEntity.getDisplayType());
                update.set("display_category", defectEntity.getDisplayCategory());
                update.set("defect_instances", defectEntity.getDefectInstances());
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 逐个更新告警详情
     * @param taskId
     * @param toolName
     * @param defectList
     */
    public void updateDefectDetailOneByOne(Long taskId, String toolName, List<DefectEntity> defectList) {
        defectList.forEach(defectEntity -> {
            try {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(taskId)
                        .and("tool_name").is(toolName)
                        .and("id").is(defectEntity.getId()));
                Update update = new Update();
                update.set("line_number", defectEntity.getLineNumber());
                update.set("severity", defectEntity.getSeverity());
                update.set("display_type", defectEntity.getDisplayType());
                update.set("display_category", defectEntity.getDisplayCategory());
                update.set("defect_instances", defectEntity.getDefectInstances());
                mongoTemplate.updateFirst(query, update, DefectEntity.class);
            } catch (Exception e) {
                log.error("fail to update defect: {}" + defectEntity.getId());
            }
        });
    }
}
