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

import com.tencent.bk.codecc.task.api.BuildToolMetaRestResource;
import com.tencent.bk.codecc.task.api.BuildToolRestResource;
import com.tencent.bk.codecc.task.service.AnalyzeConfigService;
import com.tencent.bk.codecc.task.service.ToolMetaService;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineBuildInfoVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 工具元数据注册接口
 *
 * @version V2.0
 * @date 2020/8/8
 */
@RestResource
public class BuildToolMetaRestResourceImpl implements BuildToolMetaRestResource
{

    @Autowired
    private ToolMetaService toolMetaService;

    @Override
    public Result<ToolMetaDetailVO> register(String userName, ToolMetaDetailVO toolMetaDetailVO)
    {
        return new Result<>(toolMetaService.register(userName, toolMetaDetailVO));
    }

    @Override
    public Result<List<ToolMetaDetailVO>> queryToolMetaDataList(String projectId)
    {
        return new Result<>(toolMetaService.queryToolMetaDataList(projectId));
    }
}
