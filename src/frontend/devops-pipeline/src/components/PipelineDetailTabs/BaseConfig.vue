<template>
    <bk-collapse v-model="activeName">
        <bk-collapse-item
            hide-arrow
            ext-cls="no-animation-collapse"
            v-for="panel in panels"
            :key="panel.name"
            :name="panel.name"
        >
            <header class="pipeline-base-config-panel-header">
                {{ $t(panel.name) }}
            </header>
            <div class="base-info-panel-content" slot="content">
                <p v-for="row in panel.rows" :key="row.key">
                    <ul v-if="row.key === 'parallelConfDetail'" class="parallel-conf-detail">
                        <li class="parallel-conf-detail-row" v-for="item in prarallelSettingRows" :key="item.key">
                            <label>
                                {{ $t(item.label) }}
                            </label>
                            <span>
                                {{ item.value }}
                            </span>
                        </li>
                    </ul>
                    <template v-else>
                        <label class="base-info-block-row-label">{{ $t(row.key) }}</label>
                        <span class="base-info-block-row-value">
                            <template v-if="['label', 'pipelineGroup'].includes(row.key)">
                                <template v-if="row.value.length > 0">
                                    <bk-tag v-for="label in row.value" :key="label" class="base-info-block-row-value-label">
                                        {{ label }}
                                    </bk-tag>
                                </template>
                                <template v-else>
                                    --
                                </template>
                            </template>
                            <template v-else>
                                {{ row.value || '--' }}
                            </template>
                        </span>
                    </template>
                </p>
            </div>
        </bk-collapse-item>
    </bk-collapse>
</template>
<script>
    import { convertTime } from '@/utils/util'
    export default {
        props: {
            basicInfo: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                activeName: ['baseInfo', 'executeConfig']
            }
        },
        computed: {
            panels () {
                return [{
                    name: 'baseInfo',
                    rows: this.baseInfoRows
                }, {
                    name: 'executeConfig',
                    rows: this.executeConfRows
                }]
            },
            baseInfoRows () {
                const { basicInfo } = this
                return [
                    {
                        key: 'pipelineName',
                        value: basicInfo?.pipelineName ?? '--'
                    },
                    {
                        key: 'label',
                        value: basicInfo?.labelNames ?? []
                    },
                    {
                        key: 'pipelineGroup',
                        value: basicInfo?.viewNames ?? []
                    },
                    {
                        key: 'desc',
                        value: basicInfo?.desc ?? '--'
                    },
                    {
                        key: 'creator',
                        value: basicInfo?.creator ?? '--'
                    },
                    {
                        key: 'createTime',
                        value: convertTime(basicInfo?.createTime) ?? '--'
                    }
                ]
            },
            executeConfRows () {
                const runLockType = this.basicInfo?.runLockType?.toLowerCase?.()
                return [
                    {
                        key: 'customBuildNum',
                        value: this.basicInfo?.buildNumRule ?? '--'
                    },
                    {
                        key: 'parallelSetting',
                        value: this.$t(`settings.runningOption.${runLockType ?? '--'}`)
                    },
                    ...(runLockType === 'group_lock'
                        ? [{
                            key: 'parallelConfDetail'
                        }]
                        : []
                    )
                ]
            },
            prarallelSettingRows () {
                return [
                    {
                        key: 'concurrencyGroup',
                        label: 'group.groupName',
                        value: this.basicInfo?.concurrencyGroup ?? '--'
                    },
                    {
                        key: 'concurrencyCancelInProgress',
                        label: 'settings.stopWhenNewCome',
                        value: this.$t(this.basicInfo?.concurrencyCancelInProgress ? 'true' : 'false')
                    },
                    ...(!this.basicInfo?.concurrencyCancelInProgress
                        ? [
                            {
                                key: 'maxQueueSize',
                                label: 'settings.largestNum',
                                value: this.basicInfo?.maxQueueSize ?? '--'
                            },
                            {
                                key: 'waitQueueTimeMinute',
                                label: 'settings.lagestTime',
                                value: this.basicInfo?.waitQueueTimeMinute ?? '--'
                            }

                        ]
                        : []
                    )
                ]
            }
        }
    }
</script>

<style lang="scss">
.pipeline-base-config-panel-header {
    font-size: 14px;
    font-weight: 700;
    height: 24px;
    line-height: 24px;
    border-bottom: 1px solid #DCDEE5;
}
.no-animation-collapse {
    .collapse-transition {
        transition: none !important;
    }
}
.base-info-panel-content {
    display: grid;
    grid-gap: 16px;
    grid-template-rows: minmax(18px, auto);
    margin-bottom: 32px;
    .parallel-conf-detail {
        border: 1px solid #DCDEE5;
        margin-left: 130px;
        padding: 0 25px;
        border-radius: 2px;
        width: 600px;
        .parallel-conf-detail-row {
            line-height: 32px;
            > label {
                line-height: 32px;
                color: #63656e;
            }
            > span {
                color: #313238;
            }
        }
    }

    >p,
    .parallel-conf-detail-row {
        display: grid;
        grid-auto-flow: column;
        grid-template-columns: 120px 1fr;
        align-items: flex-start;
        grid-gap: 10px;
        font-size: 12px;
        color: #63656e;

        >label {
            text-align: right;
            line-height: 18px;
            color: #979BA5;
        }

        .bk-tag {
            margin-top: 0;
        }
    }

}
</style>
