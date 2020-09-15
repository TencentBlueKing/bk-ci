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

import com.tencent.bk.codecc.defect.vo.checkerset.*;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;

import java.util.List;

public interface ICheckerSetBizService {

    /**
     * 查询规则集列表
     *
     * @param toolNames
     * @param user
     * @param projectId
     * @return
     */
    UserCheckerSetsVO getCheckerSets(List<String> toolNames, String user, String projectId);

    /**
     * 查询规则集列表
     *
     * @param toolName
     * @param user
     * @param projectId
     * @return
     */
    PipelineCheckerSetVO getPipelineCheckerSets(String toolName, String user, String projectId);

    /**
     * 更新规则集
     *
     * @param taskId
     * @param toolName
     * @param checkerSetId
     * @param updateCheckerSetReqVO
     * @param user
     * @param projectId
     * @return
     */
    Boolean updateCheckerSet(Long taskId, String toolName, String checkerSetId, UpdateCheckerSetReqVO updateCheckerSetReqVO, String user, String projectId);

    /**
     * 任务关联规则集
     *
     * @param user
     * @param taskId
     * @return
     */
    boolean addCheckerSet2Task(String user, Long taskId, AddCheckerSet2TaskReqVO addCheckerSet2TaskReqVO);

    /**
     * 查询用户创建的规则集列表
     *
     * @param toolName
     * @param user
     * @param projectId
     * @return
     */
    UserCreatedCheckerSetsVO getUserCreatedCheckerSet(String toolName, String user, String projectId);

    /**
     * 清除规则集被使用的任务列表中的指定任务
     *
     * @param toolConfig
     * @param taskId
     */
    void removeTaskInUse(ToolConfigInfoVO toolConfig, long taskId);

    /**
     * 查询规则集指定版本的差异
     *
     * @param user
     * @param projectId
     * @param toolName
     * @param checkerSetId
     * @param checkerSetDifferenceVO
     * @return
     */
    CheckerSetDifferenceVO getCheckerSetVersionDifference(String user, String projectId, String toolName, String checkerSetId,
                                                          CheckerSetDifferenceVO checkerSetDifferenceVO);

    /**
     * 清除任务关联的规则集
     *
     * @param taskId
     * @param toolNames
     * @param user
     * @param needUpdatePipeline
     * @return
     */
    Boolean clearTaskCheckerSets(long taskId, List<String> toolNames, String user, boolean needUpdatePipeline);

    /**
     * 清除任务关联的规则集
     *
     * @param taskDetail
     * @param toolNames
     * @param user
     * @param needUpdatePipeline
     * @return
     */
    Boolean clearTaskCheckerSets(TaskDetailVO taskDetail, List<String> toolNames, String user, boolean needUpdatePipeline);


    /**
     * 更新规则集参数
     *
     * @param checkerSetId
     * @param version
     * @param checkerName
     * @param paramName
     * @param displayName
     * @param paramValue
     * @return
     */
    Boolean updateCheckerSetConfigParam(String checkerSetId, Integer version, String checkerName,
                                        String paramName, String displayName, String paramValue);


}
