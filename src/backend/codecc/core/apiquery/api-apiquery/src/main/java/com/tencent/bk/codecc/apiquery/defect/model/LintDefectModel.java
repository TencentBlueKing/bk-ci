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

package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * lint类告警持久实体类
 *
 * @version V1.0
 * @date 2019/5/9
 */
@Data
public class LintDefectModel extends CommonModel
{
    @JsonProperty("task_id")
    private Long taskId;

    /**
     * 工具名称
     */
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 相对路径，相对路径的MD5是文件的唯一标志，是除去文件在服务器上存在的根目录后的路径
     * rel_path，file_path，url三者的区别：
     * rel_path: src/crypto/block.go,
     * file_path: /data/iegci/multi_tool_code_resource_5/maoyan0417001_dupc/src/crypto/block.go,
     * url: http://svn.xxx.com/codecc/test_project_proj/branches/test/Go/go-master/src/crypto/block.go,
     */
    @JsonProperty("rel_path")
    private String relPath;

    /**
     * 代码下载到服务器上的存放路径
     */
    @JsonProperty("file_path")
    private String filePath;


    @JsonProperty("defect_id")
    private String defectId;

    /**
     * 告警行号
     */
    @JsonProperty("line_num")
    private Integer lineNum;

    /**
     * 告警作者
     */
    private String author;

    /**
     * 告警规则
     */
    private String checker;


    /**
     * 严重程度
     */
    private Integer severity;

    /**
     * 告警描述
     */
    private String message;

    /**
     * 告警类型：新告警NEW(1)，历史告警HISTORY(2)
     */
    @JsonProperty("defect_type")
    private Integer defectType;

    /**
     * 告警状态：NEW(1), FIXED(2), IGNORE(4), PATH_MASK(8), CHECKER_MASK(16);
     */
    private Integer status;

    /**
     * 告警行的变更时间，用于跟工具接入时间对比，早于接入时间则判断告警是历史告警，晚于等于接入时间则为新告警
     */
    @JsonProperty("linenum_datetime")
    private Long lineUpdateTime;

    /**
     * 最后更新日期
     */
    @JsonProperty("line_update_date")
    private LocalDate lineUpdateDate;

    @JsonProperty("pinpoint_hash")
    private String pinpointHash;


    /**
     * 告警创建时间
     */
    @JsonProperty("create_time")
    private Long createTime;

    /**
     * 告警修复时间
     */
    @JsonProperty("fixed_time")
    private Long fixedTime;

    /**
     * 告警忽略时间
     */
    @JsonProperty("ignore_time")
    private Long ignoreTime;

    /**
     * 告警忽略原因类型
     */
    @JsonProperty("ignore_reason_type")
    private Integer ignoreReasonType;

    /**
     * 告警忽略原因
     */
    @JsonProperty("ignore_reason")
    private String ignoreReason;

    /**
     * 告警忽略操作人
     */
    @JsonProperty("ignore_author")
    private String ignoreAuthor;

    /**
     * 告警屏蔽时间
     */
    @JsonProperty("exclude_time")
    private Long excludeTime;

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
     * 修复时的版本号
     */
    @JsonProperty("fixed_revision")
    private String fixedRevision;

    /**
     * 修复的代码库id
     */
    @JsonProperty("fixed_repo_id")
    private String fixedRepoId;

    /**
     * 修复的分支
     */
    @JsonProperty("fixed_branch")
    private String fixedBranch;


    /**
     * 代码仓库id
     */
    @JsonProperty("repo_id")
    private String repoId;

    /**
     * 版本号
     */
    private String revision;

    /**
     * 分支名称
     */
    private String branch;

    /**
     * 代码库子模块
     */
    @JsonProperty("sub_module")
    private String subModule;


    //临时保存字段，不存数据库

    private String fileMd5;

    private Boolean newDefect;

    private String fileRevision;

    private String fileBranch;

    private String fileRepoId;
}
