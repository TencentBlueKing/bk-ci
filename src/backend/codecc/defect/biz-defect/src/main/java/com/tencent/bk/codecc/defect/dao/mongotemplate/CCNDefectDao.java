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
import com.tencent.bk.codecc.defect.model.CCNFileQueryRspEntity;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 圈复杂度持久代码
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Repository
public class CCNDefectDao
{
    private static Logger logger = LoggerFactory.getLogger(CCNDefectDao.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 圈复杂度查询告警信息方法
     *
     * @param taskId
     * @param fileList
     * @param author
     * @param severity
     * @param riskFactorConfMap
     * @param pageable
     * @return
     */
    public CCNFileQueryRspEntity findCCNFileByParam(long taskId, List<String> fileList, String author,
                                                    List<String> severity, Map<String, String> riskFactorConfMap,
                                                    Pageable pageable)
    {
        // 需要统计的数据
        int seriousCheckerCount = 0;
        int normalCheckerCount = 0;
        int promptCheckerCount = 0;
        int totalCheckerCount = 0;

        Query originalQuery = getPremiumQuery(taskId, author);

        //路径过滤
        List<Criteria> criteriaList = new ArrayList<>();
        List<Criteria> orCriteriaList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fileList))
        {
            fileList.forEach(file ->
                    criteriaList.add(Criteria.where("rel_path").regex(String.format("%s%s%s", ".*", file, ".*")))
            );
            orCriteriaList.add(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
            originalQuery.addCriteria(new Criteria().andOperator(orCriteriaList.toArray(new Criteria[0])));
        }

        //查询总的数量，并且过滤计数
        List<CCNDefectEntity> originalCCNDefectList = mongoTemplate.find(originalQuery, CCNDefectEntity.class);
        Iterator<CCNDefectEntity> it = originalCCNDefectList.iterator();
        while (it.hasNext())
        {
            CCNDefectEntity ccnDefectEntity = it.next();
            fillingRiskFactor(ccnDefectEntity, riskFactorConfMap);
            int riskFactor = ccnDefectEntity.getRiskFactor();
            //5.根据缺陷类型，当前处理人，文件类型过滤之后，需要按照严重程度统计缺陷数量
            if (ComConstants.RiskFactor.SH.value() == riskFactor)
            {
                seriousCheckerCount++;
            }
            else if (ComConstants.RiskFactor.H.value() == riskFactor)
            {
                normalCheckerCount++;
            }
            else if (ComConstants.RiskFactor.M.value() == riskFactor)
            {
                promptCheckerCount++;
            }
            boolean meetSeverity = CollectionUtils.isNotEmpty(severity) &&
                    !severity.contains(String.valueOf(riskFactor));
            if (meetSeverity)
            {
                it.remove();
                continue;
            }
            totalCheckerCount++;
        }
        long total = originalCCNDefectList.size();
        Query finalQuery = getPremiumQuery(taskId, author);

        //组装严重等级参数
        assembleSeverityParam(severity, riskFactorConfMap, orCriteriaList);
        if (orCriteriaList.size() > 0)
        {
            finalQuery.addCriteria(new Criteria().andOperator(orCriteriaList.toArray(new Criteria[0])));
        }
        List<CCNDefectEntity> ccnDefectEntityList = mongoTemplate.find(finalQuery.with(pageable), CCNDefectEntity.class);
        if (CollectionUtils.isNotEmpty(ccnDefectEntityList))
        {
            ccnDefectEntityList.forEach(ccnDefectEntity -> fillingRiskFactor(ccnDefectEntity, riskFactorConfMap));
        }
        Page<CCNDefectEntity> ccnDefectEntityPage = new PageImpl<>(ccnDefectEntityList, pageable, total);
        CCNFileQueryRspEntity ccnFileQueryRspEntity = new CCNFileQueryRspEntity();
        ccnFileQueryRspEntity.setSuperHighCount(seriousCheckerCount);
        ccnFileQueryRspEntity.setHighCount(normalCheckerCount);
        ccnFileQueryRspEntity.setMediumCount(promptCheckerCount);
        ccnFileQueryRspEntity.setTotalCount(totalCheckerCount);
        ccnFileQueryRspEntity.setDefectList(ccnDefectEntityPage);
        return ccnFileQueryRspEntity;
    }


