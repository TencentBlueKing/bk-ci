<template>
    <bk-dialog
        :value="visible"
        :mask-close="false"
        :esc-close="!confirmLoading"
        :close-icon="!confirmLoading"
        :show-footer="false"
        :title="''"
        width="450"
        ext-cls="node-action-confirm-dialog"
        @value-change="handleValueChange"
        @cancel="handleCancel"
    >
        <div
            class="confirm-body"
            v-bkloading="{ isLoading: relationsLoading }"
        >
            <!-- 图标 + 标题：居中（对齐 InfoBox warning） -->
            <div class="confirm-header">
                <div
                    class="warning-icon"
                    aria-hidden="true"
                >
                    !
                </div>
                <h3 class="dialog-title">{{ dialogTitle }}</h3>
            </div>

            <!-- 正文：左对齐（对齐原型 contentAlign: left） -->
            <div class="confirm-content">
                <div class="node-name">
                    {{ node?.displayName || node?.name || '--' }}
                </div>

                <div class="impact-primary">
                    <p class="confirm-text">
                        {{ envDescText }}
                    </p>
                    <div
                        v-if="relations.environments.length"
                        class="impact-links"
                    >
                        <a
                            v-for="env in relations.environments"
                            :key="env.envHashId || env.envName"
                            class="impact-link"
                            :href="env.url || 'javascript:;'"
                            target="_blank"
                            rel="noopener noreferrer"
                            @click="openLink(env.url, $event)"
                        >
                            <span class="link-name">{{ env.envName }}</span>
                            <svg
                                v-if="env.url"
                                class="jump-icon"
                                viewBox="0 0 1024 1024"
                                aria-hidden="true"
                            >
                                <path d="M864 128H496v64h291.2L361.6 617.6l44.8 44.8L832 236.8V528h64V160c0-17.6-14.4-32-32-32z" />
                                <path d="M832 704v128H192V192h126.4V128H160c-17.6 0-32 14.4-32 32v704c0 17.6 14.4 32 32 32h704c17.6 0 32-14.4 32-32V704h-64z" />
                            </svg>
                        </a>
                    </div>
                </div>

                <div class="impact-secondary">
                    <p class="confirm-text">
                        {{ referenceDescText }}
                    </p>
                    <div
                        v-if="relations.references.length"
                        class="impact-links"
                    >
                        <a
                            v-for="refItem in relations.references"
                            :key="refItem.id || refItem.name"
                            class="impact-link"
                            :href="refItem.url || 'javascript:;'"
                            target="_blank"
                            rel="noopener noreferrer"
                            @click="openLink(refItem.url, $event)"
                        >
                            <span class="link-name">{{ refItem.name }}</span>
                            <svg
                                v-if="refItem.url"
                                class="jump-icon"
                                viewBox="0 0 1024 1024"
                                aria-hidden="true"
                            >
                                <path d="M864 128H496v64h291.2L361.6 617.6l44.8 44.8L832 236.8V528h64V160c0-17.6-14.4-32-32-32z" />
                                <path d="M832 704v128H192V192h126.4V128H160c-17.6 0-32 14.4-32 32v704c0 17.6 14.4 32 32 32h704c17.6 0 32-14.4 32-32V704h-64z" />
                            </svg>
                        </a>
                    </div>
                </div>
            </div>

            <!-- 按钮放进 body 居中，避开 bk-dialog footer 默认右浮动 -->
            <div class="dialog-footer-actions">
                <bk-button
                    theme="primary"
                    :loading="confirmLoading"
                    :disabled="relationsLoading"
                    @click="handleConfirm"
                >
                    {{ confirmBtnText }}
                </bk-button>
                <bk-button
                    :disabled="confirmLoading"
                    @click="handleCancel"
                >
                    {{ $t('environment.cancel') }}
                </bk-button>
            </div>
        </div>
    </bk-dialog>
</template>

