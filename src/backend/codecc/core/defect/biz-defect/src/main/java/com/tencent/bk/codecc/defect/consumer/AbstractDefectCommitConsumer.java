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

package com.tencent.bk.codecc.defect.consumer;

import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.component.ScmJsonComponent;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildStackRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.TransferAuthorRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.BuildDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.service.FilterPathService;
import com.tencent.bk.codecc.defect.service.RedLineReportService;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmInfoVO;
import com.tencent.devops.common.auth.api.external.AuthTaskService;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.web.aop.annotation.EndReport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 告警提交消息队列的消费者抽象类
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Slf4j
public abstract class AbstractDefectCommitConsumer {
    /**
     * 流式分批告警处理每批最大处理数
     */
    protected static final int MAX_PER_BATCH = 15000;

    @Autowired
    public ToolBuildInfoDao toolBuildInfoDao;
    @Autowired
    public ThirdPartySystemCaller thirdPartySystemCaller;
    @Autowired
    public ScmJsonComponent scmJsonComponent;
    @Autowired
    public BuildDao buildDao;
    @Autowired
    public ToolBuildStackRepository toolBuildStackRepository;
    @Autowired
    public ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    public RedLineReportService redLineReportService;
    @Autowired
    public AuthTaskService authTaskService;
    @Autowired
    public RabbitTemplate rabbitTemplate;
    @Autowired
    public TransferAuthorRepository transferAuthorRepository;
    @Autowired
    protected ScmFileInfoService scmFileInfoService;
    @Autowired
    public FilterPathService filterPathService;
    @Autowired
    private CodeRepoInfoRepository codeRepoRepository;

