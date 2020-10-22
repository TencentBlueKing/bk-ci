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

package com.tencent.bk.codecc.klocwork.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.bk.codecc.defect.api.ServiceScmFileCacheRestResource;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.DefectDetailVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.klocwork.component.KlocworkAPIService;
import com.tencent.bk.codecc.klocwork.component.ThirdPartySystemCaller;
import com.tencent.bk.codecc.klocwork.constant.KlocworkConstants;
import com.tencent.bk.codecc.klocwork.constant.KlocworkMessageCode;
import com.tencent.bk.codecc.klocwork.service.KwDefectService;
import com.tencent.bk.codecc.klocwork.utils.KlocworkDefectAttributeUtils;
import com.tencent.bk.codecc.klocwork.vo.KWDefectDTO;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.lang3.tuple.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * coverity项目配置业务接口实现
 *
 * @version V1.0
 * @date 2019/10/1
 */
@Service
@Slf4j
public class KwDefectServiceImpl implements KwDefectService
{
    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Autowired
    private KlocworkAPIService klocworkAPIService;

    @Autowired
    private Client client;
    /**
     * 提交告警
     * 1.对于新增的告警，需要将告警的完整信息提交到defect模块
     * 2.codecc存在，platform上不存在的告警，标志为已修复
     *
     * @param commitDefectVO
     * @return
     */
    @Override
    public boolean commitDefect(CommitDefectVO commitDefectVO)
    {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();
        // 查询plaformIp
        String platformIp = thirdPartySystemCaller.getPlatformIp(taskId, toolName);
        PlatformVO platformVO = KlocworkAPIService.getInst(platformIp);
        // 查询所有的cid
        Set<Long> idSet = thirdPartySystemCaller.getDefectIds(taskId, toolName);
        log.info("task [{}] idSet size: {}", taskId, idSet.size());

        // 从platform查询所有的告警
        String responseMsg = klocworkAPIService.searchDefects(streamName, platformVO);
        List<KWDefectDTO> defectList = new Gson().fromJson(responseMsg,
                new TypeToken<List<KWDefectDTO>>()
                {
                }.getType());

        // 获取告警当前build版本号
        String[] lastTwoBuild = getCurBuild(streamName, platformVO);
        String newestBuild = lastTwoBuild[0];
        log.info("newest two builds:{}, {}", lastTwoBuild[0], lastTwoBuild[1]);

        Set<Long> platformNotExistCids = new HashSet<>(idSet);
        if (CollectionUtils.isNotEmpty(defectList))
        {
            int count = 0;
            List<DefectDetailVO> defectVOList = new ArrayList<>();
            long currTime = System.currentTimeMillis();
            Map<String, ScmBlameVO> fileChangeRecordsMap = client.get(ServiceScmFileCacheRestResource.class).loadAuthorInfoMap(taskId, streamName, toolName, buildId).getData();

            for (KWDefectDTO defectDTO : defectList)
            {
                DefectDetailVO defectDetailVO = new DefectDetailVO();
                defectDetailVO.setTaskId(taskId);
                defectDetailVO.setToolName(toolName);
                defectDetailVO.setStreamName(streamName);
                // 新告警设置创建时间
                if (KlocworkConstants.KWDefectState.New.name().equals(defectDTO.getState()) || !idSet.contains(defectDTO.getId()))
                {
                    defectDetailVO.setCreateTime(currTime);
                }

                // 赋值当前build版本号
                if (KlocworkConstants.KWDefectState.Fixed.name().equalsIgnoreCase(defectDTO.getState()))
                {
                    defectDetailVO.setPlatformBuildId(lastTwoBuild[1]);
                }
                else
                {
                    defectDetailVO.setPlatformBuildId(newestBuild);
                }

                wrapDefectVO(defectDTO, defectDetailVO, fileChangeRecordsMap);
                defectVOList.add(defectDetailVO);
                count++;
                if (count >= KlocworkConstants.BATCH_SUBMIT_COUNT)
                {
                    thirdPartySystemCaller.reportDefects(taskId, streamName, toolName, buildId, defectVOList);
                    defectVOList.clear();
                    count = 0;
                }

                // 校验是否存在CodeCC上有，但Platform上没有的告警
                if (platformNotExistCids.contains(defectDTO.getId()))
                {
                    platformNotExistCids.remove(defectDTO.getId());
                }
            }
            thirdPartySystemCaller.reportDefects(taskId, streamName, toolName, buildId, defectVOList);
            log.info("succ commit new defects: {}, {}", streamName, defectList.size());
        }

        // 2 codecc存在，platform上不存在的告警，标志为已修复
        new ArrayList<>();
        if (CollectionUtils.isNotEmpty(platformNotExistCids))
        {
            List<DefectDetailVO> updateDefectVOList = platformNotExistCids.stream().map(cid ->
            {
                DefectDetailVO defectDetailVO = new DefectDetailVO();
                defectDetailVO.setId(String.valueOf(cid));
                defectDetailVO.setStatus(ComConstants.DefectStatus.FIXED.value());
                return defectDetailVO;
            }).collect(Collectors.toList());
            if (updateDefectVOList.size() > 0)
            {
                thirdPartySystemCaller.updateDefectStatus(taskId, toolName, buildId, updateDefectVOList);
                log.info("succ update defect status: {}, {}", streamName, updateDefectVOList.size());
            }
        }

        log.info("succ commitDefect: {}", streamName);
        return true;
    }