<script>
    import useResourceType from '@/hooks/useResourceType'

    export default {
        name: 'NodeActionConfirmDialog',
        props: {
            visible: {
                type: Boolean,
                default: false
            },
            /** delete | clean */
            mode: {
                type: String,
                default: 'delete',
                validator: v => ['delete', 'clean'].includes(v)
            },
            node: {
                type: Object,
                default: () => ({})
            },
            relations: {
                type: Object,
                default: () => ({
                    environments: [],
                    references: []
                })
            },
            relationsLoading: {
                type: Boolean,
                default: false
            },
            confirmLoading: {
                type: Boolean,
                default: false
            }
        },
        setup () {
            const { isCloudDesktop } = useResourceType()
            return { isCloudDesktop }
        },
        computed: {
            dialogTitle () {
                return this.mode === 'clean'
                    ? this.$t('environment.confirmCleanNode')
                    : this.$t('environment.confirmDeleteNode')
            },
            confirmBtnText () {
                return this.mode === 'clean'
                    ? this.$t('environment.clean')
                    : this.$t('environment.delete')
            },
            envDescText () {
                const count = this.relations.environments?.length || 0
                if (!count) {
                    return this.$t('environment.confirmEnvEmpty')
                }
                return this.mode === 'clean'
                    ? this.$t('environment.confirmEnvBelongClean', [count])
                    : this.$t('environment.confirmEnvBelongDelete', [count])
            },
            referenceDescText () {
                const count = this.relations.references?.length || 0
                if (this.mode === 'clean') {
                    if (!count) {
                        return this.$t('environment.confirmCreateFlowEmpty')
                    }
                    return this.isCloudDesktop(this.node)
                        ? this.$t('environment.confirmCreateFlowUsingDesktop', [count])
                        : this.$t('environment.confirmCreateFlowUsing', [count])
                }
                if (!count) {
                    return this.$t('environment.confirmPipelineEmpty')
                }
                return this.$t('environment.confirmPipelineUsing', [count])
            }
        },
        methods: {
            openLink (url, event) {
                if (!url) {
                    event.preventDefault()
                }
            },
            handleConfirm () {
                this.$emit('confirm')
            },
            handleCancel () {
                if (this.confirmLoading) return
                this.$emit('cancel')
            },
            handleValueChange (val) {
                if (!val && !this.confirmLoading) {
                    this.$emit('cancel')
                }
            }
        }
    }
</script>

<style lang="scss">
.node-action-confirm-dialog {
    .bk-dialog-header {
        display: none;
    }

    .bk-dialog-body {
        padding: 24px 32px 24px;
    }

    .bk-dialog-footer {
        display: none !important;
    }

    .confirm-body {
        min-height: 80px;
        font-size: 12px;
        line-height: 20px;
        color: #63656E;
    }

    .confirm-header {
        text-align: center;
        margin-bottom: 16px;
    }

    // 对齐 InfoBox warning：浅橙底 + 深橙感叹号
    .warning-icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 42px;
        height: 42px;
        margin-bottom: 16px;
        border-radius: 50%;
        background: #FFE8C3;
        color: #FF9C01;
        font-size: 26px;
        font-weight: 700;
        line-height: 1;
        font-style: normal;
    }

    .dialog-title {
        margin: 0;
        font-size: 20px;
        font-weight: 400;
        line-height: 28px;
        color: #313238;
    }

    .confirm-content {
        text-align: left;
    }

    .node-name {
        margin: 0 0 8px;
        font-size: 12px;
        line-height: 20px;
        color: #313238;
        word-break: break-all;
    }

    .impact-primary {
        margin-top: 0;
        background: #F5F7FA;
        border-radius: 2px;
        padding: 8px 16px;
    }

    .impact-secondary {
        margin-top: 12px;

        .confirm-text {
            color: #979BA5;
        }
    }

    .confirm-text {
        margin: 0 0 8px;
        font-size: 12px;
        line-height: 20px;
        color: #63656E;
        word-break: keep-all;

        &:last-child {
            margin-bottom: 0;
        }
    }

    .impact-links {
        display: flex;
        flex-wrap: wrap;
        align-content: flex-start;
        gap: 8px 16px;
        max-height: 120px;
        overflow-y: auto;
        min-width: 0;
    }

    .impact-link {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        max-width: 100%;
        color: #3A84FF;
        text-decoration: none;
        line-height: 20px;
        cursor: pointer;

        &:hover {
            color: #699DF4;
        }

        .link-name {
            min-width: 0;
            word-break: break-all;
        }

        .jump-icon {
            width: 12px;
            height: 12px;
            display: block;
            fill: currentColor;
            flex: none;
        }
    }

    .dialog-footer-actions {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        margin-top: 24px;

        .bk-button {
            float: none !important;
            margin: 0 !important;
            min-width: 88px;
        }
    }
}
</style>
