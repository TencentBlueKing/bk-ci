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
                        }" @click="handleShowDetail(item, index)">
                        <div class="title">
                            <StatusIcon class="icon" :status="item.total === item.success ? 'normal' : 'error'"></StatusIcon>
                            <span class="desc" :title="getEventDescTitle(item.eventDesc)" v-html="item.eventDesc"></span>
                            <span class="trigger-time">
                                {{ new Date(item.eventTime).toLocaleString().split('.').join('-') }}
                            </span>
                            <span :class="{
                                'success-num': true,
                                'red': item.success !== item.total
                            }">
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
                                @click.stop="handleReplayAll(eventId)"
                            >
                                {{ $t('codelib.一键重新触发') }}
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
                        class="trigger-list-table"
                        v-if="activeIndex === index && eventDetailList.length"
                    >
                        <table
                            :class="{
                                'is-show-table': activeIndex === index
                            }"
                            v-bkloading="{ isLoading }">
                            <tbody>
                                <tr v-for="detail in eventDetailList" :key="detail.detailId">
                                    <td width="25%">
                                        <div class="cell">{{ detail.pipelineName }}</div>
                                    </td>
                                    <td width="75%">
                                        <div class="cell" v-if="detail.status === 'SUCCEED'">
                                            <StatusIcon :status="detail.status"></StatusIcon>
                                            {{ detail.reason }}  |
                                            <span v-html="detail.buildNum"></span>
                                        </div>
                                        <div class="cell" v-else>
                                            <div v-for="i in detail.reasonDetailList" :key="i">
                                                <StatusIcon :status="detail.status"></StatusIcon>
                                                <span style="color: red;">{{ detail.reason }}</span>  |
                                                <span>{{ i }}</span>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="replay-btn">
                                        <div class="cell">
                                            <a
                                                class="click-trigger"
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
                                                @click="handleReplay(detail)"
                                            >
                                                {{ $t('codelib.重新触发') }}
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
                            @limit-change="handleChangeLimit">
                        </bk-pagination>
                    </div>
                    <div
                        class="trigger-list-table"
                        v-else-if="activeIndex === index && !eventDetailList.length">
                        <table
                            :class="{
                                'is-show-table': activeIndex === index
                            }"
                            v-bkloading="{ isLoading }">
                            <EmptyTableStatusVue class="empty-table" type="empty" />
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
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        methods: {
            ...mapActions('codelib', [
                'fetchEventDetail',
                'replayAllEvent',
                'replayEvent'
            ]),

            /**
             * 一键重新触发
             */
            handleReplayAll (eventId) {
                this.$bkInfo({
                    title: this.$t('codelib.是否一键重新触发？'),
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
            handleShowDetail (data, index) {
                this.pagination.current = 1
                this.activeIndex === index ? this.activeIndex = -1 : this.activeIndex = index
                if (this.activeIndex === -1) return
                this.eventId = data.eventId
                this.getEventDetail()
            },

            getEventDetail () {
                this.isLoading = true
                const pipelineId = this.searchValue.find(i => i.id === 'pipelineId')?.values[0].id || ''
                this.fetchEventDetail({
                    projectId: this.projectId,
                    eventId: this.eventId,
                    page: this.pagination.current,
                    pageSize: this.pagination.limit,
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
        .trigger-list-table {
            width: 100%;
            margin: 8px 0;
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
