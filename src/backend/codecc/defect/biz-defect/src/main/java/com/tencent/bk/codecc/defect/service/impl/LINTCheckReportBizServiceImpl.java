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
package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectRepository;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.model.LintSnapShotEntity;
import com.tencent.bk.codecc.defect.model.NotRepairedAuthorEntity;
import com.tencent.bk.codecc.defect.model.common.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.LintLastAnalysisResultVO;
import com.tencent.devops.common.api.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * lint类工具组装分析产出物报告逻辑
 *
 * @version V1.0
 * @date 2019/6/28
 */
@Service("LINTCheckerReportBizService")
public class LINTCheckReportBizServiceImpl implements ICheckReportBizService
{
    private static Logger logger = LoggerFactory.getLogger(LINTCheckReportBizServiceImpl.class);

    @Autowired
    private Client client;

    @Autowired
    private TaskLogService taskLogService;

    @Autowired
    private LintDefectRepository lintDefectRepository;

    @Value("${devopsGateway.host:#{null}}")
    private String devopsHost;

    @Override
    public ToolSnapShotEntity getReport(long taskId, String projectId, String toolName)
    {
        LintSnapShotEntity lintSnapShotEntity = new LintSnapShotEntity();

        handleToolBaseInfo(lintSnapShotEntity, taskId, toolName, projectId);

        //最近一次分析概要信息
        List<ToolLastAnalysisResultVO> lastAnalysisResultVOList = taskLogService.getLastAnalysisResults(taskId, new HashSet<String>()
        {{
            add(toolName);
        }});
        if (CollectionUtils.isEmpty(lastAnalysisResultVOList))
        {
            logger.info("no analysis result found! task id: {}, tool name: {}", taskId, toolName);
            return lintSnapShotEntity;
        }
        ToolLastAnalysisResultVO toolLastAnalysisResultVO = lastAnalysisResultVOList.get(0);
        LintLastAnalysisResultVO baseLastAnalysisResultVO = (LintLastAnalysisResultVO) toolLastAnalysisResultVO.getLastAnalysisResultVO();
        lintSnapShotEntity.setNewFileTotalDefectCount(baseLastAnalysisResultVO.getDefectCount());
        lintSnapShotEntity.setNewFileChangedDefectCount(baseLastAnalysisResultVO.getDefectChange());
        lintSnapShotEntity.setNewFileTotalCount(baseLastAnalysisResultVO.getFileCount());
        lintSnapShotEntity.setNewFileChangedCount(baseLastAnalysisResultVO.getFileChange());

        //统计不同等级告警数量
        List<LintFileEntity> lintFileEntityList = lintDefectRepository.findByTaskIdAndToolName(taskId, toolName);
        if (CollectionUtils.isEmpty(lintFileEntityList))
        {
            logger.info("lint data report content is empty! task id: {}", taskId);
            return lintSnapShotEntity;
        }
        Map<Integer, Long> statisticsMap = lintFileEntityList.stream().
                flatMap(lintFileEntity ->
                        lintFileEntity.getDefectList().stream()
                ).
                peek(lintDefectEntity ->
                {
                    if (ComConstants.PROMPT_IN_DB == lintDefectEntity.getSeverity())
                    {
                        lintDefectEntity.setSeverity(ComConstants.PROMPT);
                    }
                }).
                collect(Collectors.groupingBy(LintDefectEntity::getSeverity,
                        Collectors.counting()
                ));
        lintSnapShotEntity.setTotalNewSerious(statisticsMap.containsKey(ComConstants.SERIOUS) ? statisticsMap.get(ComConstants.SERIOUS).intValue() : 0);
        lintSnapShotEntity.setTotalNewNormal(statisticsMap.containsKey(ComConstants.NORMAL) ? statisticsMap.get(ComConstants.NORMAL).intValue() : 0);
        lintSnapShotEntity.setTotalNewPrompt(statisticsMap.containsKey(ComConstants.PROMPT) ? statisticsMap.get(ComConstants.PROMPT).intValue() : 0);


        //待修复告警作者
        //1、查询所有作者对应的告警数量，并进行排序
        List<NotRepairedAuthorEntity> authorDefectList = lintFileEntityList.stream().flatMap(lintFileEntity ->
                lintFileEntity.getDefectList().stream()
        ).
                filter(lintDefectEntity ->
                        lintDefectEntity.getStatus() == ComConstants.DefectStatus.NEW.value()
                ).
                collect(Collectors.groupingBy(LintDefectEntity::getAuthor, Collectors.groupingBy(LintDefectEntity::getSeverity, Collectors.counting()))).
                entrySet().
                stream().
                map(stringMapEntry ->
                {
                    int seriousCount = stringMapEntry.getValue().containsKey(ComConstants.SERIOUS) ? stringMapEntry.getValue().get(ComConstants.SERIOUS).intValue() : 0;
                    int normalCount = stringMapEntry.getValue().containsKey(ComConstants.NORMAL) ? stringMapEntry.getValue().get(ComConstants.NORMAL).intValue() : 0;
                    int promptCount = stringMapEntry.getValue().containsKey(ComConstants.PROMPT) ? stringMapEntry.getValue().get(ComConstants.PROMPT).intValue() : 0;
                    NotRepairedAuthorEntity notRepairedAuthorEntity = new NotRepairedAuthorEntity();
                    notRepairedAuthorEntity.setName(stringMapEntry.getKey());
                    notRepairedAuthorEntity.setSeriousCount(seriousCount);
                    notRepairedAuthorEntity.setNormalCount(normalCount);
                    notRepairedAuthorEntity.setPromptCount(promptCount);
                    notRepairedAuthorEntity.setTotalCount(seriousCount + normalCount + promptCount);
                    return notRepairedAuthorEntity;
                }).
                sorted((o1, o2) -> o2.getTotalCount() - o1.getTotalCount()).
                collect(Collectors.toList());
        lintSnapShotEntity.setAuthorList(authorDefectList);
        return lintSnapShotEntity;
    }


    private void handleToolBaseInfo(LintSnapShotEntity lintSnapShotEntity, long taskId, String toolName, String projectId)
    {
        //获取工具信息
        Result<ToolConfigInfoVO> toolResult = client.get(ServiceToolRestResource.class).getToolWithNameByTaskIdAndName(taskId, toolName);
        if (toolResult.isNotOk() || null == toolResult.getData())
        {
            logger.error("get tool config info fail! task id: {}, tool name: {}", taskId, toolName);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        ToolConfigInfoVO toolConfigInfoVO = toolResult.getData();
        lintSnapShotEntity.setToolNameCn(toolConfigInfoVO.getDisplayName());
        lintSnapShotEntity.setToolNameEn(toolName);
        if (StringUtils.isNotEmpty(projectId))
        {
            String defectDetailUrl = String.format("http://%s/console/codecc/%s/task/%d/defect/lint/%s/list", devopsHost, projectId, taskId, toolName.toUpperCase());
            lintSnapShotEntity.setDefectDetailUrl(defectDetailUrl);
            String defectReportUrl = String.format("http://%s/console/codecc/%s/task/%d/defect/lint/%s/charts", devopsHost, projectId, taskId, toolName.toUpperCase());
            lintSnapShotEntity.setDefectReportUrl(defectReportUrl);
        }


    }
}
