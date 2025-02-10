package com.tencent.devops.process.engine.pojo

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.AgentReuseMutex
import com.tencent.devops.common.pipeline.container.AgentReuseMutexType
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.EnvControlTaskType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ReusedInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDispatch
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.control.VmOperateTaskGenerator.Companion.START_VM_TASK_ATOM
import com.tencent.devops.process.pojo.app.StartBuildContext
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PipelineVarUtil

/**
 * AgentReuseMutexTree 组装流水线时通过生成树可以更好地拿到复用互斥关系
 */
@Suppress("ComplexCondition")
data class AgentReuseMutexTree(
    val executeCount: Int,
    val rootNodes: MutableList<AgentReuseMutexRootNode>,
    var maxStageIndex: Int = 0
) {
    fun addNode(container: VMBuildContainer, stageIndex: Int, variables: Map<String, String>) {
        val dispatchType = container.dispatchType
        if (dispatchType !is ThirdPartyAgentDispatch) {
            return
        }
        // 判断值是否是变量
        var idIsVar = false
        val reuseId = if (dispatchType.agentType.isReuse()) {
            if (PipelineVarUtil.isVar(dispatchType.value)) {
                idIsVar = true
            }
            EnvReplacementParser.parse(dispatchType.value, contextMap = variables)
        } else {
            null
        }
        return addNode(
            jobId = container.jobId,
            reuseId = reuseId,
            dispatchType = dispatchType,
            // 逻辑上可能需要dependOn复用树
            existDep = (container.jobControlOption?.dependOnId?.contains(reuseId) == true) ||
                    (container.jobControlOption?.dependOnName == reuseId),
            stageIndex = stageIndex,
            containerId = container.id,
            isEnv = dispatchType is ThirdPartyAgentEnvDispatchType,
            idIsVar = idIsVar
        )
    }

    private fun addNode(
        jobId: String?,
        reuseId: String?,
        dispatchType: ThirdPartyAgentDispatch,
        existDep: Boolean,
        stageIndex: Int,
        containerId: String?,
        isEnv: Boolean,
        idIsVar: Boolean
    ) {
        if (reuseId.isNullOrBlank()) {
            if (jobId.isNullOrBlank()) {
                return
            }
            this.addRootNode(
                AgentReuseMutexRootNode(
                    stageSeq = stageIndex,
                    jobId = jobId,
                    agentId = when (dispatchType) {
                        is ThirdPartyAgentIDDispatchType -> dispatchType.displayName
                        is ThirdPartyAgentEnvDispatchType -> dispatchType.envName
                        else -> return
                    },
                    virtual = false,
                    type = dispatchTypeToType(dispatchType) ?: return,
                    maxStageIndex = stageIndex
                ),
                idIsVar = idIsVar
            )
            return
        }

        if (jobId.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_AGENT_REUSE_MUTEX_JOB_NULL,
                params = arrayOf("Stage${stageIndex}Job$containerId")
            )
        }

        this.addReuseNode(
            stageSeq = stageIndex,
            reuseJobId = reuseId,
            jobId = jobId,
            type = if (existDep) {
                AgentReuseMutexType.AGENT_DEP_VAR
            } else {
                dispatchTypeToType(dispatchType) ?: return
            },
            isEnv = isEnv,
            idIsVar = idIsVar
        )
    }

    private fun addRootNode(n: AgentReuseMutexRootNode, idIsVar: Boolean) {
        if (n.stageSeq > maxStageIndex) {
            maxStageIndex = n.stageSeq
        }
        // 寻找虚占节点(部分在同一Stage下先进入了reuse节点，其根节点被虚占)，没有直接添加
        val vRoot = rootNodes.firstOrNull { it.jobId == n.jobId }
        if (vRoot == null) {
            rootNodes.add(n)
            return
        }
        if (!idIsVar && !n.type.checkSameStageType(vRoot.type)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_AGENT_REUSE_MUTEX_DEP_ERROR,
                params = arrayOf(
                    "Stage-${n.stageSeq}",
                    "${vRoot.children.first().jobId}|${vRoot.children.first().type}",
                    "${n.jobId}|${n.type}"
                )
            )
        }
        vRoot.input(n)
        // 刷新同级下子节点类型和父节点一致
        vRoot.children.filter { it.type != AgentReuseMutexType.AGENT_DEP_VAR }.forEach {
            it.type = vRoot.type
        }
    }

    private fun dispatchTypeToType(
        dispatchType: ThirdPartyAgentDispatch
    ): AgentReuseMutexType? {
        return when (dispatchType) {
            is ThirdPartyAgentIDDispatchType -> {
                if (dispatchType.agentType == AgentType.ID) {
                    AgentReuseMutexType.AGENT_ID
                } else {
                    AgentReuseMutexType.AGENT_NAME
                }
            }

            is ThirdPartyAgentEnvDispatchType -> {
                if (dispatchType.agentType == AgentType.ID) {
                    AgentReuseMutexType.AGENT_ENV_ID
                } else {
                    AgentReuseMutexType.AGENT_ENV_NAME
                }
            }

            else -> null
        }
    }

    private fun addReuseNode(
        stageSeq: Int,
        reuseJobId: String,
        jobId: String,
        type: AgentReuseMutexType,
        isEnv: Boolean,
        idIsVar: Boolean
    ) {
        if (stageSeq > maxStageIndex) {
            maxStageIndex = stageSeq
        }
        var parentNode: AgentReuseMutexTreeNode? = null

        for (root in rootNodes) {
            val node = findNodeByDFS(root, reuseJobId)
            if (node != null) {
                parentNode = node
                break
            }
        }

        if (parentNode != null) {
            if (!idIsVar && parentNode.stageSeq == stageSeq && !parentNode.type.checkSameStageType(type)) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_AGENT_REUSE_MUTEX_DEP_ERROR,
                    params = arrayOf("Stage-$stageSeq", "$jobId|$type", "$reuseJobId|${parentNode.type}")
                )
            }
            if (idIsVar && parentNode.stageSeq == stageSeq && type != AgentReuseMutexType.AGENT_DEP_VAR) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_AGENT_REUSE_MUTEX_VAR_ERROR,
                    params = arrayOf("Stage-$stageSeq", "$jobId|$type", "$reuseJobId|${parentNode.type}")
                )
            }
            parentNode.children.add(
                AgentReuseMutexTreeReuseNode(
                    stageSeq = stageSeq,
                    reuseJobId = reuseJobId,
                    jobId = jobId,
                    type = if (type == AgentReuseMutexType.AGENT_DEP_VAR || parentNode.stageSeq < stageSeq) {
                        AgentReuseMutexType.AGENT_DEP_VAR
                    } else {
                        parentNode.type
                    },
                    parent = parentNode
                )
            )
            val root = parentNode.getRoot()
            if (root.maxStageIndex < stageSeq) {
                root.maxStageIndex = stageSeq
            }
            return
        }
        // 一个root节点下的都没有找到就新建一个虚占root，使用变量的需要他和vRoot一定存在引用关系
        if (idIsVar && type != AgentReuseMutexType.AGENT_DEP_VAR) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_AGENT_REUSE_MUTEX_VAR_ERROR,
                params = arrayOf("Stage-$stageSeq", "$jobId|$type", "$reuseJobId|$type")
            )
        }
        val nodeType = if (isEnv) {
            AgentReuseMutexType.AGENT_ENV_ID
        } else {
            AgentReuseMutexType.AGENT_ID
        }
        val vRoot = AgentReuseMutexRootNode.virtualInput(stageSeq, reuseJobId, nodeType)
        vRoot.children.add(
            AgentReuseMutexTreeReuseNode(
                stageSeq = stageSeq,
                reuseJobId = reuseJobId,
                jobId = jobId,
                type = nodeType,
                parent = vRoot
            )
        )
        rootNodes.add(vRoot)
    }

    // 因为有虚拟Root所以不会有循环依赖只会存在依赖不存在节点报错
    private fun findNodeByDFS(
        root: AgentReuseMutexRootNode,
        jobId: String
    ): AgentReuseMutexTreeNode? {
        return root.dfs<AgentReuseMutexTreeNode?> { node ->
            // 寻找符合的节点
            if (node.jobId == jobId) {
                return@dfs node
            }

            return@dfs null
        }
    }

    // 判断父子节点是否都是Env或者Agent
    private fun AgentReuseMutexType.checkSameStageType(childType: AgentReuseMutexType): Boolean {
        if (this == AgentReuseMutexType.AGENT_DEP_VAR || childType == AgentReuseMutexType.AGENT_DEP_VAR) {
            return true
        }
        if ((this.isAgentType() && childType.isAgentType()) || (this.isEnvType() && childType.isEnvType())) {
            return true
        }
        return false
    }

    // Job轮循完后
    // 1、检查是否存在虚占root还没有被填充，如果存在说明当前复用节点引用了一个不存在的或者在当前Stage后的Job
    // 2、判断复用节点是否和根节点是否存在先后顺序，如果存在type需要被调整为使用变量
    // @return false 说明存在节点没填充
    fun checkVirtualRootAndResetJobType() {
        rootNodes.filter { it.children.size > 0 }.forEach { root ->
            // 如果是重试部分步骤导致 root 节点存在的 stage 或者 job 没有被重试，这时直接放开到下面执行，因为部分重试不会清空
            // 如果是全部重试，因为重试不会修改 model，所以可以直接放开
            if (executeCount == 1 && root.virtual) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_AGENT_REUSE_MUTEX_DEP_NULL_NODE,
                    params = arrayOf(root.getAllChildJobId().joinToString("|"), root.jobId)
                )
            }

            root.children.forEach child@{ child ->
                if (child.type == AgentReuseMutexType.AGENT_DEP_VAR) {
                    return@child
                }
                val r = child.getRoot()
                if (child.stageSeq > r.stageSeq) {
                    child.type = AgentReuseMutexType.AGENT_DEP_VAR
                    return@child
                }
            }
        }
    }

    fun rewriteModel(
        context: StartBuildContext,
        buildContainersWithDetail: MutableList<Pair<PipelineBuildContainer, Container>>,
        fullModel: Model,
        buildTaskList: MutableList<PipelineBuildTask>
    ) {
        val treeMap = tranMap().ifEmpty { return }

        buildContainersWithDetail.forEach { (bc, c) ->
            if (treeMap.containsKey(c.jobId)) {
                bc.controlOption.agentReuseMutex = treeMap[c.jobId]?.first?.copy(
                    linkTip = "${context.pipelineId}_Pipeline" +
                            "[${context.variables[PIPELINE_NAME]}]Job[${c.name}]"
                )
            }
        }

        fullModel.stages.forEachIndexed nextStage@{ index, stage ->
            stage.containers.filter { it is VMBuildContainer && it.dispatchType is ThirdPartyAgentDispatch }
                .forEach container@{ container ->
                    if (!treeMap.containsKey(container.jobId)) {
                        return@container
                    }
                    val tm = treeMap[container.jobId]!!.first
                    // 修改container
                    val dispatch = (container as VMBuildContainer).dispatchType as ThirdPartyAgentDispatch
                    rewriteDispatch(tm, dispatch, treeMap[container.jobId]?.second != true)

                    // 修改启动插件
                    buildTaskList.firstOrNull {
                        it.containerId == container.id &&
                                it.taskType == EnvControlTaskType.VM.name &&
                                it.taskAtom == START_VM_TASK_ATOM
                    }?.taskParams = container.genTaskParams()
                }
            if (index == maxStageIndex) {
                return
            }
        }
    }

    // 单独抽出，方便测试
    fun rewriteDispatch(
        treeMutex: AgentReuseMutex,
        dispatch: ThirdPartyAgentDispatch,
        isRoot: Boolean
    ) {
        // 根节点和其同级依赖节点都需要设置被复用信息，方便Dispatch层面加锁
        if (treeMutex.type != AgentReuseMutexType.AGENT_DEP_VAR) {
            dispatch.reusedInfo = ReusedInfo(
                value = treeMutex.agentOrEnvId ?: return,
                agentType = treeMutex.type.toAgentType() ?: return,
                jobId = if (isRoot) {
                    null
                } else {
                    treeMutex.reUseJobId
                }
            )
        }

        // 复用节点都需要修改复用对象为根节点，这样才能拿到上下文
        if (!isRoot) {
            dispatch.value = treeMutex.reUseJobId ?: return
            when (dispatch) {
                is ThirdPartyAgentEnvDispatchType -> {
                    dispatch.envName = treeMutex.reUseJobId ?: return
                }

                is ThirdPartyAgentIDDispatchType -> {
                    dispatch.displayName = treeMutex.reUseJobId ?: return
                }
            }
        }
    }

    // 转换为key为JobId的Map方便检索
    fun tranMap(): Map<String, Pair<AgentReuseMutex, Boolean>> {
        val rm = mutableMapOf<String, Pair<AgentReuseMutex, Boolean>>()
        rootNodes.filter { it.children.size > 0 }.forEach { root ->
            root.bfs { node ->
                rm[node.jobId] = Pair(
                    node.initMutex(),
                    node !is AgentReuseMutexRootNode
                )
            }
        }
        return rm
    }
}

