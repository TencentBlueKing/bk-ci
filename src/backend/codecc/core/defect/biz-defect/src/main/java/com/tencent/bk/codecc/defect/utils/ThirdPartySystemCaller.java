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

package com.tencent.bk.codecc.defect.utils;

import com.tencent.bk.codecc.defect.api.ServiceReportTaskLogRestResource;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.BaseDataVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 外部服务公共调度器
 *
 * @version V1.0
 * @date 2019/6/5
 */
@Component
@Slf4j
public class ThirdPartySystemCaller
{
    @Autowired
    private Client client;

    /**
     * 调用task模块的接口获取任务信息
     *
     * @param streamName
     * @return
     */
    @NotNull
    public TaskDetailVO getTaskInfo(String streamName)
    {
        CodeCCResult<TaskDetailVO> taskInfoCodeCCResult = client.get(ServiceTaskRestResource.class).getTaskInfo(streamName);
        if (taskInfoCodeCCResult.isNotOk() || null == taskInfoCodeCCResult.getData())
        {
            log.error("get task info fail! stream name is: {}, msg: {}", streamName, taskInfoCodeCCResult.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return taskInfoCodeCCResult.getData();
    }

    /**
     * 调用task模块的接口获取任务信息
     *
     * @param taskId
     * @return
     */
    @NotNull
    public TaskDetailVO getTaskInfoWithoutToolsByTaskId(Long taskId)
    {
        CodeCCResult<TaskDetailVO> taskDetailCodeCCResult = client.get(ServiceTaskRestResource.class).getTaskInfoWithoutToolsByTaskId(taskId);
        if (taskDetailCodeCCResult.isNotOk() || null == taskDetailCodeCCResult.getData())
        {
            log.error("get task info fail! taskId: {}, msg: {}", taskId, taskDetailCodeCCResult.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return taskDetailCodeCCResult.getData();
    }


    /**
     * 获取重复率的风险系数基本数据
     *
     * @return
     */
    @NotNull
    public Map<String, String> getRiskFactorConfig(String toolName)
    {
        //获取风险系数值
        CodeCCResult<List<BaseDataVO>> baseDataCodeCCResult = client.get(ServiceBaseDataResource.class)
                .getInfoByTypeAndCode(ComConstants.PREFIX_RISK_FACTOR_CONFIG, toolName);

        if (baseDataCodeCCResult.isNotOk() || null == baseDataCodeCCResult.getData())
        {
            log.error("get risk coefficient fail!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        return baseDataCodeCCResult.getData().stream()
                .collect(Collectors.toMap(BaseDataVO::getParamName, BaseDataVO::getParamValue, (k, v) -> v));
    }

    public void uploadTaskLog(UploadTaskLogStepVO uploadTaskLogStepVO)
    {
        CodeCCResult result = client.get(ServiceReportTaskLogRestResource.class).uploadTaskLog(uploadTaskLogStepVO);

        if (result.isNotOk())
        {
            log.error("upload TaskLog fail! message: {} {}", uploadTaskLogStepVO.getStreamName(), result.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
    }
}
