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

package com.tencent.bk.codecc.apiquery.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * task entity
 *
 * @version V1.0
 * @date 2019/4/24
 */
@Data
public class TaskInfoModel
{
    /**
     * 项目主键id
     */
    @JsonProperty("task_id")
    private long taskId;

    /**
     * 项目英文名
     */
    @JsonProperty("name_en")
    private String nameEn;

    /**
     * 项目中文名
     */
    @JsonProperty("name_cn")
    private String nameCn;

    /**
     * 项目所用语言
     */
    @JsonProperty("code_lang")
    private Long codeLang;

    /**
     * 项目拥有者
     */
    @JsonProperty("task_owner")
    private List<String> taskOwner;

    /**
     * 项目成员
     */
    @JsonProperty("task_member")
    private List<String> taskMember;

    /**
     * 项目查看者
     */
    @JsonProperty("task_viewer")
    private List<String> taskViewer;

    /**
     * 任务状态，0-启用，1-停用
     */
    @JsonProperty("status")
    private Integer status;

    /***
     * 项目id
     */
    @JsonProperty("project_id")
    private String projectId;

    /**
     * 项目名
     */
    @JsonProperty("project_name")
    private String projectName;

    /**
     * 流水线id
     */
    @JsonProperty("pipeline_id")
    private String pipelineId;

    /**
     * 项目创建来源
     */
    @JsonProperty("create_from")
    private String createFrom;

    /**
     * 已接入的所有工具名称，格式; COVERITY,CPPLINT,PYLINT
     */
    @JsonProperty("tool_names")
    private String toolNames;

    /**
     * 项目接入的工具列表，查询时使用
     */
    @JsonProperty("tool_config_info_list")
    private List<ToolConfigInfoModel> toolConfigInfoList;

    /**
     * 任务停用原因
     */
    @JsonProperty("disable_reason")
    private String disableReason;

    /**
     * 任务停用时间
     */
    @JsonProperty("disable_time")
    private String disableTime;

    /**
     * 定时任务执行时间
     */
    @JsonProperty("execute_time")
    private String executeTime;

    /**
     * 定时任务执行日期
     */
    @JsonProperty("execute_date")
    private List<String> executeDate;

    /**
     * 定时任务的crontab时间表达式
     */
    @JsonProperty("timer_expression")
    private String timerExpression;

    /**
     * 编译平台
     */
    @JsonProperty("compile_plat")
    private String compilePlat;

    /**
     * 运行平台
     */
    @JsonProperty("run_plat")
    private String runPlat;

    /**
     * 描述
     */
    @JsonProperty("description")
    private String description;

    /**
     * 事业群id
     */
    @JsonProperty("bg_id")
    private int bgId;

    /**
     * 部门id
     */
    @JsonProperty("dept_id")
    private int deptId;

    /**
     * 中心id
     */
    @JsonProperty("center_id")
    private int centerId;

    /**
     * 组id
     */
    @JsonProperty("group_id")
    private int groupId;

    /**
     * 工作空间id
     */
    @JsonProperty("workspace_id")
    private long workspaceId;

    /**
     * 凭证管理的主键id
     */
    @JsonProperty("repo_hash_id")
    private String repoHashId;

    /**
     * 仓库别名
     */
    @JsonProperty("alias_name")
    private String aliasName;

    /**
     * 分支名，默认为master
     */
    @JsonProperty("branch")
    private String branch;

    /**
     * 代码库类型
     */
    @JsonProperty("scm_type")
    private String scmType;

    /**
     * 代码库的最新版本号
     */
    @JsonProperty("repo_revision")
    private String repoRevision;

    /**
     * 将默认过滤路径放到任务实体对象下面
     */
    @JsonProperty("default_filter_path")
    private List<String> defaultFilterPath;

    /**
     * 已添加的自定义过滤路径
     */
    @JsonProperty("filter_path")
    private List<String> filterPath;

    /**
     * code.yml的自定义过滤路径
     */
    @JsonProperty("test_source_filter_path")
    private List<String> testSourceFilterPath;


    /**
     * code.yml的自定义过滤路径
     */
    @JsonProperty("auto_gen_filter_path")
    private List<String> autoGenFilterPath;

    /**
     * code.yml自定义过滤路径
     */
    @JsonProperty("third_party_filter_path")
    private List<String> thirdPartyFilterPath;

    /**
     * 用于存储停用任务之后保存的定时执行任务信息
     */
    @JsonProperty("last_disable_task_info")
    private DisableTaskModel lastDisableTaskInfo;

    /**
     * 工蜂开源扫描首次创建
     */
    @JsonProperty("gongfeng_flag")
    private Boolean gongfengFlag;

    /**
     * 工蜂项目id
     */
    @JsonProperty("gongfeng_project_id")
    private Integer gongfengProjectId;

    /**
     * 工蜂最近commit_id
     */
    @JsonProperty("gongfeng_commit_id")
    private String gongfengCommitId;

    /**
     * 一个月内变更代码总量
     */
    @JsonProperty("total_work")
    private Integer totalWork;

    /**
     * 扫描方式1：增量扫描；0：全量扫描
     */
    @JsonProperty("scan_type")
    private Integer scanType;

    /**
     * 操作系统类型
     */
    @JsonProperty("os_type")
    private String osType;

    /**
     * 构建环境
     */
    @JsonProperty("build_env")
    private Map<String, String> buildEnv;

    /**
     * 定制报告信息
     */
    @JsonProperty("notify_custom_info")
    private NotifyCustomModel notifyCustomInfo;

    /**
     * 编译类型
     */
    @JsonProperty("project_build_type")
    private String projectBuildType;

    /**
     * 编译命令
     */
    @JsonProperty("project_build_command")
    private String projectBuildCommand;

    /**
     * 新告警转为历史告警的配置
     */
    @JsonProperty("new_defect_judge")
    private NewDefectJudgeModel newDefectJudge;

    /**
     * 置顶的用户
     */
    @JsonProperty("tops_user")
    private Set<String> topUser;

    /**
     * 原子插件码，
     * 旧插件: 值为空
     * 新插件: 值为CodeccCheckAtom
     */
    @JsonProperty("atom_code")
    private String atomCode;

    /**
     * 用于个性化传入工蜂项目在开源集群上运行的代码扫描的项目信息
     */
    @JsonProperty("custom_proj_info")
    private CustomProjModel customProjInfo;

    /**
     * 工蜂统计项目信息
     */
    @JsonProperty("gongfeng_stat_proj_info")
    private List<GongfengStatProjModel> gongfengStatProjInfo;

    /**
     * 最新更新时间
     */
    @JsonProperty("updated_date")
    private Long updatedDate;

    /**
     * 创建时间
     */
    @JsonProperty("create_date")
    private Long createdDate;

    /**
     * 开源停用原因
     */
    @JsonProperty("opensource_disable_reason")
    private Integer opensourceDisableReason;

    /**
     * 错误原因
     */
    @JsonProperty("latest_scan_result")
    private TaskFailRecordModel taskFailRecordModel;

    /**
     * 创建者
     */
    @JsonProperty("created_by")
    private String createdBy;
}
