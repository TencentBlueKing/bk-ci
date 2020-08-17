package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.service.AbstractAnalyzeTaskBizService;
import com.tencent.bk.codecc.defect.service.ISubsequentProcBizService;
import com.tencent.bk.codecc.defect.service.PipelineService;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service("CommonSubsequentProcService")
public class CommonSubsequentProcBizServiceImpl extends AbstractAnalyzeTaskBizService implements ISubsequentProcBizService {

    @Autowired
    private PipelineService pipelineService;

    @Override
    public void analyzeSendWebSocketMsg(ToolConfigBaseVO toolConfigBaseVO, UploadTaskLogStepVO uploadTaskLogStepVO,
                                        TaskLogEntity taskLogEntity, TaskDetailVO taskDetailVO, long taskId, String toolName) {
        sendWebSocketMsg(toolConfigBaseVO, uploadTaskLogStepVO, taskLogEntity, taskDetailVO, taskId, toolName);
    }

    @Override
    public void analyzePiplineHandleDevopsCallBack(TaskLogEntity lastTaskLogEntity, TaskLogEntity.TaskUnit taskStep, String toolName, TaskDetailVO taskVO) {
        pipelineService.handleDevopsCallBack(lastTaskLogEntity, taskStep, toolName, taskVO);
    }


    @Override
    protected void preHandleDefectsAndStatistic(UploadTaskLogStepVO uploadTaskLogStepVO, String analysisVersion) {

    }

    @Override
    protected void postHandleDefectsAndStatistic(UploadTaskLogStepVO uploadTaskLogStepVO, TaskDetailVO taskVO) {

    }

    @Override
    public int getSubmitStepNum() {
        return 0;
    }

    @Override
    public int getCodeDownloadStepNum() {
        return 0;
    }
}