/**
 * AgentReuseMutexTreeNode AgentReuseMutex依赖树节点
 */
abstract class AgentReuseMutexTreeNode(
    open val stageSeq: Int,
    open val jobId: String,
    open val agentId: String?,
    open val type: AgentReuseMutexType,
    open val parent: AgentReuseMutexTreeNode?,
    open val children: MutableList<AgentReuseMutexTreeReuseNode> = mutableListOf()
) {
    fun getRoot(): AgentReuseMutexRootNode {
        if (this is AgentReuseMutexRootNode) {
            return this
        }

        var node = this
        while (node.parent != null) {
            node = node.parent!!
        }
        return node as AgentReuseMutexRootNode
    }

    fun bfs(run: (child: AgentReuseMutexTreeNode) -> Unit) {
        val queue = ArrayDeque<AgentReuseMutexTreeNode>()
        queue.add(this)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            run(node)

            for (child in node.children) {
                queue.add(child)
            }
        }
    }

    fun <T> dfs(run: (node: AgentReuseMutexTreeNode) -> T?): T? {
        val stack = ArrayDeque<AgentReuseMutexTreeNode>()

        stack.addFirst(this)

        while (stack.isNotEmpty()) {
            val node = stack.removeFirst()

            val r = run(node)
            if (r != null) {
                return r
            }

            for (child in node.children) {
                stack.addFirst(child)
            }
        }

        return null
    }

    abstract fun initMutex(): AgentReuseMutex
}

