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

import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.service.AbstractTaskRegisterService;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通用任务注册服务层代码
 *
 * @version V1.0
 * @date 2019/5/8
 */
@Service("taskRegisterService")
public class TaskRegisterServiceImpl extends AbstractTaskRegisterService
{
    private static Logger logger = LoggerFactory.getLogger(TaskRegisterServiceImpl.class);

    @Override
    public TaskIdVO registerTask(TaskDetailVO taskDetailVO, String userName)
    {
        TaskInfoEntity taskInfoEntity = this.saveTaskInfo(taskDetailVO, userName);
        return new TaskIdVO(taskInfoEntity.getTaskId(), taskInfoEntity.getNameEn());
    }

    @Override
    public TaskInfoEntity saveTaskInfo(TaskDetailVO taskDetailVO, String userName)
    {
        //1.校验新接入的项目英文名是否已经被注册过
        if (checkeIsStreamRegistered(taskDetailVO.getNameEn()))
        {
            logger.error("the task name has been registered! task name: {}", taskDetailVO.getNameEn());
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"项目名称"}, null);
        }

        //2.创建新项目到数据库
        TaskInfoEntity taskInfoEntity = new TaskInfoEntity();
        BeanUtils.copyProperties(taskDetailVO, taskInfoEntity, "toolConfigInfoList");
        logger.info("register task successfully! name: {}", taskInfoEntity.getNameEn());
        return createTask(taskInfoEntity, userName);
    }

    @Override
    public Boolean modifyTimeAnalysisTask(List<String> executeDate, String executeTime, long taskId, String userName)
    {
        return null;
    }

    @Override
    public Boolean updateTaskFromPipeline(TaskDetailVO taskDetailVO, String userName)
    {
        return null;
    }


}
