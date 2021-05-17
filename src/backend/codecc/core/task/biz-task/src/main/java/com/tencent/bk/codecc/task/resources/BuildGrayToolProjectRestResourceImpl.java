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

import com.tencent.bk.codecc.task.api.BuildGrayToolProjectRestResource;
import com.tencent.bk.codecc.task.api.UserGrayToolProjectRestResource;
import com.tencent.bk.codecc.task.service.GrayToolProjectService;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.bk.codecc.task.vo.GrayToolReportVO;
import com.tencent.bk.codecc.task.vo.TriggerGrayToolVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

/**
 * 灰度工具项目调用接口实现
 *
 * @version V2.0
 * @date 2020/12/29
 */
@RestResource
public class BuildGrayToolProjectRestResourceImpl implements BuildGrayToolProjectRestResource {

    @Autowired
    private GrayToolProjectService grayToolProjectService;

    @Override
    public Result<Boolean> createGrayTaskPool(String toolName, String user) {
        grayToolProjectService.selectGrayTaskPool(toolName, user);
        return new Result<>(true);
    }

    @Override
    public Result<TriggerGrayToolVO> triggerGrayTaskPool(String toolName) {
        return new Result<>(grayToolProjectService.triggerGrayToolTasks(toolName));
    }

    @Override
    public Result<GrayToolReportVO> findGrayReportByToolNameAndCodeCCBuildId(String toolName, String codeccBuildId) {
        return new Result<>(grayToolProjectService.findGrayToolReportByToolNameAndBuildId(toolName, codeccBuildId));
    }

    @Override
    public Result<Set<Long>> findTaskListByToolName(String toolName) {
        return new Result<>(grayToolProjectService.findTaskListByToolName(toolName));
    }
}
