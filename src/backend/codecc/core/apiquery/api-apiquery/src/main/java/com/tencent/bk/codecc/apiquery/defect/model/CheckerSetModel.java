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
import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.util.List;

/**
 * 规则集实体类
 *
 * @version V2.0
 * @date 2020/5/13
 */
@Data
public class CheckerSetModel
{
    /**
     * 规则集ID
     */
    @JsonProperty("checker_set_id")
    private String checkerSetId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 规则集名称
     */
    @JsonProperty("checker_set_name")
    private String checkerSetName;

    /**
     * 规则集支持的语言
     */
    @JsonProperty("code_lang")
    private Long codeLang;

    /**
     * 规则集语言文本,用于查询展示使用
     */
    @JsonProperty("checker_set_lang")
    private String checkerSetLang;

    /**
     * 规则集可见范围1：公开；2：仅我的项目；
     */
    private Integer scope;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 创建时间
     */
    @JsonProperty("create_time")
    private Long createTime;

    /**
     * 最近修改时间
     */
    @JsonProperty("last_update_time")
    private Long lastUpdateTime;

    /**
     * 规则数
     */
    @JsonProperty("checker_count")
    private Integer checkerCount;


    /**
     * 规则集包含的规则和参数
     */
    @JsonProperty("checker_props")
    private List<CheckerPropsModel> checkerProps;

    /**
     * 规则集被任务使用的量
     */
    @JsonProperty("task_usage")
    private Integer taskUsage;

    /**
     * 是否启用1：启用；2：下架
     */
    private Integer enable;

    /**
     * 排序权重
     */
    @JsonProperty("sort_weight")
    private Integer sortWeight;

    /**
     * 项目ID
     */
    @JsonProperty("project_id")
    private String projectId;

    /**
     * 规则集描述
     */
    private String description;

    /**
     * 规则类型
     */
    private List<CheckerSetCategoryModel> catagories;

    /**
     * 基准规则集ID
     */
    @JsonProperty("base_checker_set_id")
    private String baseCheckerSetId;

    /**
     * 基准规则集版本号
     */
    @JsonProperty("base_checker_set_version")
    private Integer baseCheckerSetVersion;

    /**
     * 是否已初始化规则列表
     */
    @JsonProperty("init_checkers")
    private Boolean initCheckers;

    /**
     * 是否官方
     */
    private Integer official;

    /**
     * 是否是V2版本规则集，V2版本规则集只用于旧版本流水线插件
     */
    private Boolean legacy;

    /**
     * 来源标签
     */
    @JsonProperty("checker_set_source")
    private String checkerSetSource;

    @Transient
    private Boolean defaultCheckerSet;

    /*
     * --------------已废弃-----------使用中的任务ID列表
     */
//    @JsonProperty("tasks_in_use")
//    @Deprecated
//    private List<Long> tasksInUse;

    /*
     * --------------已废弃-----------工具特殊参数
     */
//    @JsonProperty("param_json")
//    @Deprecated
//    private String paramJson;

    /*
     * --------------已废弃-----------工具名称
     */
//    @JsonProperty("tool_name")
//    @Deprecated
//    private String toolName;

    /*
       --------------已废弃----------- 是否推荐
     */
//    @Deprecated
//    private Integer recommended;
}
