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

package com.tencent.bk.codecc.quartz.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext;
import com.tencent.bk.codecc.task.pojo.CodeCCAccountAuthInfo;
import com.tencent.bk.codecc.task.pojo.CodeCCCodeScan;
import com.tencent.bk.codecc.task.pojo.CodeCCPipelineReq;
import com.tencent.bk.codecc.task.pojo.CodeCCRuntimeParam;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq;
import com.tencent.devops.common.util.OkhttpUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 定时触发蓝鲸项目扫描
 *
 * @date 2020/8/21
 * @version V1.0
 */
public class TriggerCustomPipelineScheduleTask implements IScheduleTask {

    @Autowired
    private ObjectMapper objectMapper;

    private static Logger logger = LoggerFactory.getLogger(TriggerCustomPipelineScheduleTask.class);

    @Override
    public void executeTask(@NotNull QuartzJobContext quartzJobContext) {
        Map<String, Object> jobCustomParam = quartzJobContext.getJobCustomParam();
        if (null == jobCustomParam) {
            logger.info("job custom param is null");
            return;
        }

        String apigwPath = (String) jobCustomParam.get("apigwPath");
        String appCode = (String) jobCustomParam.get("appCode");
        String appSecret = (String) jobCustomParam.get("appSecret");
        String userId = (String) jobCustomParam.get("userId");
        String gitUrl = (String) jobCustomParam.get("gitUrl");
        String branch = (String) jobCustomParam.get("branch");
        String paramCode = (String) jobCustomParam.get("paramCode");
        String paramValue = (String) jobCustomParam.get("paramValue");
        String paramDesc = (String) jobCustomParam.get("paramDesc");
        String userName = (String) jobCustomParam.get("userName");
        String passWord = (String) jobCustomParam.get("passWord");

        CodeCCRuntimeParam runtimeParam = new CodeCCRuntimeParam(paramCode, paramValue, paramDesc);
        CodeCCAccountAuthInfo codeCCAuthInfo = new CodeCCAccountAuthInfo(userName, passWord, null, null);
        CodeCCPipelineReq codeCCPipelineReq = new CodeCCPipelineReq(Collections.singletonList(runtimeParam), null,
                codeCCAuthInfo, new CodeCCCodeScan(), null, null);
        TriggerPipelineReq triggerPipelineReq = new TriggerPipelineReq(gitUrl, branch, null, null,
                null, false, false, false, null, null, codeCCPipelineReq);

        String str = "%s/v2/apigw-app/codecc/task/pipelines/custom/trigger/new?app_code=%s&app_secret=%s";
        final String url = String.format(str, apigwPath, appCode, appSecret);
        String body;
        try {
            body = objectMapper.writeValueAsString(triggerPipelineReq);
        } catch (JsonProcessingException e) {
            logger.error("serialize triggerPipelineReq fail!");
            e.printStackTrace();
            return;
        }
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("accept", "application/json");
        headerMap.put("Content-Type", "application/json");
        headerMap.put("X-DEVOPS-UID", userId);
        String result = OkhttpUtils.INSTANCE.doHttpPost(url, body, headerMap);
        logger.info("trigger custom pipeline : {}", result);
    }
}
