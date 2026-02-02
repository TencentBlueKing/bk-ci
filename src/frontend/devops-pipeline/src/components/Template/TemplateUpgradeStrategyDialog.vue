<template>
    <bk-dialog
        v-model="isShow"
        :title="$t('template.upgradeSetting')"
        :width="640"
        header-position="left"
        @confirm="setStrategy"
        @cancel="toogleShow(false)"
    >
        <bk-form
            form-type="vertical"
            class="template-strategy-form"
        >
            <bk-form-item
                :label="$t('template.upgradeStrategy')"
            >
                <bk-radio-group
                    v-model="strategyConf.upgradeStrategy"
                    @change="handleUpgradeStrategyChange"
                    :disabled="isLoading"
                >
                    <bk-radio
                        class="strategy-block-radio"
                        value="AUTO"
                    >
                        {{ $t(`template.AUTO-UPGRADE`) }}
                        <span class="strategy-gray-desc">
                            ({{ $t(`template.AUTO-upgradeStrategyDesc`) }})
                        </span>
                    </bk-radio>

                    <bk-alert
                        v-if="showAutoUpgradeStrategyTips"
                        type="info"
                        :title="$t('template.autoUpgradeStrategyTips', [relatedInfo.srcMarketTemplateLatestVersionName ?? 'V1.8.0'])"
                    ></bk-alert>

                    <bk-radio
                        class="strategy-block-radio"
                        value="MANUAL"
                    >
                        {{ $t(`template.MANUAL-UPGRADE`) }}
                        <span class="strategy-gray-desc">
                            ({{ $t(`template.MANUAL-upgradeStrategyDesc`) }})
                        </span>
                    </bk-radio>
                </bk-radio-group>
            </bk-form-item>
            <bk-form-item
                v-if="isAutoUpgrade"
                :label="$t('template.settingSyncStrategy')"
            >
                <bk-checkbox
                    v-model="strategyConf.syncSettingStrategy"
                    :disabled="isLoading"
                >
                    {{ $t(`template.AUTO-SYNC`) }}
                </bk-checkbox>
                <span class="strategy-gray-desc">
                    ({{ $t(`template.syncSettingStrategyDesc`) }})
                </span>
            </bk-form-item>
        </bk-form>
    </bk-dialog>
</template>

<script>
    import useInstance from '@/hook/useInstance'
    import { STRATEGY_ENUM } from '@/utils/pipelineConst'
    import { computed, defineComponent, nextTick, ref, watch } from 'vue'
    export default defineComponent({
        setup (props, ctx) {
            const isLoading = ref(false)
            const isShow = ref(false)
            const { proxy } = useInstance()
            const relatedInfo = computed(() => proxy.$store.state.atom.pipelineInfo?.pipelineTemplateMarketRelatedInfo ?? {})
            const showAutoUpgradeStrategyTips = computed(()=>
                strategyConf.value.upgradeStrategy === STRATEGY_ENUM.AUTO && (relatedInfo.value.latestInstalledVersion !== relatedInfo.value.srcMarketTemplateLatestVersion)
            )
            const strategyConf = ref({
                upgradeStrategy: relatedInfo.value.upgradeStrategy,
                syncSettingStrategy: relatedInfo.value.settingSyncStrategy === STRATEGY_ENUM.AUTO
            })
            const isAutoUpgrade = computed(() => {
                return strategyConf.value.upgradeStrategy === STRATEGY_ENUM.AUTO
            })

            watch(() => isShow.value, (nv) => {
                if (nv) {
                    strategyConf.value.upgradeStrategy = relatedInfo.value.upgradeStrategy
                }
            }, {
                immediate: true
            })

            ctx.expose({
                show () {
                    toogleShow(true)
                },
                hide () {
                    toogleShow(false)
                }
            })

            async function setStrategy () {
                try {
                    isLoading.value = true
                    const tempParams = {
                        projectId: proxy.$route.params.projectId,
                        templateId: proxy.$route.params.templateId
                    }
                    const res = await proxy.$store.dispatch('atom/setTemplateStrategy', {
                        ...tempParams,
                        upgradeStrategy: strategyConf.value.upgradeStrategy,
                        settingSyncStrategy: strategyConf.value.syncSettingStrategy
                            ? STRATEGY_ENUM.AUTO
                            : STRATEGY_ENUM.MANUAL
                    })
                    if (res) {
                        proxy.$bkMessage({
                            theme: 'success',
                            message: proxy.$t('template.upgradeSettingSuccess')
                        })
                        proxy.$store.dispatch('atom/requestTemplateSummary', tempParams)
                        nextTick(() => {
                            toogleShow(false)
                        })
                    }
                } catch (error) {
                    console.error('Error setting strategy:', error)
                    proxy.$bkMessage({
                        theme: 'error',
                        message: error.message ?? error
                    })
                } finally {
                    isLoading.value = false
                }
            }

            function toogleShow (show) {
                isShow.value = show
            }

            function handleUpgradeStrategyChange (value) {
                strategyConf.value.syncSettingStrategy = false
            }

            return {
                isShow,
                strategyConf,
                isLoading,
                STRATEGY_ENUM,
                setStrategy,
                toogleShow,
                relatedInfo,
                showAutoUpgradeStrategyTips,
                isAutoUpgrade,
                handleUpgradeStrategyChange
            }
        }
    })
</script>

<style lang="scss">
.template-strategy-form {
    min-height: 222px;
    .strategy-gray-desc {
        font-size: 12px;
        color: #979BA5;
    }
    .strategy-block-radio {
        display: flex;
        align-items: center;
        margin-bottom: 6px !important;
        &:not(:last-child) {
            margin-bottom: 12px !important;
        }
    }
    .bk-alert {
        margin-bottom: 12px;
    }
    .bk-label-text {
        font-size: 12px;
    }
    .bk-form-item .bk-label {
        margin-bottom: 2px;
    }
    .bk-form-content {
        line-height: 1;
    }
}
</style>
