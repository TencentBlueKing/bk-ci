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

package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 添加工具的接口
 *
 * @version V1.0
 * @date 2019/10/3
 */
public abstract class AbstractRegisterToolBizService implements IRegisterToolBizService
{
    @Autowired
    protected ToolRepository toolRepository;

    @Autowired
    protected Client client;

    /**
     * 装配工具信息
     *
     * @param toolConfigInfo
     * @param taskInfoEntity
     * @param user
     * @return
     */
    public ToolConfigInfoEntity configToolEntity(ToolConfigInfoVO toolConfigInfo, TaskInfoEntity taskInfoEntity, String user)
    {
        ToolConfigInfoEntity toolConfigInfoEntity = new ToolConfigInfoEntity();
        BeanUtils.copyProperties(toolConfigInfo, toolConfigInfoEntity);
        long taskId = toolConfigInfoEntity.getTaskId();
        if (taskId <= ComConstants.COMMON_NUM_1000L)
        {
            toolConfigInfoEntity.setTaskId(taskInfoEntity.getTaskId());
        }

        Long currentTime = System.currentTimeMillis();
        toolConfigInfoEntity.setCurStep(ComConstants.Step4MutliTool.READY.value());
        toolConfigInfoEntity.setStepStatus(ComConstants.StepStatus.SUCC.value());
        toolConfigInfoEntity.setCreatedBy(user);
        toolConfigInfoEntity.setCreatedDate(currentTime);
        toolConfigInfoEntity.setUpdatedBy(user);
        toolConfigInfoEntity.setUpdatedDate(currentTime);

        return toolConfigInfoEntity;
    }
}
