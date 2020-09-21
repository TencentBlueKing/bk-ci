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

import com.tencent.bk.codecc.defect.api.UserCheckerRestResource;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.ICheckerSetBizService;
import com.tencent.bk.codecc.defect.service.IConfigCheckerPkgBizService;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.checkerset.*;
import com.tencent.bk.codecc.defect.vo.enums.CheckerListSortType;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.security.AuthMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * 配置规则包服务实现
 *
 * @version V1.0
 * @date 2019/6/5
 */
@Slf4j
@RestResource
public class UserCheckerRestResourceImpl implements UserCheckerRestResource
{

    @Autowired
    private IConfigCheckerPkgBizService configCheckerPkgBizService;

    @Autowired
    private ICheckerSetBizService checkerSetBizService;

    @Autowired
    private CheckerService checkerService;

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public CodeCCResult<GetCheckerListRspVO> checkerPkg(Long taskId, String toolName)
    {
        return new CodeCCResult<>(configCheckerPkgBizService.getConfigCheckerPkg(taskId, toolName));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public CodeCCResult<Boolean> configCheckerPkg(String user, Long taskId, String toolName, ConfigCheckersPkgReqVO packageVo)
    {
        return new CodeCCResult<>(configCheckerPkgBizService.configCheckerPkg(taskId, toolName, packageVo, user));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public CodeCCResult<Boolean> updateCheckerSet(Long taskId, String toolName, String checkerSetId, UpdateCheckerSetReqVO updateCheckerSetReqVO, String user,
            String projectId)
    {
        return new CodeCCResult<>(checkerSetBizService.updateCheckerSet(taskId, toolName, checkerSetId, updateCheckerSetReqVO, user, projectId));
    }

    @Override
    public CodeCCResult<Boolean> addCheckerSet2Task(String user, Long taskId, AddCheckerSet2TaskReqVO addCheckerSet2TaskReqVO)
    {
        addCheckerSet2TaskReqVO.setNeedUpdatePipeline(true);
        return new CodeCCResult<>(checkerSetBizService.addCheckerSet2Task(user, taskId, addCheckerSet2TaskReqVO));
    }

    @Override
    public CodeCCResult<UserCreatedCheckerSetsVO> getUserCreatedCheckerSet(String toolName, String user, String projectId)
    {
        return new CodeCCResult<>(checkerSetBizService.getUserCreatedCheckerSet(toolName, user, projectId));
    }

    @Override
    public CodeCCResult<CheckerSetDifferenceVO> getCheckerSetVersionDifference(String user, String projectId, String toolName, String checkerSetId,
            CheckerSetDifferenceVO checkerSetDifferenceVO)
    {
        return new CodeCCResult<>(checkerSetBizService.getCheckerSetVersionDifference(user, projectId, toolName, checkerSetId, checkerSetDifferenceVO));
    }

    @Override
    public CodeCCResult<Boolean> updateCheckerConfigParam(Long taskId, String toolName, String checkerName, String paramValue, String user) {
        return new CodeCCResult<>(checkerService.updateCheckerConfigParam(taskId, toolName, checkerName, paramValue, user));
    }

    @Override
    public CodeCCResult<CheckerDetailVO> queryCheckerDetail(String toolName, String checkerKey)
    {
        return new CodeCCResult<>(checkerService.queryCheckerDetail(toolName, checkerKey));
    }

    @Override
    public CodeCCResult<List<CheckerDetailVO>> queryCheckerDetailList(CheckerListQueryReq checkerListQueryReq, String projectId, Integer pageNum,
                                                                Integer pageSize, Sort.Direction sortType, CheckerListSortType sortField)
    {
        return new CodeCCResult<>(checkerService.queryCheckerDetailList(checkerListQueryReq, projectId, pageNum, pageSize, sortType, sortField));
    }


    @Override
    public CodeCCResult<List<CheckerCommonCountVO>> queryCheckerCountList(CheckerListQueryReq checkerListQueryReq, String projectId)
    {
        return new CodeCCResult<>(checkerService.queryCheckerCountListNew(checkerListQueryReq, projectId));
    }

    @Override
    public CodeCCResult<List<CheckerDetailVO>> queryCheckerByTool(String toolName) {
        return new CodeCCResult<>(checkerService.queryCheckerByTool(toolName));
    }


}
