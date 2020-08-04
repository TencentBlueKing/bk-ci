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

package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.defect.api.ServiceAnalyzeConfigRestResource;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.service.AnalyzeConfigService;
import com.tencent.bk.codecc.task.service.MetaService;
import com.tencent.bk.codecc.task.service.PlatformService;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineBuildInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.List2StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.Lists;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.*;

/**
 * 获取配置服务类
 *
 * @version V1.0
 * @date 2019/5/20
 */
@Service
@Slf4j
public class AnalyzeConfigServiceImpl implements AnalyzeConfigService
{
    private static final String KEY_INCREMENTAL_EXCEPT_TOOLS = "INCREMENTAL_EXCEPT_TOOLS";
    private static final String KEY_INCREMENTAL_TASK_WHITE_LIST = "INCREMENTAL_TASK_WHITE_LIST";
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private Client client;

    @Autowired
    private BaseDataRepository baseDataRepository;

    @Autowired
    private PlatformService platformService;

    @Autowired
    private MetaService metaService;

    @Override
    public AnalyzeConfigInfoVO getAnalyzeConfig(String streamName, String toolName, PipelineBuildInfoVO pipelineBuildInfoVO)
    {
        log.info("start to get defect config!, stream name: {}, tool type: {}", streamName, toolName);
        AnalyzeConfigInfoVO analyzeConfigInfoVO = new AnalyzeConfigInfoVO();

        // 任务详细信息
        TaskInfoEntity taskInfoEntity = taskRepository.findByNameEn(streamName);
        if (null == taskInfoEntity)
        {
            throw new CodeCCException("empty task info found out! stream name: {}", new String[]{streamName});
        }

        // 工具详细信息
        ToolConfigInfoEntity toolConfigInfoEntity = toolRepository.findByTaskIdAndToolName(taskInfoEntity.getTaskId(), toolName);
        if (toolConfigInfoEntity == null)
        {
            throw new CodeCCException("empty tool info found out ! stream name: {}, toolName: {}, task: {} ",
                new String[]{streamName, toolName, taskInfoEntity + ""});
        }
        analyzeConfigInfoVO.setTaskId(taskInfoEntity.getTaskId());
        analyzeConfigInfoVO.setMultiToolType(toolName.toUpperCase());
        analyzeConfigInfoVO.setNameEn(streamName);
        analyzeConfigInfoVO.setLanguage(taskInfoEntity.getCodeLang());
        analyzeConfigInfoVO.setLanguageStrList(metaService.convertCodeLangToBsString(taskInfoEntity.getCodeLang()));

        // 过滤路径
        analyzeConfigInfoVO.setSkipPaths(getFilterPath(taskInfoEntity));

        // 规则配置
        analyzeConfigInfoVO.setParamJson(toolConfigInfoEntity.getParamJson());
        analyzeConfigInfoVO = addChecker(analyzeConfigInfoVO, taskInfoEntity);

        log.info("add checker finish!");

        // 获取工具个性化参数
        List<AnalyzeConfigInfoVO.ToolOptions> toolOptionList = getToolOptions(taskInfoEntity, toolConfigInfoEntity);
        if (analyzeConfigInfoVO.getToolOptions() != null)
        {
            toolOptionList.addAll(analyzeConfigInfoVO.getToolOptions());
        }
        // 个性化参数去重
        Collection<AnalyzeConfigInfoVO.ToolOptions> toolOptionSet = toolOptionList.stream()
                .collect(Collectors.toMap(AnalyzeConfigInfoVO.ToolOptions::getOptionName, Function.identity(), (k, v) -> v)).values();
        analyzeConfigInfoVO.setToolOptions(new ArrayList<>(toolOptionSet));
        analyzeConfigInfoVO.setParamJson(null);

        // 增量全量
        analyzeConfigInfoVO = addScanType(analyzeConfigInfoVO, taskInfoEntity.getScanType(), pipelineBuildInfoVO,
                ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equalsIgnoreCase(taskInfoEntity.getCreateFrom()));

        analyzeConfigInfoVO.setPlatformIp(toolConfigInfoEntity.getPlatformIp());

        //如果有个性化触发信息，则传递给工具端
        if(null != taskInfoEntity.getCustomProjInfo())
        {
            analyzeConfigInfoVO.setRepoUrlMap(new HashMap<String, String>(){{put("pseudoRepo", taskInfoEntity.getCustomProjInfo().getUrl());}});
        }

        log.info("get defect config finish!, task id: {}, tool type: {}", taskInfoEntity.getTaskId(), toolName);
        return analyzeConfigInfoVO;
    }


