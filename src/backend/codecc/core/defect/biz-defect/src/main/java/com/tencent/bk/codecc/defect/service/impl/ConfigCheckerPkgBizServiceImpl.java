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

package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerPackageRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.IgnoreCheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.IgnoreCheckerDao;
import com.tencent.bk.codecc.defect.model.CheckerPackageEntity;
import com.tencent.bk.codecc.defect.model.IgnoreCheckerEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.ICheckerSetBizService;
import com.tencent.bk.codecc.defect.service.IConfigCheckerPkgBizService;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CheckerPkgRspVO;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.defect.vo.GetCheckerListRspVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.OpenCheckerVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CheckerConstants;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.utils.GlobalMessageUtil;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.FUNC_CHECKER_CONFIG;
import static com.tencent.devops.common.constant.ComConstants.OPEN_CHECKER;
import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_CHECKER_DESC;
import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_CHECKER_PACKAGE_MSG;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_TASK_CHECKER_CONFIG;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_IGNORE_CHECKER;

/**
 * 工具配置规则包实现
 *
 * @version V1.0
 * @date 2019/6/5
 */
@Service("ConfigCheckerPkgBizService")
@Slf4j
public class ConfigCheckerPkgBizServiceImpl implements IConfigCheckerPkgBizService
{
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private IgnoreCheckerDao ignoreCheckerDao;

    @Autowired
    private IgnoreCheckerRepository ignoreCheckerRepository;

    @Autowired
    private CheckerPackageRepository checkerPackageRepository;

    @Autowired
    private GlobalMessageUtil globalMessageUtil;

    @Autowired
    private CheckerService checkerService;

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @Autowired
    private Client client;

    @Autowired
    private ICheckerSetBizService checkerSetBizService;

    @Autowired
    private ToolBuildInfoService toolBuildInfoService;

