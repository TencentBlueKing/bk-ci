package com.tencent.devops.process.yaml.modelTransfer

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.io.IOContext
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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ReflectUtil
import com.tencent.devops.process.pojo.transfer.TransferMark
import com.tencent.devops.process.yaml.v2.models.YAME_META_DATA_JSON_FILTER
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.composer.Composer
import org.yaml.snakeyaml.constructor.SafeConstructor
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

        override fun addImplicitResolver(tag: Tag, regexp: Pattern, first: String?) {
            if (tag == Tag.BOOL) {
                super.addImplicitResolver(tag, BOOL_PATTERN, first)
            } else {
                super.addImplicitResolver(tag, regexp, first)
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
        CustomParser(events), Resolver(), loader
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
        }
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
            .disable(YAMLGenerator.Feature.SPLIT_LINES)
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
            res[kn.value] = TransferMark(
                startMark = TransferMark.Mark(
                    nodeTuple.valueNode.startMark.line, nodeTuple.valueNode.startMark.column
                ),
                endMark = TransferMark.Mark(
                    nodeTuple.valueNode.endMark.line, nodeTuple.valueNode.endMark.column
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
}
