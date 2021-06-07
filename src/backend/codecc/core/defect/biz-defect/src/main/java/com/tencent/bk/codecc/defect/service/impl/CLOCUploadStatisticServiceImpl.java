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

import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CLOCStatisticsDao;
import com.tencent.bk.codecc.defect.model.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.service.CLOCUploadStatisticService;
import com.tencent.bk.codecc.defect.vo.CLOCLanguageVO;
import com.tencent.bk.codecc.defect.vo.UploadCLOCStatisticVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigParamJsonVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * cloc工具上传统计数据接口
 *
 * @version V1.0
 * @date 2019/10/7
 */
@Deprecated
@Service
@Slf4j
public class CLOCUploadStatisticServiceImpl implements CLOCUploadStatisticService
{
    @Autowired
    private Client client;

    @Autowired
    private CLOCStatisticsDao clocStatisticsDao;

    @Autowired
    private CLOCStatisticRepository clocStatisticRepository;

    @Override
    public Result uploadStatistic(UploadCLOCStatisticVO uploadCLOCStatisticVO)
    {
        log.info("start to upload cloc statistic info!");
        Long taskId = uploadCLOCStatisticVO.getTaskId();
        Result<TaskDetailVO> taskDetailResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        if (taskDetailResult.isNotOk() || null == taskDetailResult.getData())
        {
            log.error("query task info fail when upload cloc statistic info!");
            return null;
        }
        TaskDetailVO taskDetailVO = taskDetailResult.getData();
        //先删除该task_id下面的
        clocStatisticsDao.batchDisableClocStatistic(taskId, uploadCLOCStatisticVO.getToolName());
        //更新统计信息
        List<CLOCLanguageVO> languageCodeList = uploadCLOCStatisticVO.getLanguageCodeList();
        if (CollectionUtils.isNotEmpty(languageCodeList))
        {
            Long currentTime = System.currentTimeMillis();
            languageCodeList.forEach(clocLanguageVO ->
            {
                CLOCStatisticEntity clocStatisticEntity = new CLOCStatisticEntity();
                clocStatisticEntity.setTaskId(uploadCLOCStatisticVO.getTaskId());
                clocStatisticEntity.setToolName(uploadCLOCStatisticVO.getToolName());
                clocStatisticEntity.setLanguage(clocLanguageVO.getLanguage());
                clocStatisticEntity.setSumCode(clocLanguageVO.getCodeSum());
                clocStatisticEntity.setSumBlank(clocLanguageVO.getBlankSum());
                clocStatisticEntity.setSumComment(clocLanguageVO.getCommentSum());
                clocStatisticEntity.setCreatedDate(currentTime);
                clocStatisticEntity.setUpdatedDate(currentTime);
                log.info("start to upload cloc statistic info, task id: {}, language: {}",
                        uploadCLOCStatisticVO.getTaskId(), clocLanguageVO.getLanguage());
                clocStatisticsDao.upsertCLOCStatistic(clocStatisticEntity);
            });
        }

        return new Result(CommonMessageCode.SUCCESS, "upload CLOC analysis statistic ok");
    }

