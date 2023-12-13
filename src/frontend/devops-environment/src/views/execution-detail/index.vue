<template>
    <div class="execution-detail" v-bkloading="{ isLoading }">
        <header class="header-wrapper">
            <bk-breadcrumb separator=">">
                <bk-breadcrumb-item :to="{ path: '/console/environment' }">{{ $t('environment.environmentManage') }}</bk-breadcrumb-item>
                <bk-breadcrumb-item>{{ stepInstanceData.name }}</bk-breadcrumb-item>
            </bk-breadcrumb>
            <div class="status-box" :class="statusStyleMap[checkStatus(stepInstanceData.status)]">
                <div class="status">
                    <span>{{ $t('environment.状态') }}：</span>
                    <span class="status-text">{{ stepStatusMap[stepInstanceData.status] }}</span>
                </div>
                <div class="time">
                    <span>{{ $t('environment.总耗时') }}：</span>
                    <span
                        class="value"
                    >
                        {{ stepInstanceData.totalTime / 1000 }}s
                    </span>
                </div>
                <div class="action">
                    <slot />
                </div>
            </div>
        </header>
        <div class="task-step-detail">
            <div class="step-info-header">
                <div class="step-info-wrapper">
                    <div class="step-type-text">{{ stepTypeText }}</div>
                    <div class="step-name-box">
                        <div class="step-name-text" v-bk-overflow-tips>
                            {{ stepInstanceData.name }}
                        </div>
                    </div>
                </div>
                
            </div>
            <div class="step-execute-host-group">
                <div class="group-tab">
                    <div :class="{
                             'tab-item': true,
                             'active': activeGroupIndex === index
                         }"
                        v-for="(value, key, index) in groupListMap"
                        :key="key"
                        @click="activeGroupIndex = index"
                    >
                        <div class="group-name" v-bk-overflow-tips>{{ key }}</div>
                        <div class="group-nums">{{ value.length }}</div>
                    </div>
                </div>
            </div>
        </div>
        <div
            ref="detailContainer"
            class="detail-container"
            :style="defailContainerStyles">
            <div class="container-left" v-bkloading="{ isLoading: isHostLoading }">
                <!-- 主机列表 -->
                <ip-list
                    :list="ipList"
                    :ip.sync="activeIp"
                    :ip-status.sync="activeIpStatus"
                    :host-id.sync="activeHostId"
                    :bk-cloud-id.sync="activeBkCloudId"
                />
            </div>
            <div class="container-right">
                <!-- 执行日志 -->
                <execution-info
                    :ip="activeIp"
                    :host-id="activeHostId"
                    :bk-cloud-id="activeBkCloudId"
                    :ip-status="activeIpStatus"
                />
            </div>
        </div>
    </div>
</template>

