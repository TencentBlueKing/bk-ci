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

package com.tencent.bk.codecc.defect.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Set;

/**
 * lint类工具文件持久化实体类
 *
 * @version V1.0
 * @date 2019/5/9
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_lint_defect")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1", def = "{'task_id': 1, 'tool_name': 1}")
})
public class LintFileEntity extends CommonEntity
{

    /**
     * 任务id
     */
    @Field("task_id")
    private long taskId;

    /**
     * 工具名称
     */
    @Field("tool_name")
    private String toolName;

    /**
     * 相对路径，相对路径的MD5是文件的唯一标志，是除去文件在服务器上存在的根目录后的路径
     * rel_path，file_path，url三者的区别：
     * rel_path: src/crypto/block.go,
     * file_path: /data/iegci/multi_tool_code_resource_5/maoyan0417001_dupc/src/crypto/block.go,
     * url: http://svn.xxx.com/codecc/test_project_proj/branches/test/Go/go-master/src/crypto/block.go,
     */
    @Field("rel_path")
    private String relPath;

    /**
     * 代码下载到服务器上的存放路径
     */
    @Field("file_path")
    @JSONField(name = "file")
    private String filePath;

    /**
     * 代码库路径
     */
    private String url;

    /**
     * 文件的最近修改时间
     */
    @Field("file_update_time")
    private long fileUpdateTime;

    /**
     * 发现该告警的最近分析版本号，项目工具每次分析都有一个版本，用于区分一个方法是哪个版本扫描出来的，根据版本号来判断是否修复
     */
    @Field("analysis_version")
    private String analysisVersion;

    /**
     * 状态：NEW(1), PATH_MASK(8)
     */
    private int status;

    /**
     * 第一次检查出告警的时间
     */
    @Field("create_time")
    private long createTime;

    /**
     * 文件被修复的时间
     */
    @Field("fixed_time")
    private long fixedTime;

    /**
     * 告警被修复的时间
     */
    @Field("exclude_time")
    private long excludeTime;

    /**
     * 本文件的告警总数，等于defectList.size()，方便用于统计
     */
    @Field("defect_count")
    private int defectCount;

    /**
     * 本文件的新告警数，方便用于统计
     */
    @Field("new_count")
    @Deprecated
    private int newCount;

    /**
     * 本文件的历史告警数，方便用于统计
     */
    @Field("history_count")
    @Deprecated
    private int historyCount;

    /**
     * 代码仓库id
     */
    @Field("repo_id")
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
    @Field("sub_module")
    private String subModule;

    /**
     * 作者清单
     */
    @Field("author_list")
    private Set<String> authorList;

    /**
     * 规则清单
     */
    @Field("checker_list")
    private Set<String> checkerList;

    /**
     * 严重程度列表
     */
    @Transient
    private Set<Integer> severityList;

    /**
     * 文件所有告警的严重程度之和，用于排序
     */
    @Transient
    private int severity;

    /**
     * 文件的md5值
     */
    @Field("md5")
    private String md5;

    /**
     * 告警清单
     */
    @Field("defect_list")
    @JSONField(name = "defects")
    private List<LintDefectEntity> defectList;

    /**
     * 告警数超过10000的文件收敛为只记录告警总数
     */
    @Transient
    private FileDefectGatherEntity gather;
}
