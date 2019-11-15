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
import com.tencent.bk.codecc.defect.model.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.DUPCFileQueryRspEntity;
import com.tencent.bk.codecc.defect.vo.CodeBlockVO;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * DUPC类告警的查詢持久化
 *
 * @version V1.0
 * @date 2019/5/21
 */
@Repository
public class DUPCDefectDao
{
    private static Logger logger = LoggerFactory.getLogger(DUPCDefectDao.class);

    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * 通过参数查询重复率文件信息
     *
     * @param taskId
     * @param fileList
     * @param author
     * @param severity
     * @param riskConfigMap
     * @param pageable
     * @return
     */
    public DUPCFileQueryRspEntity findDUPCFileByParam(long taskId, List<String> fileList, String author, List<String> severity, Map<String, String> riskConfigMap, Pageable pageable)
    {
        // 需要统计的数据
        int seriousCheckerCount = 0;
        int normalCheckerCount = 0;
        int promptCheckerCount = 0;
        int totalCheckerCount = 0;

        Query originalQuery = getPremiumQuery(taskId, author);

        //1路径过滤
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

        // 查询总的数量，并且过滤计数
        List<DUPCDefectEntity> defectList = mongoTemplate.find(originalQuery, DUPCDefectEntity.class);
        Iterator<DUPCDefectEntity> it = defectList.iterator();
        while (it.hasNext())
        {
            DUPCDefectEntity defectEntity = it.next();

            // 处理人条件不为空且不等于缺陷的处理人时，判断为true移除，否则false不移除
            /*boolean meetAuthor = matchAuthorList(defectEntity.getAuthorList(), author);
            if (!meetAuthor)
            {
                continue;
            }*/

            // 根据当前处理人，文件过滤之后，需要按照严重程度统计缺陷数量
            fillingRiskFactor(defectEntity, riskConfigMap);
            int riskFactor = defectEntity.getRiskFactor();
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

        long total = defectList.size();
        Query finalQuery = getPremiumQuery(taskId, author);

        //组装严重等级参数
        assembleSeverityParam(severity, riskConfigMap, orCriteriaList);
        if (orCriteriaList.size() > 0)
        {
            finalQuery.addCriteria(new Criteria().andOperator(orCriteriaList.toArray(new Criteria[0])));
        }

        List<DUPCDefectEntity> dupcDefectEntityList = mongoTemplate.find(finalQuery.with(pageable), DUPCDefectEntity.class);
        if (CollectionUtils.isNotEmpty(dupcDefectEntityList))
        {
            dupcDefectEntityList.forEach(dupcDefectEntity ->
            {
                fillingRiskFactor(dupcDefectEntity, riskConfigMap);
                setFileName(dupcDefectEntity);
            });
        }

        Page<DUPCDefectEntity> dupcDefectEntityPage = new PageImpl<>(dupcDefectEntityList, pageable, total);
        DUPCFileQueryRspEntity dupcFileQueryRspEntity = new DUPCFileQueryRspEntity();
        dupcFileQueryRspEntity.setSuperHighCount(seriousCheckerCount);
        dupcFileQueryRspEntity.setHighCount(normalCheckerCount);
        dupcFileQueryRspEntity.setMediumCount(promptCheckerCount);
        dupcFileQueryRspEntity.setTotalCount(totalCheckerCount);
        dupcFileQueryRspEntity.setDefectList(dupcDefectEntityPage);
        return dupcFileQueryRspEntity;
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
        BasicDBObject fieldsObj = new BasicDBObject();
        fieldsObj.put("blockList", false);
        Query query = new BasicQuery(new BasicDBObject(), fieldsObj);
        query.addCriteria(Criteria.where("task_id").is(taskId).and("status").is(ComConstants.DefectStatus.NEW.value()));

        //作者过滤
        if (StringUtils.isNotEmpty(author))
        {
            query.addCriteria(Criteria.where("author_list").regex(String.format("%s%s%s", ".*", author, ".*")));
        }
        return query;
    }


    /**
     * 根据风险系数配置给告警方法赋值风险系数
     *
     * @param riskFactorConfMap
     */
    private void fillingRiskFactor(DUPCDefectEntity dupcDefectEntity, Map<String, String> riskFactorConfMap)
    {
        if (riskFactorConfMap == null)
        {
            logger.error("Has not init risk factor config!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"风险系数"}, null);
        }
        Float sh = Float.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.SH.name()));
        Float h = Float.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.H.name()));
        Float m = Float.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.M.name()));

        String dupRateStr = dupcDefectEntity.getDupRate();
        float dupRate = Float.valueOf(StringUtils.isEmpty(dupRateStr) ? "0" : dupRateStr.substring(0, dupRateStr.length() - 1));
        if (dupRate >= m && dupRate < h)
        {
            dupcDefectEntity.setRiskFactor(ComConstants.RiskFactor.M.value());
        }
        else if (dupRate >= h && dupRate < sh)
        {
            dupcDefectEntity.setRiskFactor(ComConstants.RiskFactor.H.value());
        }
        else if (dupRate >= sh)
        {
            dupcDefectEntity.setRiskFactor(ComConstants.RiskFactor.SH.value());
        }
    }

    /**
     * 设置告警文件名
     *
     * @param dupcDefectEntity
     */
    private void setFileName(DUPCDefectEntity dupcDefectEntity)
    {
        String filePath = dupcDefectEntity.getFilePath();
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1)
        {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        dupcDefectEntity.setFileName(filePath.substring(fileNameIndex + 1));
    }


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
        query.addCriteria(Criteria.where("task_id").is(taskId).and("status").is(DefectConstants.DefectStatus.NEW.value()).and("block_list.finger_print").in(fingerPrintList));

        return mongoTemplate.find(query, DUPCDefectEntity.class);
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
            Float sh = Float.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.SH.name()));
            Float h = Float.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.H.name()));
            Float m = Float.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.M.name()));
            severity.forEach(sev ->
            {
                if (Integer.valueOf(sev) == ComConstants.RiskFactor.SH.value())
                {
                    criteriaList.add(Criteria.where("dup_rate_value").gte(sh));
                }
                else if (Integer.valueOf(sev) == ComConstants.RiskFactor.H.value())
                {
                    criteriaList.add(Criteria.where("dup_rate_value").gte(h).lt(sh));
                }
                else if (Integer.valueOf(sev) == ComConstants.RiskFactor.M.value())
                {
                    criteriaList.add(Criteria.where("dup_rate_value").gte(m).lt(h));
                }
            });
            orCriteria.add(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        }
        else
        {
            orCriteria.add(Criteria.where("dup_rate_value").gte(Integer.valueOf(riskFactorConfMap.get(ComConstants.RiskFactor.M.name()))));
        }
    }
}
