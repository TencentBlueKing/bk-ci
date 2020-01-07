/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerPackageRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.IgnoreCheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.IgnoreCheckerDao;
import com.tencent.bk.codecc.defect.model.CheckerPackageEntity;
import com.tencent.bk.codecc.defect.model.IgnoreCheckerEntity;
import com.tencent.bk.codecc.defect.service.IConfigCheckerPkgBizService;
import com.tencent.bk.codecc.defect.service.MultitoolCheckerService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CheckerPkgRspVO;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.defect.vo.IgnoreCheckerVO;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.utils.GlobalMessageUtil;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
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
 * Lint类工具的配置规则包实现
 *
 * @version V1.0
 * @date 2019/6/5
 */
@Service("LINTConfigCheckerPkgBizService")
public class LintConfigCheckerPkgBizServiceImpl implements IConfigCheckerPkgBizService
{

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private IgnoreCheckerDao ignoreCheckerDao;

    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    private IgnoreCheckerRepository ignoreCheckerRepository;

    @Autowired
    private CheckerPackageRepository checkerPackageRepository;

    @Autowired
    private GlobalMessageUtil globalMessageUtil;

    @Autowired
    private MultitoolCheckerService multitoolCheckerService;

    @Autowired
    private Client client;

    private static Logger logger = LoggerFactory.getLogger(LintConfigCheckerPkgBizServiceImpl.class);


