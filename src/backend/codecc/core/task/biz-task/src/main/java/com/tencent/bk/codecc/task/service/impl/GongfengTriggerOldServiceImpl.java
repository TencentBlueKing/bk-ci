package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldReq;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldRsp;
import com.tencent.bk.codecc.task.service.GongfengTriggerOldService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class GongfengTriggerOldServiceImpl implements GongfengTriggerOldService {
    @NotNull
    @Override
    public TriggerPipelineOldRsp triggerCustomProjectPipeline(@NotNull TriggerPipelineOldReq triggerPipelineReq,
                                                              @NotNull String userId) {
        return null;
    }
}