    /**
     * 新增 cloc 工具 statics 信息
     * 不再以更新的方式记录 cloc 工具的统计信息，
     * 改为：根据 build_id 来记录每次构建的统计信息，
     * 之后可根据 build_id 查询到当前任务下的扫描统计历史记录
     *
     * @param buildId               当前告警上报构建ID
     * @param streamName            流名称
     * @param clocLanguageMap       按语言划分告警记录
     * @param uploadCLOCStatisticVO cloc 视图信息
     */
    @Override
    public Result<CommonMessageCode> uploadNewStatistic(UploadCLOCStatisticVO uploadCLOCStatisticVO, Map<String, List<CLOCDefectEntity>> clocLanguageMap, String buildId, String streamName)
    {
        long taskId = uploadCLOCStatisticVO.getTaskId();
        String toolName = uploadCLOCStatisticVO.getToolName();
        final List<CLOCLanguageVO> languageCodeList = uploadCLOCStatisticVO.getLanguageCodeList();
        // 获取当前task上一次构建ID
        CLOCStatisticEntity lastClocStatisticEntity =
                clocStatisticRepository.findFirstByTaskIdAndToolNameOrderByUpdatedDateDesc(taskId, toolName);
        String lastBuildId = null;
        List<CLOCStatisticEntity> lastClocStatisticEntityList = Collections.emptyList();
        if (lastClocStatisticEntity != null && StringUtils.isNotBlank(lastClocStatisticEntity.getBuildId()))
        {
            lastBuildId = lastClocStatisticEntity.getBuildId();
        }
        else if (lastClocStatisticEntity != null && StringUtils.isBlank(lastClocStatisticEntity.getBuildId()))
        {
            // 兼容旧逻辑产生的数据中没有 build_id、create_time 字段
            lastClocStatisticEntityList = clocStatisticRepository.findByTaskIdAndToolName(taskId, toolName);
        }
        log.info("get last cloc statistic buildId! taskId: {} | buildId: {} | currBuildId: {}", taskId, lastBuildId, buildId);

        // 获取当前task上次一构建的statistic记录
        if (StringUtils.isNotBlank(lastBuildId))
        {
            log.info("begin find cloc statistic info: taskId: {}, lastBuildId: {}", taskId, lastBuildId);
            lastClocStatisticEntityList = clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, lastBuildId);
        }
        log.info("get last cloc statistic buildId! taskId: {} | buildId: {} | currBuildId: {}, info: {}",
                taskId, lastBuildId, buildId, lastClocStatisticEntityList.size());

        Map<String, CLOCStatisticEntity> currStatisticMap = new HashMap<>();

        long currentTime = System.currentTimeMillis();
        lastClocStatisticEntityList.forEach(stepStatistic -> {
            CLOCStatisticEntity cloctStatistic =
                    currStatisticMap.getOrDefault(stepStatistic.getLanguage(), new CLOCStatisticEntity());
            cloctStatistic.setTaskId(taskId);
            cloctStatistic.setStreamName(streamName);
            cloctStatistic.setBuildId(buildId);
            if (stepStatistic.getCreatedDate() == null) {
                cloctStatistic.setCreatedDate(currentTime);
            } else {
                cloctStatistic.setCreatedDate(stepStatistic.getCreatedDate());
            }
            cloctStatistic.setUpdatedDate(currentTime);
            cloctStatistic.setToolName(uploadCLOCStatisticVO.getToolName());
            cloctStatistic.setLanguage(stepStatistic.getLanguage());

            // 如果本次告警没有这种语言的话，代码行数全为0，如果有的话则在下面的循环中赋值
            cloctStatistic.setSumBlank(0L);
            cloctStatistic.setSumCode(0L);
            cloctStatistic.setSumComment(0L);

            // 如果本次告警没有这种语言的话，行数变化就是（上次扫描的行数）
            cloctStatistic.setBlankChange(-(stepStatistic.getSumBlank() == null ? 0 : stepStatistic.getSumBlank()));
            cloctStatistic.setCodeChange(-(stepStatistic.getSumCode() == null ? 0 : stepStatistic.getSumCode()));
            cloctStatistic.setCommentChange(-(stepStatistic.getSumComment() == null ? 0 : stepStatistic.getSumComment()));
            cloctStatistic.setFileNum(0L);
            cloctStatistic.setFileNumChange(-(stepStatistic.getFileNum() == null ? 0 : stepStatistic.getFileNum()));
            currStatisticMap.put(stepStatistic.getLanguage(), cloctStatistic);
        });

