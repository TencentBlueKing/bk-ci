package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.service.AbstractAnalyzeTaskBizService;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("STATAnalyzeTaskBizService")
public class StatAnalyzeTaskBizServiceImpl extends AbstractAnalyzeTaskBizService {

    @Override
    protected void postHandleDefectsAndStatistic(UploadTaskLogStepVO uploadTaskLogStepVO, TaskDetailVO taskVO) {
        if (uploadTaskLogStepVO.getStepNum() == ComConstants.Step4MutliTool.SCAN.value()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value()
                && !uploadTaskLogStepVO.isFastIncrement()) {
            log.info("begin to scan Stat info, taskId: {}, buildId: {}, toolName: {}", uploadTaskLogStepVO.getTaskId(),
                    uploadTaskLogStepVO.getPipelineBuildId(), uploadTaskLogStepVO.getToolName());
            asyncCommitDefect(uploadTaskLogStepVO, taskVO);

        } else if (uploadTaskLogStepVO.getStepNum() == getSubmitStepNum()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value()
                && !uploadTaskLogStepVO.isFastIncrement()) {
            // 清除全量扫描标记
            clearForceFullScan(uploadTaskLogStepVO.getTaskId(), uploadTaskLogStepVO.getToolName());
            uploadTaskLogStepVO.setFinish(true);
        }
    }

    @Override
    public int getSubmitStepNum() {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }

    @Override
    public int getCodeDownloadStepNum() {
        return ComConstants.Step4MutliTool.DOWNLOAD.value();
    }
}
