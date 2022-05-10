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

import com.tencent.bk.codecc.defect.model.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.DUPCStatisticEntity;
import com.tencent.bk.codecc.defect.vo.CodeBlockVO;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
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
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.regex.Pattern;

/**
 * DUPC类告警的查詢持久化
 *
 * @version V1.0
 * @date 2019/5/21
 */
@Repository
public class DUPCDefectDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 查询代码块信息
     *
     * @param taskId
     * @param sourceBlockList
     * @return
     */
    public List<DUPCDefectEntity> queryCodeBlocksByFingerPrint(long taskId, List<CodeBlockVO> sourceBlockList)
    {
        List<String> fingerPrintList = new ArrayList<>();
        sourceBlockList.forEach(codeBlock ->
                fingerPrintList.add(codeBlock.getFingerPrint()));

        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId).and("block_list.finger_print").in(fingerPrintList));

        return mongoTemplate.find(query, DUPCDefectEntity.class);
    }

    /**
     * 根据任务ID，作者和路径列表查询
     *
     * @param taskId
     * @param author
     * @param fileList
     * @return
     */
    public List<DUPCDefectEntity> findByTaskIdAndAuthorAndRelPaths(long taskId, String author, Set<String> fileList)
    {
        Document fieldsObj = new Document();
        fieldsObj.put("block_list", false);
        Query query = new BasicQuery(new Document(), fieldsObj);
        query.addCriteria(Criteria.where("task_id").is(taskId).and("status").is(ComConstants.DefectStatus.NEW.value()));

        //作者过滤
        if (StringUtils.isNotEmpty(author))
        {
            // 去掉人名的字符
            String authorParam = author.trim();
            if (author.contains("(") && author.endsWith(")"))
            {
                authorParam = author.substring(0, author.indexOf("("));
            }
            Pattern pattern = Pattern.compile(String.format("^.*%s.*$", authorParam), Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("author_list").regex(pattern));
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

        //查询总的数量，并且过滤计数
        return mongoTemplate.find(query, DUPCDefectEntity.class);
    }

    /**
     * 根据工具ID和工具列表查询最近一次分析记录
     *
     * @param taskId
     * @param toolSet
     * @return
     */
    public List<DUPCStatisticEntity> findFirstByTaskIdOrderByTime(long taskId, Set<String> toolSet)
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
                .first("dup_rate").as("dup_rate")
                .first("last_dup_rate").as("last_dup_rate")
                .first("dup_rate_change").as("dup_rate_change")
                .first("dupc_scan_summary").as("dupc_scan_summary");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);

        AggregationResults<DUPCStatisticEntity> queryResult = mongoTemplate.aggregate(agg, "t_dupc_statistic", DUPCStatisticEntity.class);
        return queryResult.getMappedResults();
    }

    public void upsertDupcDefect(List<DUPCDefectEntity> dupcDefectEntities)
    {
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DUPCDefectEntity.class);
        if (CollectionUtils.isNotEmpty(dupcDefectEntities))
        {
            for (DUPCDefectEntity dupcDefectEntity : dupcDefectEntities)
            {
                Query query = new Query();
                Criteria criteria = Criteria.where("task_id").is(dupcDefectEntity.getTaskId());

                if (StringUtils.isNotEmpty(dupcDefectEntity.getRelPath()))
                {
                    criteria.and("rel_path").is(dupcDefectEntity.getRelPath());
                }
                else
                {
                    criteria.and("file_path").is(dupcDefectEntity.getFilePath());
                }
                query.addCriteria(criteria);
                Update update = new Update();
                update.set("task_id", dupcDefectEntity.getTaskId())
                        .set("tool_name", dupcDefectEntity.getToolName())
                        .set("rel_path", dupcDefectEntity.getRelPath())
                        .set("url", dupcDefectEntity.getUrl())
                        .set("file_path", dupcDefectEntity.getFilePath())
                        .set("total_lines", dupcDefectEntity.getTotalLines())
                        .set("dup_lines", dupcDefectEntity.getDupLines())
                        .set("dup_rate", dupcDefectEntity.getDupRate())
                        .set("dup_rate_value", dupcDefectEntity.getDupRateValue())
                        .set("block_num", dupcDefectEntity.getBlockNum())
                        .set("author_list", dupcDefectEntity.getAuthorList())
                        .set("file_change_time", dupcDefectEntity.getFileChangeTime())
                        .set("status", dupcDefectEntity.getStatus())
                        .set("create_time", dupcDefectEntity.getCreateTime())
                        .set("fixed_time", dupcDefectEntity.getFixedTime())
                        .set("last_update_time", dupcDefectEntity.getLastUpdateTime())
                        .set("repo_id", dupcDefectEntity.getRepoId())
                        .set("revision", dupcDefectEntity.getRevision())
                        .set("branch", dupcDefectEntity.getBranch())
                        .set("sub_module", dupcDefectEntity.getSubModule())
                        .set("analysis_version", dupcDefectEntity.getAnalysisVersion())
                        .set("block_list", dupcDefectEntity.getBlockList());
                ops.upsert(query, update);
            }
            ops.execute();
        }
    }


    public void batchFixDefect(long taskId, List<DUPCDefectEntity> defectList)
    {
        if (CollectionUtils.isNotEmpty(defectList))
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DUPCDefectEntity.class);
            defectList.forEach(defectEntity ->
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("status", defectEntity.getStatus());
                update.set("fixed_time", defectEntity.getFixedTime());
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
    public List<DUPCStatisticEntity> batchFindByTaskIdInAndTool(Collection<Long> taskIdSet, String toolName)
    {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName));
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "time");
        // 以taskId进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("tool_name").as("tool_name")
                .first("time").as("time")
                .first("defect_count").as("defect_count")
                .first("defect_change").as("defect_change")
                .first("last_defect_count").as("last_defect_count")
                .first("dup_rate").as("dup_rate")
                .first("last_dup_rate").as("last_dup_rate")
                .first("super_high_count").as("super_high_count")
                .first("high_count").as("high_count")
                .first("medium_count").as("medium_count");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);

        AggregationResults<DUPCStatisticEntity> queryResult =
                mongoTemplate.aggregate(agg, "t_dupc_statistic", DUPCStatisticEntity.class);
        return queryResult.getMappedResults();
    }
}