        languageCodeList.forEach(stepLanguageVO -> {
            CLOCStatisticEntity clocStatistic =
                    currStatisticMap.getOrDefault(stepLanguageVO.getLanguage(), new CLOCStatisticEntity());
            clocStatistic.setTaskId(taskId);
            clocStatistic.setStreamName(streamName);
            clocStatistic.setBuildId(buildId);
            if (clocStatistic.getCreatedDate() == null) {
                clocStatistic.setCreatedDate(currentTime);
            }
            clocStatistic.setUpdatedDate(currentTime);
            clocStatistic.setToolName(uploadCLOCStatisticVO.getToolName());
            clocStatistic.setLanguage(stepLanguageVO.getLanguage());
            clocStatistic.setSumBlank(stepLanguageVO.getBlankSum());
            clocStatistic.setSumCode(stepLanguageVO.getCodeSum());
            clocStatistic.setSumComment(stepLanguageVO.getCommentSum());
            clocStatistic.setBlankChange(stepLanguageVO.getBlankSum() + (clocStatistic.getBlankChange() == null
                    ? 0 : clocStatistic.getBlankChange()));
            clocStatistic.setCodeChange(stepLanguageVO.getCodeSum() + (clocStatistic.getCodeChange() == null
                    ? 0 : clocStatistic.getCodeChange()));
            clocStatistic.setCommentChange(stepLanguageVO.getCommentSum() + (clocStatistic.getCommentChange() == null
                    ? 0 : clocStatistic.getCommentChange()));
            clocStatistic.setFileNum((long) clocLanguageMap.get(stepLanguageVO.getLanguage()).size());
            clocStatistic.setFileNumChange((long) clocLanguageMap.get(stepLanguageVO.getLanguage()).size()
                    + (clocStatistic.getFileNumChange() == null ? 0 : clocStatistic.getFileNumChange()));
            currStatisticMap.put(stepLanguageVO.getLanguage(), clocStatistic);
        });

        log.info("start to upload new cloc statistic info, task id: {}, build id: {}",
                taskId, buildId);

        clocStatisticsDao.batchUpsertCLOCStatistic(currStatisticMap.values());

