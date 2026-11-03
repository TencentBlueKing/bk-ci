<template>
    <bk-dialog
        :value="value"
        :width="480"
        :mask-close="false"
        :auto-close="false"
        :show-footer="true"
        header-position="left"
        ext-cls="batch-container-limit-dialog"
        :title="$t('environment.batchEditContainerLimit')"
        @cancel="close"
        @input="onInput"
    >
        <div class="limit-batch">
            <button
                v-if="targets.length > 8"
                type="button"
                class="limit-batch-toggle"
                @click="showTargets = !showTargets"
            >
                <span>{{ $t('environment.willModifyNodes', [targets.length]) }}</span>
                <i :class="['bk-icon', showTargets ? 'icon-angle-up' : 'icon-angle-down']"></i>
            </button>
            <p
                v-else
                class="limit-batch-head"
            >
                {{ $t('environment.willModifyNodes', [targets.length]) }}
            </p>
            <ul
                v-show="targets.length <= 8 || showTargets"
                :class="['limit-batch-nodes', { 'is-scroll-8': targets.length > 8 }]"
            >
                <li
                    v-for="node in targets"
                    :key="node.nodeHashId"
                >
                    {{ nodeLabel(node) }}
                </li>
            </ul>

            <template v-if="skipped.length">
                <button
                    type="button"
                    class="limit-batch-toggle is-follow"
                    @click="showSkipped = !showSkipped"
                >
                    <span>{{ $t('environment.skipModifyNodes', [skipped.length]) }}</span>
                    <i :class="['bk-icon', showSkipped ? 'icon-angle-up' : 'icon-angle-down']"></i>
                </button>
                <p class="limit-batch-reason">{{ $t('environment.containerLimitLinuxOnly') }}</p>
                <ul
                    v-show="showSkipped"
                    class="limit-batch-nodes is-scroll-4"
                >
                    <li
                        v-for="node in skipped"
                        :key="node.nodeHashId"
                    >
                        {{ nodeLabel(node) }}
                    </li>
                </ul>
            </template>

            <p
                v-if="noEdit"
                class="limit-batch-perm"
            >
                {{ $t('environment.ignoredNoEditPermission', [noEdit]) }}
            </p>

            <div class="limit-batch-field">
                <p class="field-label">{{ $t('environment.containerCpuDefault') }}</p>
                <div class="limit-batch-row">
                    <bk-select
                        v-model="cpuMode"
                        class="limit-batch-select"
                        :clearable="false"
                    >
                        <bk-option
                            id="keep"
                            :name="$t('environment.保持不变')"
                        />
                        <bk-option
                            id="unlimited"
                            :name="$t('environment.concurrencyUnlimited')"
                        />
                        <bk-option
                            id="value"
                            :name="$t('environment.specifyValue')"
                        />
                    </bk-select>
                    <bk-input
                        v-if="cpuMode === 'value'"
                        v-model="cpuValue"
                        class="limit-batch-input"
                        :placeholder="$t('environment.exampleValue')"
                    />
                    <span
                        v-if="cpuMode === 'value'"
                        class="limit-suffix"
                    >{{ $t('environment.cpuCore') }}</span>
                </div>
                <p
                    v-if="cpuMode === 'value'"
                    class="limit-batch-hint"
                >
                    {{ $t('environment.cpuBatchHint') }}
                </p>
                <p
                    v-if="cpuError"
                    class="limit-batch-error"
                >
                    {{ cpuError }}
                </p>
            </div>

            <div class="limit-batch-field">
                <p class="field-label">{{ $t('environment.containerMemoryDefault') }}</p>
                <div class="limit-batch-row">
                    <bk-select
                        v-model="memMode"
                        class="limit-batch-select"
                        :clearable="false"
                    >
                        <bk-option
                            id="keep"
                            :name="$t('environment.保持不变')"
                        />
                        <bk-option
                            id="unlimited"
                            :name="$t('environment.concurrencyUnlimited')"
                        />
                        <bk-option
                            id="value"
                            :name="$t('environment.specifyValue')"
                        />
                    </bk-select>
                    <template v-if="memMode === 'value'">
                        <bk-input
                            v-model="memAmount"
                            class="limit-batch-amount"
                            :placeholder="$t('environment.exampleValue')"
                        />
                        <bk-select
                            v-model="memUnit"
                            class="limit-batch-unit"
                            :clearable="false"
                        >
                            <bk-option
                                id="m"
                                name="m"
                            />
                            <bk-option
                                id="g"
                                name="g"
                            />
                        </bk-select>
                    </template>
                </div>
                <p
                    v-if="memMode === 'value'"
                    class="limit-batch-hint"
                >
                    {{ $t('environment.memoryBatchHint') }}
                </p>
                <p
                    v-if="memError"
                    class="limit-batch-error"
                >
                    {{ memError }}
                </p>
            </div>
        </div>
        <div slot="footer">
            <bk-button
                theme="primary"
                :loading="saving"
                @click="save"
            >
                {{ $t('environment.save') }}
            </bk-button>
            <bk-button
                :disabled="saving"
                @click="close"
            >
                {{ $t('environment.cancel') }}
            </bk-button>
        </div>
    </bk-dialog>
</template>

