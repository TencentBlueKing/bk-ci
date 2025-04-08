package com.tencent.devops.process.yaml.transfer

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.io.IOContext
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactoryBuilder
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.difflib.DiffUtils
import com.github.difflib.algorithm.myers.MeyersDiffWithLinearSpace
import com.github.difflib.patch.DeltaType
import com.tencent.devops.common.api.constant.CommonMessageCode.ELEMENT_UPDATE_WRONG_PATH
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ReflectUtil
import com.tencent.devops.common.pipeline.pojo.transfer.ElementInsertBody
import com.tencent.devops.common.pipeline.pojo.transfer.PositionResponse
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import com.tencent.devops.common.pipeline.pojo.transfer.TransferMark
import com.tencent.devops.common.pipeline.pojo.transfer.YAME_META_DATA_JSON_FILTER
import com.tencent.devops.process.yaml.v3.models.ITemplateFilter
import com.tencent.devops.process.yaml.v3.models.job.PreJob
import com.tencent.devops.process.yaml.v3.models.stage.PreStage
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.composer.Composer
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.error.Mark
import org.yaml.snakeyaml.events.Event
import org.yaml.snakeyaml.events.NodeEvent
import org.yaml.snakeyaml.nodes.AnchorNode
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeId
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.SequenceNode
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.parser.Parser
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.resolver.Resolver
import org.yaml.snakeyaml.serializer.AnchorGenerator
import java.io.StringWriter
import java.io.Writer
import java.util.function.Supplier
import java.util.regex.Pattern

@Suppress("ComplexCondition", "ComplexMethod", "NestedBlockDepth")
object TransferMapper {
    private val logger = LoggerFactory.getLogger(TransferMapper::class.java)

    /**
     * 重写StringQuotingChecker 以支持on关键字特性
     */
    class CustomStringQuotingChecker : StringQuotingChecker() {
        override fun needToQuoteName(name: String): Boolean {
            // 自定义字符串引号检查逻辑
            return reservedKeyword(name) || looksLikeYAMLNumber(name)
        }

        override fun needToQuoteValue(value: String): Boolean {
            // Only consider reserved keywords but not numbers?
            return isReservedKeyword(value) || valueHasQuotableChar(value)
        }

        /*
        *重写此处逻辑，以兼容对on关键字的特殊用法
        */
        private fun reservedKeyword(value: String): Boolean {
            if (value == "on") return false
            return if (value.isEmpty()) {
                true
            } else _isReservedKeyword(value[0].code, value)
        }
    }

    /*
    * 实现Parser接口，以支持由events生成yaml node
    * */
    class CustomParser(private val events: List<Event>) : Parser {
        var idx = 0
        private var currentEvent: Event? = null
        override fun checkEvent(choice: Event.ID): Boolean {
            peekEvent()
            return currentEvent != null && currentEvent?.`is`(choice) == true
        }

        override fun peekEvent(): Event? {
            if (currentEvent == null) {
                currentEvent = events[idx++]
            }
            return currentEvent
        }

        override fun getEvent(): Event? {
            peekEvent()
            val value = currentEvent
            currentEvent = null
            return value
        }
    }

    /*
    * Parser 的空实现
    * */
    class EmptyParser : Parser {
        override fun checkEvent(choice: Event.ID): Boolean {
            return true
        }

        override fun peekEvent(): Event? {
            return null
        }

        override fun getEvent(): Event? {
            return null
        }
    }

    private val BOOL_PATTERN = Pattern
        .compile("^(?:yes|Yes|YES|no|No|NO|true|True|TRUE|false|False|FALSE|On|ON|off|Off|OFF)$")

    private val resolver = object : Resolver() {

        override fun addImplicitResolver(tag: Tag, regexp: Pattern, first: String?, limit: Int) {
            if (tag == Tag.BOOL) {
                super.addImplicitResolver(tag, BOOL_PATTERN, first, limit)
            } else {
                super.addImplicitResolver(tag, regexp, first, limit)
            }
        }
    }

