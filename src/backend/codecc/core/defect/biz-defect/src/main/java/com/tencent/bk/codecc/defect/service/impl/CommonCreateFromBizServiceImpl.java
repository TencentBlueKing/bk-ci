package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.service.ICreateFromBizService;
import com.tencent.bk.codecc.defect.service.PipelineService;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service("CommonSubsequentProcBizService")
public class CommonCreateFromBizServiceImpl implements ICreateFromBizService {

    @Autowired
    private PipelineService pipelineService;

    @Override
    public boolean isNeedToSendWebSocketMsg() {
        return true;
    }

    @Override
    public void analyzePiplineHandleDevopsCallBack(TaskLogEntity lastTaskLogEntity, TaskLogEntity.TaskUnit taskStep, String toolName, TaskDetailVO taskVO) {
        pipelineService.handleDevopsCallBack(lastTaskLogEntity, taskStep, toolName, taskVO);
    }
}
