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

package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.defect.api.ServiceDefectTreeResource;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.service.PathFilterService;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.bk.codecc.task.vo.FilterPathOutVO;
import com.tencent.bk.codecc.task.vo.TreeNodeTaskVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.PathUtils;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import com.tencent.devops.common.web.mq.ConstantsKt;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.*;

/**
 * 路径忽略服务类实现
 *
 * @version V1.0
 * @date 2019/4/26
 */
@Service
public class PathFilterServiceImpl implements PathFilterService
{
    private static Logger logger = LoggerFactory.getLogger(PathFilterServiceImpl.class);

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
        Long taskId = filterPathInput.getTaskId();
        TaskInfoEntity taskEntity = taskRepository.findByTaskId(taskId);
        if (Objects.isNull(taskEntity))
        {
            logger.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        // 系统默认屏蔽路径
        List<BaseDataEntity> baseDataEntities = baseDataRepository.findAllByParamType(ComConstants.KEY_DEFAULT_FILTER_PATH);
        List<String> sysDefaultPathList = null;
        if (CollectionUtils.isNotEmpty(baseDataEntities))
        {
            sysDefaultPathList = baseDataEntities.stream()
                    .map(BaseDataEntity::getParamValue)
                    .collect(Collectors.toList());
        }

        FilterPathInputVO addPath = new FilterPathInputVO();
        addPath.setTaskId(filterPathInput.getTaskId());
        addPath.setPathType(filterPathInput.getPathType());
        filterPathInput.setUserName(userName);
        // 屏蔽默认路径
        if (ComConstants.PATH_TYPE_DEFAULT.equalsIgnoreCase(filterPathInput.getPathType()))
        {
            List<String> filterDir = filterPathInput.getDefaultFilterPath();
            verifyDefaultFilterFile(sysDefaultPathList, filterDir);
            // 前段传什么值就存什么值
            addPath.setDefaultFilterPath(filterDir);
            // 通知其他工具维度
            sendDefaultFiles(filterPathInput, taskEntity.getDefaultFilterPath(), filterDir);
        }
        else
        {
            ArrayList<String> customFilterPath = getCustomFilePath(filterPathInput);
            // 验证提交自定义路径
            verifyCustomizeFilterFile(sysDefaultPathList, customFilterPath, taskEntity);

            List<String> filterPath = CollectionUtils.isEmpty(taskEntity.getFilterPath()) ?
                    new ArrayList<>() : taskEntity.getFilterPath();

            if (CollectionUtils.isNotEmpty(filterPath))
            {
                for (String path : customFilterPath)
                {
                    Iterator<String> iterator = filterPath.iterator();
                    while (iterator.hasNext())
                    {
                        String dbPath = iterator.next();
                        if (dbPath.matches(path))
                        {
                            iterator.remove();
                        }
                    }
                }
            }

            filterPath.addAll(customFilterPath);
            addPath.setFilterDir(filterPath);

            // 任务通知, 其他工具類也需要屏蔽此路徑
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_TASK_FILTER_PATH,
                    ConstantsKt.ROUTE_ADD_TASK_FILTER_PATH, filterPathInput);
        }