    private val dumper = DumperOptions().apply {
        this.isPrettyFlow = false
        this.splitLines = false
        this.defaultScalarStyle = DumperOptions.ScalarStyle.LITERAL
        this.defaultFlowStyle = DumperOptions.FlowStyle.FLOW
        this.isAllowReadOnlyProperties = true
        this.isProcessComments = true
        this.anchorGenerator = CustomAnchorGenerator()
    }

    private val loader = LoaderOptions().apply {
        this.isProcessComments = true
    }

    private fun eventsComposer(events: List<Event>) = Composer(
        CustomParser(events), resolver, loader
    )

    private val constructor = SafeConstructor(loader)
    private fun node2JsonString(node: Node): String {
        constructor.setComposer(
            object : Composer(
                EmptyParser(), Resolver(), loader
            ) {
                override fun getSingleNode(): Node {
                    return node
                }
            }
        )
        val res = constructor.getSingleData(Any::class.java)
        return JsonUtil.toJson(res, false)
    }

    private fun checkCommentEvent(comments: List<Event>): List<Event> {
        var index = comments.size
        for (i in (comments.size - 1) downTo 0) {
            if (comments[i].eventId != Event.ID.Comment) {
                break
            }
            index = i
        }
        return comments.subList(index, comments.size)
    }

    private fun anchorNode(node: Node, anchors: MutableMap<String, Node>) {
        var realNode = node
        if (node.nodeId == NodeId.anchor) {
            realNode = (node as AnchorNode).realNode
        }
        if (realNode.anchor != null) {
            anchors[realNode.anchor] = realNode
        }
        when (realNode.nodeId) {
            NodeId.sequence -> {
                val seqNode = realNode as SequenceNode
                val list = seqNode.value
                for (item in list) {
                    anchorNode(item, anchors)
                }
            }

            NodeId.mapping -> {
                val mNode = realNode as MappingNode
                val map = mNode.value
                for (obj in map) {
                    val key = obj.keyNode
                    val value = obj.valueNode
                    anchorNode(key, anchors)
                    anchorNode(value, anchors)
                }
            }

            else -> {}
        }
    }

    /**
     * @param node 当前需要做替换的节点
     * @param anchors 锚点信息
     */
    private fun replaceAnchor(node: Node, anchors: Map<String, Node>) {
        when (node.nodeId) {
            NodeId.scalar -> {}
            NodeId.anchor -> {}
            NodeId.sequence -> {
                val seqNode = node as SequenceNode
                val list = seqNode.value
                for (item in list) {
                    replaceAnchor(item, anchors)
                }
                if (node.anchor != null) return
                anchors.forEach { (key, n) ->
                    if (n !is SequenceNode) return@forEach
                    if (exactlyTheSameNode(node, n)) {
                        node.anchor = key
                        return@forEach
                    }
                }
            }

            NodeId.mapping -> {
                val mNode = node as MappingNode
                val map = mNode.value
                for (obj in map) {
                    val key = obj.keyNode
                    val value = obj.valueNode
                    replaceAnchor(key, anchors)
                    replaceAnchor(value, anchors)
                }
                if (node.anchor != null) return
                anchors.forEach anchors@{ key, n ->
                    if (n !is MappingNode) return@anchors
                    val needRemove = mutableListOf<NodeTuple>()
                    // key 需要全部有
                    n.value.forEach value@{ v ->
                        val index = map.find {
                            it.keyNode is ScalarNode &&
                                    v.keyNode is ScalarNode &&
                                    (it.keyNode as ScalarNode).value == (v.keyNode as ScalarNode).value
                        } ?: return@anchors

                        if (exactlyTheSameNode(index.valueNode, v.valueNode)) {
                            needRemove.add(index)
                        }
                    }
                    // value相同的去掉
                    map.removeAll(needRemove)
                    map.add(
                        NodeTuple(
                            ScalarNode(
                                /* tag = */ Tag.MERGE,
                                /* value = */ "<<",
                                /* startMark = */ n.startMark,
                                /* endMark = */ n.endMark,
                                /* style = */ DumperOptions.ScalarStyle.PLAIN
                            ),
                            n
                        )
                    )
                }
            }

            else -> {}
        }
    }

