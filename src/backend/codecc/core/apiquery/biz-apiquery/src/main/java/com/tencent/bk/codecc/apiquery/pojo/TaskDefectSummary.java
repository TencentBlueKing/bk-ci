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

package com.tencent.bk.codecc.apiquery.pojo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 按维度统计告警数据生成excel
 *
 * @version V1.0
 * @date 2021/3/24
 */

@Data
public class TaskDefectSummary {

    @ExcelProperty("BG")
    private String bgName;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("中心")
    private String centerName;

    @ExcelProperty("蓝盾项目ID")
    private String projectId;

    @ExcelProperty("任务ID")
    private Long taskId;

    @ExcelProperty("任务名称")
    private String nameCn;

    @ExcelProperty("代码仓库")
    private String repoUrl;

    @ExcelProperty("分支")
    private String branch;

    @ExcelProperty("任务语言")
    private String codeLang;

    @ExcelProperty("质量星级")
    private String qualityStar;

    @ExcelProperty("综合评分")
    private String rdIndicatorsScore;

    @ExcelProperty("最近分析状态")
    private String analyzeDate;

    @ExcelProperty(value = {"代码缺陷", "遗留总数"})
    private Integer defectExistTotalCount;

    @ExcelProperty(value = {"代码缺陷", "遗留严重数"})
    private Integer defectExistSeriousCount;

    @ExcelProperty(value = {"代码缺陷", "新增数"})
    private Integer defectNewCount;

    @ExcelProperty(value = {"代码缺陷", "修复数"})
    private Integer defectFixedCount;

    @ExcelProperty(value = {"代码缺陷", "屏蔽数"})
    private Integer defectExcludedCount;

    @ExcelProperty(value = {"代码缺陷", "已接工具数"})
    private int defectToolNum;

    @ExcelProperty(value = {"安全漏洞", "遗留总数"})
    private Integer securityExistTotalCount;

    @ExcelProperty(value = {"安全漏洞", "遗留严重数"})
    private Integer securityExistSeriousCount;

    @ExcelProperty(value = {"安全漏洞", "新增数"})
    private Integer securityNewCount;

    @ExcelProperty(value = {"安全漏洞", "修复数"})
    private Integer securityFixedCount;

    @ExcelProperty(value = {"安全漏洞", "屏蔽数"})
    private Integer securityExcludedCount;

    @ExcelProperty(value = {"安全漏洞", "已接工具数"})
    private int securityToolNum;

    @ExcelProperty(value = {"代码规范", "遗留问题数"})
    private Integer standardExistTotalCount;

    @ExcelProperty(value = {"代码规范", "千行问题数"})
    private String standardAverageThousandDefect;

    @ExcelProperty(value = {"代码规范", "新增数"})
    private Integer standardNewCount;

    @ExcelProperty(value = {"代码规范", "修复数"})
    private Integer standardFixedCount;

    @ExcelProperty(value = {"代码规范", "已接工具数"})
    private int standardToolNum;

    @ExcelProperty(value = {"圈复杂度", "风险函数个数"})
    private Integer ccnExistTotalCount;

    @ExcelProperty(value = {"圈复杂度", "千行超标复杂度"})
    private String ccnAverageThousandDefect;

    @ExcelProperty(value = {"重复率", "重复文件"})
    private Integer dupExistTotalCount;

    @ExcelProperty(value = {"重复率", "平均重复率"})
    private String dupRate;

}
