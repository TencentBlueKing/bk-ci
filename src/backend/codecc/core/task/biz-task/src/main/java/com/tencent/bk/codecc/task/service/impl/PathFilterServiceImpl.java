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
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.api.ServiceDefectTreeResource;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.service.PathFilterService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.service.ToolService;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.bk.codecc.task.vo.FilterPathOutVO;
import com.tencent.bk.codecc.task.vo.TreeNodeTaskVO;
import com.tencent.bk.codecc.task.vo.path.CodeYmlFilterPathVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.PathUtils;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.DISABLE_ACTION;
import static com.tencent.devops.common.constant.ComConstants.ENABLE_ACTION;
import static com.tencent.devops.common.constant.ComConstants.FUNC_FILTER_PATH;

/**
 * 路径忽略服务类实现
 *
 * @version V1.0
 * @date 2019/4/26
 */
@Service
@Slf4j
public class PathFilterServiceImpl implements PathFilterService
{
    @Autowired
    private Client client;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BaseDataRepository baseDataRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ToolService toolService;

    @Override
    public void addDefaultFilterPaths(TaskInfoEntity taskInfoEntity)
    {
        List<BaseDataEntity> baseDataEntities = baseDataRepository.findAllByParamType(ComConstants.KEY_DEFAULT_FILTER_PATH);
        if (CollectionUtils.isNotEmpty(baseDataEntities))
        {
            List pathList = baseDataEntities.stream().
                    map(BaseDataEntity::getParamValue
                    ).
                    collect(Collectors.toList());
            taskInfoEntity.setDefaultFilterPath(pathList);
        }
    }