    data class NodeIndex(
        val key: String? = null,
        val index: Int? = null,
        val next: NodeIndex? = null
    ) {
        override fun toString(): String {
            return key ?: "array($index)" + (next?.toString() ?: "")
        }
    }

    private fun indexNode(node: Node, marker: TransferMark.Mark): NodeIndex? {
        var realNode = node
        if (node.nodeId == NodeId.anchor) {
            realNode = (node as AnchorNode).realNode
        }
        if (realNode is ScalarNode && checkMarker(realNode.startMark, realNode.endMark, marker)) {
            return NodeIndex(key = realNode.value, index = null, next = null)
        }
        when (realNode.nodeId) {
            NodeId.sequence -> {
                val seqNode = realNode as SequenceNode
                val list = seqNode.value
                list.forEachIndexed { index, node ->
                    indexNode(node, marker)?.run {
                        return NodeIndex(key = null, index = index, next = this)
                    }
                }
            }

            NodeId.mapping -> {
                val mNode = realNode as MappingNode
                val map = mNode.value
                for (obj in map) {
                    val key = obj.keyNode
                    val value = obj.valueNode
                    indexNode(key, marker)?.run {
                        return this
                    }
                    indexNode(value, marker)?.run {
                        val k = if (key.nodeId == NodeId.scalar) key as ScalarNode else null
                        return NodeIndex(key = k?.value ?: key.toString(), index = null, next = this)
                    }
                }
            }

            else -> {}
        }
        return null
    }

    private fun markNode(node: Node, nodeIndex: NodeIndex): TransferMark? {
        var realNode = node
        if (node.nodeId == NodeId.anchor) {
            realNode = (node as AnchorNode).realNode
        }
        if (nodeIndex.key != null && nodeIndex.next == null &&
            realNode is ScalarNode && nodeIndex.key == realNode.value
        ) {
            return TransferMark(
                startMark = TransferMark.Mark(
                    realNode.startMark.line, realNode.startMark.column
                ),
                endMark = TransferMark.Mark(
                    realNode.endMark.line, realNode.endMark.column
                )
            )
        }
        when (realNode.nodeId) {
            NodeId.sequence -> {
                val seqNode = realNode as SequenceNode
                val list = seqNode.value
                list.forEachIndexed { index, node ->
                    if (nodeIndex.index == null) return null
                    if (index != nodeIndex.index) return@forEachIndexed
                    if (nodeIndex.next == null) return TransferMark(
                        startMark = TransferMark.Mark(
                            node.startMark.line, node.startMark.column
                        ),
                        endMark = TransferMark.Mark(
                            node.endMark.line, node.endMark.column
                        )
                    )
                    return markNode(node, nodeIndex.next)
                }
            }

            NodeId.mapping -> {
                val mNode = realNode as MappingNode
                val map = mNode.value
                for (obj in map) {
                    val key = obj.keyNode
                    val value = obj.valueNode
                    if (nodeIndex.key == null) return null
                    val k = if (key.nodeId == NodeId.scalar) key as ScalarNode else null
                    if (k?.value != nodeIndex.key) continue
                    if (nodeIndex.next == null) return TransferMark(
                        startMark = TransferMark.Mark(
                            value.startMark.line, value.startMark.column
                        ),
                        endMark = TransferMark.Mark(
                            value.endMark.line, value.endMark.column
                        )
                    )
                    return markNode(value, nodeIndex.next)
                }
            }

            else -> {}
        }
        return null
    }

    private fun checkMarker(start: Mark, end: Mark, marker: TransferMark.Mark): Boolean {
        return marker.bigger(start) != false && marker.bigger(end) != true
    }

    private fun TransferMark.Mark.bigger(start: Mark) = when {
        line > start.line -> true
        line == start.line && column > start.column -> true
        line == start.line && column == start.column -> null
        else -> false
    }

