<template>
    <bk-collapse v-model="activeName">
        <bk-collapse-item v-for="panel in panels" :key="panel.name" :name="panel.name" hide-arrow>
            <header class="pipeline-base-config-panel-header">
                {{ $t(panel.name) }}
            </header>
            <div class="base-info-panel-content" slot="content">
                <p v-for="row in panel.rows" :key="row">
                    <label class="base-info-block-row-label">{{ $t(row.key) }}</label>
                    <span class="base-info-block-row-value">
                        <template v-if="['label', 'pipelineGroup'].includes(row.key)">
                            <bk-tag
                                v-for="label in row.value"
                                :key="label"
                                class="base-info-block-row-value-label"
                            >
                                {{ label }}
                            </bk-tag>
                        </template>
                        <template v-else>
                            {{ row.value }}
                        </template>
                    </span>
                </p>
            </div>
        </bk-collapse-item>
    </bk-collapse>
</template>

<script>
    import { mapState } from 'vuex'
    import { convertTime } from '@/utils/util'
    export default {
        data () {
            return {
                activeName: ['baseInfo', 'executeConfig']
            }
        },
        computed: {
            ...mapState('pipelines', ['pipelineInfo']),
            ...mapState('atom', ['pipelineSetting']),
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
                const { pipelineInfo, setting } = this
                return [
                    {
                        key: 'pipelineName',
                        value: pipelineInfo?.pipelineName ?? '--'
                    },
                    {
                        key: 'label',
                        value: setting?.labels ?? []
                    },
                    {
                        key: 'pipelineGroup',
                        value: setting?.viewNames ?? []
                    },
                    {
                        key: 'desc',
                        value: pipelineInfo?.pipelineDesc ?? '--'
                    },
                    {
                        key: 'creator',
                        value: pipelineInfo?.creator ?? '--'
                    },
                    {
                        key: 'createTime',
                        value: convertTime(pipelineInfo?.createTime) ?? '--'
                    }
                ]
            },
            executeConfRows () {
                return [
                    {
                        key: 'customBuildNum',
                        value: this.pipelineSetting?.buildNumRule ?? '${{DATE:”yyyyMMdd”}}.${{BUILD_NO_OF_DAY}}'
                    },
                    {
                        key: 'parallelSetting',
                        value: this.$t(`settings.runningOption.${this.pipelineSetting?.runLockType.toLowerCase()}`)
                    }
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
    .base-info-panel-content {
        display: grid;
        grid-gap: 16px;
        grid-template-row: minmax(18px, auto);
        margin-bottom: 32px;
        > p {
            display: grid;
            grid-auto-flow: column;
            grid-template-columns: 120px 1fr;
            align-items: flex-start;
            grid-gap: 10px;
            font-size: 12px;
            color: #63656e;
            > label {
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
