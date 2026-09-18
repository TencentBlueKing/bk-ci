<template>
    <div class="access-section">
        <!--
      不叫「本次接入」：会话按配置复用，这里展示的是这条命令历来接入的全部节点，
      可能包含用户上次打开弹窗时装的那批
    -->
        <p class="form-block-title">
            {{ $t('environment.installSession.accessedTitle') }}
            <!-- 标题旁只报数字；有节点时才出现。不写「共 N 个」：同屏还可能报未接入的构建机 -->
            <bk-tag
                v-if="nodes.length"
                theme="info"
                effect="stroke"
            >
                {{ nodes.length }}
            </bk-tag>
            <bk-popover
                placement="top"
                :max-width="360"
                ext-cls="import-form-tip"
            >
                <i class="devops-icon icon-info-circle hint-icon"></i>
                <template #content>
                    <ol class="command-tip-list">
                        <li>{{ $t('environment.installSession.accessedTip1') }}</li>
                        <li>{{ $t('environment.installSession.accessedTip2') }}</li>
                    </ol>
                </template>
            </bk-popover>
            <!-- 查询失败才在标题栏右侧提示；刷新恢复自动查询，已列出的节点保留 -->
            <span
                v-if="accessError"
                class="access-refresh-fail"
            >
                {{ $t('environment.installSession.autoFetchFail') }}
                <bk-popover
                    placement="top"
                    ext-cls="import-form-tip"
                >
                    <bk-button
                        text
                        class="access-refresh-btn"
                        @click="$emit('retry')"
                    >
                        <i class="devops-icon icon-refresh"></i>
                    </bk-button>
                    <template #content>{{ $t('environment.installSession.manualRefresh') }}</template>
                </bk-popover>
            </span>
        </p>

        <div
            v-if="nodes.length"
            class="access-list"
        >
            <!-- 状态列常在：安装进度是每行都要看的信息，不是只看异常 -->
            <div
                v-for="n in nodes"
                :key="n.agentId"
                class="access-row"
            >
                <span
                    class="status-dot"
                    :class="dotClass(n.status)"
                />
                <span class="access-name">
                    {{ n.hostname || n.agentId }}
                    <span
                        v-if="n.ip"
                        class="access-ip"
                    >（{{ n.ip }}）</span>
                </span>
                <span class="access-meta is-time">{{ nodeTime(n) }}</span>
                <span
                    class="access-state"
                    :class="{ 'is-error': n.status === 'FAILED', 'is-doing': isDoing(n.status) }"
                    v-bk-overflow-tips="{ content: stateText(n), disabled: !n.errorMessage }"
                >
                    {{ stateText(n) }}
                </span>
                <span
                    v-if="n.agentVersion"
                    class="access-meta is-version"
                >Agent {{ n.agentVersion }}</span>
                <a
                    v-if="n.nodeId"
                    :href="nodeHref(n)"
                    target="_blank"
                    class="node-link"
                >
                    {{ $t('environment.installSession.viewNode') }}
                    <i class="devops-icon icon-link node-link-icon"></i>
                </a>
            </div>
        </div>
        <div
            v-else
            class="access-empty"
        >
            <!-- 三态互斥：初态等待 → 超时仍无接入 → 有下载记录时改由下方 access-aside 提示 -->
            <p
                v-if="!waitedTooLong"
                class="empty-hint"
            >
                {{ $t('environment.installSession.waitAccess') }}
            </p>
            <p
                v-else-if="!hasStalled"
                class="empty-hint"
            >
                {{ $t('environment.installSession.noAccessYet') }}
                <bk-button
                    text
                    theme="primary"
                    @click="$emit('open-doc')"
                >
                    {{ $t('environment.installSession.troubleshoot') }}
                </bk-button>
            </p>
        </div>

        <!--
      列表之外才报：已下载脚本、尚未接入（PENDING / 安装中 / 导入中）。只报有无，不报台数。
      不放列表上方——顺利接入时模块第一眼必须是列表；也不贴进表体最后一行——
      那行常是失败节点，两条提示会黏成一件事。
      文案不写「异常节点」：这里的提示专指安装未完成，主语与列表里的失败不同。
    -->
        <p
            v-if="hasStalled"
            class="access-aside"
        >
            <i class="devops-icon icon-exclamation-circle-shape"></i>
            <span>{{ $t('environment.installSession.stalledAside') }}</span>
            <bk-button
                text
                theme="primary"
                @click="$emit('open-doc')"
            >
                {{ $t('environment.installSession.troubleshoot') }}
            </bk-button>
        </p>
    </div>
</template>

