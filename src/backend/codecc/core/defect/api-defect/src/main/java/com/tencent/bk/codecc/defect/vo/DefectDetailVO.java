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

package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警基础信息VO
 *
 * @version V1.0
 * @date 2019/10/18
 */
@Data
@ApiModel("告警基础信息视图")
public class DefectDetailVO extends DefectBaseVO
{
    @ApiModelProperty(value = "CWE(Common Weakness Enumeration),通用缺陷对照表")
    private Integer cwe;

    @ApiModelProperty(value = "第三方平台的buildId")
    private String platformBuildId;

    @ApiModelProperty(value = "第三方平台的项目ID")
    private String platformProjectId;

    @ApiModelProperty(value = "有关告警实例的数据。可以有多个告警实例。")
    private List<DefectInstance> defectInstances;

    @ApiModelProperty(value = "告警涉及的相关文件信息。可以有多个。")
    private Map<String, FileInfo> fileInfoMap = new HashMap<>();

    /**
     * 告警实例的数据
     */
    @Data
    @ApiModel("告警实例的数据")
    public static class DefectInstance
    {
        /**
         * 有关引起告警的跟踪数据。可以有多个跟踪数据。
         */
        @ApiModelProperty(value = "告警涉及的相关文件信息。可以有多个。", required = true)
        private List<Trace> traces;
    }

    @Data
    @ApiModel("告警跟踪数据视图")
    public static class Trace
    {
        @ApiModelProperty(value = "报错行的描述信息", required = true)
        private String message;

        @ApiModelProperty(value = "文件MD5与文件路径名共同唯一标志一个文件", required = true)
        private String fileMD5;

        @ApiModelProperty(value = "文件路径名", required = true)
        private String filePathname;

        @ApiModelProperty(value = "告警出现（实例）的简短标识符。用在 UI 中")
        private String tag;

        @ApiModelProperty(value = "跟踪时间序号", required = true)
        private int traceNumber;

        @ApiModelProperty(value = "行号", required = true)
        private int lineNumber;

        @ApiModelProperty(value = "是否是告警的主事件，告警也可能不存在主事件", required = true)
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
        @ApiModelProperty(value = "事件类型", required = true)
        private String kind;

        /**
         * 关联告警跟踪信息
         */
        @ApiModelProperty(value = "关联告警跟踪信息", required = true)
        List<Trace> linkTrace;
    }

    @Data
    @ApiModel("告警相关文件信息")
    public static class FileInfo
    {
        @ApiModelProperty(value = "文件路径名", required = true)
        private String filePathname;

        @ApiModelProperty(value = "文件MD5与文件路径名共同唯一标志一个文件", required = true)
        private String fileMD5;

        @ApiModelProperty(value = "文件内容", required = true)
        private String contents;

        @ApiModelProperty(value = "文件的开始行", required = true)
        private int startLine = 1;

        @ApiModelProperty(value = "告警跟踪信息在文件中的最小行")
        private int minDefectLineNum = 1;

        @ApiModelProperty(value = "告警跟踪信息在文件中的最大行")
        private int maxDefectLineNum = 1;
    }

}
