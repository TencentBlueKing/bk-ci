<template>
    <div class="execution-detail">
        <header class="header-wrapper">
            步骤执行脚本_20210622113616939
        </header>
        <div class="task-step-detail">
            <div class="step-info-header">
                <div class="step-info-wrapper">
                    <div class="step-type-text">{{ stepTypeText }}</div>
                    <div class="step-name-box">
                        <div class="step-name-text">步骤执行脚本_20210622113616939</div>
                    </div>
                </div>
            </div>
            <div class="step-execute-host-group">
                <div class="group-tab">
                    <div class="tab-item active">
                        <div class="group-name" v-bk-overflow-tips>执行成功</div>
                        <div class="group-nums">{{ 1 }}</div>
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
                    <execution-info />
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import ipList from '@/components/ipList'
    import ExecutionInfo from '@/components/executionInfo'
    export default {
        components: {
            ipList,
            ExecutionInfo
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            jobInstanceId () {
                return this.$route.params.jobInstanceId
            },
            jobInstanceType () {
                return this.$route.params.jobInstanceType
            },
            stepTypeText () {
                const typeTextMap = {
                    SCRIPT: this.$t('environment.scriptExecution'),
                    FILE: this.$t('environment.fileTransfer')
                }
                return typeTextMap[this.jobInstanceType]
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
            width: 700px;
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
