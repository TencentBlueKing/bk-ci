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

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.vo.LintDefectQueryRspVO;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.vo.CCNDefectVO;
import com.tencent.bk.codecc.defect.vo.LintDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.LintDefectVO;
import com.tencent.bk.codecc.defect.vo.LintFileVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * lint类工具持久层代码
 *
 * @version V1.0
 * @date 2019/5/10
 */
@Slf4j
@Repository
public class LintDefectDao
{
    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * 根据条件查询文件列表
     *
     * @param taskId
     * @param toolName
     * @param fileList
     * @param checker
     * @param author
     * @return
     */
    public List<LintFileEntity> findFileListByParams(long taskId, String toolName, Set<String> fileList, String checker, String author)
    {
        Criteria andCriteria = new Criteria();
        Query query = getFilePremiumQuery(taskId, toolName, fileList, checker, author, andCriteria);
        query.addCriteria(Criteria.where("defect_list").elemMatch(andCriteria));

        //查询总的数量，并且过滤计数
        return mongoTemplate.find(query, LintFileEntity.class);
    }


    /**
     * 获取原始查询条件
     *
     * @param taskId
     * @param toolName
     * @param fileList
     * @param checker
     * @param author
     * @param andCriteria
     * @return
     */
    private Query getFilePremiumQuery(long taskId, String toolName, Set<String> fileList, String checker, String author,
                                      Criteria andCriteria)
    {
        Document fieldsObj = new Document();
        fieldsObj.put("task_id", true);
        fieldsObj.put("file_path", true);
        fieldsObj.put("rel_path", true);
        fieldsObj.put("defect_count", true);
        fieldsObj.put("status", true);
        fieldsObj.put("create_time", true);
        fieldsObj.put("file_update_time", true);
        fieldsObj.put("checker_list", true);
        fieldsObj.put("defect_list", true);
        Query query = new BasicQuery(new Document(), fieldsObj);
        query.addCriteria(Criteria.where("task_id").is(taskId).
                and("tool_name").is(toolName));
        //1. 路径过滤
        List<Criteria> criteriaList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fileList))
        {
            fileList.forEach(file ->
                    criteriaList.add(Criteria.where("file_path").regex(file))
            );
            query.addCriteria(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        }

        List<Criteria> checkerListCriteria = new ArrayList<>();
        checkerListCriteria.add(Criteria.where("checker_list").exists(true));
        checkerListCriteria.add(Criteria.where("checker_list.0").exists(true));
        //2.规则集不能为空
        query.addCriteria(new Criteria().andOperator(checkerListCriteria.toArray(new Criteria[0])));

        //3. 规则类型过滤
        if (StringUtils.isNotEmpty(checker))
        {
            query.addCriteria(Criteria.where("checker_list").is(checker));
            andCriteria.and("checker").is(checker);
        }

        //4. 告警作者过滤
        if (StringUtils.isNotEmpty(author))
        {
            query.addCriteria(Criteria.where("author_list").is(author));
            andCriteria.and("author").is(author);
        }

        //5. 文件及告警状态过滤
//        query.addCriteria(Criteria.where("status").in(ComConstants.TaskFileStatus.NEW.value()));
//        query.addCriteria(Criteria.where("defect_list.status").is(ComConstants.DefectStatus.NEW.value()));
        return query;
    }


    /**
     * 根据参数查询告警
     *
     * @param entityId
     * @param checker
     * @param author
     * @return
     */
    public LintFileEntity findDefectByParam(String entityId, String checker, String author)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(new ObjectId(entityId)));
        if (StringUtils.isNotEmpty(checker))
        {
            query.addCriteria(Criteria.where("checker_list").is(checker));
        }
        if (StringUtils.isNotEmpty(author))
        {
            query.addCriteria(Criteria.where("author_list").is(author));
        }
        return mongoTemplate.findOne(query, LintFileEntity.class);
    }

    /**
     * 根据任务id和工具名称寻找lint类告警文件
     *
     * @param taskId
     * @param toolName
     * @return
     */
    public List<LintFileEntity> findFileInfoList(Long taskId, String toolName)
    {
        Document fieldsObj = new Document();
        fieldsObj.put("task_id", true);
        fieldsObj.put("file_path", true);
        fieldsObj.put("url", true);
        fieldsObj.put("rel_path", true);
        fieldsObj.put("author_list", true);
        fieldsObj.put("checker_list", true);
        fieldsObj.put("status", true);
        Query query = new BasicQuery(new Document(), fieldsObj);
        query.addCriteria(Criteria.where("task_id").is(taskId));
        if (StringUtils.isNotBlank(toolName))
        {
            query.addCriteria(Criteria.where("tool_name").is(toolName));
        }

        // 排除存在路径屏蔽的的status
        int news = ComConstants.DefectStatus.NEW.value();
        int fixed = ComConstants.DefectStatus.FIXED.value();
        int pathMask = ComConstants.DefectStatus.PATH_MASK.value();
        List<Integer> statusList = Arrays.asList((news | pathMask), (fixed | pathMask), ((news | fixed) | pathMask));

        query.addCriteria(Criteria.where("status").nin(statusList));
        return mongoTemplate.find(query, LintFileEntity.class);
    }

    /**
     * 插入或更新告警文件信息和告警列表
     *
     * @param taskId
     * @param toolName
     * @param defectFiles
     */
    public void upsertDefectListByPath(long taskId, String toolName, List<LintFileEntity> defectFiles)
    {
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintFileEntity.class);
        if (CollectionUtils.isNotEmpty(defectFiles))
        {
            for (LintFileEntity defectFile : defectFiles)
            {
                Query query = new Query();
                Criteria criteria = Criteria.where("task_id").is(taskId).and("tool_name").is(toolName);

                if (StringUtils.isNotEmpty(defectFile.getRelPath()))
                {
                    criteria.and("rel_path").is(defectFile.getRelPath());
                }
                else
                {
                    criteria.and("file_path").is(defectFile.getFilePath());
                }
                query.addCriteria(criteria);
                Update update = new Update();
                update.set("file_update_time", defectFile.getFileUpdateTime())
                        .set("file_path", defectFile.getFilePath())
                        .set("url", defectFile.getUrl())
                        .set("status", defectFile.getStatus())
                        .set("fixed_time", defectFile.getFixedTime())
                        .set("exclude_time", defectFile.getExcludeTime())
                        .set("defect_count", defectFile.getDefectCount())
                        .set("new_count", defectFile.getNewCount())
                        .set("history_count", defectFile.getHistoryCount())
                        .set("repo_id", defectFile.getRepoId())
                        .set("revision", defectFile.getRevision())
                        .set("branch", defectFile.getBranch())
                        .set("author_list", defectFile.getAuthorList())
                        .set("checker_list", defectFile.getCheckerList())
                        .set("defect_list", defectFile.getDefectList())
                        .set("md5", defectFile.getMd5())
                        .set("create_time", defectFile.getCreateTime())
                        .set("sub_module", defectFile.getSubModule());
                ops.upsert(query, update);
            }
            ops.execute();
        }
    }

    /**
     * 根据工具ID和工具列表查询最近一次分析记录
     *
     * @param taskId
     * @param toolSet
     * @return
     */
    public List<LintStatisticEntity> findFirstByTaskIdOrderByStartTime(long taskId, Set<String> toolSet)
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
                .first("file_count").as("file_count")
                .first("file_change").as("file_change")
                .first("new_defect_count").as("new_defect_count")
                .first("history_defect_count").as("history_defect_count");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);

        AggregationResults<LintStatisticEntity> queryResult = mongoTemplate.aggregate(agg, "t_lint_statistic", LintStatisticEntity.class);
        return queryResult.getMappedResults();
    }

    /**
     * 查询告警
     * @param taskIds  任务ID集合
     * @param toolName 工具名
     * @param status   告警状态
     * @return list
     */
    public List<LintFileEntity> findByTaskIdInAndToolNameIs(Collection<Long> taskIds, String toolName, Integer status)
    {
        Document fieldsObj = new Document();
        fieldsObj.put("task_id", true);
        fieldsObj.put("file_path", true);
        fieldsObj.put("defect_count", true);
        fieldsObj.put("status", true);
        fieldsObj.put("defect_list", true);
        fieldsObj.put("checker_list", true);
        fieldsObj.put("file_update_time", true);
        fieldsObj.put("createTime", true);

        Query query = new BasicQuery(new Document(), fieldsObj);

        if (!CollectionUtils.isEmpty(taskIds))
        {
            query.addCriteria(Criteria.where("task_id").in(taskIds));
        }
        if (StringUtils.isNotBlank(toolName))
        {
            query.addCriteria(Criteria.where("tool_name").is(toolName));
        }
        // 文件及告警状态过滤
        query.addCriteria(Criteria.where("status").is(ComConstants.TaskFileStatus.NEW.value()));
        query.addCriteria(Criteria.where("defect_list.status").is(ComConstants.DefectStatus.NEW.value()));

        return mongoTemplate.find(query, LintFileEntity.class);
    }


    public void batchMarkDefect(Map<String, Set<String>> fileDefectMap, String markFlag)
    {
        if (MapUtils.isNotEmpty(fileDefectMap))
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintFileEntity.class);
            long currTime = System.currentTimeMillis();
            fileDefectMap.forEach((fileEntityId, defectIdSet) ->
            {
                Query query = Query.query(new Criteria().andOperator(Criteria.where("_id").is(new ObjectId(fileEntityId)),
                        Criteria.where("defect_list").elemMatch(Criteria.where("defect_id").in(defectIdSet))));
                Update update = new Update();
                update.set("defect_list.$.mark", markFlag);
                update.set("defect_list.$.mark_time", currTime);
                ops.updateMulti(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 根据工具ID和工具列表查询最近一次分析记录
     *
     * @param taskIdSet 任务ID集合
     * @param toolName  工具名
     * @return list
     */
    public List<LintStatisticEntity> findStatByTaskIdInAndToolIs(Collection<Long> taskIdSet, String toolName)
    {
        // 以taskId tooName进行过滤
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName));
        // 根据时间倒序排列
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "time");
        // 以taskId进行分组，并取第一个的字段
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("tool_name").as("tool_name")
                .first("time").as("time")
                .first("defect_count").as("defect_count")
                .first("file_count").as("file_count")
                .first("new_defect_count").as("new_defect_count")
                .first("history_defect_count").as("history_defect_count");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);

        AggregationResults<LintStatisticEntity> queryResult =
                mongoTemplate.aggregate(agg, "t_lint_statistic", LintStatisticEntity.class);
        return queryResult.getMappedResults();
    }


    /**
     * 根据任务ID集合和工具名查询所有待修复的告警
     *
     * @param taskIdSet 任务ID集合
     * @param toolName  工具名
     * @return list
     */
    public List<LintFileEntity> findByTaskIdInAndToolIs(Collection<Long> taskIdSet, String toolName)
    {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName)
                .and("status").is(ComConstants.TaskFileStatus.NEW.value())
                .and("defect_list.status").is(ComConstants.DefectStatus.NEW.value()));

        ProjectionOperation project = Aggregation.project("task_id", "status", "create_time", "defect_list");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, project).withOptions(options);

        AggregationResults<LintFileEntity> results =
                mongoTemplate.aggregate(agg, "t_lint_defect", LintFileEntity.class);
        return results.getMappedResults();
    }

}