    /**
     * 获取配置规则包
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Override
    public List<CheckerPkgRspVO> getConfigCheckerPkg(Long taskId, String toolName)
    {
        // 获取所有规则包列表
        List<CheckerPackageEntity> checkerPkgList = checkerPackageRepository.findByToolName(toolName);

        List<CheckerPkgRspVO> result = new ArrayList<>(checkerPkgList.size());
        if (CollectionUtils.isEmpty(checkerPkgList))
        {
            return result;
        }

        // 按规则包ID分组规则明细
        Map<String, List<CheckerDetailVO>> checkersMap = getToolCheckerPkg(taskId, toolName);
        // 设置规则详情状态
        setCheckerStatus(taskId, toolName, checkersMap);

        // 规则包国际化
        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_CHECKER_PACKAGE_MSG);

        // 按规则包ID分组规则包
        Map<String, CheckerPackageEntity> checkerPkgMap = checkerPkgList.stream()
                .collect(Collectors.toMap(CheckerPackageEntity::getPkgId, Function.identity(), (k, v) -> v));
        checkerPkgMap.forEach((key, packageEntity) ->
        {
            CheckerPkgRspVO checkerPkgRsp = new CheckerPkgRspVO();
            checkerPkgRsp.setPkgId(key);
            checkerPkgRsp.setPkgName(packageEntity.getPkgName());
            checkerPkgRsp.setPkgDesc(getGlobalPkgDesc(globalMessageMap, packageEntity.getPkgName(), packageEntity.getPkgDesc()));

            // 获取规则包中的规则明细
            List<CheckerDetailVO> details = checkersMap.containsKey(key) ? checkersMap.get(key) : new ArrayList<>();

            int count = (int) details.stream().filter(CheckerDetailVO::getCheckerStatus).count();
            checkerPkgRsp.setOpenCheckerNum(count);
            checkerPkgRsp.setPkgStatus(count > 0);
            checkerPkgRsp.setCheckerList(details);
            checkerPkgRsp.setTotalCheckerNum(details.size());
            result.add(checkerPkgRsp);

        });

        return result.stream().sorted((Comparator.comparingInt(o -> Integer.valueOf(o.getPkgId())))).collect(Collectors.toList());
    }


    /**
     * 开启/关闭 规则包配置
     *
     * @param checker
     * @param taskId
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_CHECKER_CONFIG, operType = OPEN_CHECKER)
    public Boolean configCheckerPkg(ConfigCheckersPkgReqVO checker, Long taskId)
    {
        boolean isNull = Objects.isNull(checker) ||
                (CollectionUtils.isEmpty(checker.getClosedCheckers())
                        && CollectionUtils.isEmpty(checker.getOpenedCheckers()));
        if (isNull)
        {
            return false;
        }

        // 获取toolName工具屏蔽的所有规则
        checker.setTaskId(taskId);
        IgnoreCheckerEntity ignoreChecker = ignoreCheckerRepository.findByTaskIdAndToolName(taskId, checker.getToolName());
        // 获取更新的规则包
        List<String> updateIgnoreList = getUpdateCheckerPkg(checker, ignoreChecker);

        if (Objects.isNull(ignoreChecker))
        {
            ignoreChecker = new IgnoreCheckerEntity();
            ignoreChecker.setTaskId(taskId);
            ignoreChecker.setToolName(checker.getToolName());
            ignoreChecker.setIgnoreList(updateIgnoreList);
            ignoreCheckerRepository.insert(ignoreChecker);
        }
        else
        {
            // 添加到IgnoreCheckerEntity
            ignoreChecker.setIgnoreList(updateIgnoreList);
            // 更新实体
            ignoreCheckerDao.updateIgnoreChecker(ignoreChecker);
        }

        // 通知Job模块
        rabbitTemplate.convertAndSend(EXCHANGE_TASK_CHECKER_CONFIG, ROUTE_IGNORE_CHECKER, checker);

        return true;
    }


    @Override
    public Boolean createDefaultIgnoreChecker(IgnoreCheckerVO ignoreCheckerVO, String userName)
    {
        IgnoreCheckerEntity ignoreCheckerEntity = new IgnoreCheckerEntity();
        BeanUtils.copyProperties(ignoreCheckerVO, ignoreCheckerEntity, "ignoreList");
        ignoreCheckerEntity.setIgnoreList(ignoreCheckerVO.getIgnoreList());
        Long currentTime = System.currentTimeMillis();
        ignoreCheckerEntity.setCreatedBy(userName);
        ignoreCheckerEntity.setCreatedDate(currentTime);
        ignoreCheckerEntity.setUpdatedBy(userName);
        ignoreCheckerEntity.setUpdatedDate(currentTime);
        ignoreCheckerRepository.insert(ignoreCheckerEntity);
        return true;
    }


    @Override
    public IgnoreCheckerVO getIgnoreCheckerInfo(Long taskId, String toolName)
    {
        IgnoreCheckerVO ignoreCheckerVO = new IgnoreCheckerVO();
        IgnoreCheckerEntity ignoreCheckerEntity = ignoreCheckerRepository.findByTaskIdAndToolName(taskId, toolName);
        if(null == ignoreCheckerEntity)
        {
            return ignoreCheckerVO;
        }
        BeanUtils.copyProperties(ignoreCheckerEntity, ignoreCheckerVO, "ignoreList");
        ignoreCheckerVO.setIgnoreList(ignoreCheckerEntity.getIgnoreList());
        return ignoreCheckerVO;
    }


    /**
     * 设置规则详情状态
     *
     * @param taskId
     * @param toolName
     * @param checkersMap
     */
    private void setCheckerStatus(Long taskId, String toolName, Map<String, List<CheckerDetailVO>> checkersMap)
    {
        // 查询屏蔽的规则详情
        IgnoreCheckerEntity ignoreCheckers = ignoreCheckerRepository.findByTaskIdAndToolName(taskId, toolName);

        if (Objects.nonNull(ignoreCheckers) && CollectionUtils.isNotEmpty(ignoreCheckers.getIgnoreList()))
        {
            List<String> ignoreList = ignoreCheckers.getIgnoreList();
            checkersMap.forEach((key, checkers) -> checkers.forEach(checker ->
            {
                // 存在规则屏蔽列表中则为屏蔽，关闭规则
                boolean status = ignoreList.contains(checker.getCheckerKey());
                checker.setCheckerStatus(!status);
            }));
        }
        else
        {
            checkersMap.forEach((key, checkers) -> checkers.forEach(checker ->
            {
                checker.setCheckerStatus(true);
            }));
        }

    }