<script>
    import {
        BATCH_LIMIT_MAX,
        cpuLimitError,
        joinMemoryLimit,
        limitErrorKey,
        memoryLimitError,
        nodeLimitLabel,
        normalizeCpu,
        normalizeMemory,
        splitContainerLimitNodes
    } from '@/utils/containerLimit'

    export default {
        name: 'BatchContainerLimitDialog',
        props: {
            value: {
                type: Boolean,
                default: false
            },
            nodes: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                showTargets: false,
                showSkipped: false,
                cpuMode: 'keep',
                memMode: 'keep',
                cpuValue: '',
                memAmount: '',
                memUnit: 'g',
                cpuError: '',
                memError: '',
                saving: false
            }
        },
        computed: {
            grouped () {
                return splitContainerLimitNodes(this.nodes)
            },
            targets () {
                return this.grouped.targets
            },
            skipped () {
                return this.grouped.skipped
            },
            noEdit () {
                return this.grouped.noEdit
            }
        },
        watch: {
            value (show) {
                if (show) this.reset()
            },
            cpuMode () {
                this.cpuError = ''
            },
            cpuValue () {
                this.cpuError = ''
            },
            memMode () {
                this.memError = ''
            },
            memAmount () {
                this.memError = ''
            },
            memUnit () {
                this.memError = ''
            }
        },
        methods: {
            nodeLabel (node) {
                return nodeLimitLabel(node)
            },
            reset () {
                this.showTargets = false
                this.showSkipped = false
                this.cpuMode = 'keep'
                this.memMode = 'keep'
                this.cpuValue = ''
                this.memAmount = ''
                this.memUnit = 'g'
                this.cpuError = ''
                this.memError = ''
                this.saving = false
            },
            close () {
                if (this.saving) return
                this.$emit('input', false)
            },
            onInput (visible) {
                this.$emit('input', visible)
            },
            async save () {
                if (this.saving) return
                this.cpuError = ''
                this.memError = ''
                if (this.cpuMode === 'keep' && this.memMode === 'keep') {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('environment.modifyAtLeastOne')
                    })
                    return
                }
                const config = {}
                if (this.cpuMode === 'unlimited') {
                    config.cpus = ''
                } else if (this.cpuMode === 'value') {
                    const code = cpuLimitError(this.cpuValue, true)
                    if (code) {
                        this.cpuError = this.$t(limitErrorKey('cpu', code))
                        return
                    }
                    config.cpus = normalizeCpu(this.cpuValue)
                }
                if (this.memMode === 'unlimited') {
                    config.memory = ''
                } else if (this.memMode === 'value') {
                    const raw = normalizeMemory(joinMemoryLimit(this.memAmount, this.memUnit))
                    const code = memoryLimitError(raw, true)
                    if (code) {
                        this.memError = this.$t(limitErrorKey('memory', code))
                        return
                    }
                    config.memory = raw
                }
                const nodeIds = this.targets.map(node => node.nodeHashId).filter(Boolean)
                if (!nodeIds.length) return
                if (nodeIds.length > BATCH_LIMIT_MAX) {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('environment.batchLimitMax')
                    })
                    return
                }
                try {
                    this.saving = true
                    await this.$store.dispatch('environment/batchSaveNodeDockerConfig', {
                        projectId: this.$route.params.projectId,
                        nodeIds,
                        config
                    })
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('environment.successfullySaved')
                    })
                    this.saving = false
                    this.$emit('input', false)
                    this.$emit('saved')
                } catch (err) {
                    this.$bkMessage({
                        theme: 'error',
                        message: err.message || err
                    })
                } finally {
                    this.saving = false
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
.limit-batch {
    font-size: 12px;
    color: #63656e;
}

.limit-batch-head,
.limit-batch-toggle {
    margin: 0;
    color: #63656e;
    font-size: 12px;
    line-height: 20px;
    font-weight: 400;
    text-align: left;
}

.limit-batch-head {
    margin-bottom: 4px;
}

.limit-batch-toggle {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    width: auto;
    padding: 0;
    border: 0;
    background: transparent;
    cursor: pointer;

    .bk-icon {
        font-size: 12px;
        color: #979ba5;
    }

    &.is-follow {
        margin-top: 8px;
    }
}

.limit-batch-nodes {
    margin: 0;
    padding: 0;
    list-style: none;
    text-align: left;

    li {
        line-height: 20px;
        color: #313238;
    }

    &.is-scroll-8 {
        max-height: 160px;
        overflow: auto;
    }

    &.is-scroll-4 {
        max-height: 80px;
        overflow: auto;

        li {
            color: #979ba5;
        }
    }
}

.limit-batch-reason,
.limit-batch-perm,
.limit-batch-hint {
    margin: 0;
    line-height: 20px;
    color: #63656e;
    text-align: left;
}

.limit-batch-hint {
    margin-top: 8px;
    color: #979ba5;
}

.limit-batch-perm {
    margin-top: 12px;
    color: #e38b02;
}

.limit-batch-field {
    margin-top: 16px;
    text-align: left;

    &:first-of-type {
        margin-top: 24px;
    }
}

.field-label {
    margin: 0 0 8px;
    color: #63656e;
    line-height: 20px;
    text-align: left;
}

.limit-batch-row {
    display: flex;
    align-items: center;
}

.limit-batch-select {
    width: 160px;
    flex: none;
}

.limit-batch-input,
.limit-batch-amount {
    width: 120px;
    margin-left: 8px;
}

.limit-batch-unit {
    width: 72px;
    margin-left: 8px;
    flex-shrink: 0;
}

.limit-suffix {
    margin-left: 8px;
    color: #63656e;
    flex-shrink: 0;
}

.limit-batch-error {
    margin: 4px 0 0;
    color: #ea3636;
    line-height: 20px;
}
</style>

<style lang="scss">
.batch-container-limit-dialog {
    .bk-dialog-body {
        overflow: visible;
        text-align: left;

        p {
            text-align: left;
        }
    }

    .bk-dialog-footer .bk-button + .bk-button {
        margin-left: 8px;
    }
}
</style>
