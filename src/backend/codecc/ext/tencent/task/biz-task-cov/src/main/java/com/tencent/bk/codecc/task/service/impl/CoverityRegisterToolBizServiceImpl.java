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

import com.tencent.bk.codecc.coverity.api.ServiceCovConfigRestResource;
import com.tencent.bk.codecc.task.constant.TaskMessageCode;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.service.AbstractRegisterToolBizService;
import com.tencent.bk.codecc.task.vo.RegisterPlatformProjVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 添加LINT模型类工具的接口实现
 *
 * @version V1.0
 * @date 2019/10/3
 */
@Service("COVERITYRegisterToolBizService")
@Slf4j
public class CoverityRegisterToolBizServiceImpl extends AbstractRegisterToolBizService
{
    @Override
    public ToolConfigInfoEntity registerTool(ToolConfigInfoVO toolConfigInfo, TaskInfoEntity taskInfoEntity, String user)
    {
        //在coverity platform上注册项目
        RegisterPlatformProjVO registerPlatformProjVO = new RegisterPlatformProjVO();
        registerPlatformProjVO.setStreamName(taskInfoEntity.getNameEn());
        registerPlatformProjVO.setCreateFrom(taskInfoEntity.getCreateFrom());
        CodeCCResult<String> covPlatformResult = client.get(ServiceCovConfigRestResource.class).registerProject(registerPlatformProjVO);
        if (covPlatformResult.isNotOk() || null == covPlatformResult.getData())
        {
            log.error("register coverity project fail!");
            throw new CodeCCException(TaskMessageCode.REGISTER_COV_PROJ_FAIL);
        }

        String platformIp = covPlatformResult.getData();

        // 由于当前版本Coverity的TypeScript语言只有3条安全规则，没有默认规则，所以当选择了TypeScript语言时，默认打开这3条规则 TODO
//        CheckersPkgDao.openCovTsCheckers(projectInfo);

        // 装配工具信息
        ToolConfigInfoEntity toolConfigInfoEntity = configToolEntity(toolConfigInfo, taskInfoEntity, user);
        toolConfigInfoEntity.setPlatformIp(platformIp);

        log.info("register tool success! task id: {}, tool name : {}", toolConfigInfoEntity.getTaskId(), toolConfigInfoEntity.getToolName());
        return toolConfigInfoEntity;
    }
}