    /**
     * 获取工具个性化参数
     *
     * @param taskInfoEntity
     * @param toolConfigInfoEntity
     * @return
     */
    private List<AnalyzeConfigInfoVO.ToolOptions> getToolOptions(TaskInfoEntity taskInfoEntity, ToolConfigInfoEntity toolConfigInfoEntity)
    {
        List<AnalyzeConfigInfoVO.ToolOptions> toolOptionList = new ArrayList<>();
        // 编译工具个性化参数
        if (StringUtils.isNotBlank(taskInfoEntity.getProjectBuildCommand()))
        {
            AnalyzeConfigInfoVO.ToolOptions toolOption = new AnalyzeConfigInfoVO.ToolOptions();
            toolOption.setOsType(taskInfoEntity.getOsType());
            toolOption.setBuildEnv(taskInfoEntity.getBuildEnv());
            toolOption.setOptionName(taskInfoEntity.getProjectBuildType());
            toolOption.setOptionValue(taskInfoEntity.getProjectBuildCommand());
            toolOptionList.add(toolOption);
        }

        // 非编译型工具个性化参数
        if (null != toolConfigInfoEntity && StringUtils.isNotBlank(toolConfigInfoEntity.getParamJson())
                && !ComConstants.STRING_NULL_ARRAY.equals(toolConfigInfoEntity.getParamJson()))
        {
            JSONObject paramsJson = new JSONObject(toolConfigInfoEntity.getParamJson());
            for (String key : paramsJson.keySet())
            {
                AnalyzeConfigInfoVO.ToolOptions toolOption = new AnalyzeConfigInfoVO.ToolOptions();
                toolOption.setOptionName(key);
                toolOption.setOptionValue(String.valueOf(paramsJson.get(key)));
                toolOptionList.add(toolOption);
            }
        }

        if (ComConstants.Tool.PINPOINT.name().equals(toolConfigInfoEntity.getToolName()))
        {
            PlatformVO platformVO = platformService.getPlatformByToolNameAndIp(toolConfigInfoEntity.getToolName(), toolConfigInfoEntity.getPlatformIp());
            if (platformVO != null)
            {
                AnalyzeConfigInfoVO.ToolOptions pinpointTokenOption = new AnalyzeConfigInfoVO.ToolOptions();
                pinpointTokenOption.setOptionName("pinpointToken");
                pinpointTokenOption.setOptionValue(platformVO.getToken());
                toolOptionList.add(pinpointTokenOption);

                String pinpointURL = String.format("http://%s:%s", platformVO.getIp(), platformVO.getPort());
                AnalyzeConfigInfoVO.ToolOptions pinpointURLOption = new AnalyzeConfigInfoVO.ToolOptions();
                pinpointURLOption.setOptionName("pinpointURL");
                pinpointURLOption.setOptionValue(pinpointURL);
                toolOptionList.add(pinpointURLOption);
            }

        }

        return toolOptionList;
    }


