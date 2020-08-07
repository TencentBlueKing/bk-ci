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

package com.tencent.bk.codecc.task.enums;

import com.tencent.bk.codecc.task.model.CustomProjEntity;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldReq;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq;
import com.tencent.bk.codecc.task.service.ICustomPipelineService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 个性化手动触发项目来源
 *
 * @date 2020/3/24
 * @version V1.0
 */
public enum CustomProjSource
{
    TEST(new ICustomPipelineService() {
        @Override
        public void updateCustomizedCheckProjPipeline(@NotNull TriggerPipelineOldReq triggerPipelineReq, long taskId, @Nullable String userId, @NotNull String projectId, @NotNull String pipelineId) {
        }

        @NotNull
        @Override
        public Map<String, String> getParamMap(@NotNull CustomProjEntity customProjEntity) {
            return null;
        }

        @Nullable
        @Override
        public CustomProjEntity getCustomProjEntity(@NotNull TriggerPipelineOldReq triggerPipelineReq) {
            return null;
        }

        @NotNull
        @Override
        public String createCustomizedCheckProjPipeline(@NotNull TriggerPipelineOldReq triggerPipelineReq, long taskId, @Nullable String userId, @NotNull String projectId) {
            return null;
        }

        @NotNull
        @Override
        public CustomProjEntity handleWithCheckProjPipeline(@NotNull TriggerPipelineOldReq triggerPipelineReq, @NotNull String userId) {
            return null;
        }


        @NotNull
        @Override
        public String createCustomDevopsProject(@NotNull CustomProjEntity customProjEntity, @NotNull String userId) {
            return null;
        }
    });


    private ICustomPipelineService iCustomPipelineService;

    CustomProjSource(ICustomPipelineService iCustomPipelineService)
    {
        this.iCustomPipelineService = iCustomPipelineService;
    }

    public ICustomPipelineService getiCustomPipelineService(){
        return this.iCustomPipelineService;
    }
}
