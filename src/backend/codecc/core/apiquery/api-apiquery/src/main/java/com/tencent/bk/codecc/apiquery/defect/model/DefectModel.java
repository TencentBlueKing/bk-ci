/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.bk.codecc.apiquery.utils.EntityIdDeserializer;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 可跟踪告警的实体
 *
 * @version V1.0
 * @date 2019/10/20
 */
@Data
public class DefectModel extends CommonModel
{
    public static final String CLASS_TYPE = "DefectModel";

    @Id
    @JsonProperty("_id")
    @JsonDeserialize(using = EntityIdDeserializer.class)
    private String entityId;
    /**
     * 告警的唯一标志
     */
    @JsonProperty("id")
    private String id;

    /**
     * 任务ID
     */
    @JsonProperty("task_id")
    private long taskId;

    /**
     * 流名称
     */
    @JsonProperty("stream_name")
    private String streamName;

    /**
     * 工具名
     */
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 规则名称
     */
    @JsonProperty("checker_name")
    private String checkerName;

    /**
     * 用户给告警标志的状态,这个状态采用自定义的状态，而不使用klocwork的状态
     * 1:待处理(默认)，4:已忽略，8:路径屏蔽，16:规则屏蔽，32:标志位已修改
     */
    @JsonProperty("status")
    private int status;

    /**
     * 告警所在文件
     */
    @JsonProperty("file_path_name")
    private String filePathname;

    @JsonProperty("file_md5")
    private String fileMD5;

    /**
     * 规则类型，对应Coverity Platform中的Category(类别)
     */
    @JsonProperty("display_category")
    protected String displayCategory;

    /**
     * 类型子类，对应Coverity Platform中的Type(类型)
     */
    @JsonProperty("display_type")
    protected String displayType;

    /**
     * 告警处理人
     */
    @JsonProperty("author_list")
    private Set<String> authorList;

    /**
     * 告警严重程度
     */
    @JsonProperty("severity")
    private int severity;

    /**
     * 告警行号
     */
    @JsonProperty("line_number")
    protected int lineNumber;

    /**
     * 忽略告警原因类型
     */
    @JsonProperty("ignore_reason_type")
    private int ignoreReasonType;

    /**
     * 忽略告警具体原因
     */
    @JsonProperty("ignore_reason")
    private String ignoreReason;

    /**
     * 忽略告警的作者
     */
    @JsonProperty("ignore_author")
    private String ignoreAuthor;

    /**
     * 告警创建时间
     */
    @JsonProperty("create_time")
    private long createTime;

    /**
     * 告警修复时间
     */
    @JsonProperty("fixed_time")
    private long fixedTime;

    /**
     * 告警忽略时间
     */
    @JsonProperty("ignore_time")
    private long ignoreTime;

    /**
     * 告警屏蔽时间
     */
    @JsonProperty("exclude_time")
    private long excludeTime;

    /**
     * 告警是否被标记为已修改的标志，checkbox for developer, 0 is normal, 1 is tag, 2 is prompt
     */
    @JsonProperty("mark")
    private Integer mark;

    /**
     * 告警被标记为已修改的时间
     */
    @JsonProperty("mark_time")
    private Long markTime;

    /**
     * 创建时的构建号
     */
    @JsonProperty("create_build_number")
    private String createBuildNumber;

    /**
     * 修复时的构建号
     */
    @JsonProperty("fixed_build_number")
    private String fixedBuildNumber;

    /**
     * 文件对应仓库版本号
     */
    @JsonProperty("file_version")
    private String fileVersion;

    /**
     * 对应第三方缺陷管理系统的ID，这里声明为字符串可以有更好的兼容性
     */
    @JsonProperty("ext_bug_id")
    private String extBugid;

    /**
     * 第三方平台的buildId
     */
    @JsonProperty("platform_build_id")
    private String platformBuildId;

    /**
     * 第三方平台的项目ID
     */
    @JsonProperty("platform_project_id")
    private String platformProjectId;

    @JsonProperty("defect_instances")
    private List<DefectInstance> defectInstances;

    /**
     * 告警实例的数据
     */
    @Data
    public static class DefectInstance
    {
        /**
         * 有关引起告警的跟踪数据。可以有多个跟踪数据。
         */
        private List<Trace> traces;
    }

    @Data
    public static class Trace
    {
        private String message;

        @JsonProperty("file_md5")
        private String fileMD5;

        @JsonProperty("file_pathname")
        private String filePathname;

        private String tag;

        @JsonProperty("trace_number")
        private int traceNumber;

        @JsonProperty("line_number")
        private int lineNumber;

        private boolean main;

        /**
         * 事件类型:
         * MODEL：       与函数调用对应。在 Coverity Connect 中，模型事件显示在“显示详情”(Show Details) 链接旁边。
         * -----------------------------------------------------------------------------------------------------
         * PATH：        标识软件问题发生所需的 conditional 分支和决定。
         * 示例：Condition !p, taking false branch
         * Related lines 107-108 of sample code: 107 if (!p) 108 return NO_MEM;
         * -----------------------------------------------------------------------------------------------------
         * MULTI：       提供支持检查器发现的软件问题的源代码中的证据。也称为证据事件。
         * -----------------------------------------------------------------------------------------------------
         * NORMAL：      引用被标识为检查器发现的软件问题的引起因素的代码行。
         * 示例：
         * 1. alloc_fn: Storage is returned from allocation function malloc.
         * 2. var_assign: Assigning: p = storage returned from malloc(12U)
         * Related line 5 of sample code: 5 char *p = malloc(12);
         * -----------------------------------------------------------------------------------------------------
         * REMEDIATION： 提供旨在帮助您修复报告的软件问题的补救建议，而不只是报告问题。用在安全缺陷中。
         */
        private String kind;

        /**
         * 关联告警跟踪信息
         */
        @JsonProperty("link_trace")
        List<Trace> linkTrace;
    }
}