    @Override
    public GetCheckerListRspVO getConfigCheckerPkg(Long taskId, String toolName, Long codeLang, ToolConfigInfoVO toolConfig)
    {
        // 获取所有规则包列表
        GetCheckerListRspVO getCheckerListRspVO = new GetCheckerListRspVO();
        List<CheckerPackageEntity> checkerPkgList = checkerPackageRepository.findByToolName(toolName);
        List<CheckerPkgRspVO> result = new ArrayList<>(checkerPkgList.size());
        getCheckerListRspVO.setCheckerPackages(result);
        if (CollectionUtils.isEmpty(checkerPkgList))
        {
            return getCheckerListRspVO;
        }

        TaskDetailVO taskDetailVO = null;
        Result<TaskDetailVO> taskDetailVOResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        if (taskDetailVOResult.isOk() && null != taskDetailVOResult.getData()
                && CollectionUtils.isNotEmpty(taskDetailVOResult.getData().getToolConfigInfoList()))
        {
            taskDetailVO = taskDetailVOResult.getData();
        }

        // 查询工具配置
        ToolConfigInfoVO toolConfigInfo = null;
        Long finalCodeLang = null;

        if(codeLang == null &&
                toolConfig == null)
        {
            if (taskDetailVO == null)
            {
                log.error("task info is empty! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
            for (ToolConfigInfoVO toolConfigInfoVO : taskDetailVO.getToolConfigInfoList())
            {
                if (toolName.equals(toolConfigInfoVO.getToolName()))
                {
                    toolConfigInfo = toolConfigInfoVO;
                    break;
                }
            }
            finalCodeLang = taskDetailVO.getCodeLang();

            if (toolConfigInfo == null)
            {
                log.error("tool info is empty! task id: {}, tool name: {}", taskId, toolName);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
        }
        else
        {
            toolConfigInfo = toolConfig;
            finalCodeLang = codeLang;
        }

        // 按规则包ID分组规则明细
        Map<String, List<CheckerDetailVO>> checkerPkgsMap = getToolCheckerPkg(taskId, toolName, toolConfigInfo.getParamJson(), finalCodeLang);

        // 设置规则详情状态
        setCheckerStatus(taskDetailVO, toolConfigInfo, checkerPkgsMap);

        // 规则包国际化
        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_CHECKER_PACKAGE_MSG);

        // 按规则包ID分组规则包
        int allOpenCount = 0;
        Map<String, CheckerPackageEntity> checkerPkgMap = checkerPkgList.stream()
                .collect(Collectors.toMap(CheckerPackageEntity::getPkgId, Function.identity(), (k, v) -> v));
        Iterator<Map.Entry<String, CheckerPackageEntity>> checkerPkgEntryIt = checkerPkgMap.entrySet().iterator();
        while (checkerPkgEntryIt.hasNext())
        {
            Map.Entry<String, CheckerPackageEntity> checkerPkgEntry = checkerPkgEntryIt.next();
            String pkgId = checkerPkgEntry.getKey();

            // 获取规则包中的规则明细
            List<CheckerDetailVO> details = checkerPkgsMap.containsKey(pkgId) ? checkerPkgsMap.get(pkgId) : new ArrayList<>();
            if (CollectionUtils.isEmpty(details))
            {
                continue;
            }

            // 组装规则包详情
            int pkgOpenCheckerCount = (int) details.stream().filter(CheckerDetailVO::getCheckerStatus).count();
            allOpenCount += pkgOpenCheckerCount;
            CheckerPackageEntity packageEntity = checkerPkgEntry.getValue();
            CheckerPkgRspVO checkerPkgRsp = new CheckerPkgRspVO();
            checkerPkgRsp.setPkgId(pkgId);
            checkerPkgRsp.setPkgName(packageEntity.getPkgName());
            checkerPkgRsp.setPkgDesc(getGlobalPkgDesc(globalMessageMap, packageEntity.getPkgName(), packageEntity.getPkgDesc()));
            checkerPkgRsp.setOpenCheckerNum(pkgOpenCheckerCount);
            checkerPkgRsp.setPkgStatus(pkgOpenCheckerCount > 0);
            checkerPkgRsp.setCheckerList(details);
            checkerPkgRsp.setTotalCheckerNum(details.size());
            result.add(checkerPkgRsp);
        }

        // 排序
        getCheckerListRspVO.setCheckerPackages(result.stream().sorted((Comparator.comparingInt(o -> Integer.valueOf(o.getPkgId())))).collect(Collectors.toList()));

        // 加入规则集
        CheckerSetVO checkerSetVO = new CheckerSetVO();
        if (toolConfigInfo.getCheckerSet() != null)
        {
            int latestVersion = CheckerConstants.DEFAULT_VERSION;
            ToolCheckerSetVO toolCheckerSetVO = toolConfigInfo.getCheckerSet();
            List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByToolNameAndCheckerSetId(toolName, toolCheckerSetVO.getCheckerSetId());
            if (CollectionUtils.isNotEmpty(checkerSetEntities))
            {
                for (CheckerSetEntity checkerSetEntity: checkerSetEntities)
                {
                    if (checkerSetEntity.getVersion().equals(toolCheckerSetVO.getVersion()))
                    {
                        BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
                    }
                    if (checkerSetEntity.getVersion() > latestVersion)
                    {
                        latestVersion = checkerSetEntity.getVersion();
                    }
                }
                checkerSetVO.setLatestVersion(latestVersion);
            }
        }
        checkerSetVO.setCodeLang(finalCodeLang);
        checkerSetVO.setCheckerCount(allOpenCount);
        getCheckerListRspVO.setCheckerSet(checkerSetVO);

        return getCheckerListRspVO;
    }



    /**
     * 获取配置规则包
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Override
    public GetCheckerListRspVO getConfigCheckerPkg(Long taskId, String toolName)
    {
        return getConfigCheckerPkg(taskId, toolName, null, null);
    }


    @Override
    public Boolean syncConfigCheckerPkg(Long taskId, String toolName, ConfigCheckersPkgReqVO configCheckersPkgReq){
        return configCheckerPkg(taskId, toolName, configCheckersPkgReq, null);
    }

    /**
     * 开启/关闭 规则包配置
     *
     * @param taskId
     * @param toolName
     * @param configCheckersPkgReq
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_CHECKER_CONFIG, operType = OPEN_CHECKER)
    public Boolean configCheckerPkg(Long taskId, String toolName, ConfigCheckersPkgReqVO configCheckersPkgReq, String updatePipelineUser)
    {
        configCheckersPkgReq.setToolName(toolName);
        boolean isNull = Objects.isNull(configCheckersPkgReq) ||
                (CollectionUtils.isEmpty(configCheckersPkgReq.getClosedCheckers())
                        && CollectionUtils.isEmpty(configCheckersPkgReq.getOpenedCheckers()));
        if (isNull)
        {
            return false;
        }

        // 获取toolName工具屏蔽的所有规则
        configCheckersPkgReq.setTaskId(taskId);
        IgnoreCheckerEntity ignoreChecker = ignoreCheckerRepository.findFirstByTaskIdAndToolName(taskId, configCheckersPkgReq.getToolName());

        // 按规则包ID分组规则明细
        Map<String, CheckerDetailVO> allCheckersMap = checkerService.queryAllChecker(configCheckersPkgReq.getToolName());

        // 获取更新的规则包
        if (Objects.isNull(ignoreChecker))
        {
            ignoreChecker = new IgnoreCheckerEntity();
            ignoreChecker.setTaskId(taskId);
            ignoreChecker.setToolName(configCheckersPkgReq.getToolName());
        }
        getUpdateCheckerPkg(configCheckersPkgReq, ignoreChecker, allCheckersMap);
        ignoreCheckerDao.upsertIgnoreChecker(ignoreChecker);

        // 通知Job模块
        rabbitTemplate.convertAndSend(EXCHANGE_TASK_CHECKER_CONFIG, ROUTE_IGNORE_CHECKER, configCheckersPkgReq);

        // 如果之前关联了其他规则集，则需要从规则集已关联的任务列表中清除
        checkerSetBizService.clearTaskCheckerSets(taskId, Lists.newArrayList(toolName), updatePipelineUser, true);

        // 设置强制全量扫描标志
        toolBuildInfoService.setForceFullScan(taskId, Lists.newArrayList(toolName));

        return true;
    }

    /**
     * 设置规则详情状态
     *
     * @param taskDetailVO
     * @param toolConfigInfo
     * @param checkersMap
     */
    private void setCheckerStatus(TaskDetailVO taskDetailVO, ToolConfigInfoVO toolConfigInfo, Map<String, List<CheckerDetailVO>> checkersMap)
    {
        // 查询工具所有规则
        String toolName = toolConfigInfo.getToolName();
        Map<String, CheckerDetailVO> checkerDetailVOMap = checkerService.queryAllChecker(toolName);

        // 查询打开的规则
        long taskId = taskDetailVO.getTaskId();
        AnalyzeConfigInfoVO analyzeConfigInfoVO = new AnalyzeConfigInfoVO();
        analyzeConfigInfoVO.setTaskId(taskId);
        analyzeConfigInfoVO.setMultiToolType(toolName);
        analyzeConfigInfoVO.setNameEn(taskDetailVO.getNameEn());
        analyzeConfigInfoVO.setLanguage(taskDetailVO.getCodeLang());
        analyzeConfigInfoVO.setParamJson(toolConfigInfo.getParamJson());
        analyzeConfigInfoVO = checkerService.getTaskCheckerConfig(analyzeConfigInfoVO);
        Set<String> openCheckers = Sets.newHashSet();
        if (CollectionUtils.isNotEmpty(analyzeConfigInfoVO.getOpenCheckers()))
        {
            for (OpenCheckerVO checkerVO : analyzeConfigInfoVO.getOpenCheckers())
            {
                if (CollectionUtils.isNotEmpty(checkerVO.getCheckerOptions())
                        && checkerDetailVOMap.containsKey(checkerVO.getCheckerName() + "-tosa"))
                {
                    openCheckers.add(checkerVO.getCheckerName() + "-tosa");
                }
                else
                {
                    openCheckers.add(checkerVO.getCheckerName());
                }
            }
        }

        if (MapUtils.isNotEmpty(checkersMap))
        {
            checkersMap.forEach((key, checkers) -> checkers.forEach(checker ->
            {
                // 根据规则所在规则包判断是否打开
                if (openCheckers.contains(checker.getCheckerKey()))
                {
                    checker.setCheckerStatus(true);
                }
                else
                {
                    checker.setCheckerStatus(false);
                }
            }));
        }
    }


    /**
     * 获取更新的规则包
     *
     * @param configCheckersPkgReq
     * @param ignoreChecker
     * @return
     */
    private void getUpdateCheckerPkg(ConfigCheckersPkgReqVO configCheckersPkgReq, IgnoreCheckerEntity ignoreChecker, Map<String, CheckerDetailVO> allCheckersMap)
    {
        // 开启规则
        if (CollectionUtils.isNotEmpty(configCheckersPkgReq.getOpenedCheckers()))
        {
            for (String openChecker : configCheckersPkgReq.getOpenedCheckers())
            {
                CheckerDetailVO checkerDetail = allCheckersMap.get(openChecker);
                if (checkerDetail != null)
                {
                    if (ComConstants.CheckerPkgKind.DEFAULT.value().equals(checkerDetail.getPkgKind()))
                    {
                        if (CollectionUtils.isNotEmpty(ignoreChecker.getCloseDefaultCheckers()))
                        {
                            ignoreChecker.getCloseDefaultCheckers().remove(openChecker);
                        }
                        else
                        {
                            ignoreChecker.setCloseDefaultCheckers(Lists.newArrayList());
                        }
                    }
                    else
                    {
                        if (ignoreChecker.getOpenNonDefaultCheckers() == null)
                        {
                            ignoreChecker.setOpenNonDefaultCheckers(Lists.newArrayList());
                        }
                        ignoreChecker.getOpenNonDefaultCheckers().add(openChecker);
                    }
                }
            }
        }

        // 关闭规则
        if (CollectionUtils.isNotEmpty(configCheckersPkgReq.getClosedCheckers()))
        {
            for (String closeChecker : configCheckersPkgReq.getClosedCheckers())
            {
                CheckerDetailVO checkerDetail = allCheckersMap.get(closeChecker);
                if (checkerDetail != null)
                {
                    if (ComConstants.CheckerPkgKind.DEFAULT.value().equals(checkerDetail.getPkgKind()))
                    {
                        if (ignoreChecker.getCloseDefaultCheckers() == null)
                        {
                            ignoreChecker.setCloseDefaultCheckers(Lists.newArrayList());
                        }
                        ignoreChecker.getCloseDefaultCheckers().add(closeChecker);
                    }
                    else
                    {
                        if (CollectionUtils.isNotEmpty(ignoreChecker.getOpenNonDefaultCheckers()))
                        {
                            ignoreChecker.getOpenNonDefaultCheckers().remove(closeChecker);
                        }
                        else
                        {
                            ignoreChecker.setOpenNonDefaultCheckers(Lists.newArrayList());
                        }
                    }
                }
            }
        }
    }


    /**
     * 获取工具对应的规则包
     *
     * @param taskId
     * @param toolName
     * @param paramJson
     * @param codeLang
     * @return
     */
    private Map<String, List<CheckerDetailVO>> getToolCheckerPkg(Long taskId, String toolName, String paramJson, long codeLang)
    {
        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_CHECKER_DESC);
        List<CheckerDetailVO> checkerDetailEntityList = checkerService.queryAllChecker(taskId, toolName, paramJson, codeLang);
        return checkerDetailEntityList.stream()
                .filter(detailEntity -> StringUtils.isNotBlank(detailEntity.getPkgKind()))
                .map(checkerDetailVO ->
                {
                    checkerDetailVO.setCheckerDesc(getGlobalPkgDesc(globalMessageMap, checkerDetailVO.getCheckerKey(), checkerDetailVO.getCheckerDesc()));
                    return checkerDetailVO;
                }).sorted(Comparator.comparing(CheckerDetailVO::getCheckerName)).collect(Collectors.groupingBy(CheckerDetailVO::getPkgKind));
    }

    private String getGlobalPkgDesc(Map<String, GlobalMessage> globalMessageMap, String pkgName, String pkgDesc)
    {
        GlobalMessage globalMessage = globalMessageMap.get(pkgName);
        if (Objects.isNull(globalMessage))
        {
            //高频日志打印影响性能
//            logger.error("Can't find internationalization information for the corresponding package: {}", pkgName);
            return pkgDesc;
            //throw new CodeCCException(DefectMessageCode.NOT_FIND_CHECKER_PACKAGE, new String[]{pkgName}, null);
        }
        return globalMessageUtil.getMessageByLocale(globalMessage);
    }


}
