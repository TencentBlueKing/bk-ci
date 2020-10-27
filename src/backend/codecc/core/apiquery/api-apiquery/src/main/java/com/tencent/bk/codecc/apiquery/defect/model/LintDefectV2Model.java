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

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.bk.codecc.apiquery.utils.EntityIdDeserializer;
import lombok.Data;

/**
 * 拆表后的告警model
 * 
 * @date 2020/7/4
 * @version V1.0
 */
@Data
public class LintDefectV2Model extends CommonModel
{
    @JsonProperty("_id")
    @JsonDeserialize(using = EntityIdDeserializer.class)
    private String defectId;

    @JsonProperty("id")
    private String id;

    /**
     * 任务id
     */
    @JsonProperty("task_id")
    private long taskId;

    /**
     * 工具名称
     */
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 文件名(不包含路径)
     */
    @JsonProperty("file_name")
    private String fileName;

    /**
     * 告警行号
     */
    @JsonProperty("line_num")
    private int lineNum;

    /**
     * 告警作者
     */
    @JsonProperty("author")
    private String author;

    /**
     * 告警规则
     */
    @JsonProperty("checker")
    private String checker;

    /**
     * 严重程度
     */
    @JsonProperty("severity")
    private int severity;

    /**
     * 告警描述
     */
    @JsonProperty("message")
    private String message;

    /**
     * 告警类型：新告警NEW(1)，历史告警HISTORY(2)
     */
    @JsonProperty("defect_type")
    private int defectType;

    /**
     * 告警状态：NEW(1), FIXED(2), IGNORE(4), PATH_MASK(8), CHECKER_MASK(16);
     */
    private int status;

    /**
     * 告警行的变更时间，用于跟工具接入时间对比，早于接入时间则判断告警是历史告警，晚于等于接入时间则为新告警
     */
    @JsonProperty("line_update_time")
    private long lineUpdateTime;

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
    @JSONField(name = "file")
    private String filePath;

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
     * 告警屏蔽时间
     */
    @JsonProperty("exclude_time")
    private Long excludeTime;

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
     * 代码库路径
     */
    private String url;

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

    /**
     * 文件的最近修改时间
     */
    @JsonProperty("file_update_time")
    private long fileUpdateTime;

    @JsonProperty("file_md5")
    private String fileMd5;

}
