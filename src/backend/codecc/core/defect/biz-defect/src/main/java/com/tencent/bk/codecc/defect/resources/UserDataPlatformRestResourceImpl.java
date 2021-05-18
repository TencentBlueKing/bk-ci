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

import com.tencent.bk.codecc.defect.api.UserDataPlatformRestResource;
import com.tencent.bk.codecc.defect.service.DataPlatformService;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.devops.common.api.CommonPageVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 工具侧上报任务分析记录接口实现
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Slf4j
@RestResource
public class UserDataPlatformRestResourceImpl implements UserDataPlatformRestResource {
    @Autowired
    private Client client;

    @Autowired
    private DataPlatformService dataPlatformService;

    @Override
    public CodeCCResult<String> push(int pageNum, int pageSize, String dataSource) throws InterruptedException {
        Page<Long> page = pushOnePage(pageNum, pageSize, dataSource);
        long totalPages = page.getTotalPages();
        return new CodeCCResult<>(String.valueOf(totalPages));
    }

    private Page<Long> pushOnePage(int pageNum, int pageSize, String dataSource) throws InterruptedException {
        CommonPageVO commonPageVO = new CommonPageVO();
        commonPageVO.setPageNum(pageNum);
        commonPageVO.setPageSize(pageSize);
        CodeCCResult<Page<Long>> listCodeCCResultPage = client.get(ServiceTaskRestResource.class).getTaskInfoByCreateFrom(dataSource, commonPageVO);
        if (listCodeCCResultPage != null && listCodeCCResultPage.getData() != null) {
            log.info("getTaskInfoByCreateFrom listResult size: " + listCodeCCResultPage.getData().getRecords().size());
            dataPlatformService.pushStatistic(listCodeCCResultPage.getData().getRecords());
            dataPlatformService.pushLineStatistic(listCodeCCResultPage.getData().getRecords());
            dataPlatformService.pushCcnStatistic(listCodeCCResultPage.getData().getRecords());
            dataPlatformService.pushDupcStatistic(listCodeCCResultPage.getData().getRecords());
            dataPlatformService.pushClocDefect(listCodeCCResultPage.getData().getRecords());
            return listCodeCCResultPage.getData();
        } else {
            log.error("getTaskInfoByCreateFrom listResult is null.");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
    }

}
