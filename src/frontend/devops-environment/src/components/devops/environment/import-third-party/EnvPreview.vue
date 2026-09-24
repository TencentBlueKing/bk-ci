<template>
    <div
        class="env-preview"
        v-bkloading="{ isLoading: loading, opacity: 0.9 }"
    >
        <p class="env-preview-title">{{ $t('environment.installSession.envPreview') }}</p>

        <!-- 导入态：新机一般无已关联，重复导入同一台机器时可能已关联环境 -->
        <template v-if="preview.mode === 'import'">
            <template v-if="preview.associated.length || preview.matched.length || preview.pending.length">
                <div
                    v-if="preview.associated.length"
                    class="env-preview-group"
                >
                    <span class="env-preview-label">{{ $t('environment.installSession.envAssociated') }}</span>
                    <span class="env-chips">
                        <bk-tag
                            v-for="n in preview.associated"
                            :key="'assoc-' + n.envId"
                            effect="stroke"
                        >{{ n.envName }}</bk-tag>
                    </span>
                </div>
                <div
                    v-if="preview.matched.length"
                    class="env-preview-group"
                >
                    <span class="env-preview-label">{{ $t('environment.installSession.envWillJoin') }}</span>
                    <span class="env-chips">
                        <bk-tag
                            v-for="n in preview.matched"
                            :key="n.envId"
                            theme="success"
                            effect="stroke"
                        >{{ n.envName }}</bk-tag>
                    </span>
                </div>
                <div
                    v-if="preview.pending.length"
                    class="env-preview-group"
                >
                    <span class="env-preview-label">
                        {{ $t('environment.installSession.envMayJoin') }}
                        <span class="env-preview-note">{{ $t('environment.installSession.envMayJoinNote') }}</span>
                    </span>
                    <span class="env-chips">
                        <bk-tag
                            v-for="n in preview.pending"
                            :key="n.envId"
                            theme="info"
                            effect="stroke"
                        >{{ n.envName }}</bk-tag>
                    </span>
                </div>
            </template>
            <p
                v-else
                class="env-preview-line is-empty"
            >
                <i class="devops-icon icon-exclamation-circle-shape"></i>
                <span>{{ $t('environment.installSession.envEmpty') }}</span>
            </p>
        </template>

        <!-- 重装态：已关联 / 将加入 / 将从移除 / 可能加入（差异由后端计算） -->
        <template v-else>
            <template v-if="hasReinstallGroup">
                <div
                    v-if="preview.associated.length"
                    class="env-preview-group"
                >
                    <span class="env-preview-label">{{ $t('environment.installSession.envAssociated') }}</span>
                    <span class="env-chips">
                        <bk-tag
                            v-for="n in preview.associated"
                            :key="'stay-' + n.envId"
                            effect="stroke"
                        >{{ n.envName }}</bk-tag>
                    </span>
                </div>
                <div
                    v-if="preview.willJoin.length"
                    class="env-preview-group"
                >
                    <span class="env-preview-label">{{ $t('environment.installSession.envJoin') }}</span>
                    <span class="env-chips">
                        <bk-tag
                            v-for="n in preview.willJoin"
                            :key="'join-' + n.envId"
                            theme="success"
                            effect="stroke"
                        >{{ n.envName }}</bk-tag>
                    </span>
                </div>
                <div
                    v-if="preview.willLeave.length"
                    class="env-preview-group"
                >
                    <span class="env-preview-label">{{ $t('environment.installSession.envLeave') }}</span>
                    <span class="env-chips">
                        <bk-tag
                            v-for="n in preview.willLeave"
                            :key="'leave-' + n.envId"
                            theme="danger"
                            effect="stroke"
                        >{{ n.envName }}</bk-tag>
                    </span>
                </div>
                <div
                    v-if="preview.pending.length"
                    class="env-preview-group"
                >
                    <span class="env-preview-label">
                        {{ $t('environment.installSession.envMayJoin') }}
                        <span class="env-preview-note">{{ $t('environment.installSession.envMayJoinNote') }}</span>
                    </span>
                    <span class="env-chips">
                        <bk-tag
                            v-for="n in preview.pending"
                            :key="'pend-' + n.envId"
                            theme="info"
                            effect="stroke"
                        >{{ n.envName }}</bk-tag>
                    </span>
                </div>
            </template>
            <p
                v-else
                class="env-preview-line is-empty"
            >
                <i class="devops-icon icon-exclamation-circle-shape"></i>
                <span>{{ $t('environment.installSession.envEmpty') }}</span>
            </p>
        </template>
    </div>
</template>

<script>
    export default {
        name: 'EnvPreview',
        props: {
            /**
             * { mode: 'import', associated: [], matched: [], pending: [] }
             * 或 { mode: 'reinstall', associated: [], willJoin: [], willLeave: [], pending: [] }
             * 数组元素均为 { envId, envName }
             */
            preview: {
                type: Object,
                required: true
            },
            /** 加载中（防抖等待 + 请求返回前） */
            loading: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            /** 重装态任一组有值即展示分组，全空走空态文案 */
            hasReinstallGroup () {
                const p = this.preview
                return p.associated.length > 0
                    || p.willJoin.length > 0
                    || p.willLeave.length > 0
                    || p.pending.length > 0
            }
        }
    }
</script>

<style lang="scss" scoped>
    /* 左边与标签控件对齐；右边再缩进 label-width，使相对 dialog 左右外边距相等 */
    .env-preview {
        margin-top: 12px;
        margin-right: 180px;
        padding: 8px 16px 12px;
        border-radius: 2px;
        background: #f5f7fa;
    }
    .env-preview-title {
        margin: 0 0 8px;
        border-bottom: 1px solid #dcdee5;
        text-align: left;
        font-size: 12px;
        font-weight: 700;
        line-height: 22px;
        color: #63656e;
    }
    .env-preview-group {
        margin-bottom: 10px;

        &:last-child {
            margin-bottom: 0;
        }
    }
    .env-preview-label {
        display: block;
        margin-bottom: 0;
        font-size: 12px;
        color: #313238;
    }
    .env-chips {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
    }
    .env-preview-note {
        margin-left: 4px;
        font-size: 12px;
        font-weight: 400;
        color: #c4c6cc;
    }
    .env-preview-line.is-empty {
        display: flex;
        align-items: flex-start;
        gap: 6px;
        font-size: 12px;
        line-height: 18px;
        color: #63656e;

        .devops-icon {
            flex: none;
            margin-top: 2px;
            font-size: 14px;
            color: #ff9c01;
        }
    }
</style>
