<template>
    <div class="basic-information-wrapper">
        <div class="base-item-list">
            <div class="item-content">
                <div class="item-label">{{ $t('environment.nodeInfo.startUser') }}</div>
                <div class="item-value">{{ nodeDetails.startedUser || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('environment.nodeInfo.installPath') }}</div>
                <div class="item-value">{{ nodeDetails.agentInstallPath || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('environment.nodeInfo.agentVersion') }}</div>
                <div class="item-value">{{ nodeDetails.agentVersion || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('environment.nodeInfo.workerVersion') }}</div>
                <div class="item-value">{{ nodeDetails.slaveVersion || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('environment.nodeInfo.maxParallelTaskCount') }}</div>
                <div class="item-value">
                    <div class="display-item" v-if="isEditCount">
                        <input type="number" class="bk-form-input parallelTaskCount-input"
                            ref="parallelTaskCount"
                            name="parallelTaskCount"
                            :placeholder="$t('environment.nodeInfo.parallelTaskCountTips')"
                            v-validate.initial="`required|between:0,100|decimal:0`"
                            v-model="parallelTaskCount"
                            :class="{ 'is-danger': errors.has('parallelTaskCount') }">
                    </div>
                    <div class="editing-item" v-else>{{ nodeDetails.parallelTaskCount || '--' }}</div>
                </div>
                <div class="handle-btn">
                    <div v-if="isEditCount">
                        <span @click="saveHandle('parallelTaskCount')">{{ $t('environment.save') }}</span>
                        <span @click="editHandle('parallelTaskCount', false)">{{ $t('environment.cancel') }}</span>
                    </div>
                    <div :class="{ 'is-disabled': !nodeDetails.canEdit }" v-else><span @click="editHandle('parallelTaskCount', true)">{{ $t('environment.edit') }}</span></div>
                </div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('environment.status') }}</div>
                <div class="item-value" :class="nodeDetails.status === 'NORMAL' ? 'normal' : 'abnormal'">
                    {{ nodeDetails.status === 'NORMAL' ? $t('environment.nodeInfo.normal') : $t('environment.nodeInfo.abnormal') }}
                </div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('environment.nodeInfo.importTime') }}</div>
                <div class="item-value">{{ nodeDetails.createdTime || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ $t('environment.nodeInfo.lastActiveTime') }}</div>
                <div class="item-value">{{ nodeDetails.lastHeartbeatTime || '--' }}</div>
            </div>
            <div class="item-content">
                <div class="item-label">{{ nodeDetails.os === 'WINDOWS' ? $t('environment.nodeInfo.downloadLink') : $t('environment.nodeInfo.installCommand') }}</div>
                <div class="item-value" :title="agentLink">{{ agentLink }}</div>
                <div class="handle-btn">
                    <span class="agent-url" @click="copyHandle">{{ $t('environment.copy') }}</span>
                    <span @click="downloadHandle" v-if="nodeDetails.os === 'WINDOWS'">{{ $t('environment.download') }}</span>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import { copyText } from '@/utils/util'
    import { NODE_RESOURCE_ACTION, NODE_RESOURCE_TYPE } from '../../utils/permission'

    export default {
        data () {
            return {
                isEditWorkspace: false,
                isEditCount: false,
                isEditCreatedUser: false,
                isEditNoticeType: false,
                workspace: '',
                createdUser: '',
                parallelTaskCount: 0,
                noticeTypeList: [
                    { name: 'work-wechat', value: 'RTX', isChecked: true },
                    { name: 'wechat', value: 'WECHAT', isChecked: false },
                    { name: 'email', value: 'EMAIL', isChecked: false }
                ]
            }
        },
        computed: {
            ...mapState('environment', [
                'nodeDetails'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            nodeHashId () {
                return this.$route.params.nodeHashId
            },
            agentLink () {
                return this.nodeDetails.os === 'WINDOWS' ? this.nodeDetails.agentUrl : this.nodeDetails.agentScript
            }
        },
        mounted () {
        },
        methods: {
            editHandle (type, isOpen) {
                if (!this.nodeDetails.canEdit) {
                    this.handleNoPermission({
                        projectId: this.projectId,
                        resourceType: NODE_RESOURCE_TYPE,
                        resourceCode: this.nodeHashId,
                        action: NODE_RESOURCE_ACTION.EDIT
                    })
                    return
                }

                switch (type) {
                    case 'workspace':
                        this.isEditWorkspace = isOpen
                        if (isOpen) {
                            this.workspace = this.nodeDetails.workspace
                        }
                        break
                    case 'parallelTaskCount':
                        this.isEditCount = isOpen
                        if (isOpen) {
                            this.parallelTaskCount = this.nodeDetails.parallelTaskCount
                            this.$nextTick(() => {
                                this.$refs.parallelTaskCount.focus()
                            })
                        }
                        break
                    case 'createdUser':
                        this.isEditCreatedUser = isOpen
                        if (isOpen) {
                            this.createdUser = this.nodeDetails.createdUser
                        }
                        break
                    case 'noticeType':
                        this.isEditNoticeType = isOpen
                        break
                    default:
                        break
                }
            },
            async saveHandle () {
                const valid = await this.$validator.validate()
                if (valid) {
                    this.saveParallelTaskCount(this.parallelTaskCount)
                }
            },
            async saveParallelTaskCount (parallelTaskCount) {
                let message, theme
                try {
                    await this.$store.dispatch('environment/saveParallelTaskCount', {
                        projectId: this.projectId,
                        nodeHashId: this.nodeHashId,
                        parallelTaskCount
                    })
                    message = this.$t('environment.successfullySaved')
                    theme = 'success'
                    this.requestNodeDetail()
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestNodeDetail () {
                try {
                    const res = await this.$store.dispatch('environment/requestNodeDetail', {
                        projectId: this.projectId,
                        nodeHashId: this.nodeHashId
                    })
                    this.$store.commit('environment/updateNodeDetail', { res })
                    this.isEditCount = false
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
                        message: this.$t('environment.successfullyCopyed')
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';

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
                width: 180px;
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
