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

package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceCheckerRestResource;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.ICheckerSetBizService;
import com.tencent.bk.codecc.defect.service.IConfigCheckerPkgBizService;
import com.tencent.bk.codecc.defect.vo.CheckerPkgRspVO;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.defect.vo.checkerset.AddCheckerSet2TaskReqVO;
import com.tencent.bk.codecc.defect.vo.checkerset.GetCheckerSetsReqVO;
import com.tencent.bk.codecc.defect.vo.checkerset.PipelineCheckerSetVO;
import com.tencent.bk.codecc.defect.vo.checkerset.UserCheckerSetsVO;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.bk.codecc.task.vo.checkerset.ClearTaskCheckerSetReqVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 多工具规则接口实现
 *
 * @version V1.0
 * @date 2019/5/23
 */
@RestResource
public class ServiceCheckerRestResourceImpl implements ServiceCheckerRestResource
{
    @Autowired
    private IConfigCheckerPkgBizService iConfigCheckerPkgBizService;

    @Autowired
    private IConfigCheckerPkgBizService configCheckerPkgBizService;

    @Autowired
    private ICheckerSetBizService checkerSetBizService;

    @Autowired
    private CheckerService checkerService;

    @Override
    public Result<List<CheckerPkgRspVO>> queryCheckerConfiguration(Long taskId, String toolName, Long codeLang, ToolConfigInfoVO toolConfig)
    {
        return new Result<>(iConfigCheckerPkgBizService.getConfigCheckerPkg(taskId, toolName, codeLang, toolConfig).getCheckerPackages());
    }

    @Override
    public Result<Boolean> configCheckerPkg(Long taskId, String toolName, ConfigCheckersPkgReqVO packageVo)
    {
        return new Result<>(configCheckerPkgBizService.syncConfigCheckerPkg(taskId, toolName, packageVo));
    }

    @Override
    public Result<Boolean> addCheckerSet2Task(String user, Long taskId, AddCheckerSet2TaskReqVO addCheckerSet2TaskReqVO)
    {
        return new Result<>(checkerSetBizService.addCheckerSet2Task(user, taskId, addCheckerSet2TaskReqVO));
    }

    @Override
    public Result<UserCheckerSetsVO> getCheckerSets(GetCheckerSetsReqVO getCheckerSetsReqVO, String user, String projectId)
    {
        return new Result<>(checkerSetBizService.getCheckerSets(getCheckerSetsReqVO.getToolNames(), user, projectId));
    }

    @Override
    public Result<PipelineCheckerSetVO> getPipelineCheckerSets(String toolName, String user, String projectId)
    {
        return new Result<>(checkerSetBizService.getPipelineCheckerSets(toolName, user, projectId));
    }

    @Override
    public Result<Boolean> clearCheckerSet(Long taskId, ClearTaskCheckerSetReqVO clearTaskCheckerSetReqVO, String user)
    {
        return new Result<>(checkerSetBizService.clearTaskCheckerSets(taskId, clearTaskCheckerSetReqVO.getToolNames(), user,
                clearTaskCheckerSetReqVO.getNeedUpdatePipeline()));
    }

}
