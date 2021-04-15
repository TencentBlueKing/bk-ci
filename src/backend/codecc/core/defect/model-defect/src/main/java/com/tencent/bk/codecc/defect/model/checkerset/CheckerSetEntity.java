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

package com.tencent.bk.codecc.defect.model.checkerset;

import com.tencent.codecc.common.db.CommonEntity;
import com.tencent.devops.common.constant.ComConstants;
import lombok.Data;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

/**
 * 规则集实体类
 *
 * @version V4.0
 * @date 2019/10/29
 */
@Data
@Document(collection = "t_checker_set")
@CompoundIndexes({
        @CompoundIndex(name = "checker_set_id_1_version_1", def = "{'checker_set_id': 1, 'version': 1}")
})
public class CheckerSetEntity extends CommonEntity
{
    /**
     * 规则集ID
     */
    @Field("checker_set_id")
    @Indexed
    private String checkerSetId;

    /**
     * 版本号
     */
    @Indexed(background = true)
    private Integer version;

    /**
     * 规则集名称
     */
    @Field("checker_set_name")
    @Indexed
    private String checkerSetName;

    /**
     * 规则集支持的语言
     */
    @Field("code_lang")
    private Long codeLang;

    /**
     * 规则集语言文本,用于查询展示使用
     */
    @Field("checker_set_lang")
    @Indexed
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
    @Field("create_time")
    private Long createTime;

    /**
     * 最近修改时间
     */
    @Field("last_update_time")
    private Long lastUpdateTime;

    /**
     * 规则数
     */
    @Field("checker_count")
    private Integer checkerCount;


    /**
     * 规则集包含的规则和参数
     */
    @Field("checker_props")
    @Indexed
    private List<CheckerPropsEntity> checkerProps;

    /**
     * 规则集被任务使用的量
     */
    @Field("task_usage")
    @Indexed
    private Integer taskUsage;

    /**
     * 是否启用1：启用；2：下架
     */
    private Integer enable;

    /**
     * 排序权重
     */
    @Field("sort_weight")
    private Integer sortWeight;

    /**
     * 项目ID
     */
    @Field("project_id")
    private String projectId;

    /**
     * 规则集描述
     */
    private String description;

    /**
     * 规则类型
     */
    @Indexed
    private List<CheckerSetCatagoryEntity> catagories;

    /**
     * 基准规则集ID
     */
    @Field("base_checker_set_id")
    private String baseCheckerSetId;

    /**
     * 基准规则集版本号
     */
    @Field("base_checker_set_version")
    private Integer baseCheckerSetVersion;

    /**
     * 是否已初始化规则列表
     */
    @Field("init_checkers")
    private Boolean initCheckers;

    /**
     * 是否官方
     */
    private Integer official;

    /**
     * 是否是V2版本规则集，V2版本规则集只用于旧版本流水线插件
     */
    @Indexed
    private Boolean legacy;

    /**
     * 来源标签
     */
    @Field("checker_set_source")
    @Indexed
    private String checkerSetSource;

    @Transient
    private Boolean defaultCheckerSet;

    /**
     * --------------已废弃-----------使用中的任务ID列表
     */
    @Field("tasks_in_use")
    @Deprecated
    private List<Long> tasksInUse;

    /**
     * --------------已废弃-----------工具特殊参数
     */
    @Field("param_json")
    @Deprecated
    private String paramJson;

    /**
     * --------------已废弃-----------工具名称
     */
    @Field("tool_name")
    @Deprecated
    private String toolName;

    /**
     *  --------------已废弃----------- 是否推荐
     */
    @Deprecated
    private Integer recommended;
}