<script>
    import ipList from '@/components/ipList'
    import ExecutionInfo from '@/components/executionInfo'
    import { mapActions } from 'vuex'
    import {
        statusStyleMap,
        checkStatus,
        getAgentStatus
    } from '@/utils/execution'
    export default {
        components: {
            ipList,
            ExecutionInfo
        },
        data () {
            return {
                stepInstanceData: {},
                stepStatusMap: {},
                statusStyleMap,
                checkStatus,
                groupListMap: {},
                activeGroupIndex: -1,
                isLoading: false,
                activeHostId: 0,
                activeIp: '',
                activeBkCloudId: 0,
                activeIpStatus: ''
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            jobInstanceId () {
                return this.$route.query.jobInstanceId
            },
            stepInstanceId () {
                return this.$route.query.stepInstanceId
            },
            jobInstanceType () {
                // 脚本执行 - SCRIPT
                // 文件分发 - FILE
                return this.$route.query.jobInstanceType
            },
            stepTypeText () {
                const typeTextMap = {
                    SCRIPT: this.$t('environment.scriptExecution'),
                    FILE: this.$t('environment.fileTransfer')
                }
                return typeTextMap[this.jobInstanceType]
            },
            ipList () {
                const key = Object.keys(this.groupListMap)[this.activeGroupIndex]
                this.activeHostId = this.groupListMap[key] && this.groupListMap[key].length && this.groupListMap[key][0].bkHostId
                this.activeIp = this.groupListMap[key] && this.groupListMap[key].length && this.groupListMap[key][0].ip
                this.activeBkCloudId = this.groupListMap[key] && this.groupListMap[key].length && this.groupListMap[key][0].bkCloudId
                this.activeIpStatus = this.groupListMap[key] && this.groupListMap[key].length && getAgentStatus(this.groupListMap[key][0].status)
                return this.groupListMap[key] && this.groupListMap[key].map(i => {
                    return {
                        ...i,
                        result: getAgentStatus(i.status)
                    }
                })
            }
        },
        created () {
            this.stepStatusMap = {
                1: this.$t('environment.未执行'),
                2: this.$t('environment.正在执行'),
                3: this.$t('environment.执行成功'),
                4: this.$t('environment.执行失败'),
                5: this.$t('environment.跳过'),
                6: this.$t('environment.忽略错误'),
                7: this.$t('environment.等待用户'),
                8: this.$t('environment.手动结束'),
                9: this.$t('environment.状态异常'),
                10: this.$t('environment.步骤强制终止中'),
                11: this.$t('environment.步骤强制终止成功')
            }
            this.fetchStepInstanceStatus()
        },
        methods: {
            ...mapActions('environment', [
                'getStepInstanceStatus'
            ]),
            fetchStepInstanceStatus () {
                this.isLoading = true
                this.getStepInstanceStatus({
                    projectId: this.projectId,
                    jobInstanceId: this.jobInstanceId,
                    stepInstanceId: this.stepInstanceId
                }).then(res => {
                    this.stepInstanceData = res

                    this.stepInstanceData.stepHostResultList.forEach(item => {
                        const key = item.statusDesc + item.tag
                        if (!this.groupListMap[key]) {
                            this.groupListMap[key] = []
                        }
                        this.groupListMap[key].push(item)
                    })
                    this.activeGroupIndex = 0
                }).catch(error => {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }).finally(() => {
                    this.isLoading = false
                })
            }
        }
    }
</script>

<style lang="scss">
    .execution-detail {
        width: 100%;
    }
    .header-wrapper {
        align-items: center;
        background: #fff;
        box-shadow: 0 2px 4px 0 rgba(0,0,0,.1);
        color: #313238;
        display: flex;
        font-size: 16px;
        height: 50px;
        padding-left: 24px;
        padding-right: 24px;
        position: fixed;
        left: 0;
        right: 0;
        top: 50px;
        z-index: 1999;
    }
    .status-box {
        position: absolute;
        top: 0;
        bottom: 0;
        left: 50%;
        display: flex;
        align-items: center;
        font-size: 14px;
        justify-content: center;
        width: 500px;
        transform: translateX(-50%);
        &.fail,
        &.confirm-forced {
            .status-text {
                color: #ea3636;
            }
        }

        &.loading {
            .status-text {
                color: #3a84ff;
            }
        }

        &.ingore {
            .status-text {
                color: #abd88a;
            }
        }

        &.success,
        &.forced {
            .status-text {
                color: #2dcb8d;
            }
        }

        &.confirm {
            .status-text {
                color: #ff9c01;
            }
        }

        &.disable {
            .status-text {
                color: #c4c6cc;
            }
        }
    }

    .status {
        margin-right: 30px;
    }

    .time {
        min-width: 120px;
        padding-right: 10px;

        .value {
            display: inline-block;
            color: #313238;
        }
    }
    .task-step-detail {
        width: 100%;
        margin-top: 50px;
        .step-info-header {
            background: #f5f6fa;
            display: flex;
            overflow: hidden;
            padding: 45px 24px 12px;
            width: 100%;
        }
        .step-info-wrapper {
            flex: 1;
            margin-top: -25px;

            .step-type-text {
                font-size: 12px;
                line-height: 16px;
                color: #979ba5;
            }
            .step-name-box {
                display: flex;
                align-items: center;
                margin-top: 10px;
            }
            .step-name-text {
                height: 24px;
                max-width: 600px;
                overflow: hidden;
                font-size: 18px;
                line-height: 24px;
                color: #313238;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
        }
        .step-execute-host-group {
            width: 100%;
            padding: 0 24px;
            background: #f5f6fa;
            border-bottom: 1px solid #e2e2e2;

            .group-tab {
                display: flex;
                height: 40px;
                color: #63656e;
                visibility: visible;
                transition: all 0.15s;

                .tab-item {
                    position: relative;
                    display: flex;
                    height: 41px;
                    padding: 0 20px;
                    font-size: 14px;
                    color: #63656e;
                    cursor: pointer;
                    border: 1px solid transparent;
                    border-top-right-radius: 6px;
                    border-top-left-radius: 6px;
                    align-items: center;
                    flex: 0 0 auto;
                }

                .group-name {
                    max-width: 225px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }

                .group-nums {
                    display: inline-block;
                    height: 16px;
                    min-width: 16px;
                    padding: 0 4px;
                    margin-left: 8px;
                    font-size: 12px;
                    font-weight: 500;
                    line-height: 16px;
                    color: #979ba5;
                    text-align: center;
                    background: #e6e7eb;
                    border-radius: 8px;
                }
            }
            .tab-item,
            .tab-more {
                position: relative;
                display: flex;
                height: 41px;
                padding: 0 20px;
                font-size: 14px;
                color: #63656e;
                cursor: pointer;
                border: 1px solid transparent;
                border-top-right-radius: 6px;
                border-top-left-radius: 6px;
                align-items: center;
                flex: 0 0 auto;
            }

            .tab-item {
                &:hover,
                &.active {
                    color: #313238;

                    .group-nums {
                        color: #63656e;
                    }
                }

                &.active {
                    background: #fff;
                    border-color: #dcdee5;
                    border-bottom-color: #fff;
                }
            }
        }

    }

    .detail-container {
        display: flex;
        height: calc(100vh - 234px);
        padding: 20px 24px;

        .container-left {
            height: 100%;
            width: 50%;
            overflow: hidden;
            background: #fff;
            border: 1px solid #dcdee5;
            border-bottom-left-radius: 2px;
            border-top-left-radius: 2px;
        }

        .container-right {
            display: flex;
            width: 0;
            height: 100%;
            overflow: hidden;
            flex-direction: column;
            flex: 1;
        }
    }
</style>
