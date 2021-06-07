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

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.service.AbstractAnalyzeTaskBizService;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * cloc工具分析服务实现
 *
 * @version V1.0
 * @date 2019/10/14
 */
@Slf4j
@Service("CLOCAnalyzeTaskBizService")
public class CLOCAnalyzeTaskBizServiceImpl extends AbstractAnalyzeTaskBizService
{
    @Override
    protected void postHandleDefectsAndStatistic(UploadTaskLogStepVO uploadTaskLogStepVO, TaskDetailVO taskVO)
    {
        // 代码扫描步骤结束，则开始变更告警的状态并统计本次分析的告警信息
        if (uploadTaskLogStepVO.getStepNum() == ComConstants.Step4MutliTool.SCAN.value()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value()
                && !uploadTaskLogStepVO.isFastIncrement())
        {
            log.info("begin commit defect.");
            asyncCommitDefect(uploadTaskLogStepVO, taskVO);
        }
        else if (uploadTaskLogStepVO.getStepNum() == getSubmitStepNum()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value()
                && !uploadTaskLogStepVO.isFastIncrement())
        {
            log.info("begin to set cloc full scan type, task_id: {}", uploadTaskLogStepVO.getTaskId());
            clearForceFullScan(uploadTaskLogStepVO.getTaskId(), uploadTaskLogStepVO.getToolName());
            uploadTaskLogStepVO.setFinish(true);
        }
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
