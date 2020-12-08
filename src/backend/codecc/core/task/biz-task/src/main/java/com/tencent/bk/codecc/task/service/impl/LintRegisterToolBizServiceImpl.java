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

package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.service.AbstractRegisterToolBizService;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 添加LINT模型类工具的接口实现
 *
 * @version V1.0
 * @date 2019/10/3
 */
@Service("CommonRegisterToolBizService")
public class LintRegisterToolBizServiceImpl extends AbstractRegisterToolBizService
{
    private static Logger logger = LoggerFactory.getLogger(LintRegisterToolBizServiceImpl.class);

    @Override
    public ToolConfigInfoEntity registerTool(ToolConfigInfoVO toolConfigInfo, TaskInfoEntity taskInfoEntity, String user)
    {
        // 装配工具信息
        ToolConfigInfoEntity toolConfigInfoEntity = configToolEntity(toolConfigInfo, taskInfoEntity, user);

        logger.info("register tool success! task id: {}, tool name : {}", toolConfigInfoEntity.getTaskId(), toolConfigInfoEntity.getToolName());
        return toolConfigInfoEntity;
    }
}
