<template>
    <section>
        <div class="timeline-title">{{ time }}</div>
        <div class="timeline-content">
            <div class="timeline-collapse">
                <div
                    class="collapse-item"
                    v-for="(item, index) in data"
                    :key="index"
                >
                    <div
                        :class="{
                            'collapse-item-header': true,
                            'active': activeIndex === index
                        }"
                        @click="handleShowDetail(item, index)"
                    >
                        <div class="title">
                            <StatusIcon
                                class="icon"
                                :status="item.total === item.success ? 'normal' : 'error'"
                            ></StatusIcon>
                            <span
                                class="desc"
                                :title="getEventDescTitle(item.eventDesc)"
                                v-html="item.eventDesc"
                            ></span>
                            <span class="trigger-time">
                                {{ new Date(item.eventTime).toLocaleString().split('.').join('-') }}
                            </span>
                            <span
                                :class="{
                                    'success-num': true,
                                    'red': item.success !== item.total
                                }"
                            >
                                ({{ item.success }}/{{ item.total }})
                            </span>
                        </div>
                        <div class="header-right">
                            <a
                                v-if="activeIndex === index"
                                class="one-click-trigger"
                                v-perm="{
                                    hasPermission: curRepo.canUse,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: projectId,
                                        resourceType: RESOURCE_TYPE,
                                        resourceCode: curRepo.repositoryHashId,
                                        action: RESOURCE_ACTION.USE
                                    }
                                }"
                                v-bk-tooltips="$t('codelib.重放此事件，符合条件的流水线将触发执行')"
                                @click.stop="handleReplayAll(eventId)"
                            >
                                {{ $t('codelib.全部重放') }}
                            </a>
                            <bk-icon
                                :class="{
                                    'right-shape': true,
                                    'right-down': activeIndex === index
                                }"
                                type="angle-right"
                            />
                        </div>
                    </div>
                    <div
                        v-if="activeIndex === index"
                        class="filter-tab"
                    >
                        <div
                            v-for="(tab, tabIndex) in filterTabList"
                            :key="tabIndex"
                            :class="{
                                'tab-item': true,
                                'active': filterTab === tab.id
                            }"
                            @click="handleToggleTab(tab)"
                        >
                            {{ tab.name }}
                            <span class="num">{{ reasonNumMap[tab.key] }}</span>
                        </div>
                    </div>
                    <div
                        class="trigger-list-table"
                        v-if="activeIndex === index && eventDetailList.length"
                    >
                        <table
                            :class="{
                                'is-show-table': activeIndex === index
                            }"
                            v-bkloading="{ isLoading }"
                        >
                            <tbody>
                                <tr
                                    v-for="detail in eventDetailList"
                                    :key="detail.detailId"
                                >
                                    <td width="25%">
                                        <div class="cell">{{ detail.pipelineName }}</div>
                                    </td>
                                    <td width="75%">
                                        <div
                                            class="cell"
                                            v-if="detail.status === 'SUCCEED'"
                                        >
                                            <StatusIcon :status="detail.status"></StatusIcon>
                                            {{ detail.reason }}  |
                                            <span v-html="detail.buildNum"></span>
                                        </div>
                                        <div
                                            class="cell"
                                            v-else
                                        >
                                            <div
                                                v-for="i in detail.reasonDetailList"
                                                :key="i"
                                            >
                                                <StatusIcon :status="detail.status"></StatusIcon>
                                                <span style="color: red;">{{ detail.reason }}</span>  |
                                                <span>{{ i }}</span>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="replay-btn">
                                        <div class="cell">
                                            <a
                                                :class="{
                                                    'click-trigger': isZH
                                                }"
                                                v-perm="{
                                                    hasPermission: curRepo.canUse,
                                                    disablePermissionApi: true,
                                                    permissionData: {
                                                        projectId: projectId,
                                                        resourceType: RESOURCE_TYPE,
                                                        resourceCode: curRepo.repositoryHashId,
                                                        action: RESOURCE_ACTION.USE
                                                    }
                                                }"
                                                v-bk-tooltips="$t('codelib.重放此事件，仅触发当前流水线')"
                                                @click="handleReplay(detail)"
                                            >
                                                {{ $t('codelib.重放') }}
                                            </a>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <bk-pagination
                            class="trigger-table-pagination"
                            type="compact"
                            small
                            v-bind="pagination"
                            @change="handleChangePage"
                            @limit-change="handleChangeLimit"
                        >
                        </bk-pagination>
                    </div>
                    <div
                        class="trigger-list-table"
                        v-else-if="activeIndex === index && !eventDetailList.length"
                    >
                        <table
                            :class="{
                                'is-show-table': activeIndex === index
                            }"
                            v-bkloading="{ isLoading }"
                        >
                            <EmptyTableStatusVue
                                class="empty-table"
                                type="empty"
                            />
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </section>
</template>