        //如果本次为首次上报，且上报语言内容为空，则插入一条其他语言的记录
        if (MapUtils.isEmpty(currStatisticMap)
                && MapUtils.isEmpty(clocLanguageMap)) {
            log.info("first upload and empty upload need to insert others language,"
                            + " task id: {}, build id: {}", taskId, buildId);
            CLOCStatisticEntity clocStatisticEntity = new CLOCStatisticEntity();
            clocStatisticEntity.setTaskId(taskId);
            clocStatisticEntity.setStreamName(streamName);
            clocStatisticEntity.setBuildId(buildId);
            clocStatisticEntity.setCreatedDate(currentTime);
            clocStatisticEntity.setUpdatedDate(currentTime);
            clocStatisticEntity.setToolName(uploadCLOCStatisticVO.getToolName());
            clocStatisticEntity.setLanguage("OTHERS");
            clocStatisticEntity.setSumBlank(0L);
            clocStatisticEntity.setSumCode(0L);
            clocStatisticEntity.setSumComment(0L);
            clocStatisticEntity.setBlankChange(0L);
            clocStatisticEntity.setCodeChange(0L);
            clocStatisticEntity.setCommentChange(0L);
            clocStatisticEntity.setFileNum(0L);
            clocStatisticEntity.setFileNumChange(0L);
            clocStatisticRepository.save(clocStatisticEntity);
        }
        return new Result(CommonMessageCode.SUCCESS, "upload new defect statistic success");
    }


    /**
     * 根据语言设置规则集
     *
     * @param taskDetailVO
     * @param languages
     */
    private void setCheckerSetsAccordingToLanguage(TaskDetailVO taskDetailVO,
            List<String> languages) {
        List<CheckerSetVO> checkerSetVOList = new ArrayList<>();
        List<ToolConfigParamJsonVO> paramJsonVOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(languages)) {
            languages.forEach(language ->
                    {
                        switch (language) {
                            case "C#":
                                CheckerSetVO formatCheckerSet1 = new CheckerSetVO();
                                formatCheckerSet1.setCheckerSetId("standard_csharp");
                                formatCheckerSet1.setToolList(new HashSet<String>()
                                {{
                                    add("STYLECOP");
                                }});
                                checkerSetVOList.add(formatCheckerSet1);
                                CheckerSetVO formatCheckerSet1_1 = new CheckerSetVO();
                                formatCheckerSet1_1.setCheckerSetId("codecc_default_ccn_csharp");
                                formatCheckerSet1_1.setToolList(new HashSet<String>()
                                {{
                                    add("CCN");
                                }});
                                checkerSetVOList.add(formatCheckerSet1_1);
                                CheckerSetVO formatCheckerSet1_2 = new CheckerSetVO();
                                formatCheckerSet1_2.setCheckerSetId("codecc_default_dupc_csharp");
                                formatCheckerSet1_2.setToolList(new HashSet<String>()
                                {{
                                    add("DUPC");
                                }});
                                checkerSetVOList.add(formatCheckerSet1_2);
                                CheckerSetVO securityCheckerSet1 = new CheckerSetVO();
                                securityCheckerSet1.setCheckerSetId("pecker_csharp");
                                securityCheckerSet1.setToolList(new HashSet<String>()
                                {{
                                    add("WOODPECKER_SENSITIVE");
                                    add("COVERITY");
                                }});
                                checkerSetVOList.add(securityCheckerSet1);
                                break;
                            case "C++":
                                CheckerSetVO formatCheckerSet2 = new CheckerSetVO();
                                formatCheckerSet2.setCheckerSetId("standard_cpp");
                                formatCheckerSet2.setToolList(new HashSet<String>()
                                {{
                                    add("CPPLINT");
                                }});
                                checkerSetVOList.add(formatCheckerSet2);
                                CheckerSetVO formatCheckerSet2_1 = new CheckerSetVO();
                                formatCheckerSet2_1.setCheckerSetId("codecc_default_ccn_cpp");
                                formatCheckerSet2_1.setToolList(new HashSet<String>()
                                {{
                                    add("CCN");
                                }});
                                checkerSetVOList.add(formatCheckerSet2_1);
                                CheckerSetVO formatCheckerSet2_2 = new CheckerSetVO();
                                formatCheckerSet2_2.setCheckerSetId("codecc_default_dupc_cpp");
                                formatCheckerSet2_2.setToolList(new HashSet<String>()
                                {{
                                    add("DUPC");
                                }});
                                checkerSetVOList.add(formatCheckerSet2_2);
                                CheckerSetVO securityCheckerSet2 = new CheckerSetVO();
                                securityCheckerSet2.setCheckerSetId("pecker_cpp");
                                securityCheckerSet2.setToolList(new HashSet<String>()
                                {{
                                    add("WOODPECKER_SENSITIVE");
                                    add("COVERITY");
                                }});
                                checkerSetVOList.add(securityCheckerSet2);
                                break;
                            case "C/C++ Header":
                                CheckerSetVO formatCheckerSet3 = new CheckerSetVO();
                                formatCheckerSet3.setCheckerSetId("standard_cpp");
                                formatCheckerSet3.setToolList(new HashSet<String>()
                                {{
                                    add("CPPLINT");
                                }});
                                checkerSetVOList.add(formatCheckerSet3);
                                CheckerSetVO formatCheckerSet3_1 = new CheckerSetVO();
                                formatCheckerSet3_1.setCheckerSetId("codecc_default_ccn_cpp");
                                formatCheckerSet3_1.setToolList(new HashSet<String>()
                                {{
                                    add("CCN");
                                }});
                                checkerSetVOList.add(formatCheckerSet3_1);
                                CheckerSetVO formatCheckerSet3_2 = new CheckerSetVO();
                                formatCheckerSet3_2.setCheckerSetId("codecc_default_dupc_cpp");
                                formatCheckerSet3_2.setToolList(new HashSet<String>()
                                {{
                                    add("DUPC");
                                }});
                                checkerSetVOList.add(formatCheckerSet3_2);
                                CheckerSetVO securityCheckerSet3 = new CheckerSetVO();
                                securityCheckerSet3.setCheckerSetId("pecker_cpp");
                                securityCheckerSet3.setToolList(new HashSet<String>()
                                {{
                                    add("WOODPECKER_SENSITIVE");
                                    add("COVERITY");
                                }});
                                checkerSetVOList.add(securityCheckerSet3);
                                break;
                            case "Java":
                                CheckerSetVO formatCheckerSet4 = new CheckerSetVO();
                                formatCheckerSet4.setCheckerSetId("standard_java");
                                formatCheckerSet4.setToolList(new HashSet<String>()
                                {{
                                    add("CHECKSTYLE");
                                }});
                                checkerSetVOList.add(formatCheckerSet4);
                                CheckerSetVO formatCheckerSet4_1 = new CheckerSetVO();
                                formatCheckerSet4_1.setCheckerSetId("codecc_default_ccn_java");
                                formatCheckerSet4_1.setToolList(new HashSet<String>()
                                {{
                                    add("CCN");
                                }});
                                checkerSetVOList.add(formatCheckerSet4_1);
                                CheckerSetVO formatCheckerSet4_2 = new CheckerSetVO();
                                formatCheckerSet4_2.setCheckerSetId("codecc_default_dupc_java");
                                formatCheckerSet4_2.setToolList(new HashSet<String>()
                                {{
                                    add("DUPC");
                                }});
                                checkerSetVOList.add(formatCheckerSet4_2);
                                CheckerSetVO securityCheckerSet4 = new CheckerSetVO();
                                securityCheckerSet4.setCheckerSetId("pecker_java");
                                securityCheckerSet4.setToolList(new HashSet<String>()
                                {{
                                    add("WOODPECKER_SENSITIVE");
                                    add("COVERITY");
                                }});
                                checkerSetVOList.add(securityCheckerSet4);
                                break;
                            case "PHP":
                                CheckerSetVO formatCheckerSet5_1 = new CheckerSetVO();
                                formatCheckerSet5_1.setCheckerSetId("codecc_default_ccn_php");
                                formatCheckerSet5_1.setToolList(new HashSet<String>()
                                {{
                                    add("CCN");
                                }});
                                checkerSetVOList.add(formatCheckerSet5_1);
                                CheckerSetVO securityCheckerSet5 = new CheckerSetVO();
                                securityCheckerSet5.setCheckerSetId("pecker_php");
                                securityCheckerSet5.setToolList(new HashSet<String>()
                                {{
                                    add("WOODPECKER_SENSITIVE");
                                    add("HORUSPY");
                                    add("RIPS");
                                }});
                                checkerSetVOList.add(securityCheckerSet5);
                                break;
                            case "Objective C":
                                CheckerSetVO formatCheckerSet6 = new CheckerSetVO();
                                formatCheckerSet6.setCheckerSetId("standard_oc");
                                formatCheckerSet6.setToolList(new HashSet<String>()
                                {{
                                    add("OCCHECK");
                                }});
                                checkerSetVOList.add(formatCheckerSet6);
                                CheckerSetVO formatCheckerSet6_1 = new CheckerSetVO();
                                formatCheckerSet6_1.setCheckerSetId("codecc_default_ccn_oc");
                                formatCheckerSet6_1.setToolList(new HashSet<String>()
                                {{
                                    add("CCN");
                                }});
                                checkerSetVOList.add(formatCheckerSet6);
                                CheckerSetVO formatCheckerSet6_2 = new CheckerSetVO();
                                formatCheckerSet6_2.setCheckerSetId("codecc_default_dupc_oc");
                                formatCheckerSet6_2.setToolList(new HashSet<String>()
                                {{
                                    add("DUPC");
                                }});
                                checkerSetVOList.add(formatCheckerSet6_2);
                                CheckerSetVO securityCheckerSet6 = new CheckerSetVO();
                                securityCheckerSet6.setCheckerSetId("pecker_oc");
                                securityCheckerSet6.setToolList(new HashSet<String>()
                                {{
                                    add("WOODPECKER_SENSITIVE");
                                    add("COVERITY");
                                }});
                                checkerSetVOList.add(securityCheckerSet6);
                                break;
                            case "Objective C++":
                                CheckerSetVO formatCheckerSet7 = new CheckerSetVO();
                                formatCheckerSet7.setCheckerSetId("standard_oc");
                                formatCheckerSet7.setToolList(new HashSet<String>()
                                {{
                                    add("OCCHECK");
                                }});
                                checkerSetVOList.add(formatCheckerSet7);
                                CheckerSetVO formatCheckerSet7_1 = new CheckerSetVO();
                                formatCheckerSet7_1.setCheckerSetId("codecc_default_ccn_oc");
                                formatCheckerSet7_1.setToolList(new HashSet<String>()
                                {{
                                    add("CCN");
                                }});
                                checkerSetVOList.add(formatCheckerSet7_1);
                                CheckerSetVO formatCheckerSet7_2 = new CheckerSetVO();
                                formatCheckerSet7_2.setCheckerSetId("codecc_default_dupc_oc");
                                formatCheckerSet7_2.setToolList(new HashSet<String>()
                                {{
                                    add("DUPC");
                                }});
                                checkerSetVOList.add(formatCheckerSet7_2);
                                CheckerSetVO securityCheckerSet7 = new CheckerSetVO();
                                securityCheckerSet7.setCheckerSetId("pecker_oc");
                                securityCheckerSet7.setToolList(new HashSet<String>()
                                {{
                                    add("WOODPECKER_SENSITIVE");
                                    add("COVERITY");
                                }});
                                checkerSetVOList.add(securityCheckerSet7);
                                break;
                            case "Python":
                                CheckerSetVO formatCheckerSet8 = new CheckerSetVO();
                                formatCheckerSet8.setCheckerSetId("standard_python");
                                formatCheckerSet8.setToolList(new HashSet<String>()
                                {{
                                    add("PYLINT");
                                }});
                                checkerSetVOList.add(formatCheckerSet8);
                                CheckerSetVO formatCheckerSet8_1 = new CheckerSetVO();
                                formatCheckerSet8_1.setCheckerSetId("codecc_default_ccn_python");
                                formatCheckerSet8_1.setToolList(new HashSet<String>()
                                {{
                                    add("CCN");
                                }});
                                checkerSetVOList.add(formatCheckerSet8_1);
                                CheckerSetVO formatCheckerSet8_2 = new CheckerSetVO();
                                formatCheckerSet8_2.setCheckerSetId("codecc_default_dupc_python");
                                formatCheckerSet8_2.setToolList(new HashSet<String>()
                                {{
                                    add("DUPC");
                                }});
                                checkerSetVOList.add(formatCheckerSet8_2);
                                CheckerSetVO securityCheckerSet8 = new CheckerSetVO();
                                securityCheckerSet8.setCheckerSetId("pecker_python");
                                securityCheckerSet8.setToolList(new HashSet<String>()
                                {{
                                    add("WOODPECKER_SENSITIVE");
                                    add("HORUSPY");
                                    add("COVERITY");
                                }});
                                checkerSetVOList.add(securityCheckerSet8);
                                ToolConfigParamJsonVO toolConfigParamJsonVO = new ToolConfigParamJsonVO();
                                toolConfigParamJsonVO.setToolName("PYLINT");
                                toolConfigParamJsonVO.setVarName("py_version");
                                toolConfigParamJsonVO.setChooseValue("py3");
                                paramJsonVOList.add(toolConfigParamJsonVO);
                                break;
                            case "JavaScript":
                                CheckerSetVO formatCheckerSet9 = new CheckerSetVO();
                                formatCheckerSet9.setCheckerSetId("standard_js");
                                formatCheckerSet9.setToolList(new HashSet<String>()
                                {{
                                    add("ESLINT");
                                }});
                                checkerSetVOList.add(formatCheckerSet9);
                                CheckerSetVO formatCheckerSet9_1 = new CheckerSetVO();
                                formatCheckerSet9_1.setCheckerSetId("codecc_default_ccn_js");
                                formatCheckerSet9_1.setToolList(new HashSet<String>()
                                {{
                                    add("CCN");
                                }});
                                checkerSetVOList.add(formatCheckerSet9_1);
                                CheckerSetVO formatCheckerSet9_2 = new CheckerSetVO();
                                formatCheckerSet9_2.setCheckerSetId("codecc_default_dupc_js");
                                formatCheckerSet9_2.setToolList(new HashSet<String>()
                                {{
                                    add("DUPC");
                                }});
                                checkerSetVOList.add(formatCheckerSet9_2);
                                CheckerSetVO securityCheckerSet9 = new CheckerSetVO();
                                securityCheckerSet9.setCheckerSetId("pecker_js");
                                securityCheckerSet9.setToolList(new HashSet<String>()
                                {{
                                    add("WOODPECKER_SENSITIVE");
                                    add("HORUSPY");
                                    add("COVERITY");
                                }});
                                checkerSetVOList.add(securityCheckerSet9);
                                break;
                            case "Ruby":
                                CheckerSetVO formatCheckerSet10_1 = new CheckerSetVO();
                                formatCheckerSet10_1.setCheckerSetId("codecc_default_ccn_ruby");
                                formatCheckerSet10_1.setToolList(new HashSet<String>()
                                {{
                                    add("CCN");
                                }});
                                checkerSetVOList.add(formatCheckerSet10_1);
                                CheckerSetVO securityCheckerSet10 = new CheckerSetVO();
                                securityCheckerSet10.setCheckerSetId("pecker_ruby");
                                securityCheckerSet10.setToolList(new HashSet<String>()
                                {{
                                    add("WOODPECKER_SENSITIVE");
                                    add("HORUSPY");
                                    add("COVERITY");
                                }});
                                checkerSetVOList.add(securityCheckerSet10);
                                break;
                            case "Go":
                                CheckerSetVO formatCheckerSet11 = new CheckerSetVO();
                                formatCheckerSet11.setCheckerSetId("standard_go");
                                formatCheckerSet11.setToolList(new HashSet<String>()
                                {{
                                    add("GOML");
                                }});
                                checkerSetVOList.add(formatCheckerSet11);
                                CheckerSetVO formatCheckerSet11_1 = new CheckerSetVO();
                                formatCheckerSet11_1.setCheckerSetId("codecc_default_ccn_go");
                                formatCheckerSet11_1.setToolList(new HashSet<String>()
                                {{
                                    add("CCN");
                                }});
                                checkerSetVOList.add(formatCheckerSet11_1);
                                CheckerSetVO formatCheckerSet11_2 = new CheckerSetVO();
                                formatCheckerSet11_2.setCheckerSetId("codecc_default_dupc_go");
                                formatCheckerSet11_2.setToolList(new HashSet<String>()
                                {{
                                    add("DUPC");
                                }});
                                checkerSetVOList.add(formatCheckerSet11_2);
                                CheckerSetVO securityCheckerSet11 = new CheckerSetVO();
                                securityCheckerSet11.setCheckerSetId("pecker_go");
                                securityCheckerSet11.setToolList(new HashSet<String>()
                                {{
                                    add("WOODPECKER_SENSITIVE");
                                    add("COVERITY");
                                }});
                                checkerSetVOList.add(securityCheckerSet11);
                                break;
                            case "Swift":
                                CheckerSetVO securityCheckerSet12 = new CheckerSetVO();
                                securityCheckerSet12.setCheckerSetId("pecker_swift");
                                securityCheckerSet12.setToolList(new HashSet<String>()
                                {{
                                    add("COVERITY");
                                }});
                                checkerSetVOList.add(securityCheckerSet12);
                                break;
                            case "TypeScript":
                                CheckerSetVO securityCheckerSet13 = new CheckerSetVO();
                                securityCheckerSet13.setCheckerSetId("pecker_ts");
                                securityCheckerSet13.setToolList(new HashSet<String>()
                                {{
                                    add("WOODPECKER_SENSITIVE");
                                    add("COVERITY");
                                }});
                                checkerSetVOList.add(securityCheckerSet13);
                                break;
                            default:
                                break;
                        }
                    }
            );
            taskDetailVO.setCheckerSetList(checkerSetVOList);
            taskDetailVO.setDevopsToolParams(paramJsonVOList);
        }
    }


    /**
     * 根据语言设置工具
     *
     * @param languages
     * @return
     */
    private List<String> setToolAccordingToLanguage(List<String> languages)
    {
        List<String> toolList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(languages))
        {
            languages.forEach(language ->
            {
                switch (language)
                {
                    case "C#":
                        toolList.add(ComConstants.Tool.STYLECOP.name());
                        toolList.add(ComConstants.Tool.SENSITIVE.name());
                        toolList.add(ComConstants.Tool.DUPC.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
                    case "C++":
                        toolList.add(ComConstants.Tool.CPPLINT.name());
                        toolList.add(ComConstants.Tool.SENSITIVE.name());
                        toolList.add(ComConstants.Tool.DUPC.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
                    case "C/C++ Header":
                        toolList.add(ComConstants.Tool.CPPLINT.name());
                        toolList.add(ComConstants.Tool.SENSITIVE.name());
                        toolList.add(ComConstants.Tool.DUPC.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
                    case "Java":
                        toolList.add(ComConstants.Tool.CHECKSTYLE.name());
                        toolList.add(ComConstants.Tool.SENSITIVE.name());
                        toolList.add(ComConstants.Tool.DUPC.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
                    case "PHP":
                        toolList.add(ComConstants.Tool.COVERITY.name());
                        toolList.add(ComConstants.Tool.PHPCS.name());
                        toolList.add(ComConstants.Tool.RIPS.name());
                        toolList.add(ComConstants.Tool.SENSITIVE.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
                    case "Objective C":
                        toolList.add(ComConstants.Tool.OCCHECK.name());
                        toolList.add(ComConstants.Tool.SENSITIVE.name());
                        toolList.add(ComConstants.Tool.DUPC.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
                    case "Objective C++":
                        toolList.add(ComConstants.Tool.OCCHECK.name());
                        toolList.add(ComConstants.Tool.SENSITIVE.name());
                        toolList.add(ComConstants.Tool.DUPC.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
                    case "Python":
                        toolList.add(ComConstants.Tool.COVERITY.name());
                        toolList.add(ComConstants.Tool.PYLINT.name());
                        toolList.add(ComConstants.Tool.SENSITIVE.name());
                        toolList.add(ComConstants.Tool.DUPC.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.HORUSPY.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
                    case "JavaScript":
                        toolList.add(ComConstants.Tool.COVERITY.name());
                        toolList.add(ComConstants.Tool.ESLINT.name());
                        toolList.add(ComConstants.Tool.SENSITIVE.name());
                        toolList.add(ComConstants.Tool.DUPC.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.HORUSPY.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
                    case "Ruby":
                        toolList.add(ComConstants.Tool.COVERITY.name());
                        toolList.add(ComConstants.Tool.SENSITIVE.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.HORUSPY.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
//                    case "LUA":
//                        toolList.add(ComConstants.Tool.TSCLUA.name());
//                        break;
                    case "Go":
                        toolList.add(ComConstants.Tool.GOML.name());
                        toolList.add(ComConstants.Tool.DUPC.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
                    case "Swift":
                        toolList.add(ComConstants.Tool.COVERITY.name());
                        toolList.add(ComConstants.Tool.SENSITIVE.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
                    case "TypeScript":
                        toolList.add(ComConstants.Tool.COVERITY.name());
                        toolList.add(ComConstants.Tool.SENSITIVE.name());
                        toolList.add(ComConstants.Tool.CCN.name());
                        toolList.add(ComConstants.Tool.WOODPECKER_SENSITIVE.name());
                        break;
                    default:
                        break;
                }
            });
        }
        return toolList;
    }
}
