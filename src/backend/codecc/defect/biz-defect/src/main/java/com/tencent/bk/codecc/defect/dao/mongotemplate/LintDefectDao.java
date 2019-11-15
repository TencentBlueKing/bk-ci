/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

import com.mongodb.BasicDBObject;
import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.model.LintFileQueryRspEntity;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
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
@Repository
public class LintDefectDao
{
    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * 根据参数查询lint文件
     *
     * @param taskId
     * @param toolName
     * @param fileList
     * @param checker
     * @param author
     * @param fileType
     * @param severity
     * @param pkgChecker
     * @param pageable
     * @return
     */
    public LintFileQueryRspEntity findLintFileByParam(long taskId, String toolName, List<String> fileList, String checker,
                                                      String author, List<String> fileType, List<String> severity,
                                                      Set<String> pkgChecker, Pageable pageable)
    {
        LintFileQueryRspEntity lintFileQueryRspEntity = new LintFileQueryRspEntity();
        Criteria andCriteria = new Criteria();
        Query query = getFilePremiumQuery(taskId, toolName, fileList, checker, author, andCriteria);
        query.addCriteria(Criteria.where("defect_list").elemMatch(andCriteria));

        //查询总的数量，并且过滤计数
        List<LintFileEntity> originalFileInfoEntityList = mongoTemplate.find(query, LintFileEntity.class);

        //对查询到的原始数据进行过滤，并统计数量
        if (CollectionUtils.isNotEmpty(originalFileInfoEntityList))
        {
            Iterator<LintFileEntity> it = originalFileInfoEntityList.iterator();
            while (it.hasNext())
            {
                LintFileEntity lintFileEntity = it.next();
                List<LintDefectEntity> lintDefectEntityList = lintFileEntity.getDefectList();
                if (CollectionUtils.isNotEmpty(lintDefectEntityList))
                {
                    lintDefectEntityList = lintDefectEntityList.stream()
                            .filter(lint -> ComConstants.DefectStatus.NEW.value() == lint.getStatus())
                            .collect(Collectors.toList());
                    Iterator<LintDefectEntity> lintDefectIt = lintDefectEntityList.iterator();
                    while (lintDefectIt.hasNext())
                    {
                        LintDefectEntity lintDefectEntity = lintDefectIt.next();
                        //缺陷类型过滤
                        boolean meetChecker = StringUtils.isNotEmpty(checker) && !checker.equals(lintDefectEntity.getChecker());

                        //判断是否属于查询的规则包中的规则
                        if (CollectionUtils.isNotEmpty(pkgChecker))
                        {
                            meetChecker = !pkgChecker.contains(lintDefectEntity.getChecker());
                        }

                        // 处理人条件不为空且文件的处理人不包含处理人条件时，判断为true移除，否则false不移除
                        boolean meetAuthor = StringUtils.isNotEmpty(author) && !author.equals(lintDefectEntity.getAuthor());

                        // 不满足缺陷类型、处理人条件的先移除掉
                        if (meetChecker || meetAuthor)
                        {
                            lintDefectIt.remove();
                            continue;
                        }
                    }
                }

                //告警类型过滤
                boolean defectZero = checkDefectCountByDefectType(fileType, lintDefectEntityList, lintFileQueryRspEntity);
                if (CollectionUtils.isNotEmpty(fileType) && !defectZero)
                {
                    it.remove();
                    continue;
                }

                //严重等级过滤
                boolean isZero = checkDefectCountBySeverityIsZero(severity, lintDefectEntityList, lintFileQueryRspEntity);

                if (CollectionUtils.isNotEmpty(severity) && !isZero)
                {
                    it.remove();
                    continue;
                }
            }
        }
        long total = originalFileInfoEntityList.size();

        //重新获取查询条件并拼接
        Criteria finalAndCriteria = new Criteria();
        Query finalQuery = getFilePremiumQuery(taskId, toolName, fileList, checker, author, finalAndCriteria);
        //根据文件类型添加查询条件
        if (CollectionUtils.isNotEmpty(fileType))
        {
            finalAndCriteria.and("defect_type").in(fileType.stream().
                    map(Integer::valueOf).collect(Collectors.toList()));
        }
        //6.根据告警严重等级添加查询条件
        if (CollectionUtils.isNotEmpty(severity))
        {
            finalAndCriteria.and("severity").in(severity.stream().
                    map(sev ->
                    {
                        if (Integer.valueOf(sev) == ComConstants.PROMPT_IN_DB)
                        {
                            return ComConstants.PROMPT;
                        }
                        else
                        {
                            return Integer.valueOf(sev);
                        }
                    }).collect(Collectors.toList()));
        }

        finalQuery.getFieldsObject().removeField("status");
        finalQuery.getFieldsObject().removeField("defect_list");
        finalQuery.addCriteria(Criteria.where("defect_list").elemMatch(finalAndCriteria));

        List<LintFileEntity> fileInfoEntityList = mongoTemplate.find(finalQuery.with(pageable), LintFileEntity.class);
        Page<LintFileEntity> fileInfoEntityPage = new PageImpl(fileInfoEntityList, pageable, total);
        lintFileQueryRspEntity.setLintFileList(fileInfoEntityPage);

        return lintFileQueryRspEntity;
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
    private Query getFilePremiumQuery(long taskId, String toolName, List<String> fileList, String checker, String author,
                                      Criteria andCriteria)
    {
        BasicDBObject fieldsObj = new BasicDBObject();
        fieldsObj.put("file_path", true);
        fieldsObj.put("defect_count", true);
        fieldsObj.put("status", true);
        fieldsObj.put("checker_list", true);
        fieldsObj.put("defect_list", true);
        Query query = new BasicQuery(new BasicDBObject(), fieldsObj);
        query.addCriteria(Criteria.where("task_id").is(taskId).
                and("tool_name").is(toolName));
        //1. 路径过滤
        List<Criteria> criteriaList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fileList))
        {
            fileList.forEach(file ->
                    criteriaList.add(Criteria.where("rel_path").regex(String.format("%s%s%s", ".*", file, ".*")))
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
        query.addCriteria(Criteria.where("status").is(ComConstants.TaskFileStatus.NEW.value()));
        query.addCriteria(Criteria.where("defect_list.status").is(ComConstants.DefectStatus.NEW.value()));
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
        Criteria defectCriteria = new Criteria();
        if (StringUtils.isNotEmpty(checker))
        {
            query.addCriteria(Criteria.where("checker_list").is(checker));
        }
        if (StringUtils.isNotEmpty(author))
        {
            query.addCriteria(Criteria.where("author_list").is(author));
            defectCriteria.and("author").is(author);
        }
        return mongoTemplate.findOne(query, LintFileEntity.class);

    }


    /**
     * 检查文件中符合查询条件的告警数是否为零，为零返回false，不为零则返回true
     *
     * @param conditionSeverity
     * @param defectList
     * @param lintFileQueryRspEntity
     * @return
     */
    private boolean checkDefectCountBySeverityIsZero(List<String> conditionSeverity, List<LintDefectEntity> defectList,
                                                     LintFileQueryRspEntity lintFileQueryRspEntity)
    {
        // 根据条件过滤后，重新计算文件中各严重类型的告警数及所有告警总数
        int checkerCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;

        Iterator<LintDefectEntity> it = defectList.iterator();
        while (it.hasNext())
        {
            LintDefectEntity lintDefectEntity = it.next();
            int severity = lintDefectEntity.getSeverity();
            // 当从数据库中查询来的缺陷严重程度为提示（3）时，需要转换成前端表示提示的数值4
            if (severity == ComConstants.PROMPT_IN_DB)
            {
                severity = ComConstants.PROMPT;
            }

            // 1. 按照缺陷类型、处理人条件过滤后按照严重程度统计告警数量
            if (ComConstants.SERIOUS == severity)
            {
                highCount++;
            }
            else if (ComConstants.NORMAL == severity)
            {
                mediumCount++;
            }
            else if (ComConstants.PROMPT == severity)
            {
                lowCount++;
            }

            // 2. 严重程度条件不为空且与当前数据的严重程度不匹配时，判断为true移除，否则false不移除
            boolean meetSeverity = CollectionUtils.isNotEmpty(conditionSeverity) &&
                    !conditionSeverity.contains(String.valueOf(severity));
            if (meetSeverity)
            {
                it.remove();
                continue;
            }
            checkerCount++;
        }
        lintFileQueryRspEntity.setSeriousCheckerCount(lintFileQueryRspEntity.getSeriousCheckerCount() + highCount);
        lintFileQueryRspEntity.setNormalCheckerCount(lintFileQueryRspEntity.getNormalCheckerCount() + mediumCount);
        lintFileQueryRspEntity.setPromptCheckerCount(lintFileQueryRspEntity.getPromptCheckerCount() + lowCount);
        lintFileQueryRspEntity.setTotalCheckerCount(lintFileQueryRspEntity.getTotalCheckerCount() + checkerCount);

        if (CollectionUtils.isEmpty(defectList))
        {
            return false;
        }

        return true;
    }

    /**
     * 通过告警类型验证告警数量
     *
     * @param fileType
     * @param lintDefectEntityList
     * @param lintFileQueryRspEntity
     * @return
     */
    private boolean checkDefectCountByDefectType(List<String> fileType, List<LintDefectEntity> lintDefectEntityList,
                                                 LintFileQueryRspEntity lintFileQueryRspEntity)
    {
        int newDefects = 0;
        int historyDefects = 0;
        //根据告警中的告警类型判断新增告警还是历史告警
        if (CollectionUtils.isNotEmpty(lintDefectEntityList))
        {
            for (LintDefectEntity lintDefectEntity : lintDefectEntityList)
            {
                if (DefectConstants.DefectType.NEW.value() == lintDefectEntity.getDefectType())
                {
                    newDefects++;
                }
                else if (DefectConstants.DefectType.HISTORY.value() == lintDefectEntity.getDefectType())
                {
                    historyDefects++;
                }
            }
        }
        lintFileQueryRspEntity.setNewDefectCount(lintFileQueryRspEntity.getNewDefectCount() + newDefects);
        lintFileQueryRspEntity.setHistoryDefectCount(lintFileQueryRspEntity.getHistoryDefectCount() + historyDefects);

        if (CollectionUtils.isNotEmpty(fileType))
        {
            int queryDefects = 0;
            if (fileType.contains(String.valueOf(DefectConstants.DefectType.NEW.value())))
            {
                queryDefects = queryDefects + newDefects;
            }
            if (fileType.contains(String.valueOf(DefectConstants.DefectType.HISTORY.value())))
            {
                queryDefects = queryDefects + historyDefects;
            }
            if (queryDefects == 0)
            {
                return false;
            }
        }
        return true;
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
        BasicDBObject fieldsObj = new BasicDBObject();
        fieldsObj.put("task_id", true);
        fieldsObj.put("url", true);
        fieldsObj.put("rel_path", true);
        fieldsObj.put("author_list", true);
        fieldsObj.put("checker_list", true);
        Query query = new BasicQuery(new BasicDBObject(), fieldsObj);
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
     * 根据任务id和工具名称寻找lint类告警文件
     * defect_list 数据量有点多，所以单独查询
     *
     * @param taskId
     * @param toolName
     * @return
     */
    public List<LintFileEntity> findDefectList(Long taskId, String toolName, int status)
    {
        BasicDBObject fieldsObj = new BasicDBObject();
        fieldsObj.put("task_id", true);
        fieldsObj.put("defect_list", true);
        Query query = new BasicQuery(new BasicDBObject(), fieldsObj);
        query.addCriteria(Criteria.where("task_id").is(taskId));
        query.addCriteria(Criteria.where("tool_name").is(toolName));
        query.addCriteria(Criteria.where("status").is(status));
        return mongoTemplate.find(query, LintFileEntity.class);
    }


    /**
     * 批量把文件状态更新为已修复
     *
     * @param lintFileEntityList
     */
    public void batchUpdateFileToFixed(List<LintFileEntity> lintFileEntityList)
    {
        if (CollectionUtils.isNotEmpty(lintFileEntityList))
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintFileEntity.class);

            for (LintFileEntity lintFileEntity : lintFileEntityList)
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(lintFileEntity.getEntityId())));
                Update update = new Update();
                update.set("status", lintFileEntity.getStatus());
                update.set("fixed_time", lintFileEntity.getFixedTime());
                ops.updateOne(query, update);
            }
            ops.execute();
        }
    }


}
