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

import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.dao.mongorepository.FirstAnalysisSuccessTimeRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectDao;
import com.tencent.bk.codecc.defect.model.FirstAnalysisSuccessEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.model.LintStatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractAnalyzeTaskBizService;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务分析记录服务层实现
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service("LINTAnalyzeTaskBizService")
public class LintAnalyzeTaskBizServiceImpl extends AbstractAnalyzeTaskBizService
{
    private static Logger logger = LoggerFactory.getLogger(LintAnalyzeTaskBizServiceImpl.class);

    @Autowired
    private LintDefectRepository lintDefectRepository;

    @Autowired
    private FirstAnalysisSuccessTimeRepository firstSuccessTimeRepository;

    @Autowired
    private LintDefectDao lintDefectDao;

    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    @Override
    protected void preHandleDefectsAndStatistic(UploadTaskLogStepVO uploadTaskLogStepVO, String analysisVersion)
    {
        // Lint类工具无需告警预处理
    }

    /**
     * 代码提交步骤需要刷新文件和告警，并更新统计数据
     *
     * @param uploadTaskLogStepVO
     */
    @Override
    protected void postHandleDefectsAndStatistic(UploadTaskLogStepVO uploadTaskLogStepVO)
    {
        long taskId = uploadTaskLogStepVO.getTaskId();
        String toolName = uploadTaskLogStepVO.getToolName();

        // 提交告警步骤结束，变更告警的状态并统计本次分析的告警信息
        if (uploadTaskLogStepVO.getStepNum() == ComConstants.Step4MutliTool.DEFECT_SUBMI.value() && uploadTaskLogStepVO.getFlag() == DefectConstants.TASK_FLAG_SUCC)
        {
            /* 需要统计的信息：
             * 1.本次分析遗留告警总数，文件总数，用于跟上一次分析的结果比较，得到最近一次分析结果，用于项目详情页展示，例如： 告警88247(↑38) 文件1796(↑0)
             * 2.当前遗留新告警数，历史告警数，用于数据报表统计每日告警遗留趋势图
             */
            int defectCount = 0;
            int fileCount = 0;
            int newDefectCount = 0;
            int historyDefectCount = 0;

            // 需要更新状态的文件
            List<LintFileEntity> needUpdateFileEntityList = new ArrayList<>();

            List<LintFileEntity> lintFileEntityList = lintDefectRepository.findFileByTaskIdAndToolName(taskId, toolName);
            if (CollectionUtils.isNotEmpty(lintFileEntityList))
            {
                String currentAnalysisVersion = taskAnalysisDao.getCurrentAnalysisVersion(taskId, toolName);
                for (LintFileEntity fileEntity : lintFileEntityList)
                {
                    String analysisVersion = fileEntity.getAnalysisVersion();
                    int status = fileEntity.getStatus();

                    if (status == DefectConstants.DefectStatus.NEW.value())
                    {
                        // 如果文件的状态为new，且如果文件的上报版本号不等于当前的版本号，则认为文件是已经被修复的
                        if (StringUtils.isEmpty(analysisVersion) || !analysisVersion.equals(currentAnalysisVersion))
                        {
                            fileEntity.setStatus(status | DefectConstants.DefectStatus.FIXED.value());
                            fileEntity.setFixedTime(System.currentTimeMillis());
                            needUpdateFileEntityList.add(fileEntity);
                        }
                        // 否则统计状态为new的文件的告警数据
                        else
                        {
                            fileCount++;
                            defectCount += fileEntity.getDefectCount();
                            newDefectCount += fileEntity.getNewCount();
                            historyDefectCount += fileEntity.getHistoryCount();
                        }
                    }
                }
            }

            // 更新状态的文件
            lintDefectDao.batchUpdateFileToFixed(needUpdateFileEntityList);

            // 保存本次分析的统计情况
            saveStatisticInfo(taskId, toolName, defectCount, fileCount, newDefectCount, historyDefectCount);

            //设置首次成功扫描完成时间为区分文件新增与历史的时间
            FirstAnalysisSuccessEntity firstSuccessTimeEntity = firstSuccessTimeRepository.findFirstByTaskIdAndToolName(taskId, toolName);
            if (firstSuccessTimeEntity == null)
            {
                firstSuccessTimeEntity = new FirstAnalysisSuccessEntity();
                firstSuccessTimeEntity.setTaskId(taskId);
                firstSuccessTimeEntity.setToolName(toolName);
                firstSuccessTimeEntity.setFirstAnalysisSuccessTime(System.currentTimeMillis());
                firstSuccessTimeRepository.save(firstSuccessTimeEntity);
                logger.debug("lint task first analysis success:{}, {}", taskId, toolName);
            }
        }
    }

    /**
     * 保存本次分析的统计情况
     *
     * @param taskId
     * @param toolName
     * @param defectCount
     * @param fileCount
     * @param newDefectCount
     * @param historyDefectCount
     */
    private void saveStatisticInfo(long taskId, String toolName, int defectCount, int fileCount, int newDefectCount, int historyDefectCount)
    {
        int defectChange;
        int fileChange;
        LintStatisticEntity lastLintStatisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);
        if (lastLintStatisticEntity == null)
        {
            defectChange = defectCount;
            fileChange = fileCount;
        }
        else
        {
            defectChange = defectCount - lastLintStatisticEntity.getDefectChange();
            fileChange = fileCount - lastLintStatisticEntity.getFileCount();
        }

        LintStatisticEntity lintStatisticEntity = new LintStatisticEntity();
        lintStatisticEntity.setTaskId(taskId);
        lintStatisticEntity.setToolName(toolName);
        lintStatisticEntity.setFileCount(fileCount);
        lintStatisticEntity.setDefectCount(defectCount);
        lintStatisticEntity.setNewDefectCount(newDefectCount);
        lintStatisticEntity.setHistoryDefectCount(historyDefectCount);
        lintStatisticEntity.setDefectChange(defectChange);
        lintStatisticEntity.setFileChange(fileChange);

        long currentTime = System.currentTimeMillis();
        lintStatisticEntity.setTime(currentTime);
        lintStatisticEntity.setCreatedDate(currentTime);
        lintStatisticEntity.setUpdatedDate(currentTime);
        lintStatisticEntity.setCreatedBy(ComConstants.SYSTEM_USER);
        lintStatisticEntity.setUpdatedBy(ComConstants.SYSTEM_USER);
        lintStatisticRepository.save(lintStatisticEntity);
    }

    @Override
    public int getSubmitStepNum()
    {
        return ComConstants.Step4MutliTool.DEFECT_SUBMI.value();
    }


}
