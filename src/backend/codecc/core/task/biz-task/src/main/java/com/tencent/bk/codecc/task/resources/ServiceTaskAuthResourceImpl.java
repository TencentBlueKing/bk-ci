package com.tencent.bk.codecc.task.resources;
/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to
 use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import com.tencent.bk.codecc.task.api.ServiceTaskAuthResource;
import com.tencent.bk.codecc.task.service.TaskAuthV3Service;
import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum;
import com.tencent.bk.sdk.iam.dto.PageInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Collectors;

@RestResource
@Slf4j
public class ServiceTaskAuthResourceImpl implements ServiceTaskAuthResource {

    @Autowired
    private TaskAuthV3Service authV3Service;

    @Override
    public CallbackBaseResponseDTO taskInfo(CallbackRequestDTO callBackInfo) {
        log.info("get call back request dto: {}", JsonUtil.INSTANCE.toJson(callBackInfo));

        CallbackMethodEnum method = callBackInfo.getMethod();
        PageInfoDTO page = callBackInfo.getPage();
        String projectId = callBackInfo.getFilter().getParent().getId();
        switch (method) {
            case LIST_INSTANCE:
                return authV3Service.getTask(projectId, page.getOffset(), page.getLimit());
            case FETCH_INSTANCE_INFO:
                Set<Long> ids = callBackInfo.getFilter().getIdList()
                    .stream().map(it -> Long.parseLong(it.toString())).collect(Collectors.toSet());
                return authV3Service.getTask(ids);
            case SEARCH_INSTANCE:
                return authV3Service.searchTask(projectId,
                    callBackInfo.getFilter().getKeyword(),
                    page.getOffset(),
                    page.getLimit());
            default:
                return null;
        }
    }
}
