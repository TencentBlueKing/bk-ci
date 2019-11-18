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

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.common.Tree;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 告警文件树服务层
 *
 * @version V1.0
 * @date 2019/5/14
 */
public abstract class AbstractTreeService implements TreeService
{
    private static Logger logger = LoggerFactory.getLogger(AbstractTreeService.class);

    @Autowired
    private Tree tree;

    @Autowired
    private Client client;

    @Autowired
    private BizServiceFactory<TreeService> treeServiceBizServiceFactory;


    /**
     * 获取告警文件路径树
     *
     * @param taskId
     * @param toolNames
     * @return
     */
    @Override
    public TreeNodeVO getTreeNode(Long taskId, String toolNames)
    {
        // 获取文件集合
        Set<String> filePaths = new HashSet<>();
        Arrays.stream(toolNames.split(ComConstants.STRING_SPLIT))
                .forEach(tool ->
                        {
                            TreeService treeService;
                            if (ComConstants.TOOL_LINT_PREFIX.equals(tool))
                            {
                                tool = ComConstants.STRING_PREFIX_OR_SUFFIX;
                                treeService = treeServiceBizServiceFactory
                                        .createBizService(ComConstants.Tool.PYLINT.name(), ComConstants.BusinessType.TREE_SERVICE.value(), TreeService.class);
                            }
                            else
                            {
                                treeService = treeServiceBizServiceFactory
                                        .createBizService(tool, ComConstants.BusinessType.TREE_SERVICE.value(), TreeService.class);
                            }

                            filePaths.addAll(treeService.getDefectPaths(taskId, tool));
                        }
                );

        return getTreeNode(taskId, filePaths);
    }


    /**
     * 获取告警文件路径树
     *
     * @param taskId
     * @param filePaths
     * @return
     */
    @Override
    public TreeNodeVO getTreeNode(Long taskId, Set<String> filePaths)
    {
        if (CollectionUtils.isEmpty(filePaths))
        {
            return new TreeNodeVO();
        }

        Result<TaskDetailVO> taskBaseResult;
        try
        {
            taskBaseResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        }
        catch (Exception e)
        {
            logger.error("get task info fail!, task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        if (taskBaseResult.isNotOk() || Objects.isNull(taskBaseResult.getData()))
        {
            logger.error("mongorepository task info fail! taskId is: {}, msg: {}", taskId, taskBaseResult.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        TaskDetailVO taskBase = taskBaseResult.getData();
        return tree.buildTree(filePaths, taskBase.getNameEn(), Boolean.FALSE, Boolean.FALSE);
    }


    @Override
    public Boolean support(String type)
    {
        return Stream.of(ComConstants.Tool.values())
                .anyMatch(tool -> tool.name().equals(type.toUpperCase()));
    }


}
