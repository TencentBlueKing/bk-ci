<template>
    <bk-dialog
        :value="isShow"
        width="1000"
        header-position="left"
        ext-cls="import-third-party-dialog"
        :title="title"
        :mask-close="false"
        @value-change="onValueChange"
    >
        <ImportConfigForm
            v-if="step === 1"
            :form="form"
            :is-reinstall="isReinstall"
            :is-windows="isWindows"
            :docker-supported="dockerSupported"
            :defaults="defaults"
            :gateway-list="gatewayList"
            :install-type-list="installTypeList"
            :custom-tags="customTags"
            :env-preview="envPreview"
            :show-env-preview="showEnvPreview"
            :env-preview-loading="envPreviewLoading"
            :deny-reason="denyReason"
        />

        <template v-if="step === 2">
            <ConfigSummary
                :items="summaryItems"
                @edit="backToConfig"
            />

            <CommandCard
                :command="installCommand"
                :is-windows="isWindows"
                :install-type="form.installType"
                :expire-time="expireTime"
                :expired="sessionExpired"
                :regenerating="regenerating"
                @copy="copyCmd"
                @regenerate="handleRegenerate"
            />

            <AccessedNodeList
                :nodes="accessedNodes"
                :waited-too-long="waitedTooLong"
                :has-stalled="hasStalled"
                :access-error="accessError"
                @retry="retryAccess"
                @open-doc="openDoc"
            />
        </template>

        <template #footer>
            <template v-if="step === 1">
                <bk-button
                    theme="primary"
                    :loading="generating"
                    :disabled="isReinstall && !canReinstall"
                    @click="generateCmd"
                >
                    {{ $t('environment.installSession.generateCommand') }}
                </bk-button>
                <bk-button
                    class="bk-dialog-cancel"
                    @click="close"
                >
                    {{ $t('environment.installSession.cancel') }}
                </bk-button>
            </template>
            <div
                v-else
                class="step2-footer"
            >
                <span class="footer-hint">{{ $t('environment.installSession.closeHint') }}</span>
                <bk-button @click="close">{{ $t('environment.installSession.close') }}</bk-button>
            </div>
        </template>
    </bk-dialog>
</template>

