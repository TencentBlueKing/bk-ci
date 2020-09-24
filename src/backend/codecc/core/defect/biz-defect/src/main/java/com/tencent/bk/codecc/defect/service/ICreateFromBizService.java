package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;

public interface ICreateFromBizService {

    boolean isNeedToSendWebSocketMsg();

    void analyzePiplineHandleDevopsCallBack(TaskLogEntity lastTaskLogEntity, TaskLogEntity.TaskUnit taskStep, String toolName, TaskDetailVO taskVO);

}
