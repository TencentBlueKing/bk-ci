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
package com.tencent.bk.codecc.defect.service.impl.pipelinereport;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.LintStatisticEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.LintSnapShotEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * lint类工具组装分析产出物报告逻辑
 *
 * @version V1.0
 * @date 2019/6/28
 */
@Service("LINTCheckerReportBizService")
@Slf4j
public class LINTCheckReportBizServiceImpl implements ICheckReportBizService
{
    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    @Value("${bkci.public.url:#{null}}")
    private String devopsHost;

    @Autowired
    private NewDefectJudgeService newDefectJudgeService;

    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;

    @Override
    public ToolSnapShotEntity getReport(long taskId, String projectId, String toolName, String buildId)
    {
        LintSnapShotEntity lintSnapShotEntity = new LintSnapShotEntity();

        handleToolBaseInfo(lintSnapShotEntity, taskId, toolName, projectId, buildId);

        DefectQueryReqVO defectQueryReqVO = new DefectQueryReqVO();
        defectQueryReqVO.setToolName(toolName);
        // 只需要查询状态为new的告警
        defectQueryReqVO.setStatus(Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value())));
        Map<String, Boolean> filedMap = new HashMap<>();
        filedMap.put("_id", true);
        filedMap.put("severity", true);
        filedMap.put("line_update_time", true);
        filedMap.put("create_time", true);
        List<LintDefectV2Entity> defectEntityList = lintDefectV2Dao.findDefectByCondition(taskId, defectQueryReqVO, null, null, 0L, filedMap, null);
        int seriousCount = 0;
        int normalCount = 0;
        int promptCount = 0;
        int totalSeriousCount = 0;
        int totalNormalCount = 0;
        int totalPromptCount = 0;
        if (CollectionUtils.isNotEmpty(defectEntityList))
        {
            // 查询新老告警判定时间
            long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, toolName, null);
            for (LintDefectV2Entity defect : defectEntityList)
            {
                long defectLastUpdateTime = DateTimeUtils.getThirteenTimestamp(defect.getLineUpdateTime());
                int severity = defect.getSeverity();
                if (severity == ComConstants.PROMPT_IN_DB)
                {
                    severity = ComConstants.PROMPT;
                }
                switch (severity)
                {
                    case ComConstants.SERIOUS:
                    {
                        totalSeriousCount++;
                        if (defectLastUpdateTime >= newDefectJudgeTime)
                        {
                            seriousCount++;
                        }
                        break;
                    }
                    case ComConstants.NORMAL:
                    {
                        totalNormalCount++;
                        if (defectLastUpdateTime >= newDefectJudgeTime)
                        {
                            normalCount++;
                        }
                        break;
                    }
                    case ComConstants.PROMPT:
                    {
                        totalPromptCount++;
                        if (defectLastUpdateTime >= newDefectJudgeTime)
                        {
                            promptCount++;
                        }
                        break;
                    }
                }
            }
        }

        //最近一次分析概要信息
        LintStatisticEntity lintStatisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        if (lintStatisticEntity == null)
        {
            log.info("no analysis result found! task id: {}, tool name: {}", taskId, toolName);
            return lintSnapShotEntity;
        }
        lintSnapShotEntity.setNewFileTotalDefectCount(lintStatisticEntity.getDefectCount());
        lintSnapShotEntity.setNewFileChangedDefectCount(lintStatisticEntity.getDefectChange());
        lintSnapShotEntity.setNewFileTotalCount(lintStatisticEntity.getFileCount());
        lintSnapShotEntity.setNewFileChangedCount(lintStatisticEntity.getFileChange());

        //统计不同等级告警数量
        lintSnapShotEntity.setTotalNewSerious(seriousCount);
        lintSnapShotEntity.setTotalNewNormal(normalCount);
        lintSnapShotEntity.setTotalNewPrompt(promptCount);

        //统计不同等级告警总量
        lintSnapShotEntity.setTotalSerious(totalSeriousCount);
        lintSnapShotEntity.setTotalNormal(totalNormalCount);
        lintSnapShotEntity.setTotalPrompt(totalPromptCount);

        //待修复告警作者
        lintSnapShotEntity.setAuthorList(lintStatisticEntity.getAuthorStatistic());
        return lintSnapShotEntity;
    }


    private void handleToolBaseInfo(LintSnapShotEntity lintSnapShotEntity, long taskId, String toolName, String projectId, String buildId)
    {
        //获取工具信息
        lintSnapShotEntity.setToolNameCn(toolMetaCacheService.getToolDisplayName(toolName));
        lintSnapShotEntity.setToolNameEn(toolName);
        if (StringUtils.isNotEmpty(projectId))
        {
            String defectDetailUrl = String.format("%s/console/codecc/%s/task/%d/defect/lint/%s/list?buildId=%s",
                    devopsHost, projectId, taskId,
                    toolName.toUpperCase(), buildId);
            lintSnapShotEntity.setDefectDetailUrl(defectDetailUrl);
            String defectReportUrl = String.format("%s/console/codecc/%s/task/%d/defect/lint/%s/charts",
                     devopsHost, projectId, taskId,
                    toolName.toUpperCase());
            lintSnapShotEntity.setDefectReportUrl(defectReportUrl);
        }


    }
}
