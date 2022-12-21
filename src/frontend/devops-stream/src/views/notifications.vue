<template>
    <article class="notifications-home">
        <aside class="aside-nav">
            <h3 class="nav-title">
                <i class="bk-icon icon-arrows-left" @click="backHome"></i>
                {{$t('notifications')}}
            </h3>
            <ul>
                <li v-for="nav in navList"
                    :key="nav.name"
                    :class="{ 'nav-item': true, active: curNav.name === nav.name }"
                >
                    <icon :name="nav.icon" size="18"></icon>
                    <bk-badge :val="unreadNum" theme="danger" position="right" :visible="!!unreadNum">
                        <span class="mr10">{{ nav.label }}</span>
                    </bk-badge>
                </li>
            </ul>
        </aside>

        <main class="notifications-main">
            <section class="notifications-head">
                <bk-radio-group v-model="onlyUnread" class="head-tab">
                    <bk-radio-button :value="false">{{$t('all')}}</bk-radio-button>
                    <bk-radio-button :value="true">{{$t('unread')}}</bk-radio-button>
                </bk-radio-group>
                <bk-button class="notifications-button" @click="readAll">{{$t('markRead')}}</bk-button>
                <bk-search-select
                    class="notifications-input rt50"
                    clearable
                    :show-condition="false"
                    :show-popover-tag-change="true"
                    :data="computedSearchList"
                    v-model="searchValue"
                >
                    <span slot="nextfix" class="mr5"></span>
                </bk-search-select>
            </section>
            <ul v-bkloading="{ isLoading }" class="notification-list">
                <li v-for="(notification, index) in notificationList" :key="index" class="notification-time">
                    <span class="notification-item-header">{{ notification.time }}</span>
                    <bk-collapse slot="content">
                        <bk-collapse-item :name="String(request.id)" v-for="request in notification.records" :key="request.id" @click.native="readMessage(request)">
                            <span class="content-message">
                                <span>
                                    <span :class="{ 'message-status': true, 'unread': !request.haveRead }"></span>
                                    {{ request.messageTitle }} （{{ request.contentAttr.failedNum }} / {{ request.contentAttr.total }}）
                                </span>
                                <span class="message-time">{{ request.createTime | timeFilter }}</span>
                            </span>
                            <bk-table :data="request.content" :show-header="false" slot="content" class="notification-table">
                                <bk-table-column>
                                    <template slot-scope="props">
                                        {{ props.row.pipelineName || '--' }}
                                    </template>
                                </bk-table-column>
                                <bk-table-column>
                                    <template slot-scope="props">
                                        {{ props.row.buildNum | buildNumFilter }}
                                    </template>
                                </bk-table-column>
                                <bk-table-column prop="triggerReasonName" show-overflow-tooltip></bk-table-column>
                                <bk-table-column prop="triggerReasonDetail" show-overflow-tooltip></bk-table-column>
                            </bk-table>
                        </bk-collapse-item>
                    </bk-collapse>
                </li>
            </ul>

            <bk-exception class="exception-wrap-item exception-part" type="empty" v-if="notificationList.length <= 0">{{ onlyUnread ? $t('readAll') : $t('noNotice') }}</bk-exception>

            <bk-pagination small
                :current.sync="compactPaging.current"
                :count.sync="compactPaging.count"
                :limit="compactPaging.limit"
                :show-limit="false"
                @change="pageChange"
                class="notify-paging"
            />
        </main>
    </article>
</template>