data class AgentReuseMutexRootNode(
    override var stageSeq: Int,
    override var jobId: String,
    override var agentId: String,
    var virtual: Boolean,
    override var type: AgentReuseMutexType,
    override val children: MutableList<AgentReuseMutexTreeReuseNode> = mutableListOf(),
    var maxStageIndex: Int
) : AgentReuseMutexTreeNode(
    stageSeq = stageSeq,
    jobId = jobId,
    agentId = agentId,
    type = type,
    parent = null,
    children = children
) {
    fun input(root: AgentReuseMutexRootNode) {
        this.stageSeq = root.stageSeq
        this.jobId = root.jobId
        this.agentId = root.agentId
        this.type = root.type
        this.virtual = false
        this.maxStageIndex = root.maxStageIndex
    }

    fun getAllChildJobId(): List<String> {
        val jobIds = mutableListOf<String>()
        children.forEach { child ->
            child.dfs<Boolean> {
                jobIds.add(it.jobId)
                return@dfs null
            }
        }
        return jobIds
    }

    companion object {
        fun virtualInput(stageSeq: Int, reuseJobId: String, type: AgentReuseMutexType): AgentReuseMutexRootNode {
            return AgentReuseMutexRootNode(
                stageSeq = stageSeq,
                jobId = reuseJobId,
                agentId = "",
                virtual = true,
                type = type,
                children = mutableListOf(),
                maxStageIndex = stageSeq
            )
        }
    }

    override fun initMutex() = AgentReuseMutex(
        jobId = jobId,
        reUseJobId = null,
        agentOrEnvId = agentId,
        type = type,
        endJob = this.stageSeq == this.maxStageIndex
    )
}

data class AgentReuseMutexTreeReuseNode(
    override val stageSeq: Int,
    val reuseJobId: String,
    override val jobId: String,
    override var type: AgentReuseMutexType,
    override val parent: AgentReuseMutexTreeNode
) : AgentReuseMutexTreeNode(
    stageSeq = stageSeq,
    jobId = jobId,
    agentId = null,
    type = type,
    parent = parent,
    children = mutableListOf()
) {
    override fun initMutex() = AgentReuseMutex(
        jobId = jobId,
        reUseJobId = getRoot().jobId,
        agentOrEnvId = getRoot().agentId,
        type = type,
        endJob = this.getRoot().maxStageIndex == stageSeq
    )
}
