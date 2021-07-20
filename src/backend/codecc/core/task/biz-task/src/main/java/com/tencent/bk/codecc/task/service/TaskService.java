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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tencent.bk.codecc.task.enums.TaskSortType;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.pojo.GongfengPublicProjModel;
import com.tencent.bk.codecc.task.vo.NotifyCustomVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskCodeLibraryVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskListReqVO;
import com.tencent.bk.codecc.task.vo.TaskListVO;
import com.tencent.bk.codecc.task.vo.TaskMemberVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.bk.codecc.task.vo.TaskOwnerAndMemberVO;
import com.tencent.bk.codecc.task.vo.TaskStatusVO;
import com.tencent.bk.codecc.task.vo.TaskUpdateVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.bk.codecc.task.vo.tianyi.QueryMyTasksReqVO;
import com.tencent.bk.codecc.task.vo.tianyi.TaskInfoVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
    TaskListVO getTaskList(String projectId, String user, TaskSortType taskSortType, TaskListReqVO taskListReqVO);

    /**
     * 查询任务名字清单
     *
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
     * 获取任务信息
     *
     * @param taskId
     * @return
     */
    TaskDetailVO getTaskInfoById(Long taskId);

    /**
     * 通过taskid查询任务信息，不包含工具信息
     *
     * @param taskId
     * @return
     */
    TaskDetailVO getTaskInfoWithoutToolsByTaskId(Long taskId);

    /**
     * 根据流名称获取任务的有效工具及语言等信息
     *
     * @param streamName
     * @return
     */
    TaskDetailVO getTaskInfoByStreamName(String streamName);

    /**
     * 根据任务Id查询任务接入工具情况
     *
     * @param taskId
     * @return
     */
    TaskBaseVO getTaskToolList(long taskId);

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
     * 获取任务信息概览
     *
     * @param taskId
     * @return
     */
    TaskOverviewVO getTaskOverview(Long taskId, String buildNum);

    /**
     * 获取任务信息概览
     *
     * @param taskId
     * @return
     */
    TaskOverviewVO getTaskOverview(Long taskId, String buildNum, String orderBy);

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
     * 停用任务
     *
     * @param pipelineId
     * @param userName
     * @return
     */
    Boolean stopTask(String pipelineId, String disabledReason, String userName);


    /**
     * 停用任务
     *
     * @param taskId
     * @param userName
     * @return
     */
    Boolean stopTaskByAdmin(Long taskId, String disabledReason, String userName);

    /**
     * 开启任务
     *
     * @param taskId
     * @param userName
     * @return
     */
    Boolean startTaskByAdmin(Long taskId, String userName);


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
     * @param taskDetailVO
     * @return
     */
    Boolean updateCodeLibrary(Long taskId, String userName, TaskDetailVO taskDetailVO) throws JsonProcessingException;

    /**
     * 获取任务有权限的各角色人员清单
     *
     * @param taskId
     * @param projectId
     * @return
     */
    TaskMemberVO getTaskUsers(long taskId, String projectId);

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
     * 手动触发分析-不加代理
     * @param taskId
     * @param isFirstTrigger
     * @param userName
     * @return
     */
    Boolean manualExecuteTaskNoProxy(long taskId, String isFirstTrigger, String userName);

    /**
     * 手动触发分析
     *
     * @param taskId
     * @param isFirstTrigger
     * @param userName
     * @return
     */
    Boolean manualExecuteTask(long taskId, String isFirstTrigger, String userName);

    /**
     * 发送任务开始信号
     * @param taskId
     * @param buildId
     * @return
     */
    Boolean sendStartTaskSignal(Long taskId, String buildId);


    /**
     * 通过流水线ID获取任务信息
     *
     * @param pipelineId
     * @param user
     * @return
     */
    PipelineTaskVO getTaskInfoByPipelineId(String pipelineId, String user);


    /**
     * 获取任务状态
     *
     * @param taskId
     * @return
     */
    TaskStatusVO getTaskStatus(Long taskId);

    /**
     * 根据id查询task信息
     *
     * @param taskId
     * @return
     */
    TaskInfoEntity getTaskById(Long taskId);

    /**
     * 保存任务信息
     *
     * @param taskInfoEntity
     * @return
     */
    Boolean saveTaskInfo(TaskInfoEntity taskInfoEntity);

    /**
     * 根据bg id查询任务清单
     * @param bgId
     * @return
     */
    List<TaskBaseVO> getTasksByBgId(Integer bgId);

    /**
     * 通过task id查询任务清单
     * @param taskIds
     * @return
     */
    public List<TaskBaseVO> getTasksByIds(List<Long> taskIds);


    /**
     * 根据任务状态及任务接入过的工具获取
     *
     * @param taskListReqVO 请求体
     * @return list
     */
    TaskListVO getTaskDetailList(QueryTaskListReqVO taskListReqVO);


    /**
     * 根据作者名、代码仓库地址及分支名获取任务列表
     *
     * @param reqVO 查询'我的'任务请求体
     * @return list
     */
    Page<TaskInfoVO> getTasksByAuthor(QueryMyTasksReqVO reqVO);


    /**
     * 更新定时报告信息
     * @param taskId
     * @param notifyCustomVO
     */
    void updateReportInfo(Long taskId, NotifyCustomVO notifyCustomVO);

    /**
     * 更新置顶用户信息
     * @param taskId
     * @param user
     * @return
     */
    Boolean updateTopUserInfo(Long taskId, String user, Boolean topFlag);

    /**
     * 设置强制全量扫描标志
     *
     * @param taskEntity
     */
    void setForceFullScan(TaskInfoEntity taskEntity);

    /**
     * 修改任务扫描触发配置
     *
     * @param taskId
     * @param scanConfigurationVO
     * @return
     */
    Boolean updateScanConfiguration(Long taskId, String user, ScanConfigurationVO scanConfigurationVO);

    /**
     * api使用作者转换
     * @param taskId
     * @param transferAuthorPairs
     * @param userId
     * @return
     */
    Boolean authorTransferForApi(Long taskId, List<ScanConfigurationVO.TransferAuthorPair> transferAuthorPairs, String userId);


    /**
     * 按事业群ID获取部门ID集合
     *
     * @param bgId bgId
     * @return set
     */
    Set<Integer> queryDeptIdByBgId(Integer bgId);

    /**
     * 按工蜂id获取任务实体清单
     * @param gongfengProjectId
     * @return
     */
    TaskInfoEntity getTaskByGongfengId(Integer gongfengProjectId);

    /** 多条件查询任务列表
     *
     * @param taskListReqVO 请求体
     * @return 任务列表
     */
    List<TaskDetailVO> getTaskInfoList(QueryTaskListReqVO taskListReqVO);

    /**
     * 分页查询任务详情列表
     *
     * @param reqVO 请求体
     * @return page
     */
    Page<TaskDetailVO> getTaskDetailPage(QueryTaskListReqVO reqVO);

    /**
     * 刷新组织架构
     *
     * @return boolean
     */
    Boolean refreshTaskOrgInfo(Long taskId);

    /**
     * 更新任务管理员和任务成员
     * @param taskOwnerAndMemberVO
     * @param taskId
     */
    void updateTaskOwnerAndMember(TaskOwnerAndMemberVO taskOwnerAndMemberVO, Long taskId);

    /**
     * 获取蓝盾插件开源扫描任务信息
     *
     */
    List<Long> getBkPluginTaskIds();

    /**
     * 触发蓝盾插件打分任务
     *
     */
    Boolean triggerBkPluginScoring();

    List<MetadataVO> listTaskToolDimension(Long taskId);

    /**
     * 根据代码库别名获取任务信息
     *
     * @param aliasName
     */
    TaskDetailVO getTaskInfoByAliasName(String aliasName);

    /**
     * 根据代码库id获取任务状态
     *
     * @param id
     */
    TaskDetailVO getTaskInfoByGongfengId(int id, GongfengPublicProjModel gongfengPublicProjModel);

    /**
     * 根据工蜂代码库创建扫描任务并计入开源扫描任务
     */
//    Boolean createTaskByRepoId(String repoId, List<String> langs);

    /**
     * 按创建来源查询任务ID
     * @param taskCreateFrom 任务来源列表
     * @return list
     */
    List<Long> queryTaskIdByCreateFrom(List<String> taskCreateFrom);

    /**
     * 获取开源或非开源的任务ID
     *
     * @param defectStatType enum
     * @return list
     */
    List<Long> queryTaskIdByType(ComConstants.DefectStatType defectStatType);


    /**
     * 仅用于查询获取任务数量脚本
     *
     * @param day 天数
     * @return boolean
     */
    Boolean initTaskCountScript(Integer day);

    /**
     * 根据任务英文名查询任务信息，不包含工具信息
     *
     * @param nameEn 流名称
     * @return vo
     */
    TaskDetailVO getTaskInfoWithoutToolsByStreamName(String nameEn);


    /**
     * 获取工蜂代码库信息
     */
    TaskCodeLibraryVO getRepoInfo(Long taskId);

    /**
     * 添加路径白名单
     *
     */
    boolean addWhitePath(long taskId, List<String> pathList);
}