    private fun exactlyTheSameNode(l: Node, r: Node): Boolean {
        if (l.nodeId != r.nodeId) return false

        when (l.nodeId) {
            NodeId.scalar -> {
                val ln = l as ScalarNode
                val rn = r as ScalarNode
                if (ln.value != rn.value) return false
            }

            NodeId.sequence -> {
                val ls = node2JsonString(l)
                val rs = node2JsonString(r)
                return JSONArray(ls).similar(JSONArray(rs))
            }

            NodeId.mapping -> {
                val ls = node2JsonString(l)
                val rs = node2JsonString(r)
                return JSONObject(ls).similar(JSONObject(rs))
            }

            NodeId.anchor -> {
                val ln = l as AnchorNode
                val rn = r as AnchorNode
                return exactlyTheSameNode(ln.realNode, rn.realNode)
            }
        }
        return true
    }

    /**
     * 重写YAMLFactory 以支持一些yaml规范外的特性
     */
    private val CustomYAMLFactoryBuilder = object : YAMLFactoryBuilder() {
        override fun build(): YAMLFactory {
            return object : YAMLFactory(this) {
                override fun _createGenerator(out: Writer, ctxt: IOContext): YAMLGenerator {
                    val feats = _yamlGeneratorFeatures
                    return object : YAMLGenerator(
                        ctxt, _generatorFeatures, feats,
                        _quotingChecker, _objectCodec, out, _version
                    ) {
                        override fun writeString(text: String) {
                            super.writeString(removeTrailingSpaces(text))
                        }

                        /*
                        * 去掉换行符前的空格，以支持yaml block输出
                        * */
                        private fun removeTrailingSpaces(text: String): String {
                            val result = StringBuilder(text.length)
                            var start = 0
                            var end = 0
                            val chars = text.toCharArray()

                            while (end < chars.size) {
                                if (chars[end] == '\n') {
                                    val line = chars.sliceArray(start until end)
                                    var endIdx = end - start - 1
                                    while (endIdx >= 0 && line[endIdx] == ' ') {
                                        endIdx--
                                    }
                                    result.append(line.sliceArray(0 until endIdx + 1))
                                    result.append('\n')
                                    start = end + 1
                                }
                                end++
                            }

                            if (start < chars.size) {
                                val line = chars.sliceArray(start until chars.size)
                                var endIdx = end - start - 1
                                while (endIdx >= 0 && line[endIdx] == ' ') {
                                    endIdx--
                                }
                                result.append(line.sliceArray(0 until endIdx + 1))
                            }
                            return result.toString()
                        }
                    }
                }
            }
        }
    }

    private val yamlObjectMapper = ObjectMapper(
        CustomYAMLFactoryBuilder.enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .disable(YAMLGenerator.Feature.SPLIT_LINES)
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
            .stringQuotingChecker(CustomStringQuotingChecker()).build()
    ).setSerializationInclusion(JsonInclude.Include.NON_NULL).apply {
        registerKotlinModule().setFilterProvider(
            SimpleFilterProvider().addFilter(
                YAME_META_DATA_JSON_FILTER,
                SimpleBeanPropertyFilter.serializeAllExcept(YAME_META_DATA_JSON_FILTER)
            )
        )
    }

    /*
    * 默认锚点命名生成规则是重命名。而预期的行为是不改变原有锚点命名。
    * 所以此处重写AnchorGenerator方法，对锚点不进行重命名
    * */
    class CustomAnchorGenerator : AnchorGenerator {
        override fun nextAnchor(node: Node): String {
            return node.anchor
        }
    }

    private val yamlFactory = ThreadLocal.withInitial(
        Supplier<Yaml> {
            Yaml(SafeConstructor(loader), Representer(dumper), dumper, loader, resolver)
        }
    )

    private fun parseMappingNodeIndex(node: MappingNode): Map<String, TransferMark> {
        val res = mutableMapOf<String, TransferMark>()
        node.value.forEach { nodeTuple ->
            if (nodeTuple.keyNode.nodeId != NodeId.scalar) return@forEach
            val kn = nodeTuple.keyNode as ScalarNode
            /* 目前页面只关心行坐标，暂对列坐标归零处理 */
            val markFlag = if (nodeTuple.valueNode.endMark.column == 0) 1 else 0
            res[kn.value] = TransferMark(
                startMark = TransferMark.Mark(
                    nodeTuple.valueNode.startMark.line, 0
                ),
                endMark = TransferMark.Mark(
                    nodeTuple.valueNode.endMark.line - markFlag, 0
                )
            )
        }
        return res
    }

