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

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.common.Tree;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeFileUrlRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoFromAnalyzeLogRepository;
import com.tencent.bk.codecc.defect.model.CodeFileUrlEntity;
import com.tencent.bk.codecc.defect.model.CodeRepoFromAnalyzeLogEntity;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Stream;

/**
 * 告警文件树服务层
 *
 * @version V1.0
 * @date 2019/5/14
 */
@Slf4j
public abstract class AbstractTreeService implements TreeService
{
    @Autowired
    private Tree tree;

    @Autowired
    private Client client;

    @Autowired
    private BizServiceFactory<TreeService> treeServiceFactory;

    @Autowired
    private CodeFileUrlRepository codeFileUrlRepository;

    @Autowired
    private CodeRepoFromAnalyzeLogRepository codeRepoFromAnalyzeLogRepository;

    /**
     * 获取告警文件路径树
     *
     * @param taskId
     * @param toolNames
     * @return
     */
    @Override
    public TreeNodeVO getTreeNode(Long taskId, List<String> toolNames)
    {
        // 获取文件集合
        Set<String> filePaths = new HashSet<>();
        toolNames.forEach(toolName ->
                {
                    TreeService treeService = treeServiceFactory
                            .createBizService(toolName, ComConstants.BusinessType.TREE_SERVICE.value(), TreeService.class);
                    Set<String> eachToolFilePathSet = treeService.getDefectPaths(taskId, toolName);

                    filePaths.addAll(eachToolFilePathSet);
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
            log.error("get task info fail!, task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        if (taskBaseResult.isNotOk() || Objects.isNull(taskBaseResult.getData()))
        {
            log.error("mongorepository task info fail! taskId is: {}, msg: {}", taskId, taskBaseResult.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        TaskDetailVO taskBase = taskBaseResult.getData();
        return tree.buildTree(filePaths, taskBase.getNameCn(), Boolean.FALSE, Boolean.FALSE);
    }


    @Override
    public Boolean support(String type)
    {
        return Stream.of(ComConstants.Tool.values())
                .anyMatch(tool -> tool.name().equals(type.toUpperCase()));
    }


    /**
     * 获取告警文件路径的相对路径的映射
     *
     * @param taskId
     */
    @Override
    public Map<String, String> getRelatePathMap(long taskId)
    {
        List<CodeFileUrlEntity> codeFileUrlList = codeFileUrlRepository.findByTaskId(taskId);
        Map<String, String> relatePathMap = new HashMap<>(codeFileUrlList.size());

        CodeRepoFromAnalyzeLogEntity codeRepoFromAnalyzeLogEntity = codeRepoFromAnalyzeLogRepository.findCodeRepoFromAnalyzeLogEntityFirstByTaskId(taskId);
        if (codeRepoFromAnalyzeLogEntity != null)
        {
            Set<CodeRepoFromAnalyzeLogEntity.CodeRepo> codeRepoList = codeRepoFromAnalyzeLogEntity.getCodeRepoList();
            codeFileUrlList.forEach(codeFileUrlEntity ->
            {
                String url = codeFileUrlEntity.getUrl();
                String filePath = codeFileUrlEntity.getFile();
                if (StringUtils.isNotEmpty(url) && StringUtils.isNotEmpty(filePath))
                {
                    String relatePath = null;
                    for (CodeRepoFromAnalyzeLogEntity.CodeRepo codeRepo : codeRepoList)
                    {
                        String uploadUrl = codeRepo.getUrl();
                        int prefixIndex = url.indexOf(uploadUrl);
                        if (prefixIndex != -1)
                        {
                            String[] uploadUrlPart = uploadUrl.split("/");
                            String uploadUrlRoot = uploadUrlPart[uploadUrlPart.length - 1];
                            int beginIndex = uploadUrl.lastIndexOf(uploadUrlRoot) - 1;
                            relatePath = url.substring(beginIndex);
                            break;
                        }
                    }
                    relatePathMap.put(filePath.toLowerCase(), StringUtils.isEmpty(relatePath) ? filePath : relatePath);
                }
            });

        }
        else
        {
            codeFileUrlList.forEach(codeFileUrlEntity ->
            {
                String url = codeFileUrlEntity.getUrl();
                String filePath = codeFileUrlEntity.getFile();
                String[] urlSplitArr = url.split("/");
                String[] filePathSplitArr = filePath.split("/");
                StringBuffer relatePath = new StringBuffer();
                int k = urlSplitArr.length - 1;
                for (int i = filePathSplitArr.length - 1; i >= 0; i--)
                {
                    if (!filePathSplitArr[i].equalsIgnoreCase(urlSplitArr[k]))
                    {
                        break;
                    }
                    k--;
                }
                for (; k < urlSplitArr.length; k++)
                {
                    relatePath.append("/").append(urlSplitArr[k]);
                }
                relatePathMap.put(filePath.toLowerCase(), StringUtils.isEmpty(relatePath) ? filePath : relatePath.toString());
            });
        }

        return relatePathMap;
    }
}
