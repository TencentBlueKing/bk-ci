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

package com.tencent.bk.codecc.klocwork.component;

import com.tencent.bk.codecc.defect.api.ServiceReportDefectRestResource;
import com.tencent.bk.codecc.defect.api.ServiceReportTaskLogRestResource;
import com.tencent.bk.codecc.defect.vo.DefectDetailVO;
import com.tencent.bk.codecc.defect.vo.UpdateDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.api.ServicePlatformRestResource;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.CompressionUtils;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

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
     * 获取所有的platform
     *
     * @return
     */
    @NotNull
    public List<PlatformVO> getAllPlatform(String toolName)
    {
        //获取风险系数值
        CodeCCResult<List<PlatformVO>> covPlatformResult = client.get(ServicePlatformRestResource.class).getPlatformByToolName(toolName);

        if (covPlatformResult.isNotOk() || null == covPlatformResult.getData())
        {
            log.error("get platform list fail!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        return covPlatformResult.getData();
    }

    /**
     * 获取所有的CID
     *
     * @param taskId
     * @return
     */
    public String getPlatformIp(long taskId, String toolName)
    {
        CodeCCResult<String> result = client.get(ServicePlatformRestResource.class).getPlatformIp(taskId, toolName);
        if (result.isNotOk() || null == result.getData())
        {
            log.error("get task [{}] platform ip fail! message: {}", taskId, result.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return result.getData();
    }

    /**
     * 获取所有的CID
     *
     * @param taskId
     * @param toolName
     * @return
     */
    public Set<Long> getDefectIds(long taskId, String toolName)
    {
        CodeCCResult<Set<Long>> idSet = client.get(ServiceReportDefectRestResource.class).queryIds(taskId, toolName);
        if (idSet.isNotOk() || null == idSet.getData())
        {
            log.error("get task [{}] idSet fail! message: {}", taskId, idSet.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return idSet.getData();
    }

    /**
     * 上报告警到defect服务
     *  @param streamName
     * @param toolName
     * @param buildId
     * @param defectVOList
     */
    public void reportDefects(long taskId, String streamName, String toolName, String buildId, List<DefectDetailVO> defectVOList)
    {
        if (CollectionUtils.isEmpty(defectVOList))
        {
            return;
        }

        UploadDefectVO uploadDefectVO = new UploadDefectVO();
        uploadDefectVO.setTaskId(taskId);
        uploadDefectVO.setStreamName(streamName);
        uploadDefectVO.setToolName(toolName);
        uploadDefectVO.setBuildId(buildId);
        String defectVOListCompress = CompressionUtils.compressAndEncodeBase64(JsonUtil.INSTANCE.toJson(defectVOList));
        uploadDefectVO.setDefectsCompress(defectVOListCompress);

        log.info("report defects VO: {}", uploadDefectVO);
        CodeCCResult result = client.get(ServiceReportDefectRestResource.class).reportDefects(uploadDefectVO);

        if (result.isNotOk())
        {
            log.error("report defects fail! message: {} {}", streamName, result.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
    }

    public void updateDefectStatus(Long taskId, String toolName, String buildId, List<DefectDetailVO> defectList)
    {
        UpdateDefectVO updateDefectStatusVO = new UpdateDefectVO();
        updateDefectStatusVO.setTaskId(taskId);
        updateDefectStatusVO.setDefectList(defectList);
        updateDefectStatusVO.setBuildId(buildId);
        updateDefectStatusVO.setToolName(toolName);
        CodeCCResult result = client.get(ServiceReportDefectRestResource.class).updateDefectStatus(updateDefectStatusVO);

        if (result.isNotOk())
        {
            log.error("report defects fail! message: {} {}", taskId, result.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
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