    @Override
    public DefectDetailVO getFilesContent(DefectDetailVO defectDetailVO)
    {
        Map<String, DefectDetailVO.FileInfo> fileInfoMap = defectDetailVO.getFileInfoMap();
        String build = defectDetailVO.getPlatformBuildId();

        // 查询plaformIp
        String platformIp = thirdPartySystemCaller.getPlatformIp(defectDetailVO.getTaskId(), defectDetailVO.getToolName());
        fileInfoMap.forEach((contentMd5, fileInfo) ->
        {
            String fileContent = klocworkAPIService.getFileContent(platformIp, defectDetailVO.getStreamName(), build, fileInfo.getFilePathname());
            fileInfo.setContents(fileContent);
            trimCodeSegment(fileInfo);
        });

        return defectDetailVO;
    }

    /**
     * 装配DefectVO
     *
     * @param defectDTO
     * @param defectDetailVO
     * @param fileLineAuthorInfos
     */
    private void wrapDefectVO(KWDefectDTO defectDTO, DefectDetailVO defectDetailVO, Map<String, ScmBlameVO> fileLineAuthorInfos)
    {
        defectDetailVO.setId(String.valueOf(defectDTO.getId()));
        defectDetailVO.setCheckerName(defectDTO.getCode());
        defectDetailVO.setLineNumber(defectDTO.getLine());
        defectDetailVO.setDisplayCategory(defectDTO.getTitle());
        defectDetailVO.setDisplayType(defectDTO.getMessage());
        defectDetailVO.setFilePathname(defectDTO.getFile());

        int severity = KlocworkDefectAttributeUtils.getSeverity(defectDTO);
        defectDetailVO.setSeverity(severity);

        int status = KlocworkDefectAttributeUtils.getStatus(defectDTO);
        defectDetailVO.setStatus(status | ComConstants.DefectStatus.NEW.value());

        Pair<Set<String>, String> pair = KlocworkDefectAttributeUtils.getAuthor(defectDTO, fileLineAuthorInfos);
        defectDetailVO.setAuthorList(pair.getKey());
        defectDetailVO.setRevision(pair.getValue());

        List<DefectDetailVO.DefectInstance> defectInstances = new ArrayList<>();
        for (KWDefectDTO.Trace eachTrace: defectDTO.getTrace())
        {
            DefectDetailVO.DefectInstance defectInstance = new DefectDetailVO.DefectInstance();
            defectInstances.add(defectInstance);

            boolean hasMain = false;
            int mainLine = defectDTO.getLine();
            List<DefectDetailVO.Trace> traces = parseTrace(eachTrace, mainLine, hasMain);
            defectInstance.setTraces(traces);
        }

        if (defectInstances.size() == 0)
        {
            DefectDetailVO.DefectInstance defectInstance = new DefectDetailVO.DefectInstance();
            defectInstances.add(defectInstance);
            List<DefectDetailVO.Trace> traces = new ArrayList<>(1);
            defectInstance.setTraces(traces);
            DefectDetailVO.Trace trace = new DefectDetailVO.Trace();
            traces.add(trace);
            trace.setFilePathname(defectDTO.getFile());
            trace.setLineNumber(defectDTO.getLine());
            trace.setMain(true);
        }
        defectDetailVO.setDefectInstances(defectInstances);
    }

    private List<DefectDetailVO.Trace> parseTrace(KWDefectDTO.Trace eachTrace, int mainLine, boolean hasMain)
    {
        List<DefectDetailVO.Trace> traces = new ArrayList<>();
        for (KWDefectDTO.Line line: eachTrace.getLines())
        {
            DefectDetailVO.Trace detectDetailTrace = new DefectDetailVO.Trace();
            traces.add(detectDetailTrace);
            detectDetailTrace.setFilePathname(eachTrace.getFile());
            detectDetailTrace.setMessage(line.getText());
            detectDetailTrace.setLineNumber(line.getLine());
            if (!hasMain && mainLine == line.getLine())
            {
                hasMain = true;
            }
            detectDetailTrace.setMain(hasMain);
            if (line.getTrace() != null)
            {
                List<DefectDetailVO.Trace> linkTrace = parseTrace(line.getTrace(), mainLine, hasMain);
                detectDetailTrace.setLinkTrace(linkTrace);
            }
        }
        return traces;
    }

    /**
     * 获取告警当前build版本号
     *
     * @param streamName
     * @param platformVO
     * @return
     */
    private String[] getCurBuild(String streamName, PlatformVO platformVO)
    {
        String[] lastTwoBuild = new String[2];
        JSONArray builds = klocworkAPIService.getBuilds(streamName, platformVO);
        JSONObject newestBuildJson = builds.getJSONObject(0);
        lastTwoBuild[0] = newestBuildJson.getString("name");
        if (builds.length() > 1)
        {
            lastTwoBuild[1] = builds.getJSONObject(1).getString("name");
        }
        else
        {
            lastTwoBuild[1] = lastTwoBuild[0];
        }
        if (newestBuildJson == null)
        {
            log.error("Project has not been successfully built: %s", streamName);
            throw new CodeCCException(KlocworkMessageCode.GET_KW_BUILD_FAIL, new String[]{"Project has not been successfully built: " + streamName}, null);

        }
        return lastTwoBuild;
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
}
