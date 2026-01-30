package com.tencent.devops.common.pipeline.utils

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.VarRefDetail
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import java.util.Collections
import java.util.Date
import java.util.IdentityHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

/**
 * 模型变量引用解析工具类
 *
 * 两种模式（由 filterByTriggerParams 控制）：
 * - filterByTriggerParams = true（默认）：仅提取在 triggerContainer.params 中出现的变量；
 *   仍能识别上述四种形式，且每条 VarRefDetail 的 isDoubleBrace 区分单/双大括号。
 * - filterByTriggerParams = false：提取模型中所有 ${xxx}、${{xxx}} 花括号内内容，原封不动（仅允许去掉花括号内首尾空格），并区分单/双大括号。
 * - filterByTriggerParams = true：提取 ${xxx}、${{xxx}}、${variables.xxx}、${{variables.xxx}} 的 xxx（去掉空格和 variables. 前缀），并区分单/双大括号。
 *
 * 主要功能：递归解析 List/Map/Stage/Container/Element 等，循环引用检测，字段缓存，支持流水线/模板资源。
 */
object ModelVarRefUtils {

    private val logger = LoggerFactory.getLogger(ModelVarRefUtils::class.java)

    /** 双大括号：${{xxx}}、${{variables.xxx}}，group(1) 为 xxx（用于 filterByTriggerParams=true 时与 param 匹配） */
    private val DOUBLE_BRACE_PATTERN = Pattern.compile("\\$\\{\\{\\s*(?:variables\\.)?([^}\\s]+)\\s*}}")
    /** 单大括号：${xxx}、${variables.xxx}，(?!\\{) 避免与 ${{ 冲突，group(1) 为 xxx */
    private val SINGLE_BRACE_PATTERN = Pattern.compile("\\$\\{(?!\\{)\\s*(?:variables\\.)?([^}\\s]+)\\s*}")

    /** 双大括号原样：${{...}}，group(1) 为花括号内完整内容（filterByTriggerParams=false 时用，仅做 trim） */
    private val DOUBLE_BRACE_RAW_PATTERN = Pattern.compile("\\$\\{\\{([^}]*)}}")
    /** 单大括号原样：${...}，group(1) 为花括号内完整内容（filterByTriggerParams=false 时用，仅做 trim） */
    private val SINGLE_BRACE_RAW_PATTERN = Pattern.compile("\\$\\{(?!\\{)([^}]*)}")

    /** customCondition 中纯 variables.xxx（无 $ 包裹），group(2) 为 xxx，仅 extractCustomConditionVariableMatches 使用 */
    private val PLAIN_VARIABLES_PATTERN = Pattern.compile("(?:^|[^\\w.])(variables\\.(\\S+))(?=$|[^\\w.])")

    /** 支持的资源类型常量 - 流水线资源 */
    private const val RESOURCE_TYPE_PIPELINE = "PIPELINE"
    /** 支持的资源类型常量 - 模板资源 */
    private const val RESOURCE_TYPE_TEMPLATE = "TEMPLATE"

    /**
     * 需要跳过的字段名称集合
     */
    private val SKIP_FIELDS = setOf(
        "class", "declaringClass", "signature", "status", "startEpoch",
        "systemElapsed", "elementElapsed", "elapsed", "timeCost", "executeCount",
        "logger", "LOG", "log"
    )

    /**
     * 字段缓存，用于存储类的反射字段列表
     * 避免重复反射获取字段信息，提高解析性能
     * 缓存策略：最大容量500，访问后1小时过期
     */
    private val fieldCache: Cache<Class<*>, List<java.lang.reflect.Field>> = Caffeine.newBuilder()
        .maximumSize(500) // 设置缓存容量上限
        .expireAfterAccess(1, TimeUnit.HOURS) // 设置访问后过期时间
        .build()

