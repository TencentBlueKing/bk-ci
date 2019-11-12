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

package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.pojo.Result;

import java.util.List;

/**
 * 工具管理服务接口
 *
 * @version V1.0
 * @date 2019/4/25
 */
public interface ToolService
{

    /**
     * 蓝盾批量注册工具
     *
     * @param batchRegisterVO
     * @param userName
     * @return
     */
    Result<Boolean> registerTools(BatchRegisterVO batchRegisterVO, String userName);

    /**
     * 注册工具
     *
     * @param toolConfigInfo
     * @param user
     */
    void registerTool(ToolConfigInfoVO toolConfigInfo, TaskInfoEntity taskInfoEntity, String user);

    /**
     * 更新工具步骤状态
     *
     * @param toolConfigBaseVO
     */
    void updateToolStepStatus(ToolConfigBaseVO toolConfigBaseVO);

    /**
     * 工具停用启用
     *
     * @param toolNameList
     * @param manageType
     * @param userName
     * @param taskId
     * @return
     */
    Boolean toolStatusManage(List<String> toolNameList, String manageType, String userName, long taskId);


    /**
     * 获取有效工具清单
     *
     * @param taskId
     * @return
     */
    List<String> getEffectiveToolList(long taskId);


    /**
     * 停用流水线
     *
     * @param taskId
     * @param projectId
     * @return
     */
    Boolean deletePipeline(Long taskId, String projectId, String userName);


    /**
     * 根据任务id和工具名称获取工具信息
     *
     * @param taskId
     * @param toolName
     * @return
     */
    ToolConfigInfoVO getToolByTaskIdAndName(long taskId, String toolName);

    /**
     * 根据任务id和工具名称获取待名称的工具信息
     * @param taskId
     * @param toolName
     * @return
     */
    ToolConfigInfoVO getToolWithNameByTaskIdAndName(long taskId, String toolName);
}