    fun getYamlFactory(): Yaml = yamlFactory.get()

    fun getObjectMapper(): ObjectMapper = yamlObjectMapper

    fun toYaml(bean: Any): String {
        if (ReflectUtil.isNativeType(bean) || bean is String) {
            return bean.toString()
        }
        return getObjectMapper().writeValueAsString(bean)!!
    }

    fun <T> to(str: String): T = getObjectMapper().readValue(str, object : TypeReference<T>() {})

    fun <T> anyTo(any: Any?): T = getObjectMapper().readValue(
        getObjectMapper().writeValueAsString(any), object : TypeReference<T>() {}
    )

    /**
     * 获得 yaml 第一层级的坐标定位信息
     */
    fun getYamlLevelOneIndex(yaml: String): Map<String, TransferMark> {
        val node = getYamlFactory().compose(yaml.reader())
        if (node.nodeId != NodeId.mapping) return emptyMap()
        return parseMappingNodeIndex(node as MappingNode)
    }

    /*
   * yaml合并入口
   * 将minor中的内容融合进main中。
   * 融合策略：
   * 1.保留注释信息
   * 2.保留锚点信息
   * */
    fun mergeYaml(old: String, new: String): String {
        if (old.isBlank()) return new

        val oldE = getYamlFactory().parse(old.reader()).toList()
        val newL = getYamlFactory().parse(new.reader()).toList()
        val newE = newL.toMutableList()

        val patch = DiffUtils.diff(oldE, newE, MeyersDiffWithLinearSpace.factory().create())
        val anchorChecker = mutableMapOf<Int, Event>()
        for (i in (patch.deltas.size - 1) downTo 0) {
            val delta = patch.deltas[i]
            when (delta.type) {
                DeltaType.INSERT -> {
                    anchorChecker[delta.source.position]?.let { checker ->
                        delta.target.lines.forEachIndexed { index, event ->
                            if (event.eventId == checker.eventId) {
                                newE[delta.target.position + index] = checker
                            }
                        }
                    }
                }

                DeltaType.DELETE -> {
                    val sourceComment = checkCommentEvent(delta.source.lines)
                    if (sourceComment.isNotEmpty()) {
                        newE.addAll(delta.target.position, sourceComment)
                    }
                    // 锚点覆写逻辑
                    delta.source.lines.forEachIndexed { index, event ->
                        if (event !is NodeEvent) return@forEachIndexed
                        if (event.anchor != null) {
                            anchorChecker[delta.source.position + index] = event
                        }
                    }
                }

                else -> {}
            }
        }

        val newNode = eventsComposer(newE).singleNode
        val anchorNodes = mutableMapOf<String, Node>()
        anchorNode(newNode, anchorNodes)
        if (anchorNodes.isNotEmpty()) {
            replaceAnchor(newNode, anchorNodes)
        }

        val stringWriter = StringWriter()

        getYamlFactory().serialize(newNode, stringWriter)

        val out = stringWriter.toString()

//        if (!exactlyTheSameNode(eventsComposer(newL).singleNode, newNode)) {
//            throw Exception("not same node")
//        }
        if (!exactlyTheSameNode(getYamlFactory().compose(new.reader()), getYamlFactory().compose(out.reader()))) {
            logger.warn("merge yaml fail|new=\n$new\n|||out=\n$out\n")
            return new
        }
        return out
    }

    fun formatYaml(yaml: String): String {
        val res = getYamlFactory().load(yaml) as Any
        return toYaml(res)
    }

    fun indexYaml(
        yaml: String,
        line: Int,
        column: Int
    ): NodeIndex? {
        return indexNode(getYamlFactory().compose(yaml.reader()), TransferMark.Mark(line, column))
    }

    fun markYaml(
        index: NodeIndex,
        yaml: String
    ): TransferMark? {
        return markNode(getYamlFactory().compose(yaml.reader()), index)
    }

