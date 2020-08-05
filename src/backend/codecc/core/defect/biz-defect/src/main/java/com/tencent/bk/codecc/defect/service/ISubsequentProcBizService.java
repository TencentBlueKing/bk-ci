package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;

public interface ISubsequentProcBizService {

    void analyzeSendWebSocketMsg(ToolConfigBaseVO toolConfigBaseVO, UploadTaskLogStepVO uploadTaskLogStepVO,
                                 TaskLogEntity taskLogEntity, TaskDetailVO taskDetailVO, long taskId, String toolName);

    void analyzePiplineHandleDevopsCallBack(TaskLogEntity lastTaskLogEntity, TaskLogEntity.TaskUnit taskStep, String toolName, TaskDetailVO taskVO);

}
