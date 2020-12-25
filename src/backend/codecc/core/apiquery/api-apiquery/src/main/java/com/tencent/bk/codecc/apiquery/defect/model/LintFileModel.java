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

import java.util.List;
import java.util.Set;

/**
 * lint类工具文件持久化实体类
 *
 * @version V1.0
 * @date 2019/5/9
 */
@Data
public class LintFileModel extends CommonModel
{
    public final static String classType = "LintFileModel";
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

    /**
     * 代码库路径
     */
    private String url;

    /**
     * 文件的最近修改时间
     */
    @JsonProperty("file_update_time")
    private long fileUpdateTime;

    /**
     * 发现该告警的最近分析版本号，项目工具每次分析都有一个版本，用于区分一个方法是哪个版本扫描出来的，根据版本号来判断是否修复
     */
    @JsonProperty("analysis_version")
    private String analysisVersion;

    /**
     * 状态：NEW(1), PATH_MASK(8)
     */
    private int status;

    /**
     * 第一次检查出告警的时间
     */
    @JsonProperty("create_time")
    private long createTime;

    /**
     * 文件被修复的时间
     */
    @JsonProperty("fixed_time")
    private long fixedTime;

    /**
     * 告警被修复的时间
     */
    @JsonProperty("exclude_time")
    private long excludeTime;

    /**
     * 本文件的告警总数，等于defectList.size()，方便用于统计
     */
    @JsonProperty("defect_count")
    private int defectCount;

    /**
     * 本文件的新告警数，方便用于统计
     */
    @JsonProperty("new_count")
    private int newCount;

    /**
     * 本文件的历史告警数，方便用于统计
     */
    @JsonProperty("history_count")
    private int historyCount;

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
     * 作者清单
     */
    @JsonProperty("author_list")
    private Set<String> authorList;

    /**
     * 规则清单
     */
    @JsonProperty("checker_list")
    private Set<String> checkerList;

    /**
     * 严重程度列表
     */
    private Set<Integer> severityList;

    /**
     * 文件所有告警的严重程度之和，用于排序
     */
    private int severity;

    /**
     * 文件的md5值
     */
    @JsonProperty("md5")
    private String md5;

    /**
     * 告警清单
     */
    @JsonProperty("defect_list")
    private List<LintDefectModel> defectList;
}