    /**
     * 告警提交
     *
     * @param commitDefectVO
     */
    @EndReport(isOpenSource = false)
    public void commitDefect(CommitDefectVO commitDefectVO) {
        long beginTime = System.currentTimeMillis();
        try {
            log.info("commit defect! {}", commitDefectVO);

            // 发送开始提单的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0,
                    null);

            try {
                // 获取文件作者信息
                Map<String, ScmBlameVO> fileChangeRecordsMap = getAuthorInfo(commitDefectVO);

                // 获取仓库信息
                Map<String, RepoSubModuleVO> codeRepoIdMap = getRepoInfo(commitDefectVO);

                // 解析工具上报的告警文件并入库
                uploadDefects(commitDefectVO, fileChangeRecordsMap, codeRepoIdMap);
            } catch (Throwable e) {
                e.printStackTrace();
                log.error("commit defect fail!", e);
                // 发送提单失败的分析记录
                uploadTaskLog(commitDefectVO, ComConstants.StepFlag.FAIL.value(), 0, System.currentTimeMillis(),
                        e.getLocalizedMessage());
                return;
            }

            // 发送提单成功的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(),
                    commitDefectVO.getMessage());
        } catch (Throwable e) {
            log.error("commit defect fail!", e);
        }
        log.info("end commitDefect cost: {}", System.currentTimeMillis() - beginTime);
    }

    /**
     * 解析工具上报的告警文件并入库
     *
     * @param commitDefectVO
     * @param fileChangeRecordsMap
     * @param codeRepoIdMap
     */
    protected abstract void uploadDefects(CommitDefectVO commitDefectVO,
                                          Map<String, ScmBlameVO> fileChangeRecordsMap,
                                          Map<String, RepoSubModuleVO> codeRepoIdMap);

    protected Map<String, ScmBlameVO> getAuthorInfo(CommitDefectVO commitDefectVO) {
        return scmFileInfoService.loadAuthorInfoMap(
                commitDefectVO.getTaskId(),
                commitDefectVO.getStreamName(),
                commitDefectVO.getToolName(),
                commitDefectVO.getBuildId());
    }

    protected Map<String, RepoSubModuleVO> getRepoInfo(CommitDefectVO commitDefectVO) {
        JSONArray repoInfoJsonArr = scmJsonComponent.loadRepoInfo(commitDefectVO.getStreamName(),
                commitDefectVO.getToolName(), commitDefectVO.getBuildId());
        Map<String, RepoSubModuleVO> codeRepoIdMap = Maps.newHashMap();
        if (repoInfoJsonArr != null && repoInfoJsonArr.length() > 0) {
            for (int i = 0; i < repoInfoJsonArr.length(); i++) {
                JSONObject codeRepoJson = repoInfoJsonArr.getJSONObject(i);
                ScmInfoVO codeRepoInfo = JsonUtil.INSTANCE.to(codeRepoJson.toString(), ScmInfoVO.class);
                //需要判断是svn还是git，svn采用rootUrl做key，git采用url做key
                RepoSubModuleVO repoSubModuleVO = new RepoSubModuleVO();
                repoSubModuleVO.setRepoId(codeRepoInfo.getRepoId());
                if (ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(codeRepoInfo.getScmType())) {
                    codeRepoIdMap.put(codeRepoInfo.getRootUrl(), repoSubModuleVO);
                } else {
                    codeRepoIdMap.put(codeRepoInfo.getUrl(), repoSubModuleVO);
                    if (CollectionUtils.isNotEmpty(codeRepoInfo.getSubModules())) {
                        for (RepoSubModuleVO subModuleVO : codeRepoInfo.getSubModules()) {
                            RepoSubModuleVO subRepoSubModuleVO = new RepoSubModuleVO();
                            subRepoSubModuleVO.setRepoId(codeRepoInfo.getRepoId());
                            subRepoSubModuleVO.setSubModule(subModuleVO.getSubModule());
                            codeRepoIdMap.put(subModuleVO.getUrl(), subRepoSubModuleVO);
                        }
                    }
                }
            }
        }
        return codeRepoIdMap;
    }

    @NotNull
    public Map<Integer, ScmBlameChangeRecordVO> getLineAuthorMap(List<ScmBlameChangeRecordVO> changeRecords) {
        Map<Integer, ScmBlameChangeRecordVO> lineAuthorMap = new HashMap<>();
        for (ScmBlameChangeRecordVO changeRecord : changeRecords) {
            List<Object> lines = changeRecord.getLines();
            if (CollectionUtils.isNotEmpty(lines)) {
                for (Object line : lines) {
                    if (line instanceof Integer) {
                        lineAuthorMap.put((int) line, changeRecord);
                    } else {
                        if (line instanceof List) {
                            List<Integer> lineScope = (List<Integer>) line;
                            for (int i = lineScope.get(0); i <= lineScope.get(1); i++) {
                                lineAuthorMap.put(i, changeRecord);
                            }
                        }
                    }
                }
            }
        }
        return lineAuthorMap;
    }

    /**
     * 发送分析记录
     *
     * @param commitDefectVO
     * @param stepFlag
     * @param msg
     */
    protected void uploadTaskLog(CommitDefectVO commitDefectVO, int stepFlag, long startTime, long endTime,
                                 String msg) {
        UploadTaskLogStepVO uploadTaskLogStepVO = new UploadTaskLogStepVO();
        uploadTaskLogStepVO.setTaskId(commitDefectVO.getTaskId());
        uploadTaskLogStepVO.setStreamName(commitDefectVO.getStreamName());
        uploadTaskLogStepVO.setToolName(commitDefectVO.getToolName());
        uploadTaskLogStepVO.setStartTime(startTime);
        uploadTaskLogStepVO.setEndTime(endTime);
        uploadTaskLogStepVO.setFlag(stepFlag);
        uploadTaskLogStepVO.setMsg(msg);
        uploadTaskLogStepVO.setStepNum(ComConstants.Step4MutliTool.COMMIT.value());
        uploadTaskLogStepVO.setPipelineBuildId(commitDefectVO.getBuildId());
        uploadTaskLogStepVO.setTriggerFrom(commitDefectVO.getTriggerFrom());
        thirdPartySystemCaller.uploadTaskLog(uploadTaskLogStepVO);
    }

    protected void getCodeRepoMap(long taskId, boolean isFullScan, BuildEntity buildEntity,
                                  Map<String, CodeRepoEntity> repoIdMap, Map<String, CodeRepoEntity> urlMap) {
        if (!isFullScan) {
            // 校验构建号对应的仓库信息是否已存在
            CodeRepoInfoEntity codeRepoInfo = codeRepoRepository.findFirstByTaskIdAndBuildId(taskId,
                    buildEntity.getBuildId());
            if (codeRepoInfo != null && CollectionUtils.isNotEmpty(codeRepoInfo.getRepoList())) {
                repoIdMap.putAll(codeRepoInfo.getRepoList().stream().collect(Collectors.toMap(CodeRepoEntity::getRepoId,
                        Function.identity(), (k, v) -> v)));
                urlMap.putAll(codeRepoInfo.getRepoList().stream().collect(Collectors.toMap(CodeRepoEntity::getUrl,
                        Function.identity(), (k, v) -> v)));
            }
        }
    }
}
