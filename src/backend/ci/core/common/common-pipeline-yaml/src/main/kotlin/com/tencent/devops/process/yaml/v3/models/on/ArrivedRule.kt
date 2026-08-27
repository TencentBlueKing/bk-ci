package com.tencent.devops.process.yaml.v3.models.on

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 制品到达触发器 YAML 规则（对应 `on.artifact.arrived`）。
 *
 * 制品触发采用统一的「触发器 -> 事件类型」结构：
 * - 单触发器（嵌套形态）：`on.artifact.arrived`
 * - 多触发器（列表形态）：`on[].type = artifact` + `arrived`
 *
 * 单触发器 YAML：
 * ```yaml
 * on:
 *   artifact:
 *     arrived:
 *       name: 制品到达事件触发
 *       repository: pipeline
 *       kind: file
 *       watch-pipeline:
 *         - .ci/plugins/archive.yml
 *       artifacts-name:
 *         - "*.msi"
 *         - "setup-*.exe"
 *       artifacts-name-ignore:
 *         - "*_unsigned.exe"
 *       metadata:
 *         - key: quality-gate
 *           operator: eq
 *           value: passed
 * ```
 *
 * 多触发器 YAML：
 * ```yaml
 * on:
 *   - manual: true
 *   - type: artifact
 *     arrived:
 *       name: MSI 归档触发
 *       repository: pipeline
 *       kind: file
 *       artifacts-name:
 *         - "*.msi"
 * ```
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ArrivedRule(
    override val id: String? = null,
    override val name: String? = null,
    override val enable: Boolean? = true,
    @get:Schema(title = "监听仓库 pipeline/custom/image")
    val repository: String? = null,
    @JsonProperty("watch-pipeline")
    @get:Schema(title = "监听流水线")
    val watchPipeline: List<String>? = null,
    @JsonProperty("watch-root-path")
    @get:Schema(title = "监听根路径")
    val watchRootPath: String? = null,
    @get:Schema(title = "监听范围 file/folder")
    val kind: String? = null,
    @JsonProperty("artifacts-name")
    @get:Schema(title = "匹配名称 Glob")
    val artifactsName: List<String>? = null,
    @JsonProperty("artifacts-name-ignore")
    @get:Schema(title = "排除名称 Glob")
    val artifactsNameIgnore: List<String>? = null,
    @get:Schema(title = "匹配路径 Glob")
    val paths: List<String>? = null,
    @JsonProperty("paths-ignore")
    @get:Schema(title = "排除路径 Glob")
    val pathsIgnore: List<String>? = null,
    @get:Schema(title = "镜像名")
    val image: String? = null,
    @get:Schema(title = "匹配 Tag Glob")
    val tags: List<String>? = null,
    @JsonProperty("tags-ignore")
    @get:Schema(title = "排除 Tag Glob")
    val tagsIgnore: List<String>? = null,
    @get:Schema(title = "元数据过滤")
    val metadata: List<ArrivedMetadata>? = null
) : Rule(id, name, enable)

/**
 * 制品元数据过滤条件。运算符在 YAML 中使用小写（eq/ne/contains/exists/not_exists），转换为 Element 时统一大写。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ArrivedMetadata(
    @get:Schema(title = "元数据键")
    val key: String,
    @get:Schema(title = "运算符 eq/ne/contains/exists/not_exists")
    val operator: String = "eq",
    @get:Schema(title = "元数据值，exists/not_exists 可省略")
    val value: String = ""
)
