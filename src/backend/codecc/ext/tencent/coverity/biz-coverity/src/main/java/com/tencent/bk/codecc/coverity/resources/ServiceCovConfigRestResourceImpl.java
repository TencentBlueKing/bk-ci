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

package com.tencent.bk.codecc.coverity.resources;

import com.tencent.bk.codecc.coverity.api.ServiceCovConfigRestResource;
import com.tencent.bk.codecc.coverity.constant.CoverityMessageCode;
import com.tencent.bk.codecc.coverity.service.CovConfigService;
import com.tencent.bk.codecc.coverity.vo.UpdateComponentMapVO;
import com.tencent.bk.codecc.task.vo.RegisterPlatformProjVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.ws.WebServiceException;

/**
 * 构建机任务接口实现
 *
 * @version V1.0
 * @date 2019/7/21
 */
@RestResource
@Slf4j
public class ServiceCovConfigRestResourceImpl implements ServiceCovConfigRestResource
{
    @Autowired
    private CovConfigService covConfigService;

    @Override
    public CodeCCResult<String> registerProject(RegisterPlatformProjVO registerPlatformProjVO)
    {
        String streamName = registerPlatformProjVO.getStreamName();
        String platform = null;
        try
        {
            platform = covConfigService.registerProject(registerPlatformProjVO);
        }
        catch (WebServiceException e)
        {
            if ("java.net.SocketTimeoutException: Read timed out".equals(e.getMessage()))
            {
                log.error("Register coverity info time out: {}", streamName, e);
                throw new CodeCCException(CoverityMessageCode.REGISTER_COV_PROJ_TIMEOUT, new String[]{streamName}, null);
            }
            log.error("Register coverity project throw Exception: {}", streamName, e);
        }

        if (platform == null)
        {
            log.error("Fail to create coverity project in platfrom: {}", streamName);
            throw new CodeCCException(CoverityMessageCode.REGISTER_COV_PROJ_FAIL, new String[]{streamName}, null);
        }
        return new CodeCCResult<>(platform);
    }

    @Override
    public CodeCCResult<Boolean> updateComponentMap(UpdateComponentMapVO updateComponentMapVO)
    {
        log.info("begin commitDefect: {}", updateComponentMapVO);
        return new CodeCCResult<>(covConfigService.updateComponentMap(updateComponentMapVO));
    }
}
