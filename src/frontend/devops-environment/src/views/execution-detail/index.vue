<template>
    <div class="execution-detail">
        <header class="header-wrapper">
            <bk-breadcrumb separator=">">
                <bk-breadcrumb-item :to="{ path: '/console/environment' }">{{ $t('environment.environmentManage') }}</bk-breadcrumb-item>
                <bk-breadcrumb-item>{{ jobInstanceData.jobInstance.name }}</bk-breadcrumb-item>
            </bk-breadcrumb>
            <div class="status-box" :class="statusStyleMap[checkStatus(jobInstanceData.jobInstance.status)]">
                <div class="status">
                    <span>{{ $t('environment.状态') }}：</span>
                    <span class="status-text">{{ stepStatusMap[jobInstanceData.jobInstance.status] }}</span>
                </div>
                <div class="time">
                    <span>{{ $t('environment.总耗时') }}：</span>
                    <span
                        class="value"
                    >
                        {{ jobInstanceData.jobInstance.totalTime / 1000 }}s
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
                        <div class="step-name-text">
                            {{ jobInstanceData.jobInstance.name }}
                        </div>
                    </div>
                </div>
                
            </div>
            <div class="step-execute-host-group">
                <div class="group-tab">
                    <div class="tab-item active">
                        <div class="group-name" v-bk-overflow-tips>{{ stepStatusMap[jobInstanceData.jobInstance.status] }}</div>
                        <div class="group-nums">{{ jobInstanceData.stepInstanceList.length }}</div>
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
                <ip-list />
            </div>
            <div class="container-right">
                <!-- 执行日志 -->
                <execution-info
                    :job-instance-type="jobInstanceType"
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
        checkStatus
    } from '@/utils/execution'
    export default {
        components: {
            ipList,
            ExecutionInfo
        },
        data () {
            return {
                jobInstanceData: {},
                stepStatusMap: {},
                statusStyleMap,
                checkStatus,
                stepInstanceId: ''
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            jobInstanceId () {
                return this.$route.params.jobInstanceId
            },
            jobInstanceType () {
                // 脚本执行 - SCRIPT
                // 文件分发 - FILE
                return this.$route.params.jobInstanceType
            },
            stepTypeText () {
                const typeTextMap = {
                    SCRIPT: this.$t('environment.scriptExecution'),
                    FILE: this.$t('environment.fileTransfer')
                }
                return typeTextMap[this.jobInstanceType]
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
            this.fetchJobInstanceStatus()
        },
        methods: {
            ...mapActions('environment', [
                'getJobInstanceStatus'
            ]),
            fetchJobInstanceStatus () {
                try {
                    const { data } = this.getJobInstanceStatus({
                        projectId: this.projectId,
                        jobInstanceId: this.jobInstanceId
                    })
                    this.jobInstanceData = {
                        finished: true,
                        jobInstance: {
                            name: '快速执行脚本_20231110150105256',
                            status: 3,
                            createTime: 1699599665256,
                            startTime: 1699599665448,
                            endTime: 1699599666663,
                            totalTime: 1215,
                            jobInstanceId: 20003562849,
                            bkBizId: 2005000002,
                            bkScopeType: 'biz',
                            bkScopeId: '2005000002'
                        },
                        stepInstanceList: [
                            {
                                stepInstanceId: 20004250944,
                                type: 1,
                                name: '快速执行脚本_20231110150105256',
                                stepStatus: 3,
                                createTime: 1699599665256,
                                startTime: 1699599665482,
                                endTime: 1699599666647,
                                totalTime: 1165,
                                stepRetries: 0,
                                stepIpResultList: [
                                    {
                                        ip: '9.146.98.67',
                                        bkHostId: 2000000008,
                                        bkCloudId: 0,
                                        status: 9,
                                        tag: '',
                                        exitCode: 0,
                                        errorCode: 0,
                                        startTime: 1699599665627,
                                        endTime: 1699599665871,
                                        totalTime: 244
                                    }
                                ]
                            }
                        ]
                    }
                    console.log(data, 1111111)
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
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
                max-width: calc(100% - 65px);
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