    /**
     * 解析模型中的变量引用。
     * @param model 要解析的模型对象
     * @param projectId 项目ID
     * @param filterByTriggerParams true：只返回在 triggerContainer.params 中的变量；花括号内去掉空格和 variables. 前缀得到 xxx，并区分单/双大括号。
     *                             false：返回模型中所有 ${xxx}、${{xxx}}；花括号内内容原封不动，仅去掉首尾空格，并区分单/双大括号。
     * @return 变量引用详情列表
     */
    fun parseModelVarReferences(
        model: Model,
        projectId: String,
        filterByTriggerParams: Boolean = true
    ): List<VarRefDetail> {
        val startTime = System.currentTimeMillis()

        try {
            val (resourceType, resourceId) = determineResourceTypeAndId(model)
            val triggerContainer = model.getTriggerContainer()
            val paramVariables = extractParamVariables(triggerContainer.params)

            // 仅当“按 trigger 参数过滤”且参数为空时提前返回
            if (filterByTriggerParams && paramVariables.isEmpty()) {
                logger.info("No param variables found in trigger container")
                return emptyList()
            }

            val references = mutableListOf<VarRefDetail>()
            val context = ParseContext(
                projectId = projectId,
                resourceType = resourceType,
                resourceId = resourceId,
                paramVariables = paramVariables,
                filterByTriggerParams = filterByTriggerParams
            )

            // 使用高性能的循环引用检测机制，避免无限递归
            val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())

            // 递归遍历所有字段，查找变量引用
            parseObject(
                obj = model,
                context = context,
                references = references,
                path = "model",
                visited = visited
            )

            // 计算并记录方法执行耗时，用于性能监控
            val cost = System.currentTimeMillis() - startTime
            logger.info("parseVarReferences completed in ${cost}ms, found ${references.size} references")

            return references
        } catch (ignored: Throwable) {
            // 异常处理：记录错误日志并返回空列表，确保方法不会抛出异常
            logger.error("parseModelVarReferences failed", ignored)
            return emptyList()
        }
    }

    /**
     * 递归解析对象 - 高性能版本
     * 这是解析过程的核心方法，根据对象类型分发到具体的解析方法
     * 使用when表达式进行类型判断，每种类型都有专门的解析逻辑
     * 处理流程：
     * 1. 安全检查：跳过null对象
     * 2. 循环引用检测：使用IdentityHashMap避免重复解析同一对象
     * 3. 类型分发：根据对象类型调用相应的解析方法
     * 4. 异常处理：记录解析失败信息但不中断流程
     * @param obj 要解析的对象，可以是任意类型
     * @param context 解析上下文，包含项目ID、资源信息等
     * @param references 变量引用收集器，用于存储找到的变量引用
     * @param path 当前对象在模型中的路径，用于定位变量引用位置
     * @param visited 已访问对象集合，用于循环引用检测
     */
    private fun parseObject(
        obj: Any?,
        context: ParseContext,
        references: MutableList<VarRefDetail>,
        path: String,
        visited: MutableSet<Any>
    ) {
        // 安全检查：跳过null对象，避免空指针异常
        if (obj == null) return

        // 防止循环引用 - 使用IdentityHashMap确保对象身份比较
        if (obj in visited) {
            return
        }
        visited.add(obj)

        try {
            // 根据对象类型分发到具体的解析方法
            when (obj) {
                is String -> parseString(str = obj, context = context, references = references, path = path)
                is List<*> -> parseList(
                    list = obj, context = context, references = references, basePath = path, visited = visited
                )

                is Set<*> -> parseSet(
                    set = obj, context = context, references = references, basePath = path, visited = visited
                )

                is Map<*, *> -> parseMap(
                    map = obj, context = context, references = references, basePath = path, visited = visited
                )

                is Stage -> parseStage(
                    stage = obj, context = context, references = references, basePath = path, visited = visited
                )

                is Container -> parseContainer(
                    container = obj, context = context, references = references, basePath = path, visited = visited
                )

                is Element -> parseElement(
                    element = obj, context = context, references = references, basePath = path, visited = visited
                )

                is StageControlOption, is JobControlOption, is ElementAdditionalOptions -> parseControlOption(
                    option = obj, context = context, references = references, path = path, visited = visited
                )

                else -> parseGenericObject(
                    obj = obj, context = context, references = references, basePath = path, visited = visited
                )
            }
        } catch (ignored: Throwable) {
            // 异常处理：记录解析失败信息但不中断流程
            logger.warn("Parse object failed at path: $path, type: ${obj.javaClass.simpleName}, " +
                    "error: ${ignored.message}")
        }
    }

    /**
     * 字符串解析：提取 ${xxx}、${{xxx}} 中的内容，并区分单/双大括号（isDoubleBrace）。
     * filterByTriggerParams=true：仅保留在 paramVariables 中的变量，花括号内去掉空格和 variables. 前缀得到 xxx；
     * filterByTriggerParams=false：原封不动提取花括号内内容，仅去掉首尾空格（不做其他修改）。
     */
    private fun parseString(
        str: String,
        context: ParseContext,
        references: MutableList<VarRefDetail>,
        path: String
    ) {
        if (!str.contains('$') || !str.contains('{')) return

        val rawExtract = !context.filterByTriggerParams
        val matches = extractVariableMatchesFromString(str, rawExtract = rawExtract)
        if (matches.isEmpty()) return

        val toAdd = if (context.filterByTriggerParams) {
            matches.filter { context.paramVariables.contains(it.varName) }
        } else {
            matches
        }
        toAdd.forEach { m ->
            references.add(
                VarRefDetail(
                    projectId = context.projectId,
                    varName = m.varName,
                    resourceId = context.resourceId,
                    resourceType = context.resourceType,
                    stageId = context.stageId,
                    containerId = context.containerId,
                    taskId = context.taskId,
                    taskName = context.taskName,
                    positionPath = path,
                    isDoubleBrace = m.isDoubleBrace
                )
            )
        }
    }

    /** 变量匹配结果：变量名 + 是否双大括号 ${{xxx}} */
    private data class VarMatch(val varName: String, val isDoubleBrace: Boolean)

    /**
     * 从字符串中提取 ${xxx}、${{xxx}} 花括号内的内容，并区分单/双大括号。
     * @param rawExtract false（默认，filterByTriggerParams=true）：去掉花括号内空格和 variables. 前缀，得到 xxx；
     *                   true（filterByTriggerParams=false）：原封不动提取，仅去掉花括号内首尾空格。
     */
    private fun extractVariableMatchesFromString(text: String, rawExtract: Boolean = false): List<VarMatch> {
        if (!text.contains('$')) return emptyList()
        val cleanedText = removeCommentLines(text)
        if (cleanedText.isEmpty()) return emptyList()

        val result = mutableListOf<VarMatch>()
        if (rawExtract) {
            var matcher = DOUBLE_BRACE_RAW_PATTERN.matcher(cleanedText)
            while (matcher.find()) {
                matcher.group(1)?.let { content ->
                    result.add(VarMatch(varName = content.trim(), isDoubleBrace = true))
                }
            }
            matcher = SINGLE_BRACE_RAW_PATTERN.matcher(cleanedText)
            while (matcher.find()) {
                matcher.group(1)?.let { content ->
                    result.add(VarMatch(varName = content.trim(), isDoubleBrace = false))
                }
            }
        } else {
            var matcher = DOUBLE_BRACE_PATTERN.matcher(cleanedText)
            while (matcher.find()) {
                matcher.group(1)?.trim()?.takeIf { it.isNotBlank() }?.let {
                    result.add(VarMatch(varName = it, isDoubleBrace = true))
                }
            }
            matcher = SINGLE_BRACE_PATTERN.matcher(cleanedText)
            while (matcher.find()) {
                matcher.group(1)?.trim()?.takeIf { it.isNotBlank() }?.let {
                    result.add(VarMatch(varName = it, isDoubleBrace = false))
                }
            }
        }
        return result
    }

    /**
     * 优化列表解析
     * 处理List集合类型的对象，递归解析列表中的每个元素
     * 例如：如果basePath是"model.stages"，列表索引为0，则新路径为"model.stages[0]"
     * @param list 要解析的List集合
     * @param context 解析上下文
     * @param references 变量引用收集器
     * @param basePath 列表在模型中的基础路径
     * @param visited 已访问对象集合，用于循环引用检测
     */
    private fun parseList(
        list: List<*>,
        context: ParseContext,
        references: MutableList<VarRefDetail>,
        basePath: String,
        visited: MutableSet<Any>
    ) {
        // 遍历列表中的每个元素，使用索引构建新的路径
        list.forEachIndexed { index, item ->
            parseObject(
                obj = item,
                context = context,
                references = references,
                path = "$basePath[$index]",
                visited = visited
            )
        }
    }

    /**
     * 解析Set集合中的每个元素
     * 处理Set集合类型的对象，递归解析集合中的每个元素
     * Set是无序集合，但为了路径一致性，仍然使用索引构建路径
     * @param set 要解析的Set集合
     * @param context 解析上下文
     * @param references 变量引用收集器
     * @param basePath 集合在模型中的基础路径
     * @param visited 已访问对象集合，用于循环引用检测
     */
    private fun parseSet(
        set: Set<*>,
        context: ParseContext,
        references: MutableList<VarRefDetail>,
        basePath: String,
        visited: MutableSet<Any>
    ) {
        // 遍历Set中的每个元素，虽然Set是无序的，但为了路径一致性仍然使用索引
        set.forEachIndexed { index, item ->
            parseObject(
                obj = item,
                context = context,
                references = references,
                path = "$basePath[$index]",
                visited = visited
            )
        }
    }

    /**
     * Map解析
     *
     * 处理Map集合类型的对象，递归解析Map中的每个键值对
     * 根据键的类型构建不同的路径格式：
     *
     * @param map 要解析的Map集合
     * @param context 解析上下文
     * @param references 变量引用收集器
     * @param basePath Map在模型中的基础路径
     * @param visited 已访问对象集合，用于循环引用检测
     */
    private fun parseMap(
        map: Map<*, *>,
        context: ParseContext,
        references: MutableList<VarRefDetail>,
        basePath: String,
        visited: MutableSet<Any>
    ) {
        // 遍历Map中的每个键值对
        map.forEach { (key, value) ->
            // 根据键的类型构建路径：字符串键使用点号，其他键使用方括号
            val keyPath = if (key is String) ".$key" else "[$key]"
            parseObject(
                obj = value,
                context = context,
                references = references,
                path = "$basePath$keyPath",
                visited = visited
            )
        }
    }

    /**
     * Stage解析
     * 专门处理Stage对象的解析，Stage是流水线模型中的重要组件
     * 包含多个Container容器，需要特殊处理
     * 处理流程：
     * 1. 创建包含Stage ID的新上下文
     * 2. 解析Stage的基本属性
     * 3. 递归解析Stage中的所有Container容器
     * @param stage 要解析的Stage对象
     * @param context 解析上下文
     * @param references 变量引用收集器
     * @param basePath Stage在模型中的基础路径
     * @param visited 已访问对象集合，用于循环引用检测
     */
    private fun parseStage(
        stage: Stage,
        context: ParseContext,
        references: MutableList<VarRefDetail>,
        basePath: String,
        visited: MutableSet<Any>
    ) {
        // 创建包含Stage ID的新上下文，为后续解析提供Stage上下文信息
        val stageContext = context.copy(stageId = stage.id ?: "")

        // 先解析Stage的基本属性（名称、描述、控制选项等）
        parseGenericObject(
            obj = stage,
            context = stageContext,
            references = references,
            basePath = basePath,
            visited = visited
        )

        // 解析Stage中的所有Container容器
        // 每个Container都有独立的路径：basePath.containers[index]
        stage.containers.forEachIndexed { index, container ->
            parseObject(
                obj = container,
                context = stageContext,
                references = references,
                path = "$basePath.containers[$index]",
                visited = visited
            )
        }
    }

    /**
     * Container解析
     * 专门处理Container对象的解析，Container是Stage中的执行单元
     * 包含多个Element元素，需要特殊处理
     * 处理流程：
     * 1. 创建包含Container ID的新上下文
     * 2. 解析Container的基本属性
     * 3. 递归解析Container中的所有Element元素
     * @param container 要解析的Container对象
     * @param context 解析上下文
     * @param references 变量引用收集器
     * @param basePath Container在模型中的基础路径
     * @param visited 已访问对象集合，用于循环引用检测
     */
    private fun parseContainer(
        container: Container,
        context: ParseContext,
        references: MutableList<VarRefDetail>,
        basePath: String,
        visited: MutableSet<Any>
    ) {
        // 创建包含Container ID的新上下文，为后续解析提供Container上下文信息
        val containerContext = context.copy(containerId = container.id)

        // 先解析Container的基本属性（名称、类型、配置等）
        parseGenericObject(
            obj = container,
            context = containerContext,
            references = references,
            basePath = basePath,
            visited = visited
        )

        // 解析Container中的所有Element元素
        container.elements.forEachIndexed { index, element ->
            parseObject(
                obj = element,
                context = containerContext,
                references = references,
                path = "$basePath.elements[$index]",
                visited = visited
            )
        }
    }

    /**
     * Element解析
     * 专门处理Element对象的解析，Element是Container中的具体任务单元
     * 通常是具体的构建步骤或插件执行单元
     * 处理流程：
     * 1. 创建包含Element ID的新上下文
     * 2. 调用通用对象解析方法处理Element的所有属性
     * @param element 要解析的Element对象
     * @param context 解析上下文
     * @param references 变量引用收集器
     * @param basePath Element在模型中的基础路径
     * @param visited 已访问对象集合，用于循环引用检测
     */
    private fun parseElement(
        element: Element,
        context: ParseContext,
        references: MutableList<VarRefDetail>,
        basePath: String,
        visited: MutableSet<Any>
    ) {
        // 创建包含 Element ID 与 name 的新上下文，为后续解析提供 Element 上下文信息
        val elementContext = context.copy(taskId = element.id, taskName = element.name)

        // Element通常包含脚本、参数等可能包含变量引用的字段
        parseGenericObject(
            obj = element,
            context = elementContext,
            references = references,
            basePath = basePath,
            visited = visited
        )
    }

    /**
     * 通用的控制选项解析方法 - 适用于所有包含 customVariables 和 customCondition 的对象
     * 功能描述：
     * 该方法负责解析不同类型的控制选项对象（StageControlOption、JobControlOption、ElementAdditionalOptions）
     * 中的变量引用。这些控制选项通常包含自定义变量（customVariables）和自定义条件（customCondition）
     * 等可能包含变量引用的字段。
     * 设计考虑：
     * 1. 使用类型安全的属性访问，避免反射性能开销
     * 2. 支持多种控制选项类型的统一处理
     * 3. 对未知类型提供降级处理机制
     * @param option 控制选项对象，可以是StageControlOption、JobControlOption或ElementAdditionalOptions
     * @param context 解析上下文，包含项目ID、资源信息等
     * @param references 变量引用结果列表，用于收集发现的变量引用
     * @param path 当前解析路径，用于定位变量引用位置
     * @param visited 已访问对象集合，用于循环引用检测
     */
    private fun parseControlOption(
        option: Any,
        context: ParseContext,
        references: MutableList<VarRefDetail>,
        path: String,
        visited: MutableSet<Any>
    ) {
        // 根据具体类型进行安全转换和属性访问
        // 使用when表达式进行类型匹配，每种类型调用相应的内部解析方法
        when (option) {
            is StageControlOption -> parseControlOptionInternal(
                option = option,
                context = context,
                references = references,
                path = path,
                visited = visited,
                customVariables = option.customVariables,
                customCondition = option.customCondition,
                optionType = "StageControlOption"
            )
            is JobControlOption -> parseControlOptionInternal(
                option = option,
                context = context,
                references = references,
                path = path,
                visited = visited,
                customVariables = option.customVariables,
                customCondition = option.customCondition,
                optionType = "JobControlOption"
            )
            is ElementAdditionalOptions -> parseControlOptionInternal(
                option = option,
                context = context,
                references = references,
                path = path,
                visited = visited,
                customVariables = option.customVariables,
                customCondition = option.customCondition,
                optionType = "ElementAdditionalOptions"
            )
            else -> {
                // 对于未知的控制选项类型，记录日志并使用通用解析方法
                logger.info("Unknown control option type: ${option::class.simpleName}")
                parseGenericObject(
                    obj = option,
                    context = context,
                    references = references,
                    basePath = path,
                    visited = visited
                )
            }
        }
    }

    /**
     * 控制选项解析的内部实现 - 处理具体的业务逻辑
     * 功能描述：
     * 该方法负责具体解析控制选项中的变量引用，包括两个主要部分：
     * 1. 解析customVariables中的直接变量定义
     * 2. 解析customCondition中的条件表达式变量
     * @param option 控制选项对象实例
     * @param context 解析上下文，包含项目信息、参数变量列表等
     * @param references 变量引用结果收集列表
     * @param path 当前解析路径，用于定位变量位置
     * @param visited 已访问对象集合，防止循环引用
     * @param customVariables 自定义变量列表，包含键值对形式的变量定义
     * @param customCondition 自定义条件表达式字符串，可能包含变量引用
     * @param optionType 选项类型标识，用于确定上下文信息（Stage/Job/Element）
     */
    private fun parseControlOptionInternal(
        option: Any,
        context: ParseContext,
        references: MutableList<VarRefDetail>,
        path: String,
        visited: MutableSet<Any>,
        customVariables: List<NameAndValue>?,
        customCondition: String?,
        optionType: String
    ) {
        // 处理 customVariables 中的直接变量引用
        customVariables?.forEach { nameAndValue ->
            val varName = nameAndValue.key ?: ""
            val include = !context.filterByTriggerParams || context.paramVariables.contains(varName)
            if (include && varName.isNotBlank()) {
                references.add(
                    createVarRefDetail(
                        context = context,
                        varName = varName,
                        positionPath = "$path.customVariables.$varName",
                        optionType = optionType,
                        isDoubleBrace = false
                    )
                )
            }
        }

        // 处理 customCondition 中的 ${xxx}、${{xxx}}，并区分单/双大括号；filterByTriggerParams=false 时原封不动提取花括号内内容
        customCondition?.let { condition ->
            val rawExtract = !context.filterByTriggerParams
            val matches = extractCustomConditionVariableMatches(condition, rawExtract = rawExtract)
            val toAdd = if (context.filterByTriggerParams) {
                matches.filter { context.paramVariables.contains(it.varName) }
            } else {
                matches
            }
            toAdd.forEach { m ->
                references.add(
                    createVarRefDetail(
                        context = context,
                        varName = m.varName,
                        positionPath = "$path.customCondition",
                        optionType = optionType,
                        isDoubleBrace = m.isDoubleBrace
                    )
                )
            }
        }

        // 递归解析其他字段
        parseGenericObject(obj = option, context = context, references = references, basePath = path, visited = visited)
    }

    /**
     * 创建变量引用详情对象 - 根据选项类型设置不同的上下文信息
     * 功能描述：
     * 该方法负责创建VarRefDetail对象，该对象记录了变量引用的完整上下文信息。
     * 根据不同的选项类型（StageControlOption、JobControlOption、ElementAdditionalOptions），
     * 设置相应的容器ID和任务ID，确保变量引用能够准确定位到具体的作用域。
     * @param context 解析上下文，包含项目、资源、阶段、容器、任务等完整信息
     * @param varName 变量名称，即被引用的参数变量名
     * @param positionPath 变量引用位置路径，描述变量在模型结构中的具体位置
     * @param optionType 选项类型标识，决定如何设置容器ID和任务ID
     * @return VarRefDetail 包含完整上下文信息的变量引用详情对象
     */
    private fun createVarRefDetail(
        context: ParseContext,
        varName: String,
        positionPath: String,
        optionType: String,
        isDoubleBrace: Boolean = false
    ): VarRefDetail {
        return VarRefDetail(
            projectId = context.projectId,
            varName = varName,
            resourceId = context.resourceId,
            resourceType = context.resourceType,
            stageId = context.stageId,
            containerId = if (optionType != "StageControlOption") context.containerId else null,
            taskId = if (optionType == "ElementAdditionalOptions") context.taskId else null,
            taskName = if (optionType == "ElementAdditionalOptions") context.taskName else null,
            positionPath = positionPath,
            isDoubleBrace = isDoubleBrace
        )
    }

    /**
     * 通用对象解析方法
     * 使用反射机制递归解析任意对象的字段，这是解析过程的核心方法
     * 通过反射获取对象的所有字段，然后根据字段值的类型进行相应的处理
     * 性能优化策略：
     * 1. 字段缓存：避免重复反射获取字段信息
     * 2. 类型过滤：跳过简单类型和不相关类型，减少不必要的递归
     * 3. 字段跳过：跳过系统字段和性能监控字段
     * 4. 异常处理：单个字段解析失败不影响整体流程
     * 处理流程：
     * 1. 获取对象的类信息
     * 2. 从缓存中获取字段列表
     * 3. 遍历每个字段，跳过不需要处理的字段
     * 4. 根据字段值的类型进行相应的解析处理
     * 5. 处理过程中捕获异常，确保单个字段失败不影响整体
     * @param obj 要解析的通用对象
     * @param context 解析上下文
     * @param references 变量引用收集器
     * @param basePath 对象在模型中的基础路径
     * @param visited 已访问对象集合，用于循环引用检测
     */
    @Suppress("NestedBlockDepth")
    private fun parseGenericObject(
        obj: Any,
        context: ParseContext,
        references: MutableList<VarRefDetail>,
        basePath: String,
        visited: MutableSet<Any>
    ) {
        try {
            val clazz = obj::class.java

            // 获取缓存的字段列表，避免重复反射
            val fields = getCachedFields(clazz)

            // 遍历对象的所有字段
            for (field in fields) {
                // 跳过不需要处理的字段（系统字段、日志字段等）
                if (shouldSkipField(field.name)) continue

                try {
                    // 设置字段可访问，避免访问权限问题
                    field.isAccessible = true
                    val value = field.get(obj)

                    // 根据字段值的类型进行相应的处理
                    when {
                        value == null -> continue // 跳过null值
                        value is String -> parseString(value, context, references, "$basePath.${field.name}")
                        isSimpleType(value) -> continue // 跳过简单类型（数字、布尔等）
                        else -> {
                            // 只处理我们关心的复杂类型，避免过度递归
                            if (isRelevantComplexType(value)) {
                                parseObject(value, context, references, "$basePath.${field.name}", visited)
                            }
                        }
                    }
                } catch (ignored: Throwable) {
                    // 忽略无法访问的字段，继续处理其他字段
                    continue
                }
            }
        } catch (ignored: Throwable) {
            // 记录解析失败信息但不中断流程
            logger.warn("parse object(${obj::class.simpleName}) failed, path: $basePath, error: ${ignored.message}")
        }
    }

    /**
     * 获取缓存的字段列表
     * 使用缓存机制避免重复反射获取类的字段信息，显著提高解析性能
     * 缓存策略：最大容量500，访问后1小时过期
     * 字段获取逻辑：
     * 1. 从当前类开始，向上遍历继承层次
     * 2. 只获取非静态字段（静态字段通常不包含业务数据）
     * 3. 排除Object类的字段
     * 4. 将字段按声明顺序添加到列表中
     * 缓存机制的优势：
     * - 避免重复反射操作，减少性能开销
     * - 支持并发访问，线程安全
     * - 自动过期机制，避免内存泄漏
     * @param clazz 要获取字段的类
     * @return 类的所有非静态字段列表（包括父类的字段）
     */
    private fun getCachedFields(clazz: Class<*>): List<java.lang.reflect.Field> {
        return fieldCache.get(clazz) {
            val fields = mutableListOf<java.lang.reflect.Field>()
            var currentClass: Class<*>? = clazz

            // 向上遍历继承层次，获取所有父类的字段
            while (currentClass != null && currentClass != Any::class.java) {
                // 只添加非静态字段，静态字段通常不包含业务数据
                currentClass.declaredFields
                    .filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }
                    .forEach { fields.add(it) }
                currentClass = currentClass.superclass
            }
            fields
        }
    }

    /**
     * 判断是否为简单类型（不需要递归解析）
     * 简单类型是指那些不包含嵌套结构的基本数据类型和包装类型
     * 对这些类型进行递归解析没有意义，可以直接跳过以提高性能
     * 判断逻辑：
     * 1. 基本数据类型及其包装类：Number、Boolean、Char等
     * 2. 日期时间类型：Date、Temporal等
     * 3. 枚举类型：Enum
     * 4. 字节数组：ByteArray
     * 5. Java标准库中的简单类型：java.lang、java.math、java.net、java.io等
     * 特殊处理规则：
     * - 集合类型和Map类型虽然是复杂类型，但需要特殊处理，所以不视为简单类型
     * - 反射相关类型（java.lang.reflect）不被视为简单类型
     * - 原始类型（primitive）直接视为简单类型
     * 性能优化作用：
     * - 避免对简单类型进行不必要的递归解析
     * - 减少反射操作次数
     * - 提高整体解析效率
     * @param value 要判断的值
     * @return 如果是简单类型返回true，否则返回false
     */
    private fun isSimpleType(value: Any): Boolean {
        return when (value) {
            is Number, is Boolean, is Char -> true // 基本数据类型
            is Date, is java.time.temporal.Temporal -> true // 日期时间类型
            is Enum<*> -> true // 枚举类型
            is ByteArray -> true // 字节数组
            else -> {
                val className = value.javaClass.name
                // 排除集合类型和Map类型，它们需要被递归解析
                if (value is Collection<*> || value is Map<*, *>) {
                    false
                } else {
                    value.javaClass.isPrimitive || // 原始类型
                            // Java语言包
                            (className.startsWith("java.lang.") &&
                                    !className.startsWith("java.lang.reflect.")) ||
                            className.startsWith("java.math.") || // 数学包
                            className.startsWith("java.net.") || // 网络包
                            className.startsWith("java.io.") // IO包
                }
            }
        }
    }

    /**
     * 判断是否为相关的复杂类型（需要递归解析）
     * 相关复杂类型是指那些可能包含变量引用的业务对象类型
     * 对这些类型需要进行递归解析以查找潜在的变量引用
     * 判断逻辑：
     * 1. 始终处理集合类型和Map类型（List、Set、Map等）
     * 2. 处理com.tencent.devops包下的所有业务对象
     * 3. 处理Kotlin标准库中的相关类型（排除函数类型和基本类型）
     * 业务相关性判断：
     * - com.tencent.devops包下的对象都是业务相关的
     * - Kotlin标准库中的集合类型和数据结构类型是相关的
     * - 函数类型和Lambda表达式不相关，直接跳过
     * - 可空类型包装器不相关，直接跳过
     * 性能优化策略：
     * - 避免对不相关的第三方库对象进行不必要的递归解析
     * - 减少不必要的反射操作
     * - 提高解析的针对性和效率
     * @param value 要判断的值
     * @return 如果是相关复杂类型返回true，否则返回false
     */
    private fun isRelevantComplexType(value: Any): Boolean {
        val className = value.javaClass.name

        // 始终处理集合类型和Map类型，因为它们可能包含业务对象
        if (value is Collection<*> || value is Map<*, *>) {
            return true
        }

        // 处理我们关心的包路径下的对象（业务对象）
        if (className.startsWith("com.tencent.devops.")) {
            return true
        }

        // 处理Kotlin标准库中的大多数类型（排除基本类型和函数类型）
        if (className.startsWith("kotlin.")) {
            return !className.contains("Function") && // 排除函数类型
                    !className.contains("Lambda") && // 排除Lambda表达式
                    !className.endsWith("?") && // 排除可空类型包装器
                    !isKotlinPrimitiveType(className) // 排除Kotlin基本类型
        }

        return false
    }

    /**
     * 判断是否为Kotlin基本类型
     * Kotlin基本类型包括数值类型、布尔类型、字符类型、字符串类型和特殊类型
     * 这些类型不包含嵌套结构，不需要进行递归解析
     * Kotlin基本类型列表：
     * - 数值类型：Int、Long、Double、Float、Byte、Short
     * - 布尔类型：Boolean
     * - 字符类型：Char
     * - 字符串类型：String
     * - 特殊类型：Unit（相当于void）、Nothing（无值类型）
     * 判断逻辑：
     * - 直接比较类的全限定名
     * - 使用精确匹配，避免误判
     * - 只判断Kotlin标准库中的基本类型
     * 性能优化作用：
     * - 避免对基本类型进行不必要的递归解析
     * - 减少反射操作次数
     * - 提高解析效率
     * @param className 类的全限定名
     * @return 如果是Kotlin基本类型返回true，否则返回false
     */
    private fun isKotlinPrimitiveType(className: String): Boolean {
        return className == "kotlin.Int" ||
                className == "kotlin.Long" ||
                className == "kotlin.Double" ||
                className == "kotlin.Float" ||
                className == "kotlin.Boolean" ||
                className == "kotlin.Byte" ||
                className == "kotlin.Short" ||
                className == "kotlin.Char" ||
                className == "kotlin.String" ||
                className == "kotlin.Unit" ||
                className == "kotlin.Nothing"
    }

    /**
     * 判断是否应该跳过该字段
     * 为了提高解析性能，需要跳过一些不包含业务数据的字段
     * 这些字段通常是系统字段、性能监控字段或编译器生成的字段
     * 需要跳过的字段分类：
     * 1. 系统字段：class、declaringClass、signature等
     * 2. 性能监控字段：status、startEpoch、elapsed、timeCost、executeCount等
     * 3. 日志字段：logger、LOG、log等
     * 4. 以下划线开头的字段：通常是内部字段或私有字段
     * 5. 以美元符号开头的字段：通常是编译器生成的字段
     * 性能优化作用：
     * - 减少不必要的字段访问和解析
     * - 避免对系统字段的误解析
     * - 提高整体解析效率
     * 字段分类规则：
     * - SKIP_FIELDS集合包含明确的字段名列表
     * - 以下划线开头的字段通常是内部实现细节
     * - 以美元符号开头的字段通常是编译器生成的
     * @param fieldName 字段名称
     * @return 如果应该跳过该字段返回true，否则返回false
     */
    private fun shouldSkipField(fieldName: String): Boolean {
        return SKIP_FIELDS.contains(fieldName) ||
                fieldName.startsWith("_") || // 以下划线开头的字段（通常是内部字段）
                fieldName.startsWith("$") // 以美元符号开头的字段（通常是编译器生成的字段）
    }

    /**
     * 检测文本中使用的换行符，以在输出时保持与来源一致（跨平台兼容）
     * 优先级：CRLF（Windows）> LF（Linux/Mac）> CR（旧 Mac）
     *
     * @param text 原始文本
     * @return 换行符序列，默认 "\n"
     */
    private fun detectLineSeparator(text: String): String {
        return when {
            text.contains("\r\n") -> "\r\n"
            text.contains("\n") -> "\n"
            text.contains("\r") -> "\r"
            else -> "\n"
        }
    }

    /**
     * 移除文本中的注释行
     * 按行过滤，支持两种注释格式：
     * 1. 以 # 开头的行（Shell/Python 风格）
     * 2. 以 REM 开头的行（不区分大小写）：REM 后跟行尾或空白符视为注释，如 REM、REM x、rem\tfoo
     * 换行符兼容：按文本中的实际换行符（CRLF/LF/CR）拆行，输出时沿用同一换行符，以支持 Windows/Linux/Mac。
     *
     * @param text 原始文本
     * @return 移除注释行后的文本，空输入返回原串
     */
    private fun removeCommentLines(text: String): String {
        if (text.isEmpty()) return text
        val lineSep = detectLineSeparator(text)
        val filtered = text.lineSequence()
            .filter { line ->
                val t = line.trimStart()
                when {
                    t.startsWith("#") -> false
                    t.uppercase().startsWith("REM") && (t.length == 3 || t.getOrNull(3)
                        ?.isWhitespace() == true) -> false

                    else -> true
                }
            }
        return filtered.joinToString(lineSep)
    }

    /**
     * 从 customCondition 等条件字符串中提取变量匹配，并区分单/双大括号。
     * @param rawExtract true：仅从 ${xxx}、${{xxx}} 提取花括号内内容（仅 trim）；false：含纯 variables.xxx 及去掉空格/variables. 前缀的 ${}/${{}} 提取。
     */
    private fun extractCustomConditionVariableMatches(text: String, rawExtract: Boolean = false): List<VarMatch> {
        if (text.isEmpty()) return emptyList()
        val cleanedText = removeCommentLines(text)
        if (cleanedText.isEmpty()) return emptyList()

        if (rawExtract) {
            return extractVariableMatchesFromString(text, rawExtract = true)
        }
        val result = mutableListOf<VarMatch>()
        val plainMatcher = PLAIN_VARIABLES_PATTERN.matcher(cleanedText)
        while (plainMatcher.find()) {
            plainMatcher.group(2)?.trim()?.takeIf { it.isNotBlank() }?.let {
                result.add(VarMatch(varName = it, isDoubleBrace = false))
            }
        }
        result.addAll(extractVariableMatchesFromString(text, rawExtract = false))
        return result
    }

    /**
     * 确定资源类型和ID
     * 根据Model对象确定其资源类型（流水线或模板）和对应的ID
     * 资源类型和ID用于标识变量引用的上下文环境，为变量引用提供归属信息
     * @param model 模型对象
     * @return Pair<资源类型, 资源ID>
     */
    private fun determineResourceTypeAndId(model: Model): Pair<String, String> {
        return when {
            !model.pipelineId.isNullOrBlank() -> Pair(RESOURCE_TYPE_PIPELINE, model.pipelineId!!)
            !model.templateId.isNullOrBlank() -> Pair(RESOURCE_TYPE_TEMPLATE, model.templateId!!)
            else -> Pair(RESOURCE_TYPE_PIPELINE, "unknown")
        }
    }

    /**
     * 提取参数变量
     * 从BuildFormProperty列表中提取参数ID，构建参数变量集合
     * 这些参数变量是需要在模型中查找引用的目标变量
     * @param params BuildFormProperty参数列表
     * @return 参数变量名称集合（已去重）
     */
    private fun extractParamVariables(params: List<BuildFormProperty>): Set<String> {
        return params.map { it.id }.filter { it.isNotBlank() }.toSet()
    }

    /**
     * 解析上下文数据类
     * 用于在递归解析过程中传递上下文信息，包含：
     * - 项目ID：标识变量引用的项目环境
     * - 资源类型和ID：标识变量引用的资源（流水线/模板）
     * - 阶段/容器/任务ID：标识变量引用的具体位置
     * - 参数变量集合：需要查找的目标变量集合
     *
     */
    private data class ParseContext(
        val projectId: String,
        val resourceType: String,
        val resourceId: String,
        val stageId: String = "",
        val containerId: String? = null,
        val taskId: String? = null,
        /** 任务名称，对应 Element.name，在 parseElement 时设置 */
        val taskName: String? = null,
        val paramVariables: Set<String> = emptySet(),
        /** true：仅提取在 triggerContainer.params 中的变量；false：提取所有 ${xxx}、${{xxx}} 并区分单/双大括号 */
        val filterByTriggerParams: Boolean = true
    )
}