    fun indexYaml(
        position: PositionResponse,
        pYml: ITemplateFilter,
        yml: PreStep,
        type: ElementInsertBody.ElementInsertType
    ): NodeIndex {
        return when (position.type) {
            PositionResponse.PositionType.STEP -> indexInYamlSteps(position, pYml, yml, type)
            PositionResponse.PositionType.JOB, PositionResponse.PositionType.STAGE -> indexInYamlJob(
                positionResponse = position,
                preYaml = pYml,
                preStep = yml,
                type = type
            )

            else -> addInYamlLastStage(pYml, yml, type)
        }
    }

    /*
    * 光标在一个已有的 step 配置区域，则在该 step 之后 添加一个新的 step
    */
    private fun indexInYamlSteps(
        positionResponse: PositionResponse,
        preYaml: ITemplateFilter,
        preStep: PreStep,
        type: ElementInsertBody.ElementInsertType = ElementInsertBody.ElementInsertType.INSERT
    ): NodeIndex {
        if (positionResponse.stageIndex == -1) {
            return NodeIndex(
                key = ITemplateFilter::finally.name,
                next = indexInJob(
                    positionResponse,
                    preYaml.finally!!
                ) { steps ->
                    nodeIndexInStep(type, steps, positionResponse, preStep)
                }
            )
        }

        if (positionResponse.stageIndex != null) {
            return NodeIndex(
                key = ITemplateFilter::stages.name,
                next = indexInStage(
                    positionResponse,
                    preYaml.stages!!
                ) { steps ->
                    nodeIndexInStep(type, steps, positionResponse, preStep)
                }
            )
        }

        if (positionResponse.jobId != null) {
            return NodeIndex(
                key = ITemplateFilter::jobs.name,
                next = indexInJob(
                    positionResponse,
                    preYaml.jobs!!
                ) { steps ->
                    nodeIndexInStep(type, steps, positionResponse, preStep)
                }
            )
        }

        if (positionResponse.stepIndex != null) {
            return NodeIndex(
                key = PreJob::steps.name,
                next = indexInStep(
                    preYaml.steps!! as ArrayList<Any>
                ) { steps ->
                    nodeIndexInStep(type, steps, positionResponse, preStep)
                }
            )
        }
        return NodeIndex()
    }

    private fun nodeIndexInStep(
        type: ElementInsertBody.ElementInsertType,
        steps: ArrayList<Any>,
        positionResponse: PositionResponse,
        preStep: PreStep
    ) = when (type) {
        ElementInsertBody.ElementInsertType.INSERT -> {
            steps.add(positionResponse.stepIndex!! + 1, to(toYaml(preStep)))
            NodeIndex(
                index = positionResponse.stepIndex!! + 1
            )
        }

        else -> {
            steps[positionResponse.stepIndex!!] = to(toYaml(preStep))
            NodeIndex(
                index = positionResponse.stepIndex!!
            )
        }
    }

    /*
    * 光标在 job 配置区域，则在 job下的 steps 末尾添加一个新的 step
    */
    private fun indexInYamlJob(
        positionResponse: PositionResponse,
        preYaml: ITemplateFilter,
        preStep: PreStep,
        type: ElementInsertBody.ElementInsertType
    ): NodeIndex {
        if (type != ElementInsertBody.ElementInsertType.INSERT) {
            throw PipelineTransferException(
                ELEMENT_UPDATE_WRONG_PATH
            )
        }
        if (positionResponse.stageIndex == -1) {
            return NodeIndex(
                key = ITemplateFilter::finally.name,
                next = indexInJob(
                    positionResponse = positionResponse,
                    jobs = preYaml.finally!!,
                    last = true
                ) { steps ->
                    steps.add(preStep)
                    NodeIndex(
                        index = steps.size - 1
                    )
                }
            )
        }

        if (positionResponse.stageIndex != null) {
            return NodeIndex(
                key = ITemplateFilter::stages.name,
                next = indexInStage(
                    positionResponse,
                    preYaml.stages!!,
                    last = true
                ) { steps ->
                    steps.add(preStep)
                    NodeIndex(
                        index = steps.size - 1
                    )
                }
            )
        }

        if (positionResponse.jobId != null) {
            return NodeIndex(
                key = ITemplateFilter::jobs.name,
                next = indexInJob(
                    positionResponse,
                    preYaml.jobs!!,
                    last = true
                ) { steps ->
                    steps.add(preStep)
                    NodeIndex(
                        index = steps.size - 1
                    )
                }
            )
        }
        return NodeIndex()
    }

