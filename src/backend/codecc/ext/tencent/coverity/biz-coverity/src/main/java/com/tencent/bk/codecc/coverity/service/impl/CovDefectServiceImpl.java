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

package com.tencent.bk.codecc.coverity.service.impl;

import com.coverity.ws.v9.*;
import com.tencent.bk.codecc.coverity.component.CoverityService;
import com.tencent.bk.codecc.coverity.component.ThirdPartySystemCaller;
import com.tencent.bk.codecc.coverity.constant.CoverityConstants;
import com.tencent.bk.codecc.coverity.service.CovDefectService;
import com.tencent.bk.codecc.coverity.utils.CoverityDefectAttributeUtils;
import com.tencent.bk.codecc.coverity.vo.SyncDefectDetailVO;
import com.tencent.bk.codecc.defect.api.ServiceScmFileCacheRestResource;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.DefectDetailVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.web.mq.ConstantsKt;
import org.apache.commons.lang3.tuple.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * coverity项目配置业务接口实现
 *
 * @version V1.0
 * @date 2019/10/1
 */
@Service
@Slf4j
public class CovDefectServiceImpl implements CovDefectService {
    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;
    @Autowired
    protected RabbitTemplate rabbitTemplate;
    @Autowired
    private Client client;

    /**
     * 提交告警
     * 1.对于新增的告警，需要将告警的完整信息提交到defect模块
     * 2.对于已经存在的告警，只需要告警的状态（是否已经修复）提交到defect模块，让defect模块更新告警的状态
     * 2.1 codecc存在，platform上也存在的告警，取platform上的状态
     * 2.2 codecc存在，platform上不存在的告警，标志为已修复
     *
     * @param commitDefectVO
     * @return
     */
    @Override
    public boolean commitDefect(CommitDefectVO commitDefectVO) {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();
        // 查询plaformIp
        String platformIp = thirdPartySystemCaller.getPlatformIp(taskId, toolName);

        // 查询所有的cid，用于判断告警是否已经在codecc存在，如果已经存在，则不需查询告警实例
        Set<Long> cidSet = thirdPartySystemCaller.getDefectIds(taskId, toolName);
        log.info("task [{}] cidSet size: {}", taskId, cidSet.size());

        // 从coverity platform查询所有的告警
        ProjectScopeDefectFilterSpecDataObj filterSpec = new ProjectScopeDefectFilterSpecDataObj();
        List<MergedDefectDataObj> mergedDefectList = CoverityService.getInst(platformIp)
                .getDefects(streamName, filterSpec);

        // 根据CID一次获取所有新增告警的实例
        Map<Long, StreamDefectDataObj> streamDefectMap = CoverityService.getInst(platformIp)
                .getStreamDefectMap(streamName, mergedDefectList, cidSet);

        // 1. 对于新增的告警，需要将告警的完整信息提交到defect模块
        if (streamDefectMap.size() > 0) {
            int count = 0;
            List<DefectDetailVO> defectVOList = new ArrayList<>();
            long currTime = System.currentTimeMillis();
            Map<String, ScmBlameVO> fileChangeRecordsMap = client.get(ServiceScmFileCacheRestResource.class)
                    .loadAuthorInfoMap(taskId, streamName, toolName, buildId).getData();

            for (Map.Entry<Long, StreamDefectDataObj> entry : streamDefectMap.entrySet()) {
                StreamDefectDataObj streamDefect = entry.getValue();
                DefectDetailVO defectDetailVO = new DefectDetailVO();
                defectDetailVO.setCreateTime(currTime);
                wrapDefectVO(streamDefect, defectDetailVO, fileChangeRecordsMap);
                defectVOList.add(defectDetailVO);
                count++;
                if (count >= CoverityConstants.BATCH_SUBMIT_COUNT) {
                    thirdPartySystemCaller.reportDefects(taskId, streamName, toolName, buildId, defectVOList);
                    defectVOList.clear();
                    count = 0;
                }
            }
            thirdPartySystemCaller.reportDefects(taskId, streamName, toolName, buildId, defectVOList);
            log.info("succ commit new defects: {}, {}", streamName, streamDefectMap.size());
        }

        // 2.1 对于已经存在的告警，只需要告诉defect模块告警的状态，让defect模块更新告警的状态。
        List<DefectDetailVO> updateDefectVOList = new ArrayList<>();
        Set<Long> platformNotExistCids = new HashSet<>(cidSet);
        Set<Long> bothExistCids = new HashSet<>();
        for (MergedDefectDataObj mergedDefect : mergedDefectList) {
            Long cid = mergedDefect.getCid();

            // 校验是否存在CodeCC上有，但Platform上没有的告警
            if (platformNotExistCids.contains(cid)) {
                platformNotExistCids.remove(cid);
            }

            // 不是新告警就是已存在的告警，获取状态同步到codecc
            StreamDefectDataObj streamDefect = streamDefectMap.get(cid);
            if (streamDefect == null) {
                int status = CoverityDefectAttributeUtils.getStatus(mergedDefect.getDefectStateAttributeValues());
                DefectDetailVO defectDetailVO = new DefectDetailVO();
                defectDetailVO.setId(String.valueOf(cid));
                defectDetailVO.setFilePathname(mergedDefect.getFilePathname());
                defectDetailVO.setStatus(status);
                updateDefectVOList.add(defectDetailVO);

                // 已存在于codecc的待修复告警，并且在platform上还存在的告警
                if (ComConstants.DefectStatus.NEW.value() == status) {
                    bothExistCids.add(cid);
                }
            }
        }

        // 2.2 codecc存在，platform上不存在的告警，标志为已修复
        if (CollectionUtils.isNotEmpty(platformNotExistCids)) {
            platformNotExistCids.forEach(cid ->
            {
                DefectDetailVO defectDetailVO = new DefectDetailVO();
                defectDetailVO.setId(String.valueOf(cid));
                defectDetailVO.setStatus(ComConstants.DefectStatus.FIXED.value());
                updateDefectVOList.add(defectDetailVO);
            });
        }

        if (updateDefectVOList.size() > 0) {
            thirdPartySystemCaller.updateDefectStatus(taskId, toolName, buildId, updateDefectVOList);
            log.info("succ update defect status: {}, {}", streamName, updateDefectVOList.size());
        }

        // 2.3 待修复的告警，需要异步同步告警详情到codecc
        if (CollectionUtils.isNotEmpty(bothExistCids)) {
            SyncDefectDetailVO syncDefectDetailVO = new SyncDefectDetailVO();
            syncDefectDetailVO.setTaskId(taskId);
            syncDefectDetailVO.setStreamName(streamName);
            syncDefectDetailVO.setToolName(toolName);
            syncDefectDetailVO.setPlatformIp(platformIp);
            syncDefectDetailVO.setBuildId(buildId);
            syncDefectDetailVO.setCidSet(bothExistCids);
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_COV_DEFECT_DETAIL_SYNC,
                    ConstantsKt.ROUTE_COV_DEFECT_DETAIL_SYNC, syncDefectDetailVO);
        }
        log.info("succ commitDefect: {}", streamName);

