package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.pojo.CustomTriggerPipelineModel;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp;
import com.tencent.bk.codecc.task.service.GongfengTriggerService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GongfengTriggerServiceImpl implements GongfengTriggerService {
    @NotNull
    @Override
    public TriggerPipelineRsp triggerCustomProjectPipeline(@NotNull TriggerPipelineReq triggerPipelineReq,
                                                           @NotNull String appCode, @NotNull String userId) {
        return null;
    }

    @Override
    public void manualStartupCustomPipeline(@NotNull CustomTriggerPipelineModel customTriggerPipelineModel) {

    }

    @Nullable
    @Override
    public String triggerGongfengTaskByRepoId(@NotNull String repoId, @Nullable String commitId) {
        return null;
    }

    @Override
    public boolean createTaskByRepoId(@NotNull String repoId, @NotNull List<String> langs) {
        return false;
    }

    @Override
    public void stopRunningApiTask(@NotNull String codeccBuildId, @NotNull String appCode, @NotNull String userId) {

    }
}
