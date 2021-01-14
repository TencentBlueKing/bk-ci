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
import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 重复率的缺陷实体
 *
 * @version V1.0
 * @date 2019/5/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_dupc_defect")
public class DUPCDefectEntity extends CommonEntity
{

    /**
     * 任务id
     */
    @Field("task_id")
    private Long taskId;

    /**
     * 工具名称
     */
    @Field("tool_name")
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 相对路径，是文件的唯一标志，是除去文件在服务器上存在的根目录后的路径
     * rel_path，file_path，url三者的区别：
     * rel_path: src/crypto/block.go,
     * file_path: /data/iegci/multi_tool_code_resource_5/maoyan0417001_dupc/src/crypto/block.go,
     * url: http://svn.xxx.com/codecc/test_project_proj/branches/test/Go/go-master/src/crypto/block.go,
     */
    @Field("rel_path")
    @JsonProperty("rel_path")
    private String relPath;

    /**
     * 代码仓库地址
     */
    private String url;

    /**
     * 文件路径
     */
    @Field("file_path")
    @JsonProperty("file_path")
    private String filePath;

    /**
     * 总行数
     */
    @Field("total_lines")
    @JsonProperty("total_lines")
    private long totalLines;

    /**
     * 重复行数
     */
    @Field("dup_lines")
    @JsonProperty("dup_lines")
    private long dupLines;

    /**
     * 重复率
     */
    @Field("dup_rate")
    @JsonProperty("dup_rate")
    private String dupRate;

    /**
     * 重复率数值
     */
    @Field("dup_rate_value")
    @JsonProperty("dup_rate_value")
    private Float dupRateValue;

    /**
     * 块数
     */
    @Field("block_num")
    @JsonProperty("block_num")
    private int blockNum;

    /**
     * 作者列表
     */
    @Field("author_list")
    @JsonProperty("author_list")
    private String authorList;

    /**
     * 文件的最新更新时间
     */
    @Field("file_change_time")
    @JsonProperty("file_change_time")
    private Long fileChangeTime;

    /**
     * 告警方法的状态：new，closed，excluded，ignore
     */
    private Integer status;

    /**
     * 风险系数，极高-1, 高-2，中-4，低-8
     * 该参数不入库，因为风险系数是可配置的
     */
    @Field("risk_factor")
    @Transient
    private int riskFactor;

    /**
     * 告警创建时间
     */
    @Field("create_time")
    private Long createTime;

    /**
     * 告警修复时间
     */
    @Field("fixed_time")
    @JsonProperty("fixed_time")
    private Long fixedTime;

    /**
     * 告警忽略时间
     */
    @Field("ignore_time")
    @JsonProperty("ignore_time")
    private Long ignoreTime;

    /**
     * 告警屏蔽时间
     */
    @Field("exclude_time")
    @JsonProperty("exclude_time")
    private Long excludeTime;

    /**
     * 这条缺陷数据的最后更新时间
     */
    @Field("last_update_time")
    @JsonProperty("last_update_time")
    private Long lastUpdateTime;

    /**
     * 文件名
     */
    @Field("file_name")
    @Transient
    private String fileName;

    /**
     * 仓库id
     */
    @Field("repo_id")
    @JsonProperty("repo_id")
    private String repoId;

    /**
     * 文件版本号
     */
    private String revision;

    /**
     * 分支名
     */
    private String branch;

    /**
     * Git子模块
     */
    @Field("sub_module")
    @JsonProperty("sub_module")
    private String subModule;

    /**
     * 发现该告警的最近分析版本号，项目工具每次分析都有一个版本，用于区分一个方法是哪个版本扫描出来的，根据版本号来判断是否修复，格式：
     * ANALYSIS_VERSION:projId:toolName
     */
    @Field("analysis_version")
    @JsonProperty("analysis_version")
    private String analysisVersion;

    /**
     * 重复代码块列表
     */
    @Field("block_list")
    @JsonProperty("block_list")
    private List<CodeBlockEntity> blockList;
}