        return true;
    }

    /**
     * 异步同步告警详情到codecc
     *
     * @param syncDefectDetailVO
     */
    @Override
    public void syncDefectDetail(SyncDefectDetailVO syncDefectDetailVO) {
        Long taskId = syncDefectDetailVO.getTaskId();
        String streamName = syncDefectDetailVO.getStreamName();
        String toolName = syncDefectDetailVO.getToolName();
        String buildId = syncDefectDetailVO.getBuildId();
        // 根据CID一次获取所有待修复告警的实例
        List<StreamDefectDataObj> existStreamDefectList = CoverityService.getInst(syncDefectDetailVO.getPlatformIp())
                .getStreamDefectList(streamName, syncDefectDetailVO.getCidSet());

        if (CollectionUtils.isNotEmpty(existStreamDefectList)) {
            int count = 0;
            List<DefectDetailVO> defectVOList = new ArrayList<>();
            for (StreamDefectDataObj streamDefect : existStreamDefectList) {
                DefectDetailVO defectDetailVO = new DefectDetailVO();
                wrapDefectVO(streamDefect, defectDetailVO, null);
                defectVOList.add(defectDetailVO);
                count++;
                if (count >= CoverityConstants.BATCH_SUBMIT_COUNT) {
                    thirdPartySystemCaller.updateDefects(taskId, streamName, toolName, buildId, defectVOList);
                    defectVOList.clear();
                    count = 0;
                }
            }
            thirdPartySystemCaller.updateDefects(taskId, streamName, toolName, buildId, defectVOList);
            log.info("succ update defects: {}, {}", streamName, existStreamDefectList.size());
        }
    }

    @Override
    public DefectDetailVO getDefectDetail(DefectDetailVO defectDetailVO) {
        long taskId = defectDetailVO.getTaskId();
        String streamName = defectDetailVO.getStreamName();

        // 查询plaformIp
        String platformIp = thirdPartySystemCaller.getPlatformIp(taskId, ComConstants.Tool.COVERITY.name());

        // 为了兼容之前没有在codecc平台保存告警详情的项目，这里需要判断如果没有告警详情，就从platform上查询告警详情
        if (CollectionUtils.isEmpty(defectDetailVO.getDefectInstances())) {
            Long cid = Long.valueOf(defectDetailVO.getId());
            List<StreamDefectDataObj> defectList = CoverityService.getInst(platformIp)
                    .getDefectDataObj(cid, streamName);
            if (CollectionUtils.isNotEmpty(defectList)) {
                parseDefectDetail(defectDetailVO, defectList.get(0), true);
            }
        }

        // 从platform获取告警相关的文件内容
        Map<String, DefectDetailVO.FileInfo> fileInfoMap = defectDetailVO.getFileInfoMap();
        fileInfoMap.forEach((contentMd5, fileInfo) -> {
            String fileContent = CoverityService.getInst(platformIp)
                    .getFileContents(streamName, contentMd5, fileInfo.getFilePathname());
            fileInfo.setContents(fileContent);
            trimCodeSegment(fileInfo);
        });

        return defectDetailVO;
    }

    /**
     * 装配DefectVO
     *
     * @param streamDefect
     * @param defectDetailVO
     * @param fileChangeRecordsMap
     */
    private void wrapDefectVO(StreamDefectDataObj streamDefect, DefectDetailVO defectDetailVO,
                              Map<String, ScmBlameVO> fileChangeRecordsMap) {
        defectDetailVO.setId(String.valueOf(streamDefect.getCid()));
        defectDetailVO.setCheckerName(streamDefect.getCheckerName());

        int severity = CoverityDefectAttributeUtils.getSeverity(streamDefect);
        defectDetailVO.setSeverity(severity);

        int status = CoverityDefectAttributeUtils.getStatus(streamDefect);
        defectDetailVO.setStatus(status | ComConstants.DefectStatus.NEW.value());

        String displayCategory = CoverityDefectAttributeUtils.getDisplayCategory(streamDefect);
        defectDetailVO.setDisplayCategory(displayCategory);

        String displayType = CoverityDefectAttributeUtils.getDisplayType(streamDefect);
        defectDetailVO.setDisplayType(displayType);

        String filePathname = CoverityDefectAttributeUtils.getFilePathname(streamDefect);
        defectDetailVO.setFilePathname(filePathname);

        Pair<Set<String>, String> pair = CoverityDefectAttributeUtils.getAuthor(streamDefect, fileChangeRecordsMap);
        defectDetailVO.setAuthorList(pair.getKey());
        defectDetailVO.setRevision(pair.getValue());

        parseDefectDetail(defectDetailVO, streamDefect, false);
    }

    /**
     * 解析coverity platform返回的结果，转换成codecc的告警数据对象
     *
     * @param defectDetailVO
     * @param streamDefect
     * @param isNeedFileInfo 是否需要获取文件信息，true需要，false不需要
     */
    protected void parseDefectDetail(DefectDetailVO defectDetailVO, StreamDefectDataObj streamDefect,
                                     boolean isNeedFileInfo) {
        List<DefectDetailVO.DefectInstance> defectInstanceList = new ArrayList<>();
        for (DefectInstanceDataObj defectInstanceDataObj : streamDefect.getDefectInstances()) {
            DefectDetailVO.DefectInstance defectInstance = new DefectDetailVO.DefectInstance();
            defectInstanceList.add(defectInstance);
            List<EventDataObj> events = defectInstanceDataObj.getEvents();
            List<DefectDetailVO.Trace> traces = new ArrayList<>(events.size());
            for (EventDataObj eventDataObj : events) {
                DefectDetailVO.Trace trace = new DefectDetailVO.Trace();
                parseEventDataObj(defectDetailVO, eventDataObj, trace, isNeedFileInfo);
                traces.add(trace);
            }
            defectInstance.setTraces(traces);
        }
        defectDetailVO.setDefectInstances(defectInstanceList);
    }

    /**
     * 解析告警实例的跟踪事件，转换为Trace对象，并获取文件信息
     *
     * @param defectDetailVO
     * @param event
     * @param trace
     * @param isNeedFileInfo
     */
    private void parseEventDataObj(DefectDetailVO defectDetailVO, EventDataObj event,
                                   DefectDetailVO.Trace trace, boolean isNeedFileInfo) {
        if (event.getEvents() != null) {
            List<DefectDetailVO.Trace> linkTraceList = new ArrayList<>(event.getEvents().size());
            trace.setLinkTrace(linkTraceList);
            for (EventDataObj e : event.getEvents()) {
                DefectDetailVO.Trace linkTrace = new DefectDetailVO.Trace();
                linkTraceList.add(linkTrace);
                parseEventDataObj(defectDetailVO, e, linkTrace, isNeedFileInfo);
            }
        }

        String fileName = event.getFileId().getFilePathname();
        String contentsMD5 = event.getFileId().getContentsMD5();
        int lineNumber = event.getLineNumber();
        trace.setMessage(event.getEventDescription());
        trace.setFilePathname(fileName);
        trace.setFileMD5(contentsMD5);
        trace.setLineNumber(lineNumber);
        trace.setKind(event.getEventKind());
        trace.setTraceNumber(event.getEventNumber());
        trace.setMain(event.isMain());
        trace.setTag(event.getEventTag());

        if (event.isMain() || defectDetailVO.getLineNumber() == 0) {
            defectDetailVO.setLineNumber(lineNumber);
        }

        if (isNeedFileInfo) {
            DefectDetailVO.FileInfo fileInfo = defectDetailVO.getFileInfoMap().get(contentsMD5);
            if (fileInfo == null) {
                fileInfo = new DefectDetailVO.FileInfo();
                fileInfo.setFilePathname(fileName);
                fileInfo.setFileMD5(contentsMD5);
                fileInfo.setMinDefectLineNum(lineNumber);
                fileInfo.setMaxDefectLineNum(lineNumber);
                defectDetailVO.getFileInfoMap().put(contentsMD5, fileInfo);
            } else {
                if (lineNumber < fileInfo.getMinDefectLineNum()) {
                    fileInfo.setMinDefectLineNum(lineNumber);
                } else {
                    if (lineNumber > fileInfo.getMaxDefectLineNum()) {
                        fileInfo.setMaxDefectLineNum(lineNumber);
                    }
                }
            }
        }
    }

    /**
     * 截取告警文件片段
     *
     * @param fileInfo
     * @return
     */
    private void trimCodeSegment(DefectDetailVO.FileInfo fileInfo) {
        int minDefectLineNum = fileInfo.getMinDefectLineNum();
        int maxDefectLineNum = fileInfo.getMaxDefectLineNum();
        String fileContent = fileInfo.getContents();
        String[] lines = fileContent.split("\n");

        if (lines.length <= 2000) {
            return;
        }
        int startLine = 1;
        int endLine = lines.length;

        int limitLines = 500;
        if (minDefectLineNum - limitLines > 0) {
            startLine = minDefectLineNum - limitLines;
        }

        if (maxDefectLineNum + limitLines < lines.length) {
            endLine = maxDefectLineNum + limitLines;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = startLine - 1; i < endLine - 1; i++) {
            builder.append(lines[i] + "\n");
        }

        fileInfo.setContents(builder.toString());
        fileInfo.setStartLine(startLine);
    }
}
