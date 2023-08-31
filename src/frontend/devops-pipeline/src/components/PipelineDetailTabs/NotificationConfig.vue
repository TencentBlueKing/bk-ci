<template>
    <bk-collapse v-model="activeName">
        <bk-collapse-item v-for="panel in panels" :key="panel.name" :name="panel.name" hide-arrow>
            <header :class="['pipeline-notification-panel-header', {
                'panel-header-collaped': !activeName.includes(panel.name)
            }]">
                <i class="devops-icon icon-down-shape"></i>
                {{ $t(`settings.${panel.label}`) }} ({{panel.subscriptions.length}})
            </header>
            <div slot="content">
                <ul class="pipeline-notification-blocks">
                    <li
                        v-for="(subscription, index) in panel.subscriptions"
                        :key="index"
                        :class="{
                            'has-group-id': subscription['wechatGroup']
                        }"
                    >
                        <div class="pipeline-notification-row" v-for="row in blockRows" :key="row.key" v-if="subscription[row.key]">
                            <span class="notification-block-row-label">{{ $t(`settings.${row.label}`) }}</span>
                            <div class="notification-block-row-array-value" v-if="Array.isArray(subscription[row.key])">
                                <bk-tag
                                    v-for="item in subscription[row.key]"
                                    :key="item"
                                >
                                    {{ item }}
                                </bk-tag>
                            </div>
                            <span v-else v-bk-tooltips="{
                                content: subscription[row.key],
                                delay: [300, 0],
                                disabled: row.key !== 'content',
                                maxWidth: 500,
                                allowHTML: false
                            }" class="notification-block-row-value">
                                {{ subscription[row.key] }}
                            </span>
                        </div>
                    </li>

                </ul>
            </div>
        </bk-collapse-item>
    </bk-collapse>
</template>

<script>
    import { mapGetters } from 'vuex'
    export default {

        data () {
            return {
                activeName: ['success', 'fail']
            }
        },
        computed: {
            ...mapGetters('atom', [
                'getPipelineSubscriptions'
            ]),
            panels () {
                return [{
                    name: 'success',
                    label: 'whenSuc',
                    subscriptions: this.getPipelineSubscriptions('success')
                }, {
                    name: 'fail',
                    label: 'whenFail',
                    subscriptions: this.getPipelineSubscriptions('fail')
                }]
            },
            blockRows () {
                return [
                    {
                        key: 'types',
                        label: 'noticeType'
                    },
                    {
                        key: 'groups',
                        label: 'noticeGroup'
                    },
                    {
                        key: 'users',
                        label: 'additionUser'
                    },
                    {
                        key: 'content',
                        label: 'noticeContent'
                    },
                    {
                        key: 'wechatGroup',
                        label: 'noticeGroupIds'
                    }
                ]
            }
        }
    }
</script>

<style lang="scss">
    .pipeline-notification-panel-header {
        padding: 0 16px;
        height: 40px;
        background: #F5F7FA;
        > i {
            display: inline-flex;
            transition: all 0.3s ease;
        }
        &.panel-header-collaped {
            > i {
                transform: rotate(-90deg);
            }
        }
    }
    .pipeline-notification-blocks {
        display: grid;
        grid-auto-flow: column;
        grid-gap: 24px;
        grid-template-columns: repeat(2, 1fr);
        margin: 16px 0;
        > li{
            height: 238px;
            background: #FFFFFF;
            border: 1px solid #DCDEE5;
            border-radius: 2px;
            padding: 24px;
            display: grid;
            grid-gap: 10px;
            grid-template-rows: 20px 20px 20px 100px;
            &.has-group-id {
                grid-template-rows: 20px 20px 20px 80px 20px;
                .pipeline-notification-row .notification-block-row-value {
                    -webkit-line-clamp: 4;
                }
            }
            .pipeline-notification-row {
                display: grid;
                grid-gap: 16px;
                grid-auto-flow: column;
                grid-template-columns: 180px 1fr;
                font-size: 12px;
                overflow: hidden;
                .noitification-block-row-label {
                    color: #979BA5;
                    line-height: 20px;
                }
                .notification-block-row-array-value {
                    display: grid;
                    grid-auto-flow: column;
                    grid-gap: 10px;
                    grid-template-columns: auto;
                    justify-content: flex-start;
                    .bk-tag {
                        margin: 0;
                    }
                }
                .notification-block-row-value {
                    color: #63656E;
                    display: -webkit-box;
                    -webkit-box-orient: vertical;
                    -webkit-line-clamp: 5;
                    overflow: hidden;
                    word-break: break-all;
                    line-height: 20px;
                }
            }
        }
    }
</style>
