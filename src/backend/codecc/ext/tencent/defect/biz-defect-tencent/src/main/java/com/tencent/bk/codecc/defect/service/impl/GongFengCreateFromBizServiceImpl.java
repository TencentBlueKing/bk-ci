package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.service.ICreateFromBizService;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import org.springframework.stereotype.Service;


@Service("GongFengSubsequentProcBizService")
public class GongFengCreateFromBizServiceImpl implements ICreateFromBizService {

    /**
     * 创建项目来源为工蜂的不需要发送webSocket信息
     * @return
     */
    @Override
    public boolean isNeedToSendWebSocketMsg() {
        return false;
    }

    /**
     * 创建项目来源为工蜂的不需要进行流水线回调
     * @param lastTaskLogEntity
     * @param taskStep
     * @param toolName
     * @param taskVO
     */
    @Override
    public void analyzePiplineHandleDevopsCallBack(TaskLogEntity lastTaskLogEntity, TaskLogEntity.TaskUnit taskStep,
                                                   String toolName, TaskDetailVO taskVO) {
    }
}
