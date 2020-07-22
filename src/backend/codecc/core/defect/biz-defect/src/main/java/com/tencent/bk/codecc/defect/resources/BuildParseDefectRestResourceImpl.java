/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.tencent.bk.codecc.defect.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.defect.api.BuildDefectRestResource;
import com.tencent.bk.codecc.defect.api.BuildParseDefectRestResource;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.bk.codecc.defect.component.ScmJsonComponent;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;

/**
 * 解析告警上报服务
 * 
 * @date 2020/1/17
 * @version V1.0
 */
@Slf4j
public class BuildParseDefectRestResourceImpl implements BuildParseDefectRestResource
{
    @Autowired
    private BuildDefectRestResource buildDefectRestResource;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public ScmJsonComponent scmJsonComponent;

    @Override
    public CodeCCResult<Boolean> notifyReportDefects(String streamName, String toolName, String buildId) {
        log.info("start to parse defect file! stream name: {}, tool name: {}, buildId: {}", streamName, toolName, buildId);
        String defectFileContent = scmJsonComponent.loadDefects(streamName, toolName, buildId);
        try {
            UploadDefectVO uploadDefectVO = objectMapper.readValue(defectFileContent, UploadDefectVO.class);
            if (uploadDefectVO != null)
            {
                buildDefectRestResource.asyncReportDefects(uploadDefectVO);
                log.info("upload defect success!");
            }
        } catch (IOException e) {
            log.error("parse defect file fail!");
            return new CodeCCResult<>(false);
        }
        return new CodeCCResult<>(true);
    }

    @Override
    public CodeCCResult<String> querydefectReportStatus(String streamName, String toolName, String buildId) {
        log.info("start to query defect report status, stream name: {}, tool name: {}, build id: {}", streamName, toolName, buildId);
        String reportKey = String.format("%s:%s:%s:%s", RedisKeyConstants.KEY_REPORT_DEFECT, streamName, toolName, buildId);
        String defectReportStatus = redisTemplate.opsForValue().get(reportKey);
//        if(ComConstants.DefectReportStatus.SUCCESS.name().equals(defectReportStatus) ||
//                ComConstants.DefectReportStatus.FAIL.name().equals(defectReportStatus))
//        {
//            redisTemplate.delete(reportKey);
//        }  // TODO 为了兼容老插件告警上报临时处理，后面要删除

        return new CodeCCResult<>(StringUtils.isBlank(defectReportStatus) ? ComConstants.DefectReportStatus.PROCESSING.name() :
        defectReportStatus);
    }


}