<script>
    import {
        mapActions
    } from 'vuex'
    import {
        RESOURCE_ACTION,
        RESOURCE_TYPE
    } from '@/utils/permission'
    import StatusIcon from '../status-icon.vue'
    import EmptyTableStatusVue from '../empty-table-status.vue'
    export default {
        name: 'timeline-collapse',
        components: {
            StatusIcon,
            EmptyTableStatusVue
        },
        props: {
            data: {
                type: Object
            },
            time: {
                type: String
            },
            searchValue: {
                type: Object,
                default: () => {}
            },
            curRepo: {
                type: Object,
                default: () => {}
            }
        },
        data () {
            const reason = this.searchValue.find(i => i.id === 'reason')?.values[0].id || ''
            return {
                RESOURCE_ACTION,
                RESOURCE_TYPE,
                isLoading: false,
                activeIndex: -1,
                eventDetailList: [],
                eventId: '',
                pagination: {
                    current: 1,
                    limit: 10,
                    count: 0
                },
                isZH: true,
                filterTab: reason,
                reason,
                reasonNumMap: {}
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            filterTabList () {
                return [
                    { name: this.$t('codelib.触发成功'), id: 'TRIGGER_SUCCESS', key: 'triggerSuccess' },
                    { name: this.$t('codelib.触发失败'), id: 'TRIGGER_FAILED', key: 'triggerFailed' },
                    { name: this.$t('codelib.触发器不匹配'), id: 'TRIGGER_NOT_MATCH', key: 'triggerNotMatch' }
                ]
            }
        },
        created () {
            this.isZH = ['zh-CN', 'zh', 'zh_cn'].includes(document.documentElement.lang)
        },
        methods: {
            ...mapActions('codelib', [
                'fetchEventDetail',
                'fetchTriggerReasonNum',
                'replayAllEvent',
                'replayEvent'
            ]),

            /**
             * 全部重放
             */
            handleReplayAll (eventId) {
                this.$bkInfo({
                    title: this.$t('codelib.是否全部重放？'),
                    subTitle: this.$t('codelib.将使用此事件重新触发关联的流水线'),
                    confirmLoading: true,
                    confirmFn: async () => {
                        this.replayAllEvent({
                            projectId: this.projectId,
                            eventId
                        }).then(() => {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('codelib.重放成功')
                            })
                            this.$emit('replay')
                        })
                    }
                })
            },

            /**
             * 重新触发
             */
            handleReplay (payload) {
                const { detailId, pipelineName } = payload
                this.$bkInfo({
                    extCls: 'replay-dialog',
                    width: 400,
                    title: this.$t('codelib.是否重新触发？'),
                    subTitle: this.$t('codelib.确认以此事件重新触发流水线X吗？', [pipelineName]),
                    confirmLoading: true,
                    confirmFn: async () => {
                        this.replayEvent({
                            projectId: this.projectId,
                            detailId
                        }).then(() => {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('codelib.重放成功')
                            })
                            this.$emit('replay')
                        })
                    }
                })
            },

            /**
             * 展示触发事件详情
             */
            async handleShowDetail (data, index) {
                this.pagination.current = 1
                this.activeIndex === index ? this.activeIndex = -1 : this.activeIndex = index
                if (this.activeIndex === -1) return
                this.eventId = data.eventId
                await this.handleFetchTriggerReasonNum()
                await this.getEventDetail()
            },

            getEventDetail () {
                this.isLoading = true
                const pipelineId = this.searchValue.find(i => i.id === 'pipelineId')?.values[0].id || ''
                this.fetchEventDetail({
                    projectId: this.projectId,
                    eventId: this.eventId,
                    page: this.pagination.current,
                    pageSize: this.pagination.limit,
                    reason: this.filterTab,
                    pipelineId
                }).then(res => {
                    this.eventDetailList = res.records
                    this.pagination.count = res.count
                }).finally(() => {
                    this.isLoading = false
                })
            },
            handleChangeLimit (limit) {
                this.pagination.limit = limit
                this.pagination.current = 1
                this.getEventDetail()
            },
            handleChangePage (page) {
                this.pagination.current = page
                this.getEventDetail()
            },
            getEventDescTitle (str) {
                return str.replace(/(<\/?font.*?>)|(<\/?span.*?>)|(<\/?a.*?>)/gi, '')
            },
            handleToggleTab (tab) {
                this.filterTab = tab.id
                this.getEventDetail()
            },

            async handleFetchTriggerReasonNum () {
                try {
                    const pipelineId = this.searchValue.find(i => i.id === 'pipelineId')?.values[0].id || ''

                    this.reasonNumMap = await this.fetchTriggerReasonNum({
                        projectId: this.projectId,
                        eventId: this.eventId,
                        pipelineId
                    })
                    if (!this.reason) {
                        this.filterTab = this.filterTabList.find(i => this.reasonNumMap[i.key] > 0)?.id
                    }
                } catch (e) {
                    console.error(e)
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @keyframes fade-in {
        0% {
            opacity: 0;
        }
        30% {
            opacity: 0.3;
        }
        60% {
            opacity: 0.8;
        }
        100% {
            opacity: 1;
        }
    }
    .timeline-title {
        font-size: 14px;
        color: #63656e;
        padding-bottom: 10px;
        display: inline-block;
    }
    .timeline-content {
        word-break: break-all;
        font-size: 14px;
        color: #666;
    }
    .timeline-collapse {
        .collapse-item-header {
            position: relative;
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0 10px;
            height: 28px;
            font-size: 12px;
            border-radius: 2px;
            background-color: #FAFBFD;
            margin-bottom: 8px;
            cursor: pointer;
            &.active {
                background-color: #F0F5FF;
            }
            .title {
                display: flex;
                flex: 1;
                overflow: hidden;
                align-items: center;
            }
            .icon {
                flex-shrink: 0;
            }
            .desc {
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
        }
        .header-right {
            display: flex;
            align-items: center;
            flex-shrink: 0;
            margin-left: 50px;
            .one-click-trigger {
                font-size: 12px;
                margin-right: 22px;
            }
        }
        .right-shape {
            font-size: 24px !important;
            color: #C4C6CC;
            transition: 200ms transform;
            &.right-down {
                transform: rotate(90deg);
            }
        }
        .trigger-user {
            color: #979BA5;
        }
        .trigger-time {
            padding-left: 8px;
            color: #979BA5;
            flex-shrink: 0;
        }
        .success-num {
            padding-left: 24px;
            flex-shrink: 0;
            &.red {
                color: red;
            }
        }
        .filter-tab {
            display: flex;
            align-items: center;
            width: fit-content;
            height: 42px;
            border: 1px solid #dfe0e5;
            border-bottom: none;
            .tab-item {
                font-size: 12px;
                height: 42px;
                line-height: 42px;
                padding: 0 15px;
                border-right: 1px solid #dfe0e5;
                cursor: pointer;
                &:last-child {
                    border: none
                }
                &:hover {
                    color: #3a84ff;
                }
                &.active {
                    color: #3a84ff;
                }
            }
            .num {
                color: #979ba5;
                background: #f0f5ff;
                border-radius: 50%;
                padding: 2px;
            }
        }
        .trigger-list-table {
            width: 100%;
            margin-bottom: 8px;
            border: 1px solid #dfe0e5;
            border-radius: 2px;
            transition: opacity 3s linear;
            table {
                width: 100%;
            }
            tr {
                border-bottom: 1px solid #dfe0e5;
                height: 42px;
                max-height: 56px;
            }
            td {
                padding: 8px 16px 8px;
                .cell {
                    font-size: 12px;
                    line-height: 20px;
                    display: -webkit-box;
                    overflow: hidden;
                    -webkit-box-orient: vertical;
                }
            }
        }
        .trigger-table-pagination {
            height: 43px;
            padding: 5px 20px 0;
        }
        .is-show-table {
            animation: fade-in 1s ease-in-out;
        }
        .replay-btn {
            .cell {
                display: inline-block;
                width: 110px;
            }
            .click-trigger {
                display: inline-block;
                margin-left: 22px;
            }
        }
    }
    
</style>
<style lang="scss">
    .replay-dialog {
        .bk-dialog-header-inner {
            white-space: pre-wrap !important;
        }
    }
    .empty-table {
        .part-img {
            margin-top: 0 !important;
        }
        .part-text {
            margin-bottom: 50px !important;
        }
    }
    .trigger-user {
        color: #979BA5 !important;
    }
</style>
