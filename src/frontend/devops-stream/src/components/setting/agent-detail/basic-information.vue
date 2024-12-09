<template>
    <div class="basic-information-wrapper">
        <div class="base-item-list">
            <div class="item-content">
                <div class="item-label">{{ $t('setting.nodeInfo.startUser') }}</div>
                <div class="item-value">{{ nodeDetails.startedUser || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('setting.nodeInfo.installPath') }}</div>
                <div class="item-value">{{ nodeDetails.agentInstallPath || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('setting.nodeInfo.agentVersion') }}</div>
                <div class="item-value">{{ nodeDetails.agentVersion || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('setting.nodeInfo.workerVersion') }}</div>
                <div class="item-value">{{ nodeDetails.slaveVersion || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('setting.nodeInfo.maxParallelTaskCount') }}</div>
                <div class="item-value">
                    <div class="display-item" v-if="isEditCount">
                        <input type="number" class="bk-form-input parallelTaskCount-input"
                            ref="parallelTaskCount"
                            name="parallelTaskCount"
                            placeholder="please input a number between:0 and 100"
                            v-validate.initial="`required|between:0,100|decimal:0`"
                            v-model="parallelTaskCount"
                            :class="{ 'is-danger': errors.has('parallelTaskCount') }">
                    </div>
                    <div class="editing-item" v-else>{{ nodeDetails.parallelTaskCount || '--' }}</div>
                </div>
                <div class="handle-btn">
                    <div v-if="isEditCount">
                        <span @click="saveHandle('parallelTaskCount')">{{ $t('setting.save') }}</span>
                        <span @click="editHandle('parallelTaskCount', false)">{{ $t('setting.cancel') }}</span>
                    </div>
                    <div :class="{ 'is-disabled': !nodeDetails.canEdit }" v-else><span @click="editHandle('parallelTaskCount', true)">{{ $t('edit') }}</span></div>
                </div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('setting.nodeInfo.dockerMaxConcurrency') }}</div>
                <div class="item-value">
                    <div class="display-item" v-if="isEditDockerCount">
                        <input type="number" class="bk-form-input parallelTaskCount-input"
                            ref="dockerParallelTaskCount"
                            name="dockerParallelTaskCount"
                            :placeholder="$t('setting.nodeInfo.parallelTaskCountTips')"
                            v-validate.initial="`required|between:0,100|decimal:0`"
                            v-model="dockerParallelTaskCount"
                            :class="{ 'is-danger': errors.has('dockerParallelTaskCount') }">
                    </div>
                    <div class="editing-item" v-else>{{ nodeDetails.dockerParallelTaskCount || '--' }}</div>
                </div>
                <div class="handle-btn">
                    <div v-if="isEditDockerCount">
                        <span @click="saveHandle('dockerParallelTaskCount')">{{ $t('setting.save') }}</span>
                        <span @click="editHandle('dockerParallelTaskCount', false)">{{ $t('setting.cancel') }}</span>
                    </div>
                    <div
                        v-else
                        :class="{ 'is-disabled': !nodeDetails.canEdit }"
                    >
                        <span @click="editHandle('dockerParallelTaskCount', true)">{{ $t('setting.edit') }}</span>
                    </div>
                </div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('status') }}</div>
                <div class="item-value" :class="nodeDetails.status === 'NORMAL' ? 'normal' : 'abnormal'">
                    {{ nodeDetails.status === 'NORMAL' ? $t('setting.nodeInfo.normal') : $t('setting.nodeInfo.abnormal') }}
                </div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('setting.nodeInfo.importTime') }}</div>
                <div class="item-value">{{ nodeDetails.createdTime || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('creator') }}</div>
                <div class="item-value">{{ nodeDetails.createdUser || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('setting.nodeInfo.lastActiveTime') }}</div>
                <div class="item-value">{{ nodeDetails.lastHeartbeatTime || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ nodeDetails.os === 'WINDOWS' ? $t('setting.nodeInfo.downloadLink') : $t('setting.nodeInfo.installCommand') }}</div>
                <div class="item-value" :title="agentLink">{{ agentLink }}</div>
                <div class="handle-btn">
                    <span class="agent-url" @click="copyHandle">{{ $t('copy') }}</span>
                    <span @click="downloadHandle" v-if="nodeDetails.os === 'WINDOWS'">{{ $t('pipeline.download') }}</span>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { setting } from '@/http'
    import { mapState } from 'vuex'
    import { copyText } from '@/utils/util'

    export default {
        data () {
            return {
                nodeDetails: {},
                isEditCount: false,
                parallelTaskCount: 0,
                isEditDockerCount: false,
                dockerParallelTaskCount: 0
            }
        },
        computed: {
            ...mapState(['projectId']),
            nodeHashId () {
                return this.$route.params.agentId
            },
            agentLink () {
                return this.nodeDetails.os === 'WINDOWS' ? this.nodeDetails.agentUrl : this.nodeDetails.agentScript
            }
        },
        created () {
            this.requestNodeDetail()
        },
        methods: {
            editHandle (type, isOpen) {
                if (!this.nodeDetails.canEdit) {
                    return
                }

                switch (type) {
                    case 'parallelTaskCount':
                        this.isEditCount = isOpen
                        if (isOpen) {
                            this.parallelTaskCount = this.nodeDetails.parallelTaskCount
                            this.$nextTick(() => {
                                this.$refs.parallelTaskCount.focus()
                            })
                        }
                        break
                    case 'dockerParallelTaskCount':
                        this.isEditDockerCount = isOpen
                        if (isOpen) {
                            this.dockerParallelTaskCount = this.nodeDetails.dockerParallelTaskCount
                            this.$nextTick(() => {
                                this.$refs.dockerParallelTaskCount.focus()
                            })
                        }
                        break
                    default:
                        break
                }
            },
            async saveHandle (type) {
                const valid = await this.$validator.validate()
                if (!valid) return
                switch (type) {
                    case 'parallelTaskCount':
                        this.saveParallelTaskCount(this.parallelTaskCount, 'parallelTaskCount')
                        break
                    case 'dockerParallelTaskCount':
                        this.saveParallelTaskCount(this.dockerParallelTaskCount, 'dockerParallelTaskCount')
                        break
                    default:
                        break
                }
               
            },
            async saveParallelTaskCount (count, type) {
                let message, theme
                const fn = type === 'dockerParallelTaskCount'
                    ? setting.saveDockerParallelTaskCount
                    : setting.saveParallelTaskCount
                const params = {
                    projectId: this.projectId,
                    nodeHashId: this.nodeHashId,
                    count: count
                }
                try {
                    await fn(params)
                    message = this.$t('setting.successfullySaved')
                    theme = 'success'
                    this.requestNodeDetail()
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.isEditCount = false
                    this.isEditDockerCount = false
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestNodeDetail () {
                try {
                    this.nodeDetails = await setting.requestNodeDetail(this.projectId, this.nodeHashId)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            downloadHandle () {
                window.location.href = this.nodeDetails.agentUrl
            },
            copyHandle () {
                if (copyText(this.agentLink)) {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('setting.successfullyCopyed')
                    })
                }
            }
        }
    }
</script>

<style lang="postcss">
    @import '@/css/conf';

    .basic-information-wrapper {
        padding: 20px 0;
        .base-item-list {
            border-top: 1px solid #DDE4EB;
        }
        .item-content {
            display: flex;
            align-items: center;
            border-bottom: 1px solid #DDE4EB;
            .item-label {
                width: 188px;
                padding: 12px 20px;
                border-right: 1px solid #DDE4EB;
            }
            .item-value {
                width: 40%;
                padding: 6px 20px;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .disabled {
                color: #C3CDD7;
            }
            .handle-btn {
                padding: 10px 30px;
                color: $primaryColor;
                span {
                    margin-right: 10px;
                    cursor: pointer;
                }
            }
            .is-disabled span {
                color: #CCC;
                cursor: default;
            }
            .bk-form-input {
                height: 28px;
                width: 320px;
            }
            .notice-type-content {
                height: 28px;
            }
            .notice-type-checkbox {
                margin-right: 12px;
                width: 80px;
                padding: 0;
                display: inline-block;
                overflow: hidden;
                white-space: nowrap;
                text-overflow: ellipsis;
                .type-item {
                    position: relative;
                    top: -10px;
                }
            }
            .normal {
                color: #30D878;
            }
            .abnormal {
                color: #FF5656;
            }
            .is-danger {
                border-color: #ff5656;
                background-color: #fff4f4;
                color: #ff5656;
            }
        }
    }
</style>
