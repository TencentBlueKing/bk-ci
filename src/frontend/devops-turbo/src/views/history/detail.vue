<template>
    <article class="history-detail-home">
        <template v-if="hasPermission">
            <bk-breadcrumb separator-class="bk-icon icon-angle-right" class="bread-crumb">
                <bk-breadcrumb-item :to="{ name: 'history' }"> {{ $t('turbo.历史列表') }}</bk-breadcrumb-item>
                <bk-breadcrumb-item :to="{ name: 'history', query: { pipelineId: detail.pipelineId, clientIp: detail.clientIp } }">{{ detail.pipelineName || detail.clientIp }}</bk-breadcrumb-item>
                <bk-breadcrumb-item>#{{ detail.executeCount }}</bk-breadcrumb-item>
            </bk-breadcrumb>

            <section class="g-turbo-box hisory-detail-data" v-bkloading="{ isloading }">
                <header class="detail-header">
                    <span class="header-title">
                        【#{{ detail.executeCount }}】
                        <span class="header-name">{{ detail.pipelineName || detail.clientIp }}</span>
                        <task-status :status="detail.status" :show-name="false"></task-status>
                    </span>
                    <span class="header-time">
                        <span><span> {{ $t('turbo.开始时间：') }} </span><span>{{ detail.startTime }}</span></span>
                        <span><span> {{ $t('turbo.总耗时：') }} </span><span>{{ detail.elapsedTime }}</span></span>
                    </span>
                </header>
                <bk-divider></bk-divider>
                <ul class="detail-list">
                    <li class="task-item" v-for="task in detail.displayFields" :key="task.fieldName">
                        <span class="task-title">{{ task.fieldName }}:</span>
                        <span class="task-value">{{ task.fieldValue }}</span>
                        <logo name="cc-jump-link" class="task-link" @click.native="openLink(task.linkAddress)" v-if="task.linkAddress"></logo>
                    </li>
                </ul>
                <bk-link theme="primary" :href="detail.recordViewUrl" target="_blank" class="detail-link" v-if="detail.recordViewUrl">
                    {{ $t('turbo.查看编译过程数据') }}
                    <logo name="cc-jump-link" size="14"></logo>
                </bk-link>
            </section>
        </template>
        <permission-exception v-else :message="errMessage" />
    </article>
</template>

<script>
    import { getTurboRecord } from '@/api'
    import logo from '@/components/logo'
    import taskStatus from '@/components/task-status'
    import permissionException from '@/components/exception/permission.vue'

    export default {
        components: {
            logo,
            taskStatus,
            permissionException
        },

        data () {
            return {
                detail: {},
                isloading: false,
                hasPermission: true,
                errMessage: ''
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                const id = this.$route.params.id
                this.isloading = true
                getTurboRecord(id).then((res) => {
                    this.detail = res || {}
                }).catch((err) => {
                    if (err.code === 2300017) {
                        this.hasPermission = false
                        this.errMessage = err.message
                    } else {
                        this.$bkMessage({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }
                }).finally(() => {
                    this.isloading = false
                })
            },

            openLink (linkAddress) {
                window.open(linkAddress, '_blank')
            }
        }
    }
</script>

<style lang="scss" scoped>
    .history-detail-home {
        padding: 10px 20px 28px;
        margin: 0 auto;
        .bread-crumb {
            font-size: 12px;
            margin-bottom: 10px;
            ::v-deep .bk-breadcrumb-separator {
                font-size: 14px;
            }
            .bk-breadcrumb-item:last-child {
                color: #000000;
            }
        }
    }

    .hisory-detail-data {
        padding: 25px 32px;
        .detail-header {
            line-height: 22px;
            margin-bottom: 17px;
            display: flex;
            justify-content: space-between;
            .header-title {
                color: #000;
                display: flex;
                align-items: center;
                .header-name {
                    display: inline-block;
                    margin: 0 10px 0 2px;
                }
            }
            .header-time {
                color: #63656e;
                display: flex;
                align-items: center;
                line-height: 18px;
                >:first-child {
                    display: inline-block;
                    padding-right: 35px;
                    border-right: 1px solid #D8D8D8;
                }
                >:last-child {
                    display: inline-block;
                    margin-left: 35px;
                }
            }
        }
        .detail-list {
            padding: 15px 0 16px;
            &::after {
                content: '';
                display: table;
                clear: both;
            }
            .task-item {
                width: 100%;
                float: left;
                line-height: 22px;
                margin-top: 16px;
                display: flex;
                align-items: center;
                .task-title {
                    display: inline-block;
                    width: 150px;
                    color: #999999;
                }
                .task-value {
                    color: #222222;
                }
                .task-link {
                    color: #3a84ff;
                    margin-left: 11px;
                    cursor: pointer;
                }
            }
        }
        .detail-link ::v-deep .bk-link-text {
            display: flex;
            align-items: center;
            svg {
                margin-left: 5px;
            }
        }
    }

    ::v-deep .bk-divider {
        font-size: 0;
    }
</style>