<script>
    import Vue from 'vue'
    import { computed, getCurrentInstance, onBeforeUnmount, reactive, ref, watch } from 'vue'
    import {
        DEFAULT_PARALLEL_TASK_COUNT,
        DEFAULT_DOCKER_PARALLEL_TASK_COUNT,
        DOCKER_SUPPORTED_OS,
        ENV_PREVIEW_DEBOUNCE,
        INSTALL_TYPE_LIST,
        POLL_INTERVAL,
        SESSION_STATUS,
        WAIT_TOO_LONG,
        formatDateTime,
        osOf,
    } from './import-third-party/constants'
    import AccessedNodeList from './import-third-party/AccessedNodeList.vue'
    import CommandCard from './import-third-party/CommandCard.vue'
    import ConfigSummary from './import-third-party/ConfigSummary.vue'
    import ImportConfigForm from './import-third-party/ImportConfigForm.vue'

    /**
     * 导入第三方构建机弹窗（import = 导入节点，reinstall = 重装 Agent）。
     * 基于后端「安装会话（installSession）」机制（PR #13584）：
     * 生成命令 = 创建/复用会话（同配置在有效期内复用同一条命令），已接入节点按 sessionId 轮询。
     * 本组件负责状态编排（表单 / 步骤 / 已接入节点轮询），
     * 两步视图分别拆在 import-third-party/ 下的子组件中：
     * - 第一步配置表单：ImportConfigForm（内含标签 → 动态环境预览 EnvPreview）
     * - 第二步：ConfigSummary（只读摘要）+ CommandCard（安装命令）+ AccessedNodeList（已接入节点）
     */
    export default {
        name: 'ImportThirdPartyDialog',
        components: {
            ImportConfigForm,
            ConfigSummary,
            CommandCard,
            AccessedNodeList,
        },
        props: {
            isShow: { type: Boolean, default: false },
            mode: { type: String, default: 'import' },
            node: { type: Object, default: null },
            /** 导入态默认操作系统（LINUX / MACOS / WINDOWS），由入口透传，如 URL type 参数 */
            defaultOs: { type: String, default: '' },
        },
        setup (props, { emit }) {
            const instance = getCurrentInstance()
            const proxy = instance.proxy
            const projectId = computed(() => proxy.$route.params.projectId)

            const isReinstall = computed(() => props.mode === 'reinstall')
            const form = reactive({
                os: 'LINUX',
                zone: 'SHENZHEN',
                installType: 'SERVICE',
                parallelTaskCount: '',
                dockerParallelTaskCount: '',
                tags: [{ tagKeyId: '', tagValueId: '' }],
            })

            /** 网关（接入点）列表 */
            const gatewayList = ref([])
            /** 项目自定义标签（canUpdate === 'TRUE'） */
            const customTags = ref([])
            /** 当前安装会话 */
            const commandText = ref('')
            const sessionId = ref('')
            const sessionStatus = ref(SESSION_STATUS.ACTIVE)
            const sessionExpiredAt = ref('')
            /** 会话过期：命令卡片置灰并给出「重新生成」入口 */
            const sessionExpired = computed(() => sessionStatus.value === SESSION_STATUS.EXPIRED)
            /** 生成命令 / 重新生成请求进行中（按钮 loading） */
            const generating = ref(false)
            const regenerating = ref(false)

            /** 环境预览接口的返回（已换算为模板消费的结构） */
            const envPreviewData = ref(null)
            /** 环境预览请求中（防抖等待 + 接口返回前展示 loading） */
            const envPreviewLoading = ref(false)

            /** 重装上下文校验：canReinstall=false 时展示原因并禁止生成命令 */
            const canReinstall = ref(true)
            const denyReason = ref('')

            const step = ref(1)
            const waitedTooLong = ref(false)
            const accessedNodes = ref([])
            const accessError = ref(false)
            const timers = []
            let pollTimer = null
            let envPreviewTimer = null

            const title = computed(() =>
                isReinstall.value
                    ? proxy.$t('environment.installSession.reinstallTitle', { name: props.node?.displayName || props.node?.ip || '' })
                    : proxy.$t('environment.installSession.title')
            )
            const isWindows = computed(() => form.os === 'WINDOWS')
            const dockerSupported = computed(() => DOCKER_SUPPORTED_OS.includes(form.os))
            const installCommand = computed(() => commandText.value)
            const expireTime = computed(() => sessionExpiredAt.value)

            /** 重装目标 Agent：入口可能传 agentHashId 或 agentId */
            const targetAgentId = computed(() => props.node?.agentHashId || props.node?.agentId || '')

            const defaults = {
                parallelTaskCount: DEFAULT_PARALLEL_TASK_COUNT,
                dockerParallelTaskCount: 4,
            }

            const tagText = computed(() => {
                const rows = form.tags
                    .map((r) => {
                        const key = customTags.value.find((k) => k.tagKeyId === r.tagKeyId)
                        const value = key?.tagValues.find((v) => v.tagValueId === r.tagValueId)
                        return key && value ? `${key.tagKeyName}: ${value.tagValueName}` : ''
                    })
                    .filter(Boolean)
                return rows.length ? rows.join(proxy.$t('environment.installSession.tagSeparator')) : proxy.$t('environment.installSession.notSet')
            })

            const hasPickedTag = computed(() => form.tags.some((t) => t.tagKeyId && t.tagValueId))
            /** 有效标签（键与值都已选）签名：仅当它变化才需要重新预览，增删空行不触发请求 */
            const pickedTagSignature = computed(() => form.tags
                .filter((t) => t.tagKeyId && t.tagValueId)
                .map((t) => `${t.tagKeyId}:${t.tagValueId}`)
                .join(','))
            /**
             * 标签 → 动态环境预览：只读反馈，兑现"打标签能进哪个环境"的承诺。
             * 后端 installSessions/preview 统一返回 associated/willJoin/willLeave/pending：
             * 导入态 associated 为该机器已关联的环境（重复导入同一台机器时存在），
             * matched 由 willJoin 映射，pending 依赖接入后写入的内置标签；
             * 重装态直接给 associated/willJoin/willLeave（diff 由后端计算），前端不再自行 diff
             */
            const envPreview = computed(() => {
                return (
                    envPreviewData.value
                    || (isReinstall.value
                        ? { mode: 'reinstall', associated: [], willJoin: [], willLeave: [], pending: [] }
                        : { mode: 'import', associated: [], matched: [], pending: [] })
                )
            })
            const showEnvPreview = computed(() => {
                const p = envPreview.value
                if (p.mode === 'import') {
                    return (
                        hasPickedTag.value
                        || p.associated.length > 0
                    )
                }
                return (
                    hasPickedTag.value
                    || p.associated.length > 0
                    || p.willJoin.length > 0
                    || p.willLeave.length > 0
                )
            })

            /**
             * "已下载脚本、尚未接入"直接来自后端节点状态机（PENDING/INSTALLING/IMPORTING），
             * 不再是前端自行记录的启发式
             */
            const hasStalled = computed(() =>
                accessedNodes.value.some((n) => ['PENDING', 'INSTALLING', 'IMPORTING'].includes(n.status))
            )

            /** 第二步的只读摘要：留空的并发要写出实际生效的默认值，别让用户回头猜 */
            const summaryItems = computed(() => {
                const osLabel = { LINUX: 'Linux', MACOS: 'macOS', WINDOWS: 'Windows' }[form.os]
                const zoneLabel = gatewayList.value.find((g) => g.zoneName === form.zone)?.showName || form.zone
                const items = [
                    { label: proxy.$t('environment.installSession.osLabel'), value: osLabel },
                    { label: proxy.$t('environment.installSession.zoneLabel'), value: zoneLabel },
                ]
                if (isWindows.value) {
                    const installType = INSTALL_TYPE_LIST.find((i) => i.id === form.installType)
                    items.push({
                        label: proxy.$t('environment.installSession.installModeLabel'),
                        value: installType ? proxy.$t(installType.label) : '',
                    })
                }
                items.push({
                    label: proxy.$t('environment.installSession.parallelLabel'),
                    value: form.parallelTaskCount
                        || proxy.$t('environment.installSession.defaultValueSuffix', { n: defaults.parallelTaskCount }),
                })
                if (dockerSupported.value) {
                    items.push({
                        label: proxy.$t('environment.installSession.dockerParallelLabel'),
                        value: form.dockerParallelTaskCount
                            || proxy.$t('environment.installSession.defaultValueSuffix', { n: defaults.dockerParallelTaskCount }),
                    })
                }
                items.push({ label: proxy.$t('environment.installSession.tagsLabel'), value: tagText.value })
                return items
            })

            const stopPolling = () => {
                if (pollTimer) {
                    clearInterval(pollTimer)
                    pollTimer = null
                }
            }

            const clearTimers = () => {
                stopPolling()
                if (envPreviewTimer) {
                    clearTimeout(envPreviewTimer)
                    envPreviewTimer = null
                }
                while (timers.length) clearTimeout(timers.pop())
            }

            /** 组装 AgentInstallSessionRequest（创建会话与预览共用） */
            const buildSessionRequest = () => {
                const rows = form.tags
                    // 系统内置标签（os/arch，负 id）由后端按 Agent 自动维护，仅用于回显，不随请求提交
                    .filter((t) => t.tagKeyId && t.tagValueId && Number(t.tagKeyId) > 0)
                    .map(({ tagKeyId, tagValueId }) => ({ tagKeyId, tagValueId }))
                return {
                    mode: isReinstall.value ? 'REINSTALL' : 'FIRST_IMPORT',
                    os: form.os,
                    zone: form.zone,
                    ...(isWindows.value ? { installType: form.installType } : {}),
                    // 后端必填（0 = 无限制）；留空沿用约定默认值
                    parallelTaskCount: form.parallelTaskCount === ''
                        ? DEFAULT_PARALLEL_TASK_COUNT
                        : Number(form.parallelTaskCount),
                    // Docker 最大构建并发数：仅支持 Docker 的操作系统生效，留空沿用约定默认值
                    ...(dockerSupported.value
                        ? {
                            dockerParallelTaskCount: form.dockerParallelTaskCount === ''
                                ? DEFAULT_DOCKER_PARALLEL_TASK_COUNT
                                : Number(form.dockerParallelTaskCount),
                        }
                        : {}),
                    ...(rows.length ? { tags: rows } : {}),
                    ...(isReinstall.value && targetAgentId.value ? { targetAgentId: targetAgentId.value } : {}),
                }
            }

            /** 轮询：会话详情（拿 status/命令/过期时间）+ 节点结果（状态机列表） */
            const fetchAccessedNodes = async () => {
                if (!sessionId.value) return
                try {
                    const base = { projectId: projectId.value, sessionId: sessionId.value }
                    const [detailRes, nodesRes] = await Promise.all([
                        proxy.$store.dispatch('environment/requestInstallSessionDetail', base),
                        proxy.$store.dispatch('environment/requestInstallSessionNodes', base),
                    ])
                    // 会话已过期/撤销：停止轮询，交由命令卡片给出「重新生成」入口
                    if (detailRes?.status && detailRes.status !== SESSION_STATUS.ACTIVE) {
                        sessionStatus.value = detailRes.status
                        if (detailRes.command) commandText.value = detailRes.command
                        if (detailRes.expiredAt) sessionExpiredAt.value = formatDateTime(detailRes.expiredAt)
                        stopPolling()
                        return
                    }
                    // 安装中（INSTALLING）的节点不展示
                    accessedNodes.value = (nodesRes || []).filter((n) => n.status !== 'INSTALLING')
                    accessError.value = false
                    if (accessedNodes.value.some((n) => n.status === 'SUCCEEDED')) waitedTooLong.value = false
                } catch (err) {
                    // 查询失败：停止轮询等手动刷新，已列出的节点保留
                    accessError.value = true
                    stopPolling()
                }
            }

            const startPolling = () => {
                stopPolling()
                fetchAccessedNodes()
                pollTimer = setInterval(fetchAccessedNodes, POLL_INTERVAL)
            }

            /** 查询失败后手动恢复自动刷新；已接入的节点保持在列表里 */
            const retryAccess = () => {
                accessError.value = false
                startPolling()
            }

            /** 加载网关（接入点）列表，当前 zone 不在列表中时回落到第一个 */
            const loadGateways = async () => {
                try {
                    const res = await proxy.$store.dispatch('environment/requestGateway', {
                        projectId: projectId.value,
                        model: form.os,
                    })
                    gatewayList.value = res || []
                    if (gatewayList.value.length && !gatewayList.value.some((g) => g.zoneName === form.zone)) {
                        form.zone = gatewayList.value[0].zoneName
                    }
                } catch (err) {
                    proxy.$bkMessage({ message: err.message ? err.message : err, theme: 'error' })
                }
            }

            /** 加载项目标签 */
            const loadTags = async () => {
                try {
                    const res = await proxy.$store.dispatch('environment/requestNodeTagList', {
                        projectId: projectId.value,
                        createMode: true
                    })
                    customTags.value = res || []
                } catch (err) {
                    proxy.$bkMessage({ message: err.message ? err.message : err, theme: 'error' })
                }
            }

            /** 嵌套/扁平标签 → 统一的 {tagKeyId, tagValueId}，兼容重装上下文（扁平）与原节点快照（嵌套 tagValues） */
            const flatTagList = (tags) => (tags || [])
                .map((t) => ({
                    tagKeyId: t.tagKeyId,
                    tagValueId: t.tagValueId ?? (t.tagValues && t.tagValues[0] ? t.tagValues[0].tagValueId : undefined),
                }))
                .filter((t) => t.tagKeyId != null && t.tagValueId != null)

            /** 标签快照 → 有效表单行；只保留当前项目仍存在的标签（键 + 值都能在 customTags 中匹配到） */
            const validTagRows = (tags) => (tags || [])
                .map((t) => {
                    const key = customTags.value.find((k) => k.tagKeyId === t.tagKeyId)
                    if (!key) return null
                    return key.tagValues.some((v) => v.tagValueId === t.tagValueId)
                        ? { tagKeyId: t.tagKeyId, tagValueId: t.tagValueId }
                        : null
                })
                .filter(Boolean)

            /** 重装态：拉取重装上下文回填表单，并校验是否允许重装 */
            const loadReinstallContext = async () => {
                if (!targetAgentId.value) return
                try {
                    const res = await proxy.$store.dispatch('environment/requestReinstallContext', {
                        projectId: projectId.value,
                        agentId: targetAgentId.value,
                    })
                    if (res) {
                        canReinstall.value = res.canReinstall !== false
                        denyReason.value = res.denyReason || ''
                        if (res.os) form.os = res.os
                        if (res.zone) form.zone = res.zone
                        if (res.installType) form.installType = res.installType
                        form.parallelTaskCount = res.parallelTaskCount ?? ''
                        form.dockerParallelTaskCount = res.dockerParallelTaskCount ?? ''
                        // 仅回传用户标签；系统内置标签（os/arch 等）由后端按 Agent 自动维护，不展示、不提交
                        const userRows = validTagRows(flatTagList(res.tags))
                        form.tags = userRows.length ? userRows : [{ tagKeyId: '', tagValueId: '' }]
                    }
                } catch (err) {
                    proxy.$bkMessage({ message: err.message ? err.message : err, theme: 'error' })
                }
            }

            /** 标签变化 → 请求环境预览（防抖），重装态的变更差异由后端直接返回 */
            const requestEnvPreview = () => {
                if (envPreviewTimer) clearTimeout(envPreviewTimer)
                // 选标签/打开弹窗时立即进入 loading，避免防抖等待与请求期间展示旧数据或空态闪烁
                envPreviewLoading.value = true
                envPreviewTimer = setTimeout(async () => {
                    const rows = form.tags
                        .filter((t) => t.tagKeyId && t.tagValueId)
                        .map(({ tagKeyId, tagValueId }) => ({ tagKeyId, tagValueId }))
                    if (!rows.length && !isReinstall.value) {
                        envPreviewData.value = { mode: 'import', associated: [], matched: [], pending: [] }
                        envPreviewLoading.value = false
                        return
                    }
                    try {
                        const res = await proxy.$store.dispatch('environment/requestInstallSessionPreview', {
                            projectId: projectId.value,
                            params: buildSessionRequest(),
                        })
                        // 兼容个别拦截器未解包 data 层的情形（正常解包时 res.data 为 undefined）
                        const payload = res?.data ?? res
                        const envs = payload?.environments || {}
                        console.log('[ImportThirdPartyDialog] preview res:', res, '=> envs:', envs)
                        if (isReinstall.value) {
                            envPreviewData.value = {
                                mode: 'reinstall',
                                associated: envs.associated || [],
                                willJoin: envs.willJoin || [],
                                willLeave: envs.willLeave || [],
                                pending: envs.pending || [],
                            }
                        } else {
                            // 后端 FIRST_IMPORT 也统一返回 associated/willJoin/willLeave/pending 结构
                            // （联调确认；对接文档写的 matchedEnvironments/pendingEnvironments 未落地，
                            //  保留 ?? 回退兼容；重复导入同一台机器时 associated 可能有值）
                            envPreviewData.value = {
                                mode: 'import',
                                associated: envs.associated || [],
                                matched: envs.matchedEnvironments ?? envs.willJoin ?? [],
                                pending: envs.pendingEnvironments ?? envs.pending ?? [],
                            }
                        }
                        console.log('[ImportThirdPartyDialog] preview mapped:', envPreviewData.value)
                    } catch (err) {
                        // 保留现场便于联调：环境预览失败不阻断表单，但需在控制台可见
                        console.warn('[ImportThirdPartyDialog] env preview failed:', err)
                        envPreviewData.value = null
                    } finally {
                        envPreviewLoading.value = false
                    }
                }, ENV_PREVIEW_DEBOUNCE)
            }

            /**
             * 确认配置 → 创建/复用安装会话。同一份配置在有效期内拿到的是同一条命令（reused），
             * 它此前接入的节点原样带出来，不清零
             */
            const generateCmd = async () => {
                if (isReinstall.value && !canReinstall.value) return
                // 生成命令前先校验标签：存在「有键无值 / 有值无键」的不完整行时，
                // 提示用户补全并阻止后续逻辑，待检查通过才放行
                const hasIncompleteTag = form.tags.some(
                    (t) => (t.tagKeyId && !t.tagValueId) || (!t.tagKeyId && t.tagValueId)
                )
                if (hasIncompleteTag) {
                    proxy.$bkMessage({
                        theme: 'warning',
                        message: proxy.$t('environment.installSession.tagIncomplete')
                    })
                    return
                }
                generating.value = true
                try {
                    const res = await proxy.$store.dispatch('environment/requestCreateInstallSession', {
                        projectId: projectId.value,
                        params: buildSessionRequest(),
                    })
                    commandText.value = res?.command || ''
                    sessionId.value = res?.sessionId || ''
                    sessionStatus.value = SESSION_STATUS.ACTIVE
                    sessionExpiredAt.value = formatDateTime(res?.expiredAt)
                    accessedNodes.value = []
                    waitedTooLong.value = false
                    accessError.value = false
                    step.value = 2
                    if (res?.reused) {
                        proxy.$bkMessage({
                            theme: 'primary',
                            message: proxy.$t('environment.installSession.reusedTip')
                        })
                    }
                    startPolling()
                    timers.push(
                        setTimeout(() => {
                            if (!accessedNodes.value.length) waitedTooLong.value = true
                        }, WAIT_TOO_LONG)
                    )
                } catch (err) {
                    proxy.$bkMessage({ message: err.message ? err.message : err, theme: 'error' })
                } finally {
                    generating.value = false
                }
            }

            /** 会话过期后重新生成：拿到新会话（新 sessionId），恢复命令与轮询 */
            const handleRegenerate = async () => {
                if (!sessionId.value || regenerating.value) return
                regenerating.value = true
                try {
                    const res = await proxy.$store.dispatch('environment/requestRegenerateInstallSession', {
                        projectId: projectId.value,
                        sessionId: sessionId.value,
                    })
                    commandText.value = res?.command || commandText.value
                    sessionId.value = res?.sessionId || sessionId.value
                    sessionStatus.value = SESSION_STATUS.ACTIVE
                    sessionExpiredAt.value = formatDateTime(res?.expiredAt)
                    proxy.$bkMessage({ theme: 'success', message: proxy.$t('environment.installSession.regeneratedTip') })
                    startPolling()
                } catch (err) {
                    proxy.$bkMessage({ message: err.message ? err.message : err, theme: 'error' })
                } finally {
                    regenerating.value = false
                }
            }

            /** 回第一步改配置。改回原配置会由后端复用原会话，所以这里不销毁任何记录 */
            const backToConfig = () => {
                clearTimers()
                waitedTooLong.value = false
                accessError.value = false
                step.value = 1
            }

            watch(
                () => form.os,
                () => {
                    if (!dockerSupported.value) form.dockerParallelTaskCount = ''
                    // 网关列表与 OS 相关；重装态 OS 锁定，不重复拉取
                    if (!isReinstall.value && props.isShow) loadGateways()
                }
            )

            // 只监听「有效标签」组合：增删空行、选键未选值等不改变查询结果的操作不重新预览
            watch(pickedTagSignature, () => requestEnvPreview())

            watch(
                () => props.isShow,
                (v) => {
                    if (!v) {
                        clearTimers()
                        return
                    }
                    form.os = isReinstall.value ? osOf(props.node) : (props.defaultOs || 'LINUX')
                    form.zone = 'SHENZHEN'
                    form.installType = 'SERVICE'
                    waitedTooLong.value = false
                    accessedNodes.value = []
                    accessError.value = false
                    step.value = 1
                    canReinstall.value = true
                    denyReason.value = ''
                    sessionStatus.value = SESSION_STATUS.ACTIVE
                    form.parallelTaskCount = ''
                    form.dockerParallelTaskCount = ''
                    form.tags = [{ tagKeyId: '', tagValueId: '' }]
                    envPreviewData.value = null
                    envPreviewLoading.value = false
                    commandText.value = ''
                    sessionId.value = ''
                    sessionExpiredAt.value = ''
                    const init = async () => {
                        await loadGateways()
                        await loadTags()
                        if (isReinstall.value) await loadReinstallContext()
                        requestEnvPreview()
                    }
                    init()
                }
            )

            onBeforeUnmount(clearTimers)

            const close = () => emit('update:isShow', false)
            /** bk-dialog 关闭（X / 遮罩 / esc）时同步给父组件 */
            const onValueChange = (v) => {
                if (!v) close()
            }
            const copyCmd = async () => {
                const text = installCommand.value
                try {
                    if (navigator.clipboard && window.isSecureContext) {
                        await navigator.clipboard.writeText(text)
                    } else {
                        // 非安全上下文（http）下 clipboard API 不可用，退回 execCommand
                        const textarea = document.createElement('textarea')
                        textarea.value = text
                        textarea.style.position = 'fixed'
                        textarea.style.opacity = '0'
                        document.body.appendChild(textarea)
                        textarea.select()
                        document.execCommand('copy')
                        document.body.removeChild(textarea)
                    }
                    proxy.$bkMessage({ theme: 'success', message: proxy.$t('environment.installSession.copiedTip') })
                } catch (err) {
                    proxy.$bkMessage({ theme: 'warning', message: proxy.$t('environment.installSession.copyFailTip') })
                }
            }
            const openDoc = () => {
                // BKCI_DOCS 挂在 Vue.prototype 上（非 $ 前缀全局属性），
                // composition 的 instance.proxy 不会转发，需直接读 Vue.prototype
                const url = Vue.prototype.BKCI_DOCS?.ENV_FAQ_DOC
                window.open(url, '_blank')
            }

            return {
                form,
                title,
                isReinstall,
                isWindows,
                dockerSupported,
                defaults,
                gatewayList,
                installTypeList: INSTALL_TYPE_LIST,
                expireTime,
                customTags,
                envPreview,
                showEnvPreview,
                envPreviewLoading,
                installCommand,
                step,
                generating,
                regenerating,
                canReinstall,
                denyReason,
                sessionExpired,
                summaryItems,
                generateCmd,
                handleRegenerate,
                backToConfig,
                accessedNodes,
                waitedTooLong,
                accessError,
                hasStalled,
                retryAccess,
                close,
                onValueChange,
                copyCmd,
                openDoc,
            }
        },
    }