    /**
     * 設置屏蔽状态
     *
     * @param taskId
     * @param toolName
     * @param status
     * @return
     */
    public void updateFilePathStatus(Long taskId, String toolName, String status)
    {
        Long nowTime = System.currentTimeMillis();
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId))
                .addCriteria(Criteria.where("tool_name").is(toolName));
        Update update = new Update();
        update.set("status", status);
        update.set("exclude_time", nowTime);
        update.set("last_update_time", nowTime);
        mongoTemplate.updateMulti(query, update, CCNDefectEntity.class).isUpdateOfExisting();
    }


    /**
     * 获取原始查询条件
     *
     * @param taskId
     * @param author
     * @return
     */
    private Query getPremiumQuery(long taskId, String author)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId).and("status").is(ComConstants.DefectStatus.NEW.value()));

        //作者过滤
        if (StringUtils.isNotEmpty(author))
        {
            query.addCriteria(Criteria.where("author").is(author));
        }
        return query;
    }


    /**
     * 根据风险系数配置给告警方法赋值风险系数
     *
     * @param riskFactorConfMap
     */
    private void fillingRiskFactor(CCNDefectEntity ccnDefectEntity, Map<String, String> riskFactorConfMap)
    {
        if (riskFactorConfMap == null)
        {
            logger.error("Has not init risk factor config!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"风险系数"}, null);
        }
        int sh = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.SH.name()));
        int h = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.H.name()));
        int m = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.M.name()));

        int ccn = ccnDefectEntity.getCcn();
        if (ccn >= m && ccn < h)
        {
            ccnDefectEntity.setRiskFactor(ComConstants.RiskFactor.M.value());
        }
        else if (ccn >= h && ccn < sh)
        {
            ccnDefectEntity.setRiskFactor(ComConstants.RiskFactor.H.value());
        }
        else if (ccn >= sh)
        {
            ccnDefectEntity.setRiskFactor(ComConstants.RiskFactor.SH.value());
        }
    }

    /**
     * 拼接严重等级参数
     *
     * @param severity
     * @param riskFactorConfMap
     * @param orCriteria
     */
    private void assembleSeverityParam(List<String> severity, Map<String, String> riskFactorConfMap,
                                       List<Criteria> orCriteria)
    {
        if (CollectionUtils.isNotEmpty(severity))
        {
            List<Criteria> criteriaList = new ArrayList<>();
            int sh = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.SH.name()));
            int h = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.H.name()));
            int m = Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.M.name()));
            severity.forEach(sev ->
            {
                if (Integer.valueOf(sev) == ComConstants.RiskFactor.SH.value())
                {
                    criteriaList.add(Criteria.where("ccn").gte(sh));
                }
                else if (Integer.valueOf(sev) == ComConstants.RiskFactor.H.value())
                {
                    criteriaList.add(Criteria.where("ccn").gte(h).lt(sh));
                }
                else if (Integer.valueOf(sev) == ComConstants.RiskFactor.M.value())
                {
                    criteriaList.add(Criteria.where("ccn").gte(m).lt(h));
                }
            });
            orCriteria.add(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        }
        else
        {
            orCriteria.add(Criteria.where("ccn").gte(Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.M.name()))));
        }
    }


    public void upsertCCNDefectListBySignature(List<CCNDefectEntity> ccnDefectEntityList)
    {
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CCNDefectEntity.class);
        if (CollectionUtils.isNotEmpty(ccnDefectEntityList))
        {
            for (CCNDefectEntity ccnDefectEntity : ccnDefectEntityList)
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("func_signature").is(ccnDefectEntity.getFuncSignature()).
                        and("task_id").is(ccnDefectEntity.getTaskId()));
                Update update = new Update();
                update.set("func_signature", ccnDefectEntity.getFuncSignature())
                        .set("task_id", ccnDefectEntity.getTaskId())
                        .set("function_name", ccnDefectEntity.getFunctionName())
                        .set("long_name", ccnDefectEntity.getLongName())
                        .set("ccn", ccnDefectEntity.getCcn())
                        .set("latest_datetime", ccnDefectEntity.getLatestDateTime())
                        .set("author", ccnDefectEntity.getAuthor())
                        .set("start_lines", ccnDefectEntity.getStartLines())
                        .set("end_lines", ccnDefectEntity.getEndLines())
                        .set("total_lines", ccnDefectEntity.getTotalLines())
                        .set("condition_lines", ccnDefectEntity.getConditionLines())
                        .set("status", ccnDefectEntity.getStatus())
                        .set("create_time", ccnDefectEntity.getCreateTime())
                        .set("fixed_time", ccnDefectEntity.getFixedTime())
                        .set("ignore_time", ccnDefectEntity.getIgnoreTime())
                        .set("exclude_time", ccnDefectEntity.getExcludeTime())
                        .set("rel_path", ccnDefectEntity.getRelPath())
                        .set("file_path", ccnDefectEntity.getFilePath())
                        .set("url", ccnDefectEntity.getUrl())
                        .set("repo_id", ccnDefectEntity.getRepoId())
                        .set("revision", ccnDefectEntity.getRevision())
                        .set("branch", ccnDefectEntity.getBranch())
                        .set("sub_module", ccnDefectEntity.getSubModule())
                        .set("analysis_version", ccnDefectEntity.getAnalysisVersion());
                ops.upsert(query, update);
            }
            ops.execute();
        }
    }


}
