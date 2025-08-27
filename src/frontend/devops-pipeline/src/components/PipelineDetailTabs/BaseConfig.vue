<template>
    <bk-collapse
        class="info-collapse-panel"
        v-model="activeName"
    >
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
            <div
                class="base-info-panel-content"
                slot="content"
            >
                <p
                    v-for="row in panel.rows"
                    :key="row.key"
                >
                    <ul
                        v-if="row.key === 'parallelConfDetail'"
                        class="parallel-conf-detail"
                    >
                        <li
                            class="parallel-conf-detail-row"
                            v-for="item in parallelSettingRows"
                            :key="item.key"
                        >
                            <label>
                                {{ $t(item.label) }}
                            </label>
                            <span>
                                {{ item.value }}
                            </span>
                        </li>
                    </ul>
                    <template v-else>
                        <label
                            v-if="row.key !== 'namingConvention'"
                            class="base-info-block-row-label"
                        >{{ $t(row.key) }}</label>
                        <bk-popover
                            v-else
                            theme="light"
                            :width="892"
                            placement="top-start"
                        >
                            <label class="base-info-block-row-label dotted">{{ $t(row.key) }}</label>
                            <div slot="content">
                                <NamingConventionTip />
                            </div>
                        </bk-popover>
                        <span class="base-info-block-row-value">
                            <template v-if="Array.isArray(row.value)">
                                <template v-if="row.value.length > 0">
                                    <bk-tag
                                        v-for="label in row.value"
                                        :key="label"
                                        class="base-info-block-row-value-label"
                                        @click="goPipelineManageList(row.key, label.id)"
                                    >
                                        {{ row.key === 'pipelineGroup' ? label.name : label }}
                                    </bk-tag>
                                </template>
                                <template v-else>
                                    --
                                </template>
                            </template>
                            <template v-else>
                                <span>{{ row.value || '--' }}</span>
                                <span
                                    v-if="row.grayDesc"
                                    class="base-info-block-row-value-gray"
                                >{{ row.grayDesc }}</span>
                            </template>
                        </span>
                    </template>
                </p>
            </div>
        </bk-collapse-item>
    </bk-collapse>
</template>
<script>
    import NamingConventionTip from '@/components/namingConventionTip.vue'
    import { convertTime } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'
    
    export default {
        components: {
            NamingConventionTip
        },
        props: {
            basicInfo: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                activeName: ['baseInfo', 'executeConfig'],
                namingStyle: {
                    CLASSIC: this.$t('CLASSIC'),
                    CONSTRAINED: this.$t('CONSTRAINED')
                },
                currentGroups: []
            }
        },
        computed: {
            ...mapGetters('atom', [
                'isTemplate'
            ]),
            ...mapState('pipelines', [
                'allPipelineGroup'
            ]),
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
                const { inheritedDialect, projectDialect, pipelineDialect } = basicInfo?.pipelineAsCodeSettings ?? {}
                const namingConvention = inheritedDialect ? this.namingStyle[projectDialect] : this.namingStyle[pipelineDialect]
                return this.isTemplate
                    ? [
                        {
                            key: 'name',
                            value: basicInfo?.name
                        },
                        {
                            key: 'desc',
                            value: basicInfo?.desc
                        },
                        {
                            key: 'template.templateType',
                            value: this.$t(`template.${basicInfo?.type}`)
                        },
                        {
                            key: 'label',
                            value: basicInfo?.labelNames ?? []
                        },
                        {
                            key: 'creator',
                            value: basicInfo?.creator
                        },
                        {
                            key: 'createTime',
                            value: convertTime(basicInfo?.createdTime)
                        }
                    ]
                    : [
                        {
                            key: 'pipelineName',
                            value: basicInfo?.pipelineName
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
                            value: basicInfo?.desc
                        },
                        {
                            key: 'namingConvention',
                            value: namingConvention,
                            grayDesc: inheritedDialect ? ` ( ${this.$t('inheritedProject')} )` : ''
                        },
                        {
                            key: 'modificationDetail',
                            value: basicInfo?.versionUpdater,
                            grayDesc: ` | ${convertTime(basicInfo?.versionUpdateTime)}`
                        },
                        {
                            key: 'creatorDetail',
                            value: basicInfo?.creator,
                            grayDesc: ` | ${convertTime(basicInfo?.createTime)}`
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
                        key: 'settings.whenVariableExceedsLength',
                        value: this.$t(this.basicInfo?.failIfVariableInvalid ? 'settings.errorAndHalt' : 'settings.clearTheValue')
                    },
                    {
                        key: 'parallelSetting',
                        value: this.$t(`settings.runningOption.${runLockType ?? '--'}`)
                    },
                    ...(['group_lock', 'multiple'].includes(runLockType)
                        ? [{
                            key: 'parallelConfDetail'
                        }]
                        : []
                    )
                ]
            },
            parallelSettingRows () {
                const runLockType = this.basicInfo?.runLockType?.toLowerCase?.()
                if (runLockType === 'group_lock') {
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
                                    value: Number.isInteger(this.basicInfo?.waitQueueTimeMinute) ? `${this.basicInfo?.waitQueueTimeMinute}${this.$t('settings.minutes')}` : '--'
                                }

                            ]
                            : []
                        )
                    ]
                }

                return [
                    ...(!this.basicInfo?.concurrencyCancelInProgress
                        ? [
                            {
                                key: 'maxConRunningQueueSize',
                                label: 'settings.concurrentMaxConcurrency',
                                value: this.basicInfo?.maxConRunningQueueSize ?? '--'
                            },
                            {
                                key: 'waitQueueTimeMinute',
                                label: 'settings.concurrentTimeout',
                                value: Number.isInteger(this.basicInfo?.waitQueueTimeMinute) ? `${this.basicInfo?.waitQueueTimeMinute}${this.$t('settings.minutes')}` : '--'
                            }
                        ]
                        : []
                    )
                ]
            }
        },
        async mounted () {
            if (!this.allPipelineGroup.length) {
                const res = await this.requestAllPipelineGroup({
                    projectId: this.$route.params.projectId
                })
                this.currentGroups = res.data
            }
        },
        methods: {
            ...mapActions('pipelines', ['requestAllPipelineGroup']),
            goPipelineManageList (key, viewId) {
                if (key === 'pipelineGroup') {
                    this.$router.push({
                        name: 'PipelineManageList',
                        params: {
                            viewId
                        }
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import url('@/scss/info-collapsed.scss');
</style>
