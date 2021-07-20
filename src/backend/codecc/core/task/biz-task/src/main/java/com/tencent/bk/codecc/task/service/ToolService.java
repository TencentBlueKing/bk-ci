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
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.vo.*;
import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
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
     * @param taskInfoEntity
     * @param userName
     * @return
     */
    Result<Boolean> registerTools(BatchRegisterVO batchRegisterVO, TaskInfoEntity taskInfoEntity, String userName);

    /**
     * 注册工具
     *  @param toolConfigInfo
     * @param user
     * @return
     */
    ToolConfigInfoEntity registerTool(ToolConfigInfoVO toolConfigInfo, TaskInfoEntity taskInfoEntity, String user);

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
     * 获取有效工具清单
     *
     * @param taskInfoEntity
     * @return
     */
    List<String> getEffectiveToolList(TaskInfoEntity taskInfoEntity);

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
     *
     * @param taskId
     * @param toolName
     * @return
     */
    ToolConfigInfoWithMetadataVO getToolWithMetadataByTaskIdAndName(long taskId, String toolName);

    /**
     * 更新流水线工具配置
     *
     * @param taskId
     * @param toolList
     * @param userName
     * @return
     */
    Boolean updatePipelineTool(Long taskId, List<String> toolList, String userName);

    /**
     * 清除任务和工具关联的规则集
     *
     * @param taskId
     * @param toolNames
     * @return
     */
    Boolean clearCheckerSet(Long taskId, List<String> toolNames);

    /**
     * 清除任务和工具关联的规则集
     *
     * @param taskId
     * @param toolCheckerSets
     * @return
     */
    Boolean addCheckerSet2Task(Long taskId, List<ToolCheckerSetVO> toolCheckerSets);

    /**
     * 修改工具特殊参数和规则集
     *
     * @param user
     * @param taskId
     * @param paramJsonAndCheckerSetsVO
     * @return
     */
    Boolean updateParamJsonAndCheckerSets(String user, Long taskId, ParamJsonAndCheckerSetsVO paramJsonAndCheckerSetsVO);

    /**
     * 获取工具platform配置信息
     *
     * @param taskId   任务ID
     * @param toolName 工具名
     * @return vo
     */
    ToolConfigPlatformVO getToolConfigPlatformInfo(Long taskId, String toolName);

    /**
     * 更新工具配置特殊参数
     *
     * @param taskId               任务ID
     * @param userName             变更人
     * @param toolConfigPlatformVO 变更内容
     * @return boolean
     */
    Boolean updateToolPlatformInfo(Long taskId, String userName,
            ToolConfigPlatformVO toolConfigPlatformVO);

    /**
     * 更新任务下的工具
     *
     * @param taskId
     * @param user
     * @param batchRegisterVO
     * @return
     */
    Result<Boolean> updateTools(Long taskId, String user, BatchRegisterVO batchRegisterVO);

    /**
     * 批量查询工具配置信息
     *
     * @param queryReqVO 请求体
     * @return list
     */
    List<ToolConfigInfoVO> batchGetToolConfigList(QueryTaskListReqVO queryReqVO);

    /**
     * 批量更新工具跟进状态：非停用任务中未跟进状态(0,1)且已成功分析一次的工具
     * @return boolean
     */
    Boolean batchUpdateToolFollowStatus(Integer pageSize);

    /**
     * 仅用于初始化查询工具数量
     *
     * @param day 天数
     * @return
     */
    Boolean initToolCountScript(Integer day);
}
