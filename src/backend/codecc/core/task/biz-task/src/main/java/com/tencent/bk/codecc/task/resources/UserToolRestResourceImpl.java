/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.UserToolRestResource;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.service.ToolService;
import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.ParamJsonAndCheckerSetsVO;
import com.tencent.bk.codecc.task.vo.RepoInfoVO;
import com.tencent.bk.codecc.task.vo.ToolConfigPlatformVO;
import com.tencent.bk.codecc.task.vo.ToolStatusUpdateReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.security.AuthMethod;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 工具管理接口实现类
 *
 * @version V1.0
 * @date 2019/5/7
 */
@RestResource
public class UserToolRestResourceImpl implements UserToolRestResource
{
    private static Logger logger = LoggerFactory.getLogger(UserToolRestResourceImpl.class);

    @Autowired
    private ToolService toolService;

    @Autowired
    private PipelineService pipelineService;

    @Override
    public Result<Boolean> registerTools(
            BatchRegisterVO batchRegisterVO,
            String userName
    )
    {
        logger.info("register tools: {}", JsonUtil.INSTANCE.toJson(batchRegisterVO));
        return toolService.registerTools(batchRegisterVO, null, userName);
    }


    @Override
    public Result<List<RepoInfoVO>> getRepoList(String projCode)
    {
        return new Result<>(pipelineService.getRepositoryList(projCode));
    }

    @Override
    public Result<List<String>> listBranches(String projCode, String url, String type)
    {
        return new Result<>(pipelineService.getRepositoryBranches(projCode, url, type));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> updateToolStatus(ToolStatusUpdateReqVO toolStatusUpdateReqVO,
                                            String userName, long taskId)
    {
        checkToolUpdateParam(toolStatusUpdateReqVO, taskId);
        return new Result<>(toolService.toolStatusManage(toolStatusUpdateReqVO.getToolNameList(),
                toolStatusUpdateReqVO.getManageType(), userName, taskId));
    }


    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> deletePipeline(Long taskId, String projectId, String userName)
    {
        return new Result<>(toolService.deletePipeline(taskId, projectId, userName));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> updateParamJsonAndCheckerSets(String user, Long taskId, ParamJsonAndCheckerSetsVO paramJsonAndCheckerSetsVO)
    {
        return new Result<>(toolService.updateParamJsonAndCheckerSets(user, taskId, paramJsonAndCheckerSetsVO));
    }


    @Override
    public Result<Boolean> updateToolPlatformInfo(Long taskId, String userName,
            ToolConfigPlatformVO toolConfigPlatformVO)
    {
        return new Result<>(toolService.updateToolPlatformInfo(taskId, userName, toolConfigPlatformVO));
    }


    /**
     * 工具停用启用校验入参
     *
     * @param toolStatusUpdateReqVO
     * @param taskId
     */
    private void checkToolUpdateParam(ToolStatusUpdateReqVO toolStatusUpdateReqVO, long taskId)
    {
        if (ComConstants.CommonJudge.COMMON_N.value().equalsIgnoreCase(toolStatusUpdateReqVO.getManageType()))
        {
            if (StringUtils.isEmpty(toolStatusUpdateReqVO.getStopReason()))
            {
                logger.error("stop reason can not be empty when diable tool! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{String.valueOf(taskId)}, null);
            }
        }
    }


}