<script>
    import { SESSION_NODE_STATUS_I18N, formatDateTime } from './constants'

    /**
     * 导入第三方构建机弹窗 · 第二步：当前安装会话下的节点结果列表。
     * 节点来自后端 installSessions/{sessionId}/nodes，带安装状态机
     * （PENDING / INSTALLING / IMPORTING / SUCCEEDED / FAILED）与失败原因。
     * 列表数据与轮询节奏由父组件维护，本组件只负责展示与重试 / 排查指引的透传。
     */
    export default {
        name: 'AccessedNodeList',
        props: {
            /** AgentInstallSessionNodeInfo[]：{ agentId, nodeId, status, hostname, ip, errorMessage, startedAt, finishedAt, agentVersion } */
            nodes: {
                type: Array,
                default: () => []
            },
            /** 超过阈值仍无节点接入 */
            waitedTooLong: {
                type: Boolean,
                default: false
            },
            /** 存在"已下载脚本、尚未完成接入"（PENDING / INSTALLING / IMPORTING）的节点 */
            hasStalled: {
                type: Boolean,
                default: false
            },
            /** 自动查询接入节点失败 */
            accessError: {
                type: Boolean,
                default: false
            }
        },
        methods: {
            stateText (n) {
                const key = SESSION_NODE_STATUS_I18N[n.status]
                const base = key ? this.$t(key) : (n.status || '')
                return n.status === 'FAILED' && n.errorMessage
                    ? this.$t('environment.installSession.failedWithReason', { status: base, reason: n.errorMessage })
                    : base
            },
            /** 未完成态：等待启动 / 安装中 / 导入中 */
            isDoing (status) {
                return ['PENDING', 'INSTALLING', 'IMPORTING'].includes(status)
            },
            dotClass (status) {
                return {
                    'is-error': status === 'FAILED',
                    'is-doing': this.isDoing(status)
                }
            },
            nodeTime (n) {
                return formatDateTime(n.finishedAt || n.startedAt)
            },
            /** 新标签页打开，弹窗留在原地继续观察其余构建机接入 */
            nodeHref (n) {
                return this.$router.resolve({
                    name: 'nodeList',
                    params: this.$route.params,
                    query: { nodeHashId: n.nodeId, tabName: 'overview' }
                }).href
            }
        }
    }
</script>

<style lang="scss" scoped>
    /* 区块标题栏（与 ImportConfigForm 的同款式样；间距规则改为组件内自管） */
    .form-block-title {
        display: flex;
        align-items: center;
        gap: 8px;
        box-sizing: border-box;
        /* 上方是命令卡片（原 24px 区块间距），下方紧跟列表（8px） */
        margin: 24px 0 8px;
        padding: 0 16px;
        border-radius: 2px;
        background: #f0f1f5;
        font-size: 14px;
        font-weight: 400;
        line-height: 30px;
        color: #313238;
    }
    .hint-icon {
        flex: none;
        font-size: 14px;
        color: #979ba5;
        cursor: pointer;
    }
    .empty-hint {
        margin: 0;
        font-size: 12px;
        color: #63656e;
        line-height: 20px;
    }
    .access-list {
        /* 持续追加的实时列表不做分页：翻页会被新进来的行打乱，超高改内部滚动 */
        max-height: 216px;
        overflow: auto;
        border: 1px solid #dcdee5;
        border-radius: 2px;
    }
    /* 列表下方的次级提示：与表体留出间距、浅底，避免被读成最后一行 */
    .access-aside {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-top: 8px;
        padding: 6px 12px;
        background: #fff8e6;
        border-radius: 2px;
        font-size: 12px;
        line-height: 20px;
        color: #63656e;

        .devops-icon {
            flex: none;
            font-size: 14px;
            color: #ff9c01;
        }
    }
    .access-refresh-fail {
        display: inline-flex;
        align-items: center;
        gap: 16px;
        margin-left: auto;
        font-size: 12px;
        line-height: 20px;
        color: #ea3636;
    }
    .access-refresh-btn {
        min-width: auto;
        padding: 0;
        height: auto;
        color: #3a84ff;

        .devops-icon {
            font-size: 14px;
        }
    }
    .access-empty {
        padding: 16px;
        background: #fff;
        border: 1px dashed #dcdee5;
        border-radius: 2px;
    }
    .access-row {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 0 16px;
        font-size: 12px;
        color: #313238;
        line-height: 36px;

        & + .access-row {
            border-top: 1px solid #f0f1f5;
        }
    }
    /* 各列定宽，状态文字吃掉剩余空间，「查看节点」因此在每一行都落在同一个横坐标上 */
    .access-name {
        min-width: 200px;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
    }
    .access-ip {
        color: #979ba5;
    }
    .access-meta {
        color: #979ba5;
        white-space: nowrap;

        &.is-time {
            min-width: 132px;
        }

        &.is-version {
            flex: none;
        }
    }
    .status-dot {
        flex: none;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #2dcb56;

        &.is-doing {
            background: #3a84ff;
        }

        &.is-error {
            background: #ea3636;
        }
    }
    .access-state {
        flex: 1;
        min-width: 0;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        color: #63656e;

        &.is-doing {
            color: #3a84ff;
        }

        &.is-error {
            color: #ea3636;
        }
    }
    .node-link {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        flex: none;
        color: #3a84ff;
    }
    .node-link-icon {
        font-size: 12px;
    }
</style>
