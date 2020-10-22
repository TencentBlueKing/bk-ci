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

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.vo.DefectDetailVO;
import com.tencent.bk.codecc.task.api.ServicePlatformRestResource;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * pinpoint查询服务实现
 *
 * @version V1.0
 * @date 2019/12/11
 */
@Slf4j
@Service("PINPOINTQueryWarningBizService")
public class PinpointQueryWarningBizServiceImpl extends CommonQueryWarningBizServiceImpl
{
    @Override
    public DefectDetailVO getFilesContent(DefectDetailVO defectDetailVO)
    {
        Map<String, DefectDetailVO.FileInfo> fileInfoMap = defectDetailVO.getFileInfoMap();
        String build = defectDetailVO.getPlatformBuildId();

        // 查询plaformIp
        CodeCCResult<String> result = client.get(ServicePlatformRestResource.class).getPlatformIp(defectDetailVO.getTaskId(), defectDetailVO.getToolName());
        if (result.isNotOk() || null == result.getData())
        {
            log.error("get task [{}] platform ip fail! message: {}", defectDetailVO.getTaskId(), result.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        String platformIp = result.getData();

        // 查询plaformIp
        CodeCCResult<PlatformVO> platformVOResult = client.get(ServicePlatformRestResource.class).getPlatformByToolNameAndIp(defectDetailVO.getTaskId(), defectDetailVO.getToolName(), platformIp);
        if (result.isNotOk() || null == result.getData())
        {
            log.error("get task [{}] platform ip fail! message: {}", defectDetailVO.getTaskId(), result.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        PlatformVO platformVO = platformVOResult.getData();
        String urlPrefix = String.format("http://%s:%s/online_api/project/bugcase/source/code/and/reference/%s/%s", platformVO.getIp(), platformVO.getPort(), defectDetailVO.getPlatformProjectId(), build);
        fileInfoMap.forEach((fileMD5, fileInfo) ->
        {
            String url = String.format("%s?case_id=%s&filemd5=%s", urlPrefix, defectDetailVO.getId(), fileMD5);
            String fileContent = getFileContent(url, platformVO.getToken());
            fileInfo.setContents(fileContent);
            trimCodeSegment(fileInfo);
        });

        return defectDetailVO;
    }

    /**
     * 以doGet请求方式发送请求
     *
     * @param url
     * @param token
     * @return
     */
    public static String getFileContent(String url, String token)
    {
        // Instantiate an HttpClient
        HttpClient client = new HttpClient();

        // Instantiate a GET HTTP method
        GetMethod method = new GetMethod(url);
        method.setRequestHeader("Content-type", "application/json;charset=utf-8");
        method.setRequestHeader("Pinpoint-Token", token);
        String fileContent = null;
        try
        {
            int statusCode = client.executeMethod(method);
            if (HttpStatus.SC_OK == statusCode)
            {
                log.debug("Response status: {}", HttpStatus.getStatusText(statusCode));

                // Get data as a String
                String rspBody = method.getResponseBodyAsString();
                if (StringUtils.isNotEmpty(rspBody))
                {
                    JSONObject rspJson = new JSONObject(rspBody);
                    int code = rspJson.getInt("code");
                    if (HttpStatus.SC_OK != code)
                    {
                        log.error("Request url[{}] fail! rspBody ---> {}", url, rspBody);
                    }
                    else
                    {
                        JSONObject data = rspJson.getJSONObject("data");
                        if (data != null && data.has("code"))
                        {
                            Object fileCode = data.get("code");
                            if (fileCode != null)
                            {
                                fileContent = fileCode.toString();
                            }
                        }
                    }
                }
            }
            else
            {
                log.error("Request url[{}] fail! HttpStatus ---> {}", url, statusCode);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
        }
        catch (IOException e)
        {
            log.error("Request url[{}] exception!", url, e);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        finally
        {
            // 释放连接
            method.releaseConnection();
        }
        return fileContent;
    }


    /**
     * 截取告警文件片段
     *
     * @param fileInfo
     * @return
     */
    private void trimCodeSegment(DefectDetailVO.FileInfo fileInfo)
    {
        int minDefectLineNum = fileInfo.getMinDefectLineNum();
        int maxDefectLineNum = fileInfo.getMaxDefectLineNum();
        String fileContent = fileInfo.getContents();
        String[] lines = fileContent.split("\n");

        if (lines.length <= 2000)
        {
            return;
        }
        int startLine = 1;
        int endLine = lines.length;

        int limitLines = 500;
        if (minDefectLineNum - limitLines > 0)
        {
            startLine = minDefectLineNum - limitLines;
        }

        if (maxDefectLineNum + limitLines < lines.length)
        {
            endLine = maxDefectLineNum + limitLines;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = startLine - 1; i < endLine - 1; i++)
        {
            builder.append(lines[i] + "\n");
        }

        fileInfo.setContents(builder.toString());
        fileInfo.setStartLine(startLine);
    }

    @Override
    public int getSubmitStepNum()
    {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }
}
