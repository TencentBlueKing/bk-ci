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
                        <div>
                            <StatusIcon :status="item.total === item.success ? 'normal' : 'error'"></StatusIcon>
                            <span v-html="item.eventDesc"></span>
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
                        <bk-icon
                            :class="{
                                'right-shape': true,
                                'right-down': activeIndex === index
                            }"
                            svg
                            type="angle-right"
                            width="24"
                            height="24"
                        />
                        <a
                            v-if="activeIndex === index"
                            class="one-click-trigger"
                            @click.stop="handleReplayAll(item.eventId)"
                        >
                            {{ $t('codelib.一键重新触发') }}
                        </a>
                    </div>
                    <table
                        :class="{
                            'trigger-list-table': true,
                            'is-show-table': activeIndex === index
                        }"
                        v-if="activeIndex === index"
                        v-bkloading="{ isLoading }">
                        <tbody>
                            <tr v-for="detail in eventDetailList" :key="detail.detailId">
                                <td width="25%">
                                    <div class="cell">{{ detail.pipelineName }}</div>
                                </td>
                                <td width="55%">
                                    <div class="cell" v-if="detail.status === 'SUCCEED'">
                                        <StatusIcon :status="detail.status"></StatusIcon>
                                        {{ detail.reason }}  |
                                        <span v-html="detail.buildNum"></span>
                                    </div>
                                    <div class="cell">
                                        <div v-for="i in detail.reasonDetailList" :key="i">
                                            <StatusIcon :status="detail.status"></StatusIcon>
                                            <span style="color: red;">{{ detail.reason }}</span>  |
                                            <span>{{ i }}</span>
                                        </div>
                                    </div>
                                </td>
                                <td width="15%">
                                    <div class="cell">
                                        <a class="click-trigger" @click="handleReplay(detail.detailId)">{{ $t('codelib.重新触发') }}</a>
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </section>
</template>

<script>
    import {
        mapActions
    } from 'vuex'
    import StatusIcon from '../status-icon.vue'
    export default {
        name: 'timeline-collapse',
        components: {
            StatusIcon
        },
        props: {
            data: {
                type: Object
            },
            time: {
                type: String
            }
        },
        data () {
            return {
                isLoading: false,
                activeIndex: -1,
                eventDetailList: []
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
            handleReplayAll (id) {
                this.replayAllEvent({
                    projectId: this.projectId,
                    eventId: id
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('codelib.触发成功')
                    })
                })
            },

            /**
             * 重新触发
             */
            handleReplay (id) {
                this.replayEvent({
                    projectId: this.projectId,
                    detailId: id
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('codelib.触发成功')
                    })
                })
            },

            /**
             * 展示触发事件详情
             */
            handleShowDetail (data, index) {
                this.activeIndex === index ? this.activeIndex = -1 : this.activeIndex = index
                if (this.activeIndex === -1) return
                this.isLoading = true
                this.fetchEventDetail({
                    projectId: this.projectId,
                    eventId: data.eventId
                }).then(res => {
                    this.eventDetailList = res.records
                }).finally(() => {
                    this.isLoading = false
                })
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
                background-color: #E1ECFF;
            }
            .one-click-trigger {
                position: absolute;
                right: 60px;
                font-size: 12px;
            }
        }
        .right-shape {
            transition: 200ms transform;
            &.right-down {
                transform: rotate(90deg);
            }
        }
        .trigger-time {
            padding-left: 8px;
            color: #979BA5;
        }
        .success-num {
            padding-left: 24px;
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
        .is-show-table {
            animation: fade-in 1s ease-in-out;
        }
    }
</style>
