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
import com.tencent.bk.codecc.defect.service.IConfigCheckerPkgBizService;
import com.tencent.bk.codecc.defect.service.MultitoolCheckerService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.IgnoreCheckerVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

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
    private MultitoolCheckerService multitoolCheckerService;

    @Autowired
    private IConfigCheckerPkgBizService iConfigCheckerPkgBizService;

    @Override
    public Result<Map<String, CheckerDetailVO>> queryOpenChecker(ToolConfigInfoVO toolConfigInfoVO)
    {
        return new Result<>(multitoolCheckerService.queryOpenCheckers(toolConfigInfoVO));
    }

    @Override
    public Result<List<CheckerDetailVO>> queryAllChecker(ToolConfigInfoVO toolConfigInfoVO)
    {
        return new Result<>(multitoolCheckerService.queryAllChecker(toolConfigInfoVO));
    }

    @Override
    public Result<Boolean> mergeIgnoreChecker(long taskId, String toolName, List<String> ignoreCheckers)
    {
        return new Result<>(multitoolCheckerService.mergeIgnoreChecker(taskId, toolName, ignoreCheckers));
    }

    @Override
    public Result<Boolean> createDefaultIgnoreChecker(IgnoreCheckerVO ignoreCheckerVO, String userName)
    {
        return new Result<>(iConfigCheckerPkgBizService.createDefaultIgnoreChecker(ignoreCheckerVO, userName));
    }

    @Override
    public Result<IgnoreCheckerVO> getIgnoreCheckerInfo(Long taskId, String toolName)
    {
        return new Result<>(iConfigCheckerPkgBizService.getIgnoreCheckerInfo(taskId, toolName));
    }

}
