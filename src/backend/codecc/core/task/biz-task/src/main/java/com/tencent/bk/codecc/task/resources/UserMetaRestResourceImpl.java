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

import com.tencent.bk.codecc.task.api.UserMetaRestResource;
import com.tencent.bk.codecc.task.service.MetaService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.vo.BuildEnvVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.OpenScanAndEpcToolNameMapVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * 任务管理接口的实现类
 *
 * @version V1.0
 * @date 2019/4/19
 */
@RestResource
public class UserMetaRestResourceImpl implements UserMetaRestResource
{
    @Autowired
    private MetaService metaService;

    @Autowired
    private PipelineService pipelineService;

    @Override
    public Result<List<ToolMetaBaseVO>> toolList(Boolean isDetail)
    {
        return new Result<>(metaService.toolList(isDetail));
    }

    @Override
    public Result<ToolMetaDetailVO> toolDetail(String toolName)
    {
        return new Result<>(metaService.queryToolDetail(toolName));
    }

    @Override
    public Result<Map<String, List<MetadataVO>>> metadatas(String metadataType)
    {
        return new Result<>(metaService.queryMetadatas(metadataType));
    }

    @Override
    public Result<List<BuildEnvVO>> getBuildEnv(String os)
    {
        return new Result<>(pipelineService.getBuildEnv(os));
    }

    @Override
    public Result<OpenScanAndEpcToolNameMapVO> getOpenScanAndEpcToolNameMap() {
        return new Result<>(metaService.getOpenScanAndEpcToolNameMap());
    }
}