        return updateEntityAndNotify(addPath, taskEntity.getPipelineId(), userName);
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
        TaskInfoEntity taskEntity = taskRepository.findByTaskId(taskId);
        if (Objects.isNull(taskEntity))
        {
            logger.error("taskInfo not exists! task id is: {}", taskId);
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
                logger.error("This path does not exist in the default path: {}", path);
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
                logger.error("This path does not exist in the custom path: {}", path);
                throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{path}, null);
            }

            // send = isSendNotify(path, filterPath);

            filterPath.remove(path);
            filterPathInput.setFilterDir(filterPath);
        }

        updateEntityAndNotify(filterPathInput, taskEntity.getPipelineId(), userName);

        // 任务通知, 其他工具類也需要移除屏蔽路徑
        filterPathInput.setDefaultFilterPath(Collections.singletonList(path));
        filterPathInput.setFilterDir(Collections.singletonList(path));
        rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_TASK_FILTER_PATH,
                ConstantsKt.ROUTE_DEL_TASK_FILTER_PATH, filterPathInput);

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
        TaskInfoEntity taskInfo = taskRepository.findByTaskId(taskId);
        if (taskInfo == null)
        {
            logger.error("task not exists! task id is: {}", taskId);
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
        // 直接从task表中toolNames取
        String defectFileToolNames = String.format("%s,%s,%s", ComConstants.Tool.CCN.name(),
                ComConstants.Tool.DUPC.name(), ComConstants.TOOL_LINT_PREFIX);

        Result<TreeNodeVO> treeNode = client.get(ServiceDefectTreeResource.class).getTreeNode(taskId, defectFileToolNames);
        if (treeNode.isNotOk())
        {
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        // 获取树列表
        TreeNodeTaskVO treeNodeVO = new TreeNodeTaskVO();
        BeanUtils.copyProperties(Objects.requireNonNull(treeNode.getData()), treeNodeVO);
        return treeNodeVO;
    }


    /**
     * 获取自定义路径
     *
     * @param filterPathInput
     * @return
     */
    @NotNull
    private ArrayList<String> getCustomFilePath(FilterPathInputVO filterPathInput)
    {
        ArrayList<String> customFilterPath = new ArrayList<>();

        List<String> fileDir = filterPathInput.getFilterDir();
        List<String> filterFile = filterPathInput.getFilterFile();
        List<String> customPath = filterPathInput.getCustomPath();
        // 手工输入路径
        if (!CollectionUtils.isEmpty(customPath))
        {
            customFilterPath.addAll(customPath);
        }
        // 选择屏蔽文件
        if (!CollectionUtils.isEmpty(filterFile))
        {
            customFilterPath.addAll(filterFile);
            //PathUtils.convertPaths(filterFile, customFilterPath, PathUtils.FILE);
        }
        // 选择屏蔽文件夹
        if (!CollectionUtils.isEmpty(fileDir))
        {
            PathUtils.convertPaths(fileDir, customFilterPath, PathUtils.DIR);
        }

        return customFilterPath.stream()
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
            logger.error("default filter path is empty!");
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
            logger.error(errMsg);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{noneMatchFile}, null);
        }

    }


    /**
     * 验证自定义过滤路径数据
     *
     * @param defaultPath
     * @param customizePath
     * @return
     */
    private void verifyCustomizeFilterFile(List<String> defaultPath, List<String> customizePath, TaskInfoEntity taskEntity)
    {
        if (CollectionUtils.isEmpty(defaultPath))
        {
            return;
        }
        if (CollectionUtils.isEmpty(customizePath))
        {
            logger.error("add filter path list is empty!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"自定义路径"}, null);
        }

        // 1.提交的路径存在默认路径中
        Collector<CharSequence, ?, String> joining = Collectors.joining(ComConstants.STRING_DELIMITER, ComConstants.STRING_PREFIX_OR_SUFFIX, ComConstants.STRING_PREFIX_OR_SUFFIX);
        String defaultPaths = customizePath.stream()
                .filter(defaultPath::contains)
                .collect(joining);
        if (StringUtils.isNotBlank(defaultPaths))
        {
            logger.error("The added path [{}] exists in the default path ", defaultPaths);
            throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{defaultPaths}, null);
        }

        if (CollectionUtils.isNotEmpty(taskEntity.getFilterPath()))
        {
            // 2.验证提交的路径是否添加过
            String matchFile = customizePath.stream()
                    .filter(file -> taskEntity.getFilterPath().contains(file))
                    .collect(joining);
            if (StringUtils.isNotBlank(matchFile))
            {
                logger.error("Filter path [{}] have existed!", matchFile);
                throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{matchFile}, null);
            }
        }

    }


    private Boolean updateEntityAndNotify(FilterPathInputVO filterPathInput, String pipelineId, String userName)
    {
        // 删除路径之后，这些文件不应该加入分析[告警/重复率..], 将版本号改成0是判断的一个依据
        if (StringUtils.isNotBlank(pipelineId))
        {
            filterPathInput.setSvnRevision(String.valueOf(0L));
        }

        // 更新屏蔽路径数据
        return taskDao.updateFilterPath(filterPathInput, userName);
    }


    private void sendDefaultFiles(FilterPathInputVO filterPathInput, List<String> dbDefaultPath, List<String> filterDir)
    {
        // 处理job消息数据
        if (CollectionUtils.isEmpty(dbDefaultPath))
        {
            dbDefaultPath = new ArrayList<>();
        }

        // 去重DB中的数据 -- 同步去重后的数据到job模块[添加]
        List<String> removeDbFiles = new ArrayList<>(filterDir);
        removeDbFiles.removeAll(dbDefaultPath);
        if (CollectionUtils.isNotEmpty(removeDbFiles))
        {
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_TASK_FILTER_PATH,
                    ConstantsKt.ROUTE_ADD_TASK_FILTER_PATH, filterPathInput);
        }

        // 去重前端中的数据 -- 同步去重后的数据到job模块[删除]
        List<String> destList = new ArrayList<>(filterDir);
        dbDefaultPath.removeAll(destList);
        if (CollectionUtils.isNotEmpty(dbDefaultPath))
        {
            filterPathInput.setDefaultFilterPath(dbDefaultPath);
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_TASK_FILTER_PATH,
                    ConstantsKt.ROUTE_DEL_TASK_FILTER_PATH, filterPathInput);
        }
    }


//    private boolean isSendNotify(String path, List<String> filterPath)
//    {
//        // 如果提交的文件夹包含数据库中文件，则直接移除数据库文件而不需要通知移除屏蔽路徑[ 不通知 ]
//        String key = ".*";
//        if (path.endsWith(key))
//        {
//            Iterator<String> iterator = filterPath.iterator();
//            while (iterator.hasNext())
//            {
//                String dbPath = iterator.next();
//                if (dbPath.matches(path))
//                {
//                    iterator.remove();
//                }
//            }
//        }
//        return true;
//    }



}