</script>

<style lang="scss" scoped>
    .step2-footer {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 16px;
        width: 100%;
    }
    .footer-hint {
        font-size: 12px;
        line-height: 20px;
        color: #979ba5;
    }
</style>

<!-- ext-cls 挂在 .bk-dialog-wrapper 上（bk-magic-vue），所以这里用全局样式。
     弹窗高度自适应视口：标签行不断新增、环境预览变长时超出部分由 body 内部滚动，
     不再出现「顶部间距很大、底部没空间」的情况 -->
<style lang="scss">
.import-third-party-dialog {
    .bk-dialog {
        top: 12% !important;
    }
    .bk-dialog-content {
        display: flex;
        flex-direction: column;
        /* 上下各留 40px，超高时内部滚动 */
        max-height: calc(100vh - 80px);
    }
    .bk-dialog-tool,
    .bk-dialog-header,
    .bk-dialog-footer {
        flex-shrink: 0;
    }
    .bk-dialog-body {
        flex: 1;
        min-height: 0;
        overflow-y: auto;
    }
}
</style>

<!-- popover 挂到 body，scoped 够不着。
     devops-environment 走 bk-magic-vue（基于 tippy），bk-popover 实际渲染的
     DOM 是 .tippy-popper（挂 ext-cls）+ .tippy-tooltip，不是 bkui-vue 的 .bk-popover.bk-pop2-content；
     旧选择器命中不到，所以弹窗走的是 .tippy-tooltip.light-theme（白底黑字）。
     选 .tippy-tooltip 并对 light-theme 也覆盖，统一回项目惯例的 #333 深底白字 -->
<style>
.import-form-tip .tippy-tooltip,
.import-form-tip .tippy-tooltip.light-theme {
  padding: 7px 14px;
  background: #333;
  color: #fff;
  text-align: left;
}
.import-form-tip .tippy-tooltip .tippy-arrow,
.import-form-tip[x-placement^="top"] .tippy-arrow {
  border-top-color: #333;
}
.import-form-tip[x-placement^="bottom"] .tippy-arrow {
  border-bottom-color: #333;
}
.import-form-tip[x-placement^="left"] .tippy-arrow {
  border-left-color: #333;
}
.import-form-tip[x-placement^="right"] .tippy-arrow {
  border-right-color: #333;
}
.import-form-tip .tippy-tooltip .tippy-content {
  color: #fff;
}
.import-form-tip .command-tip-list {
  margin: 0;
  padding-left: 0px;
  text-align: left;
  list-style-type: decimal;
  list-style-position: outside;
  font-size: 12px;
  line-height: 20px;
  color: #fff;
}
.import-form-tip .command-tip-list li + li {
  margin-top: 4px;
}
</style>
