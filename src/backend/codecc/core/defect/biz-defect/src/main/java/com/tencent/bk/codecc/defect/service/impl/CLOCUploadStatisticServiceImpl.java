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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CLOCStatisticsDao;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.service.CLOCUploadStatisticService;
import com.tencent.bk.codecc.defect.service.IConfigCheckerPkgBizService;
import com.tencent.bk.codecc.defect.vo.CLOCLanguageVO;
import com.tencent.bk.codecc.defect.vo.UploadCLOCStatisticVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigParamJsonVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * cloc工具上传统计数据接口
 *
 * @version V1.0
 * @date 2019/10/7
 */
@Service
public class CLOCUploadStatisticServiceImpl implements CLOCUploadStatisticService
{
    private static Logger logger = LoggerFactory.getLogger(CLOCUploadStatisticServiceImpl.class);

    @Autowired
    private Client client;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CLOCStatisticsDao clocStatisticsDao;

    @Autowired
    private CLOCStatisticRepository clocStatisticRepository;

    @Autowired
    private IConfigCheckerPkgBizService configCheckerPkgBizService;

    @Override
    public CodeCCResult uploadStatistic(UploadCLOCStatisticVO uploadCLOCStatisticVO)
    {
        logger.info("start to upload cloc statistic info!");
        Long taskId = uploadCLOCStatisticVO.getTaskId();
        CodeCCResult<TaskDetailVO> taskDetailCodeCCResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        if (taskDetailCodeCCResult.isNotOk() || null == taskDetailCodeCCResult.getData())
        {
            logger.error("query task info fail when upload cloc statistic info!");
            return null;
        }
        TaskDetailVO taskDetailVO = taskDetailCodeCCResult.getData();
        //先删除该task_id下面的
        clocStatisticsDao.batchDisableClocStatistic(taskId);
        //更新统计信息
        List<CLOCLanguageVO> languageCodeList = uploadCLOCStatisticVO.getLanguageCodeList();
        if(CollectionUtils.isNotEmpty(languageCodeList))
        {
            Long currentTime = System.currentTimeMillis();
            languageCodeList.forEach(clocLanguageVO -> {
                CLOCStatisticEntity clocStatisticEntity = new CLOCStatisticEntity();
                clocStatisticEntity.setTaskId(uploadCLOCStatisticVO.getTaskId());
                clocStatisticEntity.setToolName("CLOC");
                clocStatisticEntity.setLanguage(clocLanguageVO.getLanguage());
                clocStatisticEntity.setSumCode(clocLanguageVO.getCodeSum());
                clocStatisticEntity.setSumBlank(clocLanguageVO.getBlankSum());
                clocStatisticEntity.setSumComment(clocLanguageVO.getCommentSum());
                clocStatisticEntity.setCreatedDate(currentTime);
                clocStatisticEntity.setUpdatedDate(currentTime);
                logger.info("start to upload cloc statistic info, task id: {}, language: {}",
                        uploadCLOCStatisticVO.getTaskId(), clocLanguageVO.getLanguage());
                clocStatisticsDao.upsertCLOCStatistic(clocStatisticEntity);
            });
        }


        /*if (gongfengFlag != null && gongfengFlag)
        {
            List<String> languages = uploadCLOCStatisticVO.getLanguages();
            taskDetailVO.setAtomCode("CodeccCheckAtom");
            setCheckerSetsAccordingToLanguage(taskDetailVO, languages);
            logger.info("checker set id size: {}", taskDetailVO.getCheckerSetList().size());
            taskDetailVO.setGongfengFlag(false);
            *//*List<String> toolList = setToolAccordingToLanguage(languages);
            if (CollectionUtils.isEmpty(toolList))
            {
                logger.info("qualified tool list is empty!");
                return null;
            }*//*
            //将taskDetail里面加上devopsTools参数
//            toolList.add(ComConstants.Tool.CLOC.name());
            try
            {
                if(StringUtils.isEmpty(taskDetailVO.getPipelineName()))
                {
                    if(null != taskDetailVO.getGongfengProjectId())
                    {
                        taskDetailVO.setPipelineName(String.format("CODEPIPELINE_%d", taskDetailVO.getGongfengProjectId()));
                    }
                    else
                    {
                        taskDetailVO.setPipelineName(taskDetailVO.getNameCn());
                    }
                }
                taskDetailVO.setDevopsCodeLang(objectMapper.writeValueAsString(languages));
            }
            catch (JsonProcessingException e)
            {
                logger.error("serialize tool info fail!");
                e.printStackTrace();
                return null;
            }

            //注册工具清单及工蜂flag
            Result<TaskIdVO> taskResult = client.get(ServiceTaskRestResource.class).registerPipelineTask(taskDetailVO, taskDetailVO.getProjectId(), taskDetailVO.getTaskOwner().get(0));
            if (taskResult.isNotOk() || null == taskResult.getData())
            {
                logger.error("update task info fail! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }

            *//*if(toolList.contains(ComConstants.Tool.COVERITY.name())){
                configCheckerPkgBizService.syncConfigCheckerPkg(taskId, ComConstants.Tool.COVERITY.name(),
                        new ConfigCheckersPkgReqVO(
                                "20",
                                taskId,
                                ComConstants.Tool.COVERITY.name(),
                                new ArrayList<String>(){{
                                    add("CPP_APPID_SECRET");
                                    add("CPP_APPID_SECRET");
                                    add("DANGER_FUNS");
                                    add("JAVA_OPEN_REDIRECT");
                                    add("JAVA_DATABASE_PWD");
                                    add("JAVA_SQL_INJECT");
                                    add("JAVA_SSRF");
                                    add("CPP_KEY_LEAK");
                                    add("JAVA_XSS");
                                    add("JAVA_OGNL_INJECT");
                                    add("JAVA_FILE_UPLOAD");
                                    add("JAVA_XXE");
                                    add("JAVA_HOST_PWD");
                                    add("JAVA_SSL_MIMT");
                                    add("JAVA_COMMAND_INJECTION");
                                    add("JAVA_MAIL_PWD");
                                    add("CPP_DATABASE_PWD");
                                    add("FORMAT_STRING");
                                    add("CPP_HOST_PWD");
                                    add("JS_DOM_XSS");
                                    add("JAVA_APPID_SECRET");
                                    add("JAVA_KEY_LEAK");
                                }},
                                new ArrayList<>()
                        ));
            }*//*


            //更新流水线工具信息
            *//*Result<Boolean> pipelineResult = client.get(ServiceToolRestResource.class).updatePipelineTool(taskId, taskDetailVO.getTaskOwner().get(0), toolList);
            if (pipelineResult.isNotOk() || true != pipelineResult.getData())
            {
                logger.error("update pipeline info fail! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }*//*

            logger.info("update gongfeng task info by cloc info successfully! task id: {}", uploadCLOCStatisticVO.getTaskId());
        }*/

        return new CodeCCResult(CommonMessageCode.SUCCESS, "upload CLOC analysis statistic ok");
    }


