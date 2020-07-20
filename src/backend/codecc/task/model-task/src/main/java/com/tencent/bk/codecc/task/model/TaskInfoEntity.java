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

package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * task entity
 *
 * @version V1.0
 * @date 2019/4/24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_task_detail")
public class TaskInfoEntity extends CommonEntity
{
    /**
     * 项目主键id
     */
    @Field("task_id")
    @Indexed(unique = true)
    private long taskId;

    /**
     * 项目英文名
     */
    @Field("name_en")
    private String nameEn;

    /**
     * 项目中文名
     */
    @Indexed
    @Field("name_cn")
    private String nameCn;

    /**
     * 项目所用语言
     */
    @Field("code_lang")
    private Long codeLang;

    /**
     * 项目负责人/管理员
     */
    @Field("task_owner")
    private List<String> taskOwner;

    /**
     * 项目成员
     */
    @Field("task_member")
    private List<String> taskMember;


    /**
     * 任务状态，0-启用，1-停用
     */
    @Field("status")
    private Integer status;

    /***
     * 项目id
     */
    @Indexed
    @Field("project_id")
    private String projectId;

    /**
     * 项目名
     */
    @Field("project_name")
    private String projectName;

    /**
     * 流水线id
     */
    @Indexed
    @Field("pipeline_id")
    private String pipelineId;

    /**
     * 流水线名
     */
    @Field("pipeline_name")
    private String pipelineName;

    /**
     * 项目创建来源
     */
    @Field("create_from")
    private String createFrom;

    /**
     * 已接入的所有工具名称，格式; COVERITY,CPPLINT,PYLINT
     */
    @Field("tool_names")
    private String toolNames;

    /**
     * 项目接入的工具列表，查询时使用
     */
    @DBRef
    @Field("tool_config_info_list")
    private List<ToolConfigInfoEntity> toolConfigInfoList;

    /**
     * 任务停用原因
     */
    @Field("disable_reason")
    private String disableReason;

    /**
     * 任务停用时间
     */
    @Field("disable_time")
    private String disableTime;

    /**
     * 定时任务执行时间
     */
    @Field("execute_time")
    private String executeTime;

    /**
     * 定时任务执行日期
     */
    @Field("execute_date")
    private List<String> executeDate;

    /**
     * 编译平台
     */
    @Field("compile_plat")
    private String compilePlat;

    /**
     * 运行平台
     */
    @Field("run_plat")
    private String runPlat;

    /**
     * 描述
     */
    @Field("description")
    private String description;


    /**
     * 事业群id
     */
    @Field("bg_id")
    private int bgId;

    /**
     * 部门id
     */
    @Field("dept_id")
    private int deptId;

    /**
     * 中心id
     */
    @Field("center_id")
    private int centerId;

    /**
     * 工作空间id
     */
    @Field("workspace_id")
    private long workspaceId;

    /**
     * 凭证管理的主键id
     */
    @Field("repo_hash_id")
    private String repoHashId;

    /**
     * 分支名，默认为master
     */
    @Field("branch")
    private String branch;

    /**
     * 代码库类型
     */
    @Field("scm_type")
    private String scmType;

    /**
     * 代码库的最新版本号
     */
    @Field("repo_revision")
    private String repoRevision;

    /**
     * 将默认过滤路径放到任务实体对象下面
     */
    @Field("default_filter_path")
    private List<String> defaultFilterPath;

    /**
     * 已添加的自定义过滤路径
     */
    @Field("filter_path")
    private List<String> filterPath;

    /**
     * SVN版本
     */
    @Field("svn_revision")
    private String svnRevision;

    /**
     * 用于存储停用任务之后保存的定时执行任务信息
     */
    @Field("last_disable_task_info")
    private DisableTaskEntity lastDisableTaskInfo;

}
