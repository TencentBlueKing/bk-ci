package com.tencent.devops.turbo.model

import com.tencent.devops.turbo.model.pojo.DisplayFieldEntity
import com.tencent.devops.turbo.model.pojo.ParamConfigEntity
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime

@Document(collection = "t_turbo_model_config_entity")
data class TTurboEngineConfigEntity(
    @Id
    var id: String? = null,
    // 优先级数字
    @Field("priority_num")
    var priorityNum: Int? = null,
    // 编译加速模型代码
    @Indexed(background = true)
    @Field("engine_code")
    var engineCode: String,
    // 编译加速模型名字
    @Field("engine_name")
    var engineName: String,
    // 编译加速模型描述
    @Field("desc")
    var desc: String,
    // 编译加速spel表达式用于计算编译节约时间
    @Field("spel_expression")
    var spelExpression: String,
    // spel表达式参数数组
    @Field("spel_param_list")
    var spelParamMap: Map<String, Any?>,
    // 参数配置
    @Field("param_config")
    var paramConfig: List<ParamConfigEntity>?,
    // 创建编译加速记录的cron表达式
    @Field("create_cron_expression")
    var createCronExpression: String? = null,
    // 更新编译加速记录的cron表达式
    @Field("update_cron_expression")
    var updateCronExpression: String? = null,
    // 是否有效
    @Field("enabled")
    @Indexed(background = true)
    var enabled: Boolean = true,
    // 是否推荐
    @Field("recommend")
    @Indexed(background = true)
    var recommend: Boolean? = false,
    // 推荐理由
    @Field("recommend_reason")
    var recommendReason: String? = null,
    // 插件提示
    @Field("plugin_tips")
    var pluginTips: String? = null,

    // 用户手册
    @Field("user_manual")
    var userManual: String? = null,
    // 文档链接指引
    @Field("doc_url")
    var docUrl: String? = null,
    // 显示字段名
    @Field("display_fields")
    var displayFields: List<DisplayFieldEntity>? = null,

    @Field("updated_by")
    var updatedBy: String,
    @Field("updated_date")
    var updatedDate: LocalDateTime,
    @Field("created_by")
    var createdBy: String,
    @Field("created_date")
    var createdDate: LocalDateTime

)
