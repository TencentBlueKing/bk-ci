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
                        <div class="step-action-box">
                            <div class="log-search-box">
                                <compose-form-item>
                                    <bk-select
                                        v-model="searchModel"
                                        :clearable="false"
                                        style="width: 100px;">
                                        <bk-option
                                            id="log"
                                            :name="$t('environment.搜索日志')" />
                                        <bk-option
                                            id="ip"
                                            :name="$t('environment.搜索 IP')" />
                                    </bk-select>
                                    <bk-input
                                        v-if="searchModel === 'log'"
                                        key="log"
                                        :disabled="isFile"
                                        right-icon="bk-icon icon-search"
                                        style="width: 292px;"
                                        v-bk-tooltips="{
                                            content: isFile ? $t('environment.分发文件步骤不支持日志搜索') : '',
                                            disabled: !isFile
                                        }"
                                        :value="keyword"
                                        @right-icon-click="handleLogSearch"
                                        @keyup="handleLogSearch" />
                                    <bk-input
                                        v-if="searchModel === 'ip'"
                                        key="ip"
                                        right-icon="bk-icon icon-search"
                                        style="width: 292px;"
                                        :value="searchIp"
                                        @right-icon-click="handleIPSearch"
                                        @keyup="handleIPSearch" />
                                </compose-form-item>
                            </div>
                            <div class="task-instance-action">
                                <div
                                    class="action-btn detail-btn"
                                    @click="handleShowDetail"
                                    v-bk-tooltips.bottom="$t('environment.步骤内容')"
                                >
                                    <icon name="detail-line" size="14" />
                                </div>
                            </div>
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
                        v-for="(item, index) in stepResultGroupList"
                        :key="index"
                        @click="handleChangeGroup(index)"
                    >
                        <div class="group-name" v-bk-overflow-tips>{{ `${item.resultTypeDesc}` }} {{ item.tag ? `(${item.tag})` : '' }}</div>
                        <div class="group-nums">{{ item.hostSize }}</div>
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
                    :is-search="isSearch"
                    :host-id.sync="activeHostId"
                    :ip-status.sync="activeIpStatus"
                    :bk-cloud-id.sync="activeBkCloudId"
                    :active-group-index="activeGroupIndex"
                    :pagination-change-loading="paginationChangeLoading"
                    @on-pagination-change="handleIpListPageChange"
                    @on-clear-filter="handleClearFilter"
                />
            </div>
            <div class="container-right">
                <!-- 执行日志 -->
                <execution-info
                    :finished="jobInstanceFinished"
                    :ip="activeIp"
                    :host-id="activeHostId"
                    :bk-cloud-id="activeBkCloudId"
                    :ip-status="activeIpStatus"
                />
            </div>
        </div>
        <bk-sideslider
            :is-show.sync="isShowDetail"
            quick-close
            ext-cls="step-detail-sideslider"
            :show-footer="false"
            :title="$t('environment.查看步骤内容')"
            :width="960">
            <div slot="content">
                <step-detail-view
                    :step-instance-id="stepInstanceId"
                    :job-instance-id="jobInstanceId" />
            </div>
        </bk-sideslider>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    import ipList from '@/components/ipList'
    import StepDetailView from './step-detail-view'
    import ExecutionInfo from '@/components/executionInfo'
    import ComposeFormItem from '@/components/compose-form-item'
    import {
        statusStyleMap,
        checkStatus,
        getAgentStatus
    } from '@/utils/execution'
    export default {
        components: {
            ipList,
            ExecutionInfo,
            StepDetailView,
            ComposeFormItem
        },
        data () {
            return {
                // 搜索模式
                searchModel: 'log',
                stepInstanceData: {},
                stepResultGroupList: [],
                jobInstanceFinished: true,
                stepStatusMap: {},
                statusStyleMap,
                checkStatus,
                groupListMap: {},
                activeGroupIndex: 0,
                isLoading: false,
                activeHostId: 0,
                activeIp: '',
                activeBkCloudId: 0,
                activeIpStatus: '',
                isShowDetail: false,
                isHostLoading: true,
                paginationChangeLoading: false,
                hasLoadEnd: false,
                maxHostNumPerGroup: 20,
                keyword: '',
                searchIp: ''
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
            isFile () {
                return this.jobInstanceType === 'FILE'
            },
            stepTypeText () {
                const typeTextMap = {
                    SCRIPT: this.$t('environment.scriptExecution'),
                    FILE: this.$t('environment.fileTransfer')
                }
                return typeTextMap[this.jobInstanceType]
            },
            ipList () {
                if (this.stepResultGroupList.length) {
                    const activeItem = this.stepResultGroupList[this.activeGroupIndex].hostResultList[0] || {}
                    if (!this.activeHostId) {
                        this.activeHostId = activeItem.bkHostId
                        this.activeIp = activeItem.ip
                        this.activeBkCloudId = activeItem.bkCloudId
                        this.activeIpStatus = getAgentStatus(activeItem.status)
                    }
    
                    return this.stepResultGroupList[this.activeGroupIndex].hostResultList.map(i => {
                        return {
                            ...i,
                            result: getAgentStatus(i.status)
                        }
                    })
                } else {
                    this.activeHostId = 0
                    this.activeIp = ''
                    this.activeBkCloudId = 0
                    this.activeIpStatus = ''
                    return []
                }
            },
            resultType () {
                if (this.stepResultGroupList.length) {
                    return this.stepResultGroupList[this.activeGroupIndex].resultType
                }
                return ''
            },
            tag () {
                if (this.stepResultGroupList.length) {
                    return this.stepResultGroupList[this.activeGroupIndex].tag
                }
                return ''
            },
            isSearch () {
                return !!this.keyword || !!this.searchIp
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
            this.fetchStepInstanceStatus()
        },
        methods: {
            ...mapActions('environment', [
                'getJobInstanceStatus',
                'getStepInstanceStatus'
            ]),
            /**
             * @description: 获取步骤执行结果
             */
            fetchJobInstanceStatus () {
                if (this.jobInstanceFinished) {
                    this.isLoading = true
                }
                this.getJobInstanceStatus({
                    projectId: this.projectId,
                    jobInstanceId: this.jobInstanceId
                }).then(res => {
                    this.jobInstanceFinished = res.finished
                    if (!this.jobInstanceFinished) {
                        setTimeout(() => {
                            this.fetchJobInstanceStatus()
                            this.fetchStepInstanceStatus()
                        }, 5000)
                    }
                }).finally(() => {
                    this.isLoading = false
                })
            },
            fetchStepInstanceStatus () {
                this.getStepInstanceStatus({
                    projectId: this.projectId,
                    params: {
                        jobInstanceId: this.jobInstanceId,
                        stepInstanceId: this.stepInstanceId,
                        status: this.resultType,
                        tag: this.tag,
                        keyword: this.keyword,
                        searchIp: this.searchIp,
                        maxHostNumPerGroup: this.maxHostNumPerGroup
                    }
                }).then(res => {
                    this.stepInstanceData = res
                    this.stepResultGroupList = res.stepResultGroupList || []
                    if (this.stepResultGroupList.length) {
                        this.hasLoadEnd = this.ipList.length === this.stepResultGroupList[this.activeGroupIndex].hostSize
                    }
                }).catch(error => {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }).finally(() => {
                    this.isHostLoading = false
                    this.paginationChangeLoading = false
                })
            },
            handleShowDetail () {
                this.isShowDetail = true
            },
            handleChangeGroup (index) {
                this.activeGroupIndex = index
                this.hasLoadEnd = false
                this.fetchStepInstanceStatus()
            },
            handleIpListPageChange (pageSize) {
                if (this.hasLoadEnd || !this.stepResultGroupList.length) return
                this.paginationChangeLoading = true
                this.maxHostNumPerGroup = pageSize
                this.fetchStepInstanceStatus()
            },

            handleClearFilter () {
                this.keyword = ''
                this.searchIp = ''
                this.fetchStepInstanceStatus()
            },
            handleLogSearch (value, event) {
                if (event.isComposing) {
                    // 跳过输入法复合事件
                    return
                }

                // 输入框的值被清空直接触发搜索
                // enter键开始搜索
                if ((value === '' && value !== this.keyword)
                    || event.keyCode === 13
                    || event.type === 'click') {
                    this.activeHostId = 0
                    this.activeIp = ''
                    this.activeBkCloudId = 0
                    this.activeIpStatus = ''
                    this.isHostLoading = true
                    this.maxHostNumPerGroup = 20
                    this.keyword = value
                    this.searchIp = ''
                    this.fetchStepInstanceStatus()
                }
            },
            handleIPSearch (value, event) {
                if (event.isComposing) {
                    // 跳过输入法复合事件
                    return
                }
                // 输入框的值被清空直接触发搜索
                // enter键开始搜索
                if ((value === '' && value !== this.searchIp)
                    || event.keyCode === 13
                    || event.type === 'click') {
                    this.activeHostId = 0
                    this.activeIp = ''
                    this.activeBkCloudId = 0
                    this.activeIpStatus = ''
                    this.isHostLoading = true
                    this.maxHostNumPerGroup = 20
                    this.keyword = ''
                    this.searchIp = value
                    this.fetchStepInstanceStatus()
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
        z-index: 500;
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
                justify-content: space-between;
                align-items: center;
                margin-top: 10px;
            }
            .step-name-text {
                flex: 1;
                height: 24px;
                max-width: 600px;
                overflow: hidden;
                font-size: 18px;
                line-height: 24px;
                color: #313238;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            .step-action-box {
                display: flex;
            }
            .action-btn {
                display: flex;
                width: 32px;
                height: 32px;
                margin-left: 8px;
                font-size: 16px;
                color: #979ba5;
                cursor: pointer;
                background: #fff;
                border: 1px solid #c4c6cc;
                border-radius: 2px;
                align-items: center;
                justify-content: center;
            }
        }
        .log-search-box {
            position: relative;
            display: flex;
            flex: 0 0 391px;
            background: #fff;

            .search-loading {
            position: absolute;
            top: 1px;
            right: 13px;
            bottom: 1px;
            display: flex;
            align-items: center;
            color: #c4c6cc;
            background: #fff;

            .loading-flag {
                animation: list-loading-ani 1s linear infinite;
            }
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
            max-width: 850px;
            height: 100%;
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
            min-width: 650px;
        }
    }
    .step-detail-sideslider {
        .bk-sideslider-wrapper {
            overflow-y: hidden;
        }
    }
</style>