<script>
    import { notifications } from '@/http'
    import { mapState } from 'vuex'
    import { timeFormatter } from '@/utils'

    export default {
        filters: {
            buildNumFilter (val) {
                return val ? `# ${val}` : '--'
            },

            timeFilter (val) {
                return timeFormatter(val)
            }
        },

        data () {
            return {
                navList: [
                    { label: 'Inbox', name: 'inbox', icon: 'notify' }
                ],
                compactPaging: {
                    limit: 10,
                    current: 1,
                    count: 0
                },
                curNav: { name: 'inbox' },
                notificationList: [],
                onlyUnread: false,
                isLoading: false,
                unreadNum: 0,
                searchValue: [],
                messageId: '',
                triggerUserId: ''
            }
        },

        computed: {
            ...mapState(['projectId']),

            computedSearchList () {
                const datas = [
                    {
                        name: '事件ID',
                        id: 1,
                        multiable: false
                    },
                    {
                        name: '触发人',
                        id: 2,
                        multiable: false
                    }
                ]
                return datas.filter(data => {
                    return !this.searchValue.find(val => val.id === data.id)
                })
            }
        },

        watch: {
            onlyUnread () {
                this.getMessages()
            },
            searchValue () {
                this.messageId = ''
                this.triggerUserId = ''
                this.searchValue.forEach((val) => {
                    if (val.id === 1) {
                        this.messageId = val?.values[0]?.id
                    }
                    if (val.id === 2) {
                        this.triggerUserId = val?.values[0]?.id
                    }
                })
                this.getMessages()
            }
        },

        created () {
            this.initData()
            this.getMessages()
            this.getUnreadNum()
            document.title = this.$t('notifications')
        },

        methods: {
            initData () {
                this.messageId = this.$route.query.id
                if (this.messageId) {
                    this.searchValue = [{
                        id: 1,
                        name: '事件ID',
                        values: [{
                            id: this.messageId,
                            name: this.messageId
                        }]
                    }]
                }
            },

            getMessages () {
                this.isLoading = true
                const params = {
                    messageType: 'REQUEST',
                    page: this.compactPaging.current,
                    pageSize: this.compactPaging.limit,
                    projectId: this.projectId,
                    messageId: this.messageId,
                    triggerUserId: this.triggerUserId
                }
                if (this.onlyUnread) {
                    params.haveRead = false
                }
                notifications.getUserMessages(params).then((res) => {
                    this.notificationList = res.records
                    this.compactPaging.count = res.count
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            readAll () {
                notifications.readAllMessages(this.projectId).then(() => {
                    this.getMessages()
                    this.getUnreadNum()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            readMessage (message) {
                if (!message.haveRead) {
                    notifications.readMessage(message.id, this.projectId).then(() => {
                        message.haveRead = true
                        this.getUnreadNum()
                    }).catch((err) => {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    })
                }
            },

            getUnreadNum () {
                return notifications.getUnreadNotificationNum(this.projectId).then((res = {}) => {
                    this.unreadNum = res.data || 0
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            pageChange (page) {
                this.compactPaging.current = page
                this.getMessages()
            },

            goToPage ({ name }) {
                this.$router.push({ name })
            },

            backHome () {
                this.goToPage({ name: 'buildList', params: this.$route.params })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .notifications-home {
        display: flex;
        flex-direction: row;
        background: #f5f6fa;
        .notifications-main {
            flex: 1;
            background: #fff;
            margin: 25px;
            padding: 25px;
            over-flow: auto;
            .notifications-head {
                display: flex;
                align-items: center;
                margin-bottom: 20px;
                .head-tab {
                    width: 200px;
                }
            }
            .notifications-input {
                width: 500px;
                position: absolute;
                &.rt275 {
                    right: 275px;
                }
                &.rt50 {
                    right: 50px
                }
            }
            .notification-list {
                max-height: calc(100% - 90px);
                overflow: auto;
            }
            .notification-time {
                border: 1px solid #f5f6fa;
                margin-bottom: 25px;
                .notification-item-header {
                    display: block;
                    line-height: 40px;
                    padding: 0 10px;
                    background: #f5f6fa;
                }
                .notification-table {
                    width: calc(100% - 40px);
                    margin: 0 20px;
                }
                /deep/ .bk-collapse-item-content {
                    margin-bottom: 20px;
                }
            }
            .content-message {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding-right: 20px;
                .message-time {
                    color: #9fa2ad;
                }
            }
            .message-status {
                display: inline-block;
                margin-right: 8px;
                height: 12px;
                width: 12px;
                border-radius: 100px;
                background: #f0f1f5;
                &.unread {
                    background: #ff5656;
                }
            }
        }
        /deep/ .bk-badge {
            display: inline-block;
            margin-left: 15px;
            line-height: 14px;
        }
    }
    .notify-paging {
        margin: 12px 0 0;
        display: flex;
        align-items: center;
        justify-content: center;
        /deep/ span {
            outline: none;
            margin-left: 0;
        }
    }
</style>