    /**
     * 获取过滤路径
     *
     * @param taskInfoEntity
     * @return
     */
    private String getFilterPath(TaskInfoEntity taskInfoEntity)
    {
        StringBuilder filterPathStr = new StringBuilder();
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getDefaultFilterPath()))
        {
            taskInfoEntity.getDefaultFilterPath()
                    .forEach(filterPath -> filterPathStr.append(filterPath).append(";"));
        }
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getFilterPath()))
        {
            taskInfoEntity.getFilterPath()
                    .forEach(filterPath -> filterPathStr.append(filterPath).append(";"));
        }
        return filterPathStr.toString();
    }

    /**
     * 增加规则信息
     *
     * @param analyzeConfigInfoVO
     * @param taskInfoEntity
     */
    private AnalyzeConfigInfoVO addChecker(AnalyzeConfigInfoVO analyzeConfigInfoVO, TaskInfoEntity taskInfoEntity)
    {
        // 如果是老插件，并且工具是重复率或者圈复杂度，则不需要查询规则
        if (StringUtils.isEmpty(taskInfoEntity.getAtomCode()) &&
                (ComConstants.Tool.DUPC.name().equalsIgnoreCase(analyzeConfigInfoVO.getMultiToolType())
                        || ComConstants.Tool.CCN.name().equalsIgnoreCase(analyzeConfigInfoVO.getMultiToolType())))
        {
            return analyzeConfigInfoVO;
        }

        // 查询规则配置
        CodeCCResult<AnalyzeConfigInfoVO> codeCCResult = client.get(ServiceAnalyzeConfigRestResource.class).getTaskCheckerConfig(analyzeConfigInfoVO);
        if (codeCCResult == null || codeCCResult.isNotOk())
        {
            log.error("Get checker configuration failed! taskId={}, toolName={}", analyzeConfigInfoVO.getTaskId(),
                    analyzeConfigInfoVO.getMultiToolType());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{"Get checker configuration failed!"}, null);
        }
        return codeCCResult.getData();
    }

    private AnalyzeConfigInfoVO addScanType(AnalyzeConfigInfoVO analyzeConfigInfoVO, Integer taskScanType, PipelineBuildInfoVO pipelineBuildInfoVO, boolean isPipelineTask)
    {
        // 查询不支持增量的工具
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findByParamTypeIn(Lists.newArrayList(KEY_INCREMENTAL_EXCEPT_TOOLS, KEY_INCREMENTAL_TASK_WHITE_LIST));
        Map<String, BaseDataEntity> baseDataEntityMap = baseDataEntityList.stream().collect(Collectors.toMap(BaseDataEntity::getParamType, Function.identity(), (k, v) -> v));
        List<String> incrementalExceptTools = List2StrUtil.fromString(baseDataEntityMap.get(KEY_INCREMENTAL_EXCEPT_TOOLS).getParamValue(), ComConstants.STRING_SPLIT);

        // 支持coverity增量的任务白名单
        List<String> incrementalTasks = null;
        if (baseDataEntityMap.get(KEY_INCREMENTAL_TASK_WHITE_LIST) != null)
        {
            incrementalTasks = List2StrUtil.fromString(baseDataEntityMap.get(KEY_INCREMENTAL_TASK_WHITE_LIST).getParamValue(), ComConstants.STRING_SPLIT);
        }

        // Coverity直接用配置的扫描方式，其他工具根据仓库列表和白名单是否修改，以及是否设置过强制全量扫描标志来判断
        int scanType = ComConstants.ScanType.FULL.code;
        if (!incrementalExceptTools.contains(analyzeConfigInfoVO.getMultiToolType())
                || (ComConstants.Tool.COVERITY.name().equalsIgnoreCase(analyzeConfigInfoVO.getMultiToolType())
                        && CollectionUtils.isNotEmpty(incrementalTasks) && incrementalTasks.contains(String.valueOf(analyzeConfigInfoVO.getTaskId()))))
        {
            scanType = taskScanType == null ? ComConstants.ScanType.FULL.code : taskScanType;

            // diff模式不全量处理
            if (scanType ==  ComConstants.ScanType.DIFF_MODE.code) {
                analyzeConfigInfoVO.setScanType(scanType);
                return analyzeConfigInfoVO;
            }

            analyzeConfigInfoVO.setScanType(scanType);
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String buildId = request.getHeader(CODECC_AUTH_HEADER_DEVOPS_BUILD_ID);
            analyzeConfigInfoVO.setBuildId(buildId);
            analyzeConfigInfoVO.setRepoIds(pipelineBuildInfoVO.getRepoIds());
            analyzeConfigInfoVO.setRepoWhiteList(pipelineBuildInfoVO.getRepoWhiteList());
            analyzeConfigInfoVO.setPipelineTask(isPipelineTask);
            // 查询工具构建信息
            log.info("start to get build info: {}, {}, {}", analyzeConfigInfoVO.getTaskId(), analyzeConfigInfoVO.getMultiToolType(), buildId);
            CodeCCResult<AnalyzeConfigInfoVO> codeCCResult = client.get(ServiceAnalyzeConfigRestResource.class).getBuildInfo(analyzeConfigInfoVO);
            if (codeCCResult == null || codeCCResult.isNotOk())
            {
                log.warn("Get tool build info failed! taskId={}, toolName={}, buildId={}", analyzeConfigInfoVO.getTaskId(), analyzeConfigInfoVO.getMultiToolType(), buildId);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{"Get tool build info failed!"}, null);
            }
            analyzeConfigInfoVO = codeCCResult.getData();
        }
        else
        {
            analyzeConfigInfoVO.setScanType(scanType);
        }

        return analyzeConfigInfoVO;
    }
}