    /**
     * 添加路径屏蔽文件
     *
     * @param filterPathInput
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_FILTER_PATH, operType = ENABLE_ACTION)
    public Boolean addFilterPaths(FilterPathInputVO filterPathInput, String userName)
    {
        filterPathInput.setUserName(userName);
        Long taskId = filterPathInput.getTaskId();
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity))
        {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        FilterPathInputVO addPath = new FilterPathInputVO();
        addPath.setTaskId(filterPathInput.getTaskId());
        addPath.setPathType(filterPathInput.getPathType());

        List<String> tools = toolService.getEffectiveToolList(taskEntity);
        filterPathInput.setEffectiveTools(tools);

        // 屏蔽默认路径
        if (ComConstants.PATH_TYPE_DEFAULT.equalsIgnoreCase(filterPathInput.getPathType()))
        {
            processDefaultFilterPath(filterPathInput, taskEntity, addPath);
        }
        else if (ComConstants.PATH_TYPE_CODE_YML.equalsIgnoreCase(filterPathInput.getPathType())) {
            if (!processCodeYmlFilterPath(filterPathInput, taskEntity, addPath, tools)) {
                return true;
            }
        }
        else
        {
            processNormalFilterPath(filterPathInput, taskEntity, addPath);
        }

        // 设置强制全量扫描标志
        taskService.setForceFullScan(taskEntity);

        return taskDao.updateFilterPath(addPath, userName);
    }

    /**
     * 处理普通屏蔽路径
     *
     * @param filterPathInput
     * @param taskEntity
     * @param addPath
     */
    private void processNormalFilterPath(FilterPathInputVO filterPathInput,
                                         TaskInfoEntity taskEntity, FilterPathInputVO addPath) {
        ArrayList<String> newFilterPath = getFilePath(filterPathInput);

        List<String> oldFilterPath = CollectionUtils.isEmpty(taskEntity.getFilterPath()) ?
                new ArrayList<>() : taskEntity.getFilterPath();

        if (CollectionUtils.isNotEmpty(oldFilterPath))
        {
            for (String path : newFilterPath)
            {
                Iterator<String> iterator = oldFilterPath.iterator();
                while (iterator.hasNext())
                {
                    String dbPath = iterator.next();
                    try{
                        if (dbPath.matches(path))
                        {
                            iterator.remove();
                        }
                    } catch (PatternSyntaxException e){
                        log.error("invalid regex pattern");
                        iterator.remove();
                    }
                }
            }
        }

        oldFilterPath.addAll(newFilterPath);
        addPath.setFilterDir(oldFilterPath);

        // 任务通知, 其他工具類也需要屏蔽此路徑
        if (CollectionUtils.isNotEmpty(filterPathInput.getEffectiveTools()))
        {
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_TASK_FILTER_PATH,
                    ConstantsKt.ROUTE_ADD_TASK_FILTER_PATH, filterPathInput);
        }
    }

    /**
     * 处理code.yml的屏蔽路径
     *
     * @param filterPathInput
     * @param taskEntity
     * @param addPath
     * @param tools
     * @return
     */
    private boolean processCodeYmlFilterPath(FilterPathInputVO filterPathInput, TaskInfoEntity taskEntity,
                                             FilterPathInputVO addPath, List<String> tools) {
        Long taskId = taskEntity.getTaskId();
        List<String> hisTestSourceFilterPath = taskEntity.getTestSourceFilterPath();
        List<String> hisAutoGenFilterPath = taskEntity.getAutoGenFilterPath();
        List<String> hisThirdPartyFilterPath = taskEntity.getThirdPartyFilterPath();
        Boolean hisScanTestSource = taskEntity.getScanTestSource();

        List<String> newTestSourceFilterPath = filterPathInput.getTestSourceFilterPath();
        List<String> newAutoGenFilterPath = filterPathInput.getAutoGenFilterPath();
        List<String> newThirdPartyFilterPath = filterPathInput.getThirdPartyFilterPath();
        Boolean newScanTestSource = filterPathInput.getScanTestSource();

        // 判断是否有变更
        boolean isTestSourceEqual = isEqualCollection(newTestSourceFilterPath, hisTestSourceFilterPath);
        boolean isGenFilterEqual = isEqualCollection(newAutoGenFilterPath, hisAutoGenFilterPath);
        boolean isThirdPartyEqual = isEqualCollection(newThirdPartyFilterPath, hisThirdPartyFilterPath);

        boolean isScanTestSourceEqual = (newScanTestSource == null && hisScanTestSource == null)
                || (newScanTestSource != null && (newScanTestSource.equals(hisScanTestSource)
                || (newScanTestSource == false && hisScanTestSource == null)));
        if (isTestSourceEqual && isGenFilterEqual && isThirdPartyEqual && isScanTestSourceEqual) {
            log.info("code.yml path no change, need not to update...({})", taskId);
            return false;
        }

        addPath.setTestSourceFilterPath(newTestSourceFilterPath);
        addPath.setAutoGenFilterPath(newAutoGenFilterPath);
        addPath.setThirdPartyFilterPath(newThirdPartyFilterPath);
        addPath.setScanTestSource(newScanTestSource);
        addPath.setPathType(ComConstants.PATH_TYPE_CODE_YML);

        /*
         * 对于.code.yml中的测试代码，也要进行代码规范检查，这里判断是否需要扫描测试代码
         * 1.任务被标志为扫描测试代码
         * 2.工具是代码规范工具
         */
        BaseDataEntity standardToolsEntity = baseDataRepository
                .findFirstByParamType(ComConstants.BaseConfig.STANDARD_TOOLS.name());
        Set<String> standardToolSet = null;
        if (standardToolsEntity != null && StringUtils.isNotEmpty(standardToolsEntity.getParamValue())) {
            String[] standardToolArr = standardToolsEntity.getParamValue().split(ComConstants.STRING_SPLIT);
            standardToolSet = Sets.newHashSet(standardToolArr);
        }
        List<String> addTestPathList;
        List<String> addThirdPathList = minus(newThirdPartyFilterPath, hisThirdPartyFilterPath);
        List<String> addAutoGenPathList = minus(newAutoGenFilterPath, hisAutoGenFilterPath);

        List<String> delTestPathList;
        List<String> delThirdPathList = minus(hisThirdPartyFilterPath, newThirdPartyFilterPath);
        List<String> delAutoGenPathList = minus(hisAutoGenFilterPath, newAutoGenFilterPath);
        for (String toolName : tools) {
            boolean isScanTestSource = newScanTestSource != null && newScanTestSource
                    && CollectionUtils.isNotEmpty(standardToolSet) && standardToolSet.contains(toolName);
            /*
             * 1.如果扫描测试代码标志变化，且当前为true，则需要把原来屏蔽告警放出来，并且本次不屏蔽
             * 2.如果扫描测试代码标志变化，且当前为false，则原来没有屏蔽告警，本次需要屏蔽
             * 3.如果扫描测试代码标志没变化，且当前为true，则原来没有屏蔽告警，本次也不需要屏蔽
             * 4.如果扫描测试代码标志没变化，且当前为false，则原来屏蔽告警，本次也需要屏蔽
             */
            if (!isScanTestSourceEqual) {
                if (isScanTestSource) {
                    addTestPathList = null;
                    delTestPathList = hisTestSourceFilterPath;
                } else {
                    addTestPathList = newTestSourceFilterPath;
                    delTestPathList = null;
                }
            } else {
                if (isScanTestSource) {
                    addTestPathList = null;
                    delTestPathList = null;
                } else {
                    addTestPathList = minus(newTestSourceFilterPath, hisTestSourceFilterPath);
                    delTestPathList = minus(hisTestSourceFilterPath, newTestSourceFilterPath);
                }
            }

            filterPathInput.setEffectiveTools(Lists.newArrayList(toolName));
            if (CollectionUtils.isNotEmpty(delTestPathList) || CollectionUtils.isNotEmpty(delThirdPathList)
                    || CollectionUtils.isNotEmpty(delAutoGenPathList)) {
                filterPathInput.setTestSourceFilterPath(delTestPathList);
                filterPathInput.setAutoGenFilterPath(delAutoGenPathList);
                filterPathInput.setThirdPartyFilterPath(delThirdPathList);
                rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_TASK_FILTER_PATH,
                        ConstantsKt.ROUTE_DEL_TASK_FILTER_PATH, filterPathInput);
            }

            if (CollectionUtils.isNotEmpty(addTestPathList) || CollectionUtils.isNotEmpty(addThirdPathList)
                    || CollectionUtils.isNotEmpty(addAutoGenPathList)) {
                filterPathInput.setTestSourceFilterPath(addTestPathList);
                filterPathInput.setAutoGenFilterPath(addThirdPathList);
                filterPathInput.setThirdPartyFilterPath(addAutoGenPathList);
                rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_TASK_FILTER_PATH,
                        ConstantsKt.ROUTE_ADD_TASK_FILTER_PATH, filterPathInput);
            }
        }
        return true;
    }

    /**
     * 处理默认屏蔽路径
     *
     * @param filterPathInput
     * @param taskEntity
     * @param addPath
     */
    private void processDefaultFilterPath(FilterPathInputVO filterPathInput, TaskInfoEntity taskEntity,
                                          FilterPathInputVO addPath) {
        List<String> filterDir = filterPathInput.getDefaultFilterPath();

        // 前段传什么值就存什么值
        addPath.setDefaultFilterPath(filterPathInput.getDefaultFilterPath());

        if (CollectionUtils.isEmpty(filterPathInput.getEffectiveTools())) {
            return;
        }

        // 获取系统默认屏蔽路径
        List<BaseDataEntity> baseDataEntities = baseDataRepository.findAllByParamType(ComConstants.KEY_DEFAULT_FILTER_PATH);
        List<String> sysDefaultPathList = baseDataEntities.stream()
                .map(BaseDataEntity::getParamValue).collect(Collectors.toList());

        verifyDefaultFilterFile(sysDefaultPathList, filterDir);

        List<String> hisDefaultPath = taskEntity.getDefaultFilterPath() != null
                ? taskEntity.getDefaultFilterPath() : new ArrayList<>();

        // 提取出增加的屏蔽路径
        List<String> addFiles = new ArrayList<>(filterDir);
        addFiles.removeAll(hisDefaultPath);
        if (CollectionUtils.isNotEmpty(addFiles)) {
            filterPathInput.setDefaultFilterPath(addFiles);
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_TASK_FILTER_PATH,
                    ConstantsKt.ROUTE_ADD_TASK_FILTER_PATH, filterPathInput);
        }

        // 提取出删除的屏蔽路径
        hisDefaultPath.removeAll(filterDir);
        if (CollectionUtils.isNotEmpty(hisDefaultPath)) {
            filterPathInput.setDefaultFilterPath(hisDefaultPath);
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_TASK_FILTER_PATH,
                    ConstantsKt.ROUTE_DEL_TASK_FILTER_PATH, filterPathInput);
        }
    }


    /**
     * 删除路径屏蔽
     *
     * @param path
     * @param pathType
     * @param userName
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_FILTER_PATH, operType = DISABLE_ACTION)
    public Boolean deleteFilterPath(String path, String pathType, Long taskId, String userName)
    {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity))
        {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        // 删除默认过滤文件
        FilterPathInputVO filterPathInput = new FilterPathInputVO();
        filterPathInput.setTaskId(taskId);
        filterPathInput.setPathType(pathType);
        filterPathInput.setUserName(userName);
        // 是否发送通知
        // boolean send = true;
        if (ComConstants.PATH_TYPE_DEFAULT.equalsIgnoreCase(pathType))
        {
            List<String> defaultFilterPath = taskEntity.getDefaultFilterPath();
            if (CollectionUtils.isEmpty(defaultFilterPath) || !defaultFilterPath.contains(path))
            {
                log.error("This path does not exist in the default path: {}", path);
                throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{path}, null);
            }

            defaultFilterPath.remove(path);
            filterPathInput.setDefaultFilterPath(defaultFilterPath);
        }
        else
        {
            List<String> filterPath = taskEntity.getFilterPath();
            if (CollectionUtils.isEmpty(filterPath) || !filterPath.contains(path))
            {
                log.error("This path does not exist in the custom path: {}", path);
                throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{path}, null);
            }

            // send = isSendNotify(path, filterPath);

            filterPath.remove(path);
            filterPathInput.setFilterDir(filterPath);
        }

        taskDao.updateFilterPath(filterPathInput, userName);

        // 任务通知, 其他工具類也需要移除屏蔽路徑
        filterPathInput.setDefaultFilterPath(Collections.singletonList(path));
        filterPathInput.setFilterDir(Collections.singletonList(path));
        List<String> tools = toolService.getEffectiveToolList(taskEntity);
        if (CollectionUtils.isNotEmpty(tools))
        {
            filterPathInput.setEffectiveTools(tools);
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_TASK_FILTER_PATH,
                    ConstantsKt.ROUTE_DEL_TASK_FILTER_PATH, filterPathInput);
        }
        // 设置强制全量扫描标志
        taskService.setForceFullScan(taskEntity);

        return true;
    }


    /**
     * 获取屏蔽路径列表[默认/自定义]
     *
     * @param taskId
     * @return
     */
    @Override
    public FilterPathOutVO getFilterPath(Long taskId)
    {
        TaskInfoEntity taskInfo = taskRepository.findFirstByTaskId(taskId);
        if (taskInfo == null)
        {
            log.error("task not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{"任务参数"}, null);
        }

        FilterPathOutVO filterPathOut = new FilterPathOutVO();
        filterPathOut.setFilterPaths(taskInfo.getFilterPath());
        filterPathOut.setTaskId(taskId);

        // 设置默认的屏蔽路径
        List<BaseDataEntity> baseDataEntities = baseDataRepository.findAllByParamType(ComConstants.KEY_DEFAULT_FILTER_PATH);
        if (CollectionUtils.isNotEmpty(baseDataEntities))
        {
            // 所有默认屏蔽路径
            List<String> pathList = baseDataEntities.stream().
                    map(BaseDataEntity::getParamValue).
                    collect(Collectors.toList());

            // 添加默认列表
            List<String> defaultFilterPath = CollectionUtils.isNotEmpty(taskInfo.getDefaultFilterPath()) ?
                    taskInfo.getDefaultFilterPath() : new ArrayList<>();

            filterPathOut.setDefaultAddPaths(defaultFilterPath);
            filterPathOut.setDefaultFilterPath(pathList);
        }

        return filterPathOut;
    }


    /**
     * 获取路径屏蔽树
     *
     * @param taskId
     * @return
     */
    @Override
    public TreeNodeTaskVO filterPathTree(Long taskId)
    {
        TreeNodeTaskVO treeNodeVO = new TreeNodeTaskVO();
        List<String> tools = toolService.getEffectiveToolList(taskId);
        if (CollectionUtils.isEmpty(tools))
        {
            return treeNodeVO;
        }

        Result<TreeNodeVO> treeNode = client.get(ServiceDefectTreeResource.class).getTreeNode(taskId, tools);
        if (treeNode.isNotOk())
        {
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        // 获取树列表
        BeanUtils.copyProperties(Objects.requireNonNull(treeNode.getData()), treeNodeVO);
        return treeNodeVO;
    }

    @Override
    public Boolean codeYmlFilterPath(Long taskId, String userName, CodeYmlFilterPathVO codeYmlFilterPathVO) {
        FilterPathInputVO filterPathVo = new FilterPathInputVO();
        filterPathVo.setTaskId(taskId);
        filterPathVo.setPathType(ComConstants.PATH_TYPE_CODE_YML);
        filterPathVo.setTestSourceFilterPath(codeYmlFilterPathVO.getTestSourceFilterPath());
        filterPathVo.setAutoGenFilterPath(codeYmlFilterPathVO.getAutoGenFilterPath());
        filterPathVo.setThirdPartyFilterPath(codeYmlFilterPathVO.getThirdPartyFilterPath());
        filterPathVo.setScanTestSource(codeYmlFilterPathVO.getScanTestSource());

        addFilterPaths(filterPathVo, userName);

        return true;
    }

    // 必须顺序、数量完全一样，不去重
    private boolean isEqualCollection(List<String> c1, List<String> c2)
    {
        if (CollectionUtils.isEmpty(c1) && CollectionUtils.isEmpty(c2)) {
            return true;
        }

        // 排除其中一个为空的情况
        if (CollectionUtils.isEmpty(c1)) {
            return false;
        }
        if (CollectionUtils.isEmpty(c2)) {
            return false;
        }

        if (c1.size() != c2.size()) {
            return false;
        }

        for (int i = 0; i < c1.size(); i++) {
            if (!c1.get(i).equals(c2.get(i))) {
                return false;
            }
        }
        return true;
    }

    // 求集合c1 - c2
    private List<String> minus(List<String> c1, List<String> c2)
    {
        List<String> result = new ArrayList<>();

        if (CollectionUtils.isEmpty(c1))
        {
            return result;
        }
        if (CollectionUtils.isEmpty(c2))
        {
            return c1;
        }

        c1.forEach((item1) ->
        {
            if (!c2.contains(item1))
            {
                result.add(item1);
            }
        });
        return result;
    }

    @Override
    public CodeYmlFilterPathVO listCodeYmlFilterPath(Long taskId) {
        TaskInfoEntity taskInfo = taskRepository.findFirstByTaskId(taskId);
        if (taskInfo == null)
        {
            log.error("task not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{"任务参数"}, null);
        }
        CodeYmlFilterPathVO codeYmlFilterPathVo = new CodeYmlFilterPathVO();
        codeYmlFilterPathVo.setAutoGenFilterPath(taskInfo.getAutoGenFilterPath());
        codeYmlFilterPathVo.setTestSourceFilterPath(taskInfo.getTestSourceFilterPath());
        codeYmlFilterPathVo.setThirdPartyFilterPath(taskInfo.getThirdPartyFilterPath());
        codeYmlFilterPathVo.setScanTestSource(taskInfo.getScanTestSource());
        return codeYmlFilterPathVo;
    }

    /**
     * 获取自定义路径
     *
     * @param filterPathInput
     * @return
     */
    @NotNull
    private ArrayList<String> getFilePath(FilterPathInputVO filterPathInput)
    {
        ArrayList<String> filterPath = new ArrayList<>();

        List<String> fileDir = filterPathInput.getFilterDir();
        List<String> filterFile = filterPathInput.getFilterFile();
        List<String> customPath = filterPathInput.getCustomPath();
        // 手工输入路径
        if (!CollectionUtils.isEmpty(customPath))
        {
            filterPath.addAll(customPath);
        }
        // 选择屏蔽文件
        if (!CollectionUtils.isEmpty(filterFile))
        {
            filterPath.addAll(filterFile);
            //PathUtils.convertPaths(filterFile, customFilterPath, PathUtils.FILE);
        }
        // 选择屏蔽文件夹
        if (!CollectionUtils.isEmpty(fileDir))
        {
            PathUtils.convertPaths(fileDir, filterPath, PathUtils.DIR);
        }

        return filterPath.stream()
                .filter(e -> !ComConstants.STRING_PREFIX_OR_SUFFIX.equals(e.trim()))
                .collect(Collectors.toCollection(ArrayList::new));
    }


    /**
     * 验证默認屏蔽文件信息
     *
     * @param filterDir
     */
    private void verifyDefaultFilterFile(List<String> defaultList, List<String> filterDir)
    {
        if (CollectionUtils.isEmpty(filterDir))
        {
            return;
        }
        if (CollectionUtils.isEmpty(defaultList))
        {
            log.error("default filter path is empty!");
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{"默认路径"}, null);
        }

        // 1.验证提交的默认路径不存在默认屏蔽路径中
        String noneMatchFile = filterDir.stream()
                .filter(file -> !defaultList.contains(file))
                .collect(Collectors.joining(
                        ComConstants.STRING_DELIMITER,
                        ComConstants.STRING_PREFIX_OR_SUFFIX,
                        ComConstants.STRING_PREFIX_OR_SUFFIX)
                );
        if (StringUtils.isNotBlank(noneMatchFile))
        {
            String errMsg = String.format("Filter path [%s] is not common filter path!", noneMatchFile);
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{noneMatchFile}, null);
        }

    }
}
