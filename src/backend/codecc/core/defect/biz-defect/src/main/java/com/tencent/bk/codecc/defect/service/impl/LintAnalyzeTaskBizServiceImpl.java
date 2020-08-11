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

import com.tencent.bk.codecc.defect.service.AbstractAnalyzeTaskBizService;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 任务分析记录服务层实现
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service("LINTAnalyzeTaskBizService")
@Slf4j
public class LintAnalyzeTaskBizServiceImpl extends AbstractAnalyzeTaskBizService
{
    @Autowired
    private ScmFileInfoService scmFileInfoService;

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
    protected void postHandleDefectsAndStatistic(UploadTaskLogStepVO uploadTaskLogStepVO, TaskDetailVO taskVO)
    {
        log.info("begin [ lint tool ] analyze task biz service postHandleDefectsAndStatistic ...");
        long taskId = uploadTaskLogStepVO.getTaskId();
        String toolName = uploadTaskLogStepVO.getToolName();

        // 代码扫描步骤结束，则开始变更告警的状态并统计本次分析的告警信息
        if (uploadTaskLogStepVO.getStepNum() == ComConstants.Step4MutliTool.SCAN.value()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value())
        {
            log.info("begin commit defect.");
            asyncCommitDefect(uploadTaskLogStepVO, taskVO);
        }
        else if (uploadTaskLogStepVO.getStepNum() == getSubmitStepNum()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value())
        {
            log.info("begin statistic defect count.");
            handleSubmitSuccess(uploadTaskLogStepVO, taskVO);
        }
    }

    private void handleSubmitSuccess(UploadTaskLogStepVO uploadTaskLogStepVO, TaskDetailVO taskVO)
    {
        long taskId = uploadTaskLogStepVO.getTaskId();
        String toolName = uploadTaskLogStepVO.getToolName();

        // 保存首次分析成功时间
        saveFirstSuccessAnalyszeTime(taskId, toolName);

        // 清除强制全量扫描标志
        clearForceFullScan(taskId, toolName);

        // 处理md5.json
        scmFileInfoService.parseFileInfo(uploadTaskLogStepVO.getTaskId(),
            uploadTaskLogStepVO.getStreamName(),
            uploadTaskLogStepVO.getToolName(),
            uploadTaskLogStepVO.getPipelineBuildId());
    }


    @Override
    public int getSubmitStepNum()
    {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }

    @Override
    public int getCodeDownloadStepNum()
    {
        return ComConstants.Step4MutliTool.DOWNLOAD.value();
    }
}
