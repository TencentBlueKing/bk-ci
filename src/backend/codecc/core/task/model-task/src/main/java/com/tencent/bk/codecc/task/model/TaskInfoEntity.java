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

package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_task_detail")
@CompoundIndexes({
        @CompoundIndex(name = "project_id_1_create_from_1", def = "{'project_id': 1, 'create_from': 1}",
                background = true)
})
public class TaskInfoEntity extends CommonEntity {
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
    @Indexed
    private String nameEn;

    /**
     * 项目中文名
     */
    @Field("name_cn")
    private String nameCn;

    /**
     * 项目所用语言
     */
    @Field("code_lang")
    private Long codeLang;

    /**
     * 项目拥有者
     */
    @Field("task_owner")
    private List<String> taskOwner;

    /**
     * 项目成员
     */
    @Field("task_member")
    private List<String> taskMember;

    /**
     * 项目查看者
     */
    @Field("task_viewer")
    private List<String> taskViewer;

    /**
     * 任务状态，0-启用，1-停用
     */
    @Field("status")
    @Indexed(background = true)
    private Integer status;

    /***
     * 项目id
     */
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
     * 项目创建来源
     */
    @Indexed
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
     * 定时任务的crontab时间表达式
     */
    @Field("timer_expression")
    private String timerExpression;

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
    @Indexed
    @Field("bg_id")
    private int bgId;

    /**
     * 部门id
     */
    @Indexed
    @Field("dept_id")
    private int deptId;

    /**
     * 中心id
     */
    @Field("center_id")
    private int centerId;

    /**
     * 组id
     */
    @Field("group_id")
    private int groupId;

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
     * 仓库别名
     */
    @Field("alias_name")
    private String aliasName;

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
     * code.yml的自定义过滤路径
     */
    @Field("test_source_filter_path")
    private List<String> testSourceFilterPath;


    /**
     * code.yml的自定义过滤路径
     */
    @Field("auto_gen_filter_path")
    private List<String> autoGenFilterPath;

    /**
     * code.yml自定义过滤路径
     */
    @Field("third_party_filter_path")
    private List<String> thirdPartyFilterPath;

    /**
     * 路径白名单
     */
    @Field("white_paths")
    private List<String> whitePaths;

    /**
     * 用于存储停用任务之后保存的定时执行任务信息
     */
    @Field("last_disable_task_info")
    private DisableTaskEntity lastDisableTaskInfo;

    /**
     * 工蜂开源扫描首次创建
     */
    @Field("gongfeng_flag")
    private Boolean gongfengFlag;

    /**
     * 工蜂项目id
     */
    @Indexed
    @Field("gongfeng_project_id")
    private Integer gongfengProjectId;

    /**
     * 工蜂最近commit_id
     */
    @Field("gongfeng_commit_id")
    private String gongfengCommitId;

    /**
     * 一个月内变更代码总量
     */
    @Field("total_work")
    private Integer totalWork;

    /**
     * 扫描方式1：增量扫描；0：全量扫描 2：diff模式
     */
    @Field("scan_type")
    private Integer scanType;

    /**
     * 操作系统类型
     */
    @Field("os_type")
    private String osType;

    /**
     * 构建环境
     */
    @Field("build_env")
    private Map<String, String> buildEnv;

    /**
     * 定制报告信息
     */
    @Field("notify_custom_info")
    private NotifyCustomEntity notifyCustomInfo;

    /**
     * 编译类型
     */
    @Field("project_build_type")
    private String projectBuildType;

    /**
     * 编译命令
     */
    @Field("project_build_command")
    private String projectBuildCommand;

    /**
     * 新告警转为历史告警的配置
     */
    @Field("new_defect_judge")
    private NewDefectJudgeEntity newDefectJudge;

    /**
     * 置顶的用户
     */
    @Field("tops_user")
    private Set<String> topUser;

    /**
     * 原子插件码，
     * 旧插件: 值为空
     * 新插件: 值为CodeccCheckAtom
     */
    @Field("atom_code")
    private String atomCode;

    /**
     * 用于个性化传入工蜂项目在开源集群上运行的代码扫描的项目信息
     */
    @Field("custom_proj_info")
    @DBRef
    private CustomProjEntity customProjInfo;

    /**
     * 对于不同失效原因，需要专门字段进行标识
     */
    @Field("opensource_disable_reason")
    private Integer opensourceDisableReason;

    /*
    * 是否回写工蜂
    */
    @Field("mr_comment_enable")
    private Boolean mrCommentEnable;

    /*
     * 是否扫描测试代码，true-扫描，false-不扫描，默认不扫描
     */
    @Field("scan_test_source")
    private Boolean scanTestSource;

    /**
     * 任务失败记录
     */
    @Field("latest_scan_result")
    private TaskFailRecordEntity taskFailRecordEntity;
}
