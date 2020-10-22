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

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.api.ServiceAnalyzeConfigRestResource;
import com.tencent.bk.codecc.defect.api.ServiceToolBuildInfoResource;
import com.tencent.bk.codecc.defect.vo.SetForceFullScanReqVO;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.service.AnalyzeConfigService;
import com.tencent.bk.codecc.task.service.MetaService;
import com.tencent.bk.codecc.task.service.PlatformService;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineBuildInfoVO;
import com.tencent.devops.common.api.CodeRepoVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.GsonUtils;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.util.PathUtils;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    // 不支持增量的工具列表
    private static final String KEY_INCREMENTAL_EXCEPT_TOOLS = "INCREMENTAL_EXCEPT_TOOLS";

    // 支持coverity增量的灰度任务白名单列表
    private static final String KEY_INCREMENTAL_TASK_WHITE_LIST = "INCREMENTAL_TASK_WHITE_LIST";

    // 支持快速增量的灰度任务白名单列表
    private static final String KEY_FAST_INCREMENTAL_TASK_WHITE_LIST = "FAST_INCREMENTAL_TASK_WHITE_LIST";

    // 支持快速增量的开源灰度任务白名单列表
    private static final String KEY_FAST_INCREMENTAL_OPENSOURCE_TASK_WHITE_LIST = "FAST_INCREMENTAL_OPENSOURCE_TASK_WHITE_LIST";

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
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    protected ToolMetaCacheService toolMetaCacheService;
    @Autowired
    private ToolDao toolDao;

    @Override
    public AnalyzeConfigInfoVO getAnalyzeConfig(String streamName, String toolName, PipelineBuildInfoVO pipelineBuildInfoVO)
    {
        log.info("start to get defect config!, stream name: {}, tool type: {}, {}",
                streamName, toolName, GsonUtils.toJson(pipelineBuildInfoVO));
        AnalyzeConfigInfoVO analyzeConfigInfoVO = new AnalyzeConfigInfoVO();

        // 任务详细信息
        TaskInfoEntity taskInfoEntity = taskRepository.findByNameEn(streamName);
        if (null == taskInfoEntity)
        {
            throw new CodeCCException("empty task info found out! stream name: {}", new String[]{streamName});
        }

        // 工具详细信息
        ToolConfigInfoEntity toolConfigInfoEntity =
                toolRepository.findByTaskIdAndToolName(taskInfoEntity.getTaskId(), toolName);
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
        analyzeConfigInfoVO = addScanType(analyzeConfigInfoVO, pipelineBuildInfoVO, taskInfoEntity, toolConfigInfoEntity);

        analyzeConfigInfoVO.setPlatformIp(toolConfigInfoEntity.getPlatformIp());

        //如果有个性化触发信息，则传递给工具端
        if (null != taskInfoEntity.getCustomProjInfo())
        {
            analyzeConfigInfoVO.setRepoUrlMap(new HashMap<String, String>()
            {{
                put("pseudoRepo", taskInfoEntity.getCustomProjInfo().getUrl());
            }});
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
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getThirdPartyFilterPath()))
        {
            taskInfoEntity.getThirdPartyFilterPath()
                    .forEach(filterPath -> filterPathStr.append(filterPath).append(";"));
        }
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getTestSourceFilterPath()))
        {
            taskInfoEntity.getTestSourceFilterPath()
                    .forEach(filterPath -> filterPathStr.append(filterPath).append(";"));
        }
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getAutoGenFilterPath()))
        {
            taskInfoEntity.getAutoGenFilterPath()
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

    /**
     * 获取扫描方式
     *
     * @param analyzeConfigInfoVO
     * @param taskInfoEntity
     * @param pipelineBuildInfoVO
     * @param toolConfigInfoEntity
     * @return
     */
    private AnalyzeConfigInfoVO addScanType(AnalyzeConfigInfoVO analyzeConfigInfoVO, PipelineBuildInfoVO pipelineBuildInfoVO,
                                            TaskInfoEntity taskInfoEntity, ToolConfigInfoEntity toolConfigInfoEntity)
    {
        Integer taskScanType = taskInfoEntity.getScanType();
        long taskId = analyzeConfigInfoVO.getTaskId();
        String toolName = analyzeConfigInfoVO.getMultiToolType();

        // 查询不支持增量的工具和支持增量的任务白名单
        List<String> paramTypes = Lists.newArrayList(
                KEY_INCREMENTAL_EXCEPT_TOOLS,
                KEY_INCREMENTAL_TASK_WHITE_LIST,
                KEY_FAST_INCREMENTAL_TASK_WHITE_LIST,
                KEY_FAST_INCREMENTAL_OPENSOURCE_TASK_WHITE_LIST);
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findByParamTypeIn(paramTypes);
        Map<String, BaseDataEntity> baseDataEntityMap = baseDataEntityList.stream().collect(Collectors.toMap(BaseDataEntity::getParamType, Function.identity(), (k, v) -> v));
        List<String> incrementalExceptTools = List2StrUtil.fromString(baseDataEntityMap.get(KEY_INCREMENTAL_EXCEPT_TOOLS).getParamValue(), ComConstants.STRING_SPLIT);

        // 1.支持增量的工具才支持diff模式，diff模式不强制全量处理
        if (!incrementalExceptTools.contains(toolName) && !ComConstants.Tool.COVERITY.name().equalsIgnoreCase(toolName))
        {
            if (taskScanType != null && taskScanType == ComConstants.ScanType.DIFF_MODE.code)
            {
                analyzeConfigInfoVO.setScanType(taskScanType);
                return analyzeConfigInfoVO;
            }
        }

        // 2.判断是否是强制全量
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String buildId = request.getHeader(CODECC_AUTH_HEADER_DEVOPS_BUILD_ID);
        analyzeConfigInfoVO.setBuildId(buildId);
        analyzeConfigInfoVO.setRepoIds(pipelineBuildInfoVO.getRepoIds());
        analyzeConfigInfoVO.setCodeRepos(pipelineBuildInfoVO.getCodeRepos());
        analyzeConfigInfoVO.setRepoWhiteList(pipelineBuildInfoVO.getRepoWhiteList());
        boolean isPipelineTask = ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equalsIgnoreCase(taskInfoEntity.getCreateFrom());
        analyzeConfigInfoVO.setPipelineTask(isPipelineTask);
        analyzeConfigInfoVO.setScanType(taskScanType);
        analyzeConfigInfoVO.setAtomCode(taskInfoEntity.getAtomCode());

        log.info("start to get build info: {}, {}, {}", taskId, toolName, buildId);
        CodeCCResult<AnalyzeConfigInfoVO> result = client.get(ServiceAnalyzeConfigRestResource.class).getBuildInfo(analyzeConfigInfoVO);
        if (result == null || result.isNotOk())
        {
            log.error("Get tool build info failed! taskId={}, toolName={}, buildId={}", taskId, toolName, buildId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{"Get tool build info failed!"}, null);
        }
        analyzeConfigInfoVO = result.getData();

        // 如果是强制全量，说明配置有变更，直接返回全量扫描
        if (analyzeConfigInfoVO.getScanType() != null
                && ComConstants.ScanType.FULL.code == analyzeConfigInfoVO.getScanType()) {
            return analyzeConfigInfoVO;
        } else if (isToolImageChange(toolConfigInfoEntity, buildId)) {
            // 如果工具镜像有变更，也应该强制全量
            analyzeConfigInfoVO.setScanType(ComConstants.ScanType.FULL.code);
            SetForceFullScanReqVO setForceFullScanReqVO = new SetForceFullScanReqVO();
            setForceFullScanReqVO.setLandunBuildId(buildId);
            setForceFullScanReqVO.setToolNames(Lists.newArrayList(toolName));
            client.get(ServiceToolBuildInfoResource.class).setToolBuildStackFullScan(taskId, setForceFullScanReqVO);
            return analyzeConfigInfoVO;
        }

        BaseDataEntity fastIncrBaseDataEnity = baseDataEntityMap.get(KEY_FAST_INCREMENTAL_TASK_WHITE_LIST);
        String status = fastIncrBaseDataEnity.getParamStatus();
        List<String> fastIncrTasks = List2StrUtil.fromString(fastIncrBaseDataEnity.getParamValue(), ComConstants.STRING_SPLIT);
        BaseDataEntity fastIncrOpensourceBaseDataEnity = baseDataEntityMap.get(KEY_FAST_INCREMENTAL_OPENSOURCE_TASK_WHITE_LIST);
        String opensourceStatus = fastIncrOpensourceBaseDataEnity.getParamStatus();
        List<String> fastIncrOpensourceTasks = List2StrUtil.fromString(fastIncrOpensourceBaseDataEnity.getParamValue(), ComConstants.STRING_SPLIT);

        /*
         * 普通任务的灰度开关:
         * DISABLE标志灰度开关关闭，所有任务都可以执行快速增量
         * ENABLE标志灰度开关打开，只有在白名单里面的任务才可以执行快速增量
         */
        boolean fastIncrGrey = ComConstants.Status.DISABLE.name().equalsIgnoreCase(status)
                || (ComConstants.Status.ENABLE.name().equalsIgnoreCase(status) && fastIncrTasks.contains(String.valueOf(taskId)));

        /*
         * 开源任务的灰度开关:
         * DISABLE标志灰度开关关闭，所有任务都可以执行快速增量
         * ENABLE标志灰度开关打开，只有在白名单里面的任务才可以执行快速增量
         */
        boolean opensourceFastIncrGrey = ComConstants.Status.DISABLE.name().equalsIgnoreCase(opensourceStatus)
                || (ComConstants.Status.ENABLE.name().equalsIgnoreCase(opensourceStatus) && fastIncrOpensourceTasks.contains(String.valueOf(taskId)));
        log.info("fastIncrGrey: {}, opensourceFastIncrGrey: {}", fastIncrGrey, opensourceFastIncrGrey);
        /*
         * 3.判断是否进行快速增量，满足以下条件才执行快速增量
         * a. V1(AtomCode为空空)、V2（CodeccCheckAtom）插件不支持快速增量，不需要判断（这么写是为了后面再有新的codecc插件版本时，不需要改这里的代码即可兼容）
         * b. 代码没有变化
         * c. 工具镜像没有变化（在上面步骤2已经判断过了，如果镜像有变更，直接返回强制全量了）
         * d. 是灰度任务或者灰度开关关闭
         */
        if (StringUtils.isNotEmpty(taskInfoEntity.getAtomCode())
                && !ComConstants.AtomCode.CODECC_V2.code().equalsIgnoreCase(taskInfoEntity.getAtomCode())
                && !isCodeRepoChange(analyzeConfigInfoVO.getLastCodeRepos(), analyzeConfigInfoVO.getCodeRepos())) {
            String toolPattern = toolMetaCacheService.getToolPattern(toolName);

            if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(taskInfoEntity.getCreateFrom())
                    && opensourceFastIncrGrey) {
                log.info("工蜂项目快速增量: taskId {}, toolName {}, buildId {}", taskId, toolName, buildId);

                // 通过消息队列异步生成分析记录及扫描结果等信息
                String exchange = String.format("%s%s.opensource",
                        ConstantsKt.PREFIX_EXCHANGE_FAST_INCREMENT,
                        toolPattern.toLowerCase());
                String routingKey = String.format("%s%s.opensource",
                        ConstantsKt.PREFIX_ROUTE_FAST_INCREMENT,
                        toolPattern.toLowerCase());
                rabbitTemplate.convertAndSend(exchange, routingKey, analyzeConfigInfoVO);

                analyzeConfigInfoVO.setScanType(ComConstants.ScanType.FAST_INCREMENTAL.code);
                return analyzeConfigInfoVO;
            } else if (!ComConstants.BsTaskCreateFrom.GONGFENG_SCAN
                    .value()
                    .equalsIgnoreCase(taskInfoEntity.getCreateFrom())
                    && fastIncrGrey) {
                log.info("普通项目快速增量: taskId {}, toolName {}, buildId {}", taskId, toolName, buildId);

                // 通过消息队列异步生成分析记录及扫描结果等信息
                String exchange = String.format("%s%s",
                        ConstantsKt.PREFIX_EXCHANGE_FAST_INCREMENT,
                        toolPattern.toLowerCase());
                String routingKey = String.format("%s%s",
                        ConstantsKt.PREFIX_ROUTE_FAST_INCREMENT,
                        toolPattern.toLowerCase());
                rabbitTemplate.convertAndSend(exchange, routingKey, analyzeConfigInfoVO);

                analyzeConfigInfoVO.setScanType(ComConstants.ScanType.FAST_INCREMENTAL.code);
                return analyzeConfigInfoVO;
            }
        }

        // TODO 未来如果是coverity工具，局部增量（其他配置不变，只有规则变化时，coverity/klocwork等编译工具不需要重新执行构建，只需要执行analyze和commit）

        // 4.判断是否是支持增量分析的工具或者任务
        int scanType = ComConstants.ScanType.FULL.code;
        if (taskScanType != null) {
            // 支持增量的工具才可能做增量扫描
            if (!incrementalExceptTools.contains(toolName)) {
                scanType = taskScanType;
            } else if (ComConstants.Tool.COVERITY.name().equalsIgnoreCase(toolName)
                    && baseDataEntityMap.get(KEY_INCREMENTAL_TASK_WHITE_LIST) != null) {
                // 在coverity增量的任务白名单里面的任务才可能做增量扫描
                List<String> incrementalTasks = List2StrUtil.fromString(baseDataEntityMap.get(KEY_INCREMENTAL_TASK_WHITE_LIST).getParamValue(),
                        ComConstants.STRING_SPLIT);
                if (incrementalTasks.contains(String.valueOf(taskId))) {
                    scanType = taskScanType;
                }
            }
        }
        analyzeConfigInfoVO.setScanType(scanType);

        return analyzeConfigInfoVO;
    }

    /**
     * 代码是否有变化： true变化，false没变化
     *
     * @param lastCodeRepos
     * @param codeRepos
     * @return
     */
    private boolean isCodeRepoChange(List<CodeRepoVO> lastCodeRepos, List<CodeRepoVO> codeRepos) {
        log.info("lastCodeRepoSet: {}\ncurrCodeRepoSet:{}",
                GsonUtils.toJson(lastCodeRepos),
                GsonUtils.toJson(codeRepos));
        if (lastCodeRepos != null && codeRepos != null && lastCodeRepos.size() == codeRepos.size()) {
            Set<String> lastCodeRepoSet = lastCodeRepos.stream().map(repo -> String.format("%s_%s",
                    PathUtils.formatRepoUrlToHttp(repo.getUrl() == null ? "" : repo.getUrl()),
                    repo.getRevision() == null ? "" : repo.getRevision()))
                    .collect(Collectors.toSet());
            Set<String> codeReposSet = codeRepos.stream().map(repo -> String.format("%s_%s",
                    PathUtils.formatRepoUrlToHttp(repo.getUrl() == null ? "" : repo.getUrl()),
                    repo.getRevision() == null ? "" : repo.getRevision()))
                    .collect(Collectors.toSet());
            return !CollectionUtils.isEqualCollection(lastCodeRepoSet, codeReposSet);
        } else {
            log.info("default code repo change");
            return true;
        }
    }

    /**
     * 工具镜像是否有变化： true变化，false没变化
     *
     * @param toolConfigInfoEntity
     * @param buildId
     * @return
     */
    private boolean isToolImageChange(ToolConfigInfoEntity toolConfigInfoEntity, String buildId)
    {
        long taskId = toolConfigInfoEntity.getTaskId();
        String toolName = toolConfigInfoEntity.getToolName();
        log.info("check is tool image changed, taskId:{}, toolName:{}, buildId:{}", taskId, toolName, buildId);

        String oldToolImageRevision = toolConfigInfoEntity.getToolImageRevision();
        String newToolImageRevision;

        ToolMetaBaseVO toolMetaBaseVO = toolMetaCacheService.getToolBaseMetaCache(toolName);

        // coverity,klocwork,pinpoint工具没有镜像，通过工具版本号来判断是否有变化
        if (ComConstants.Tool.COVERITY.name().equals(toolName) || ComConstants.Tool.KLOCWORK.name().equals(toolName) || ComConstants.Tool.PINPOINT.name().equals(toolName))
        {
            newToolImageRevision = toolMetaBaseVO.getToolVersion();
        }
        else
        {
            newToolImageRevision = toolMetaBaseVO.getToolImageRevision();
        }

        if (StringUtils.isNotEmpty(newToolImageRevision) && !newToolImageRevision.equals(oldToolImageRevision))
        {
            log.info("need to update tool image! taskId: {}, toolName: {}, buildId: {}", taskId, toolName, buildId);
            toolDao.updateToolImageRevision(taskId, toolName, newToolImageRevision);

            // 这里加上这个判断，是为了兼容初次做这个判断的时候，oldToolImageRevision肯定为空，但是不应该认为镜像有变化
            if (StringUtils.isNotEmpty(oldToolImageRevision))
            {
                log.info("tool image has changed! taskId: {}, toolName: {}, buildId: {}", taskId, toolName, buildId);
                return true;
            }
        }

        log.info("tool image no changed, taskId:{}, toolName:{}, buildId:{}", taskId, toolName, buildId);
        return false;
    }

}