    /**
     * 根据语言设置规则集
     * @param taskDetailVO
     * @param languages
     */
    private void setCheckerSetsAccordingToLanguage(TaskDetailVO taskDetailVO, List<String> languages)
    {
        List<CheckerSetVO> checkerSetVOList = new ArrayList<>();
        List<ToolConfigParamJsonVO> paramJsonVOList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(languages))
        {
            languages.forEach(language ->
                    {
                        switch (language)
                        {
                            case "C#":
                                CheckerSetVO formatCheckerSet1 = new CheckerSetVO();
                                formatCheckerSet1.setCheckerSetId("standard_csharp");
                                formatCheckerSet1.setToolList(new HashSet<String>(){{add("STYLECOP");}});
                                checkerSetVOList.add(formatCheckerSet1);
                                CheckerSetVO formatCheckerSet1_1 = new CheckerSetVO();
                                formatCheckerSet1_1.setCheckerSetId("codecc_default_ccn_csharp");
                                formatCheckerSet1_1.setToolList(new HashSet<String>(){{add("CCN");}});
                                checkerSetVOList.add(formatCheckerSet1_1);
                                CheckerSetVO formatCheckerSet1_2 = new CheckerSetVO();
                                formatCheckerSet1_2.setCheckerSetId("codecc_default_dupc_csharp");
                                formatCheckerSet1_2.setToolList(new HashSet<String>(){{add("DUPC");}});
                                checkerSetVOList.add(formatCheckerSet1_2);
                                CheckerSetVO securityCheckerSet1 = new CheckerSetVO();
                                securityCheckerSet1.setCheckerSetId("pecker_csharp");
                                securityCheckerSet1.setToolList(new HashSet<String>(){{add("WOODPECKER_SENSITIVE");add("COVERITY");}});
                                checkerSetVOList.add(securityCheckerSet1);
                                break;
                            case "C++":
                                CheckerSetVO formatCheckerSet2 = new CheckerSetVO();
                                formatCheckerSet2.setCheckerSetId("standard_cpp");
                                formatCheckerSet2.setToolList(new HashSet<String>(){{add("CPPLINT");}});
                                checkerSetVOList.add(formatCheckerSet2);
                                CheckerSetVO formatCheckerSet2_1 = new CheckerSetVO();
                                formatCheckerSet2_1.setCheckerSetId("codecc_default_ccn_cpp");
                                formatCheckerSet2_1.setToolList(new HashSet<String>(){{add("CCN");}});
                                checkerSetVOList.add(formatCheckerSet2_1);
                                CheckerSetVO formatCheckerSet2_2 = new CheckerSetVO();
                                formatCheckerSet2_2.setCheckerSetId("codecc_default_dupc_cpp");
                                formatCheckerSet2_2.setToolList(new HashSet<String>(){{add("DUPC");}});
                                checkerSetVOList.add(formatCheckerSet2_2);
                                CheckerSetVO securityCheckerSet2 = new CheckerSetVO();
                                securityCheckerSet2.setCheckerSetId("pecker_cpp");
                                securityCheckerSet2.setToolList(new HashSet<String>(){{add("WOODPECKER_SENSITIVE");add("COVERITY");}});
                                checkerSetVOList.add(securityCheckerSet2);
                                break;
                            case "C/C++ Header":
                                CheckerSetVO formatCheckerSet3 = new CheckerSetVO();
                                formatCheckerSet3.setCheckerSetId("standard_cpp");
                                formatCheckerSet3.setToolList(new HashSet<String>(){{add("CPPLINT");}});
                                checkerSetVOList.add(formatCheckerSet3);
                                CheckerSetVO formatCheckerSet3_1 = new CheckerSetVO();
                                formatCheckerSet3_1.setCheckerSetId("codecc_default_ccn_cpp");
                                formatCheckerSet3_1.setToolList(new HashSet<String>(){{add("CCN");}});
                                checkerSetVOList.add(formatCheckerSet3_1);
                                CheckerSetVO formatCheckerSet3_2 = new CheckerSetVO();
                                formatCheckerSet3_2.setCheckerSetId("codecc_default_dupc_cpp");
                                formatCheckerSet3_2.setToolList(new HashSet<String>(){{add("DUPC");}});
                                checkerSetVOList.add(formatCheckerSet3_2);
                                CheckerSetVO securityCheckerSet3 = new CheckerSetVO();
                                securityCheckerSet3.setCheckerSetId("pecker_cpp");
                                securityCheckerSet3.setToolList(new HashSet<String>(){{add("WOODPECKER_SENSITIVE");add("COVERITY");}});
                                checkerSetVOList.add(securityCheckerSet3);
                                break;
                            case "Java":
                                CheckerSetVO formatCheckerSet4 = new CheckerSetVO();
                                formatCheckerSet4.setCheckerSetId("standard_java");
                                formatCheckerSet4.setToolList(new HashSet<String>(){{add("CHECKSTYLE");}});
                                checkerSetVOList.add(formatCheckerSet4);
                                CheckerSetVO formatCheckerSet4_1 = new CheckerSetVO();
                                formatCheckerSet4_1.setCheckerSetId("codecc_default_ccn_java");
                                formatCheckerSet4_1.setToolList(new HashSet<String>(){{add("CCN");}});
                                checkerSetVOList.add(formatCheckerSet4_1);
                                CheckerSetVO formatCheckerSet4_2 = new CheckerSetVO();
                                formatCheckerSet4_2.setCheckerSetId("codecc_default_dupc_java");
                                formatCheckerSet4_2.setToolList(new HashSet<String>(){{add("DUPC");}});
                                checkerSetVOList.add(formatCheckerSet4_2);
                                CheckerSetVO securityCheckerSet4 = new CheckerSetVO();
                                securityCheckerSet4.setCheckerSetId("pecker_java");
                                securityCheckerSet4.setToolList(new HashSet<String>(){{add("WOODPECKER_SENSITIVE");add("COVERITY");}});
                                checkerSetVOList.add(securityCheckerSet4);
                                break;
                            case "PHP":
                                CheckerSetVO formatCheckerSet5_1 = new CheckerSetVO();
                                formatCheckerSet5_1.setCheckerSetId("codecc_default_ccn_php");
                                formatCheckerSet5_1.setToolList(new HashSet<String>(){{add("CCN");}});
                                checkerSetVOList.add(formatCheckerSet5_1);
                                CheckerSetVO securityCheckerSet5 = new CheckerSetVO();
                                securityCheckerSet5.setCheckerSetId("pecker_php");
                                securityCheckerSet5.setToolList(new HashSet<String>(){{add("WOODPECKER_SENSITIVE");add("HORUSPY");add("RIPS");}});
                                checkerSetVOList.add(securityCheckerSet5);
                                break;
                            case "Objective C":
                                CheckerSetVO formatCheckerSet6 = new CheckerSetVO();
                                formatCheckerSet6.setCheckerSetId("standard_oc");
                                formatCheckerSet6.setToolList(new HashSet<String>(){{add("OCCHECK");}});
                                checkerSetVOList.add(formatCheckerSet6);
                                CheckerSetVO formatCheckerSet6_1 = new CheckerSetVO();
                                formatCheckerSet6_1.setCheckerSetId("codecc_default_ccn_oc");
                                formatCheckerSet6_1.setToolList(new HashSet<String>(){{add("CCN");}});
                                checkerSetVOList.add(formatCheckerSet6);
                                CheckerSetVO formatCheckerSet6_2 = new CheckerSetVO();
                                formatCheckerSet6_2.setCheckerSetId("codecc_default_dupc_oc");
                                formatCheckerSet6_2.setToolList(new HashSet<String>(){{add("DUPC");}});
                                checkerSetVOList.add(formatCheckerSet6_2);
                                CheckerSetVO securityCheckerSet6 = new CheckerSetVO();
                                securityCheckerSet6.setCheckerSetId("pecker_oc");
                                securityCheckerSet6.setToolList(new HashSet<String>(){{add("WOODPECKER_SENSITIVE");add("COVERITY");}});
                                checkerSetVOList.add(securityCheckerSet6);
                                break;
                            case "Objective C++":
                                CheckerSetVO formatCheckerSet7 = new CheckerSetVO();
                                formatCheckerSet7.setCheckerSetId("standard_oc");
                                formatCheckerSet7.setToolList(new HashSet<String>(){{add("OCCHECK");}});
                                checkerSetVOList.add(formatCheckerSet7);
                                CheckerSetVO formatCheckerSet7_1 = new CheckerSetVO();
                                formatCheckerSet7_1.setCheckerSetId("codecc_default_ccn_oc");
                                formatCheckerSet7_1.setToolList(new HashSet<String>(){{add("CCN");}});
                                checkerSetVOList.add(formatCheckerSet7_1);
                                CheckerSetVO formatCheckerSet7_2 = new CheckerSetVO();
                                formatCheckerSet7_2.setCheckerSetId("codecc_default_dupc_oc");
                                formatCheckerSet7_2.setToolList(new HashSet<String>(){{add("DUPC");}});
                                checkerSetVOList.add(formatCheckerSet7_2);
                                CheckerSetVO securityCheckerSet7 = new CheckerSetVO();
                                securityCheckerSet7.setCheckerSetId("pecker_oc");
                                securityCheckerSet7.setToolList(new HashSet<String>(){{add("WOODPECKER_SENSITIVE");add("COVERITY");}});
                                checkerSetVOList.add(securityCheckerSet7);
                                break;
                            case "Python":
                                CheckerSetVO formatCheckerSet8 = new CheckerSetVO();
                                formatCheckerSet8.setCheckerSetId("standard_python");
                                formatCheckerSet8.setToolList(new HashSet<String>(){{add("PYLINT");}});
                                checkerSetVOList.add(formatCheckerSet8);
                                CheckerSetVO formatCheckerSet8_1 = new CheckerSetVO();
                                formatCheckerSet8_1.setCheckerSetId("codecc_default_ccn_python");
                                formatCheckerSet8_1.setToolList(new HashSet<String>(){{add("CCN");}});
                                checkerSetVOList.add(formatCheckerSet8_1);
                                CheckerSetVO formatCheckerSet8_2 = new CheckerSetVO();
                                formatCheckerSet8_2.setCheckerSetId("codecc_default_dupc_python");
                                formatCheckerSet8_2.setToolList(new HashSet<String>(){{add("DUPC");}});
                                checkerSetVOList.add(formatCheckerSet8_2);
                                CheckerSetVO securityCheckerSet8 = new CheckerSetVO();
                                securityCheckerSet8.setCheckerSetId("pecker_python");
                                securityCheckerSet8.setToolList(new HashSet<String>(){{add("WOODPECKER_SENSITIVE");add("HORUSPY");add("COVERITY");}});
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
                                formatCheckerSet9.setToolList(new HashSet<String>(){{add("ESLINT");}});
                                checkerSetVOList.add(formatCheckerSet9);
                                CheckerSetVO formatCheckerSet9_1 = new CheckerSetVO();
                                formatCheckerSet9_1.setCheckerSetId("codecc_default_ccn_js");
                                formatCheckerSet9_1.setToolList(new HashSet<String>(){{add("CCN");}});
                                checkerSetVOList.add(formatCheckerSet9_1);
                                CheckerSetVO formatCheckerSet9_2 = new CheckerSetVO();
                                formatCheckerSet9_2.setCheckerSetId("codecc_default_dupc_js");
                                formatCheckerSet9_2.setToolList(new HashSet<String>(){{add("DUPC");}});
                                checkerSetVOList.add(formatCheckerSet9_2);
                                CheckerSetVO securityCheckerSet9 = new CheckerSetVO();
                                securityCheckerSet9.setCheckerSetId("pecker_js");
                                securityCheckerSet9.setToolList(new HashSet<String>(){{add("WOODPECKER_SENSITIVE");add("HORUSPY");add("COVERITY");}});
                                checkerSetVOList.add(securityCheckerSet9);
                                break;
                            case "Ruby":
                                CheckerSetVO formatCheckerSet10_1 = new CheckerSetVO();
                                formatCheckerSet10_1.setCheckerSetId("codecc_default_ccn_ruby");
                                formatCheckerSet10_1.setToolList(new HashSet<String>(){{add("CCN");}});
                                checkerSetVOList.add(formatCheckerSet10_1);
                                CheckerSetVO securityCheckerSet10 = new CheckerSetVO();
                                securityCheckerSet10.setCheckerSetId("pecker_ruby");
                                securityCheckerSet10.setToolList(new HashSet<String>(){{add("WOODPECKER_SENSITIVE");add("HORUSPY");add("COVERITY");}});
                                checkerSetVOList.add(securityCheckerSet10);
                                break;
                            case "Go":
                                CheckerSetVO formatCheckerSet11 = new CheckerSetVO();
                                formatCheckerSet11.setCheckerSetId("standard_go");
                                formatCheckerSet11.setToolList(new HashSet<String>(){{add("GOML");}});
                                checkerSetVOList.add(formatCheckerSet11);
                                CheckerSetVO formatCheckerSet11_1 = new CheckerSetVO();
                                formatCheckerSet11_1.setCheckerSetId("codecc_default_ccn_go");
                                formatCheckerSet11_1.setToolList(new HashSet<String>(){{add("CCN");}});
                                checkerSetVOList.add(formatCheckerSet11_1);
                                CheckerSetVO formatCheckerSet11_2 = new CheckerSetVO();
                                formatCheckerSet11_2.setCheckerSetId("codecc_default_dupc_go");
                                formatCheckerSet11_2.setToolList(new HashSet<String>(){{add("DUPC");}});
                                checkerSetVOList.add(formatCheckerSet11_2);
                                CheckerSetVO securityCheckerSet11 = new CheckerSetVO();
                                securityCheckerSet11.setCheckerSetId("pecker_go");
                                securityCheckerSet11.setToolList(new HashSet<String>(){{add("WOODPECKER_SENSITIVE");add("COVERITY");}});
                                checkerSetVOList.add(securityCheckerSet11);
                                break;
                            case "Swift":
                                CheckerSetVO securityCheckerSet12 = new CheckerSetVO();
                                securityCheckerSet12.setCheckerSetId("pecker_swift");
                                securityCheckerSet12.setToolList(new HashSet<String>(){{add("COVERITY");}});
                                checkerSetVOList.add(securityCheckerSet12);
                                break;
                            case "TypeScript":
                                CheckerSetVO securityCheckerSet13 = new CheckerSetVO();
                                securityCheckerSet13.setCheckerSetId("pecker_ts");
                                securityCheckerSet13.setToolList(new HashSet<String>(){{add("WOODPECKER_SENSITIVE");add("COVERITY");}});
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