    /**
     * 获取更新的规则包
     *
     * @param checker
     * @param ignoreChecker
     * @return
     */
    private List<String> getUpdateCheckerPkg(ConfigCheckersPkgReqVO checker, IgnoreCheckerEntity ignoreChecker)
    {
        List<String> updateIgnoreList = new ArrayList<>();
        if (isNotEmpty(ignoreChecker))
        {
            updateIgnoreList.addAll(ignoreChecker.getIgnoreList());
        }

        // [开启规则] : 需从屏蔽规则表中移除这些规则 [即所有代码都需要扫描该规则]
        List<String> openedCheckers = checker.getOpenedCheckers();
        if (CollectionUtils.isNotEmpty(openedCheckers))
        {
            if (!isNotEmpty(ignoreChecker))
            {
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"规则列表"}, null);
            }

            List<String> openIgnoreList = ignoreChecker.getIgnoreList();
            openedCheckers.forEach(open ->
            {
                // 如果屏蔽列表不包含提交的列表
                if (!openIgnoreList.contains(open))
                {
                    throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{open}, null);
                }

                // 从表中移除提交的规则
                updateIgnoreList.remove(open);
            });
        }

        // [关闭规则] : 需从提交的规则添加到IgnoreCheckerEntity表中
        List<String> closedCheckers = checker.getClosedCheckers();
        if (CollectionUtils.isNotEmpty(closedCheckers))
        {
            // 1. 判断添加的每一个规则是否存在表中.[注意此时的IgnoreCheckerEntity的list可能为空, 因为没有过规则]
            List<String> ignores = Optional.ofNullable(ignoreChecker)
                    .filter(ignore -> CollectionUtils.isNotEmpty(ignore.getIgnoreList()))
                    .map(IgnoreCheckerEntity::getIgnoreList)
                    .orElseGet(ArrayList::new);

            closedCheckers.forEach(close ->
            {
                if (ignores.contains(close))
                {
                    throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{close}, null);
                }
            });

            // 添加屏蔽规则
            updateIgnoreList.addAll(closedCheckers);
        }

        return updateIgnoreList;
    }


    /**
     * 获取工具对应的规则包
     *
     * @param toolName
     * @return
     */
    private Map<String, List<CheckerDetailVO>> getToolCheckerPkg(Long taskId, String toolName)
    {
        Map<String, GlobalMessage> globalMessageMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_CHECKER_DESC);
        Result<ToolConfigInfoVO> toolConfigInfoVOResult = client.get(ServiceToolRestResource.class).getToolByTaskIdAndName(taskId, toolName);
        if(toolConfigInfoVOResult.isNotOk() || null == toolConfigInfoVOResult.getData())
        {
            logger.error("tool info is empty! task id: {}, tool name: {}", taskId, toolName);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        ToolConfigInfoVO toolConfigInfoVO = toolConfigInfoVOResult.getData();
        List<CheckerDetailVO> checkerDetailEntityList = multitoolCheckerService.queryAllChecker(toolConfigInfoVO);
        return checkerDetailEntityList.stream()
                .filter(detailEntity -> StringUtils.isNotBlank(detailEntity.getPkgKind()))
                .map(checkerDetailVO ->
                {
                    checkerDetailVO.setCheckerDesc(getGlobalPkgDesc(globalMessageMap, checkerDetailVO.getCheckerKey(), checkerDetailVO.getCheckerDesc()));
                    return checkerDetailVO;
                }).collect(Collectors.groupingBy(CheckerDetailVO::getPkgKind));
    }


    /**
     * 判断忽略列表是否为空
     *
     * @param ignoreChecker
     * @return
     */
    private Boolean isNotEmpty(IgnoreCheckerEntity ignoreChecker)
    {
        boolean condition = false;
        if (Objects.nonNull(ignoreChecker) && CollectionUtils.isNotEmpty(ignoreChecker.getIgnoreList()))
        {
            condition = true;
        }
        return condition;
    }


    private String getGlobalPkgDesc(Map<String, GlobalMessage> globalMessageMap, String pkgName, String pkgDesc)
    {
        GlobalMessage globalMessage = globalMessageMap.get(pkgName);
        if (Objects.isNull(globalMessage))
        {
            logger.error("Can't find internationalization information for the corresponding package: {}", pkgName);
            return pkgDesc;
            //throw new CodeCCException(DefectMessageCode.NOT_FIND_CHECKER_PACKAGE, new String[]{pkgName}, null);
        }
        return globalMessageUtil.getMessageByLocale(globalMessage);
    }


}
