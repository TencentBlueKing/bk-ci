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

package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.vo.*;

import java.util.List;
import java.util.Map;

/**
 * 任务服务接口
 *
 * @version V1.0
 * @date 2019/4/23
 */
public interface TaskService
{
    /**
     * 查询任务清单
     *
     * @param projectId
     * @param user
     * @return
     */
    TaskListVO getTaskList(String projectId, String user);

    /**
     * 查询任务名字清单
     * @param projectId
     * @param user
     * @return
     */
    TaskListVO getTaskBaseList(String projectId, String user);

    /**
     * 根据任务Id查询任务完整信息
     *
     * @return
     */
    TaskBaseVO getTaskInfo();

    /**
     * 根据任务Id查询任务接入工具情况
     *
     * @param taskId
     * @return
     */
    TaskBaseVO getTaskToolList(long taskId);

    /**
     * 根据任务英文名查询任务基本信息
     *
     * @param nameEn
     * @return
     */
    TaskBaseVO getTaskInfo(String nameEn);


    /**
     * 修改任务基本信息
     *
     * @param taskUpdateVO
     * @param userName
     * @return
     */
    Boolean updateTask(TaskUpdateVO taskUpdateVO, Long taskId, String userName);

    /**
     * 修改任务基本信息 - 内部服务间调用
     *
     * @param taskUpdateVO
     * @param userName
     * @return
     */
    Boolean updateTaskByServer(TaskUpdateVO taskUpdateVO, String userName);


    /**
     * 获取任务信息
     *
     * @param taskId
     * @return
     */
    TaskDetailVO getTaskInfoById(Long taskId);

    /**
     * 获取任务信息概览
     *
     * @param taskId
     * @return
     */
    TaskOverviewVO getTaskOverview(Long taskId);


    /**
     * 开启任务
     *
     * @param taskId
     * @param userName
     * @return
     */
    Boolean startTask(Long taskId, String userName);


    /**
     * 停用任务
     *
     * @param taskId
     * @param userName
     * @return
     */
    Boolean stopTask(Long taskId, String disabledReason, String userName);


    /**
     * 获取代码库配置信息
     *
     * @param taskId
     * @return
     */
    TaskCodeLibraryVO getCodeLibrary(Long taskId);


    /**
     * 更新代码库配置信息
     *
     * @param taskId
     * @param taskCodeLibrary
     * @return
     */
    Boolean updateCodeLibrary(Long taskId, String userName, TaskCodeLibraryVO taskCodeLibrary);

    /**
     * 获取任务成员和管理员清单
     *
     * @param taskId
     * @param projectId
     * @return
     */
    TaskMemberVO getTaskMemberAndAdmin(long taskId, String projectId);

    /**
     * 检查任务是否存在
     *
     * @param taskId
     * @return
     */
    Boolean checkTaskExists(long taskId);


    /**
     * 获取所有的基础工具信息
     *
     * @return
     */
    Map<String, ToolMetaBaseVO> getToolMetaListFromCache();

    /**
     * 手动触发分析
     *
     * @param taskId
     * @param abortFlag
     * @param isFirstTrigger
     * @param userName
     * @return
     */
    Boolean manualExecuteTask(long taskId, String isFirstTrigger, String userName);


    /**
     * 通过流水线ID获取任务信息
     *
     * @param pipelineId
     * @return
     */
    TaskDetailVO getTaskInfoByPipelineId(String pipelineId);


    /**
     * 获取任务状态
     * @param taskId
     * @return
     */
    public TaskStatusVO getTaskStatus(Long taskId);

}