    /*
    * 光标在 stage 配置/流水线配置区域，则在最后一个 stage 的最后一个 job 末尾添加一个新的 step
    */
    private fun addInYamlLastStage(
        preYaml: ITemplateFilter,
        preStep: PreStep,
        type: ElementInsertBody.ElementInsertType
    ): NodeIndex {
        if (type != ElementInsertBody.ElementInsertType.INSERT) {
            throw PipelineTransferException(
                ELEMENT_UPDATE_WRONG_PATH
            )
        }
        if (preYaml.finally != null) {
            return NodeIndex(
                key = ITemplateFilter::finally.name,
                next = indexInJob(
                    positionResponse = PositionResponse(),
                    jobs = preYaml.finally!!,
                    last = true
                ) { steps ->
                    steps.add(preStep)
                    NodeIndex(
                        index = steps.size - 1
                    )
                }
            )
        }

        if (preYaml.stages != null) {
            return NodeIndex(
                key = ITemplateFilter::stages.name,
                next = indexInStage(
                    positionResponse = PositionResponse(),
                    stages = preYaml.stages!!,
                    last = true
                ) { steps ->
                    steps.add(preStep)
                    NodeIndex(
                        index = steps.size - 1
                    )
                }
            )
        }

        if (preYaml.jobs != null) {
            return NodeIndex(
                key = ITemplateFilter::jobs.name,
                next = indexInJob(
                    positionResponse = PositionResponse(),
                    jobs = preYaml.jobs!!,
                    last = true
                ) { steps ->
                    steps.add(preStep)
                    NodeIndex(
                        index = steps.size - 1
                    )
                }
            )
        }

        if (preYaml.steps != null) {
            preYaml.steps!!.add(to(toYaml(preStep)))
            return NodeIndex(
                index = preYaml.steps!!.size - 1
            )
        }
        return NodeIndex()
    }

    private fun indexInStage(
        positionResponse: PositionResponse,
        stages: ArrayList<Map<String, Any>>,
        last: Boolean = false,
        action: (steps: ArrayList<Any>) -> NodeIndex?
    ): NodeIndex {
        if (stages.isEmpty()) {
            stages.add(mutableMapOf(PreStage::jobs.name to LinkedHashMap<String, Any>()))
        }
        val index = if (last && positionResponse.stageIndex == null) stages.lastIndex else positionResponse.stageIndex!!
        val jobs = stages[index][PreStage::jobs.name] as LinkedHashMap<String, Any>
        return NodeIndex(
            index = index,
            next = NodeIndex(
                key = PreStage::jobs.name,
                next = indexInJob(positionResponse, jobs, last, action)
            )
        )
    }

    private fun indexInJob(
        positionResponse: PositionResponse,
        jobs: LinkedHashMap<String, Any>,
        last: Boolean = false,
        action: (steps: ArrayList<Any>) -> NodeIndex?
    ): NodeIndex? {
        if (jobs.isEmpty()) {
            val job = LinkedHashMap<String, Any>()
            job[PreJob::steps.name] = ArrayList<Any>()
            jobs["job_1"] = job
        }
        val key = if (last && positionResponse.jobId == null) jobs.entries.last().key else positionResponse.jobId
            ?: return null
        val job = jobs[key] as LinkedHashMap<String, Any>
        val steps = job[PreJob::steps.name] as ArrayList<Any>
        return NodeIndex(
            key = key,
            next = NodeIndex(key = PreJob::steps.name, next = indexInStep(steps, action))
        )
    }

    private fun indexInStep(
        steps: ArrayList<Any>,
        action: (steps: ArrayList<Any>) -> NodeIndex?
    ): NodeIndex? {
        return action(steps)
    }
}
