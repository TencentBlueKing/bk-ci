package com.tencent.bk.codecc.task.service.impl;

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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to
 use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.service.TaskAuthV3Service;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.devops.common.auth.api.pojo.external.callback.FetchInstanceInfo;
import com.tencent.devops.common.auth.api.pojo.external.callback.ListInstanceInfo;
import com.tencent.devops.common.auth.api.pojo.external.callback.SearchInstanceInfo;
import com.tencent.devops.common.service.utils.PageableUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskAuthV3ServiceImpl implements TaskAuthV3Service {
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskDao taskDao;

    private static final Logger logger = LoggerFactory.getLogger(TaskAuthV3ServiceImpl.class);

    @Override
    public ListInstanceResponseDTO getTask(String projectId, Long offset, Long limit) {
        int pageNum = (int) (offset / limit);
        Pageable pageable = PageableUtils.getPageable(pageNum, Math.toIntExact(offset));

        List<TaskInfoEntity> taskList =
            taskRepository.findByProjectId(projectId, pageable);

        ListInstanceInfo result = new ListInstanceInfo();

        if (CollectionUtils.isEmpty(taskList)) {
            logger.info("$projectId 项目下无codecc任务");
            return result.buildListInstanceFailResult();
        }

        List<InstanceInfoDTO> instList = taskList.stream().map(taskInfo -> {
            InstanceInfoDTO instanceInfoDTO = new InstanceInfoDTO();
            instanceInfoDTO.setId(String.valueOf(taskInfo.getTaskId()));
            instanceInfoDTO.setDisplayName(taskInfo.getNameCn());
            return instanceInfoDTO;
        }).collect(Collectors.toList());

        logger.info("get task count for project is: {}, {}", projectId, instList.size());
        return result.buildListInstanceResult(instList, taskList.size());
    }

    @Override
    public FetchInstanceInfoResponseDTO getTask(Set<Long> ids) {
        Set<TaskInfoEntity> taskSet = taskRepository.findByTaskIdIn(ids);
        FetchInstanceInfo result = new FetchInstanceInfo();
        if (CollectionUtils.isEmpty(taskSet)) {
            logger.info("无任务");
            return result.buildFetchInstanceFailResult();
        }
        List<InstanceInfoDTO> entityList = taskSet.stream().map(it -> {
            InstanceInfoDTO entity = new InstanceInfoDTO();
            entity.setId(String.valueOf(it.getTaskId()));
            entity.setDisplayName(it.getNameCn());
            return entity;
        }).collect(Collectors.toList());

        logger.info("get task count for ids is: {}", entityList.size());

        return result.buildFetchInstanceResult(entityList);
    }

    @Override
    public SearchInstanceInfo searchTask(String projectId, String keyword, Long offset, Long limit) {
        List<TaskInfoEntity> taskList = taskDao.findByCodeccNameCn(projectId, keyword, offset, limit);
        SearchInstanceInfo result = new SearchInstanceInfo();
        if (CollectionUtils.isEmpty(taskList)) {
            logger.info("{} 项目下无任务: ", projectId);
            return result.buildSearchInstanceFailResult();
        }
        List<InstanceInfoDTO> instList = taskList.stream().map(it -> {
            InstanceInfoDTO entity = new InstanceInfoDTO();
            entity.setId(String.valueOf(it.getTaskId()));
            entity.setDisplayName(it.getNameCn());
            return entity;
        }).collect(Collectors.toList());

        logger.info("get task count for project is: {}, {}", projectId, instList.size());
        return result.buildSearchInstanceResult(instList, instList.size());
    }
}
