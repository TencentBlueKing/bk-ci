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

import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.service.AnalyzeConfigService;
import com.tencent.bk.codecc.task.service.MetaService;
import com.tencent.bk.codecc.task.service.ToolService;
import com.tencent.bk.codecc.task.vo.*;
import com.tencent.bk.codecc.task.vo.checkerset.UpdateCheckerSet2TaskReqVO;
import com.tencent.bk.codecc.task.vo.checkerset.ClearTaskCheckerSetReqVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineBuildInfoVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * 服务间调用的工具配置接口
 *
 * @version V1.0
 * @date 2019/5/7
 */
@Slf4j
@RestResource
public class ServiceToolRestResourceImpl implements ServiceToolRestResource
{
    @Autowired
    private ToolService toolService;

    @Autowired
    private AnalyzeConfigService analyzeConfigService;

    @Autowired
    private MetaService metaService;

    @Autowired
    private TaskRepository taskRepository;

    @Override
    public CodeCCResult updateToolStepStatus(ToolConfigBaseVO toolConfigBaseVO)
    {
        toolService.updateToolStepStatus(toolConfigBaseVO);
        return new CodeCCResult(CommonMessageCode.SUCCESS, "mongotemplate tool config ok");
    }


    @Override
    public CodeCCResult<ToolConfigInfoVO> getToolByTaskIdAndName(long taskId, String toolName)
    {
        return new CodeCCResult<>(toolService.getToolByTaskIdAndName(taskId, toolName));
    }

    @Override
    public CodeCCResult<ToolConfigInfoWithMetadataVO> getToolWithMetadataByTaskIdAndName(long taskId, String toolName)
    {
        return new CodeCCResult<>(toolService.getToolWithMetadataByTaskIdAndName(taskId, toolName));
    }


    @Override
    public CodeCCResult<String> findToolOrder()
    {
        return new CodeCCResult<>(metaService.getToolOrder());
    }


    @Override
    public CodeCCResult<Boolean> updatePipelineTool(Long taskId, String userName, List<String> toolList)
    {
        return new CodeCCResult<>(toolService.updatePipelineTool(taskId, toolList, userName));
    }

    @Override
    public CodeCCResult<Boolean> clearCheckerSet(Long taskId, ClearTaskCheckerSetReqVO clearTaskCheckerSetReqVO)
    {
        return new CodeCCResult<>(toolService.clearCheckerSet(taskId, clearTaskCheckerSetReqVO.getToolNames()));
    }

    @Override
    public CodeCCResult<Boolean> addCheckerSet2Task(Long taskId, UpdateCheckerSet2TaskReqVO addCheckerSet2TasklReqVO)
    {
        return new CodeCCResult<>(toolService.addCheckerSet2Task(taskId, addCheckerSet2TasklReqVO.getToolCheckerSets()));
    }

    @Override
    public CodeCCResult<AnalyzeConfigInfoVO> getAnalyzeConfig(String streamName, String toolName, PipelineBuildInfoVO pipelineBuildInfoVO)
    {
        return new CodeCCResult<>(analyzeConfigService.getAnalyzeConfig(streamName, toolName, pipelineBuildInfoVO));
    }

    @Override
    public CodeCCResult<Boolean> updateTools(Long taskId, String user, BatchRegisterVO batchRegisterVO)
    {
        return toolService.updateTools(taskId, user, batchRegisterVO);
    }

    @Override
    public CodeCCResult<List<ToolConfigInfoVO>> batchGetToolConfigList(QueryTaskListReqVO queryTaskListReqVO)
    {
        return new CodeCCResult<>(toolService.batchGetToolConfigList(queryTaskListReqVO));
    }

}
