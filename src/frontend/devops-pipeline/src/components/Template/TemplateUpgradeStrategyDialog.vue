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
                    :disabled="isLoading"
                >
                    <bk-radio
                        v-for="item in UPGRADE_STRATEGY_ENUM"
                        class="strategy-block-radio"
                        :key="item"
                        :value="item"
                    >
                        {{ $t(`template.${item}-UPGRADE`) }}
                        <span class="strategy-gray-desc">
                            ({{ $t(`template.${item}-upgradeStrategyDesc`) }})
                        </span>
                    </bk-radio>
                </bk-radio-group>
            </bk-form-item>
            <bk-form-item :label="$t('template.settingSyncStrategy')">
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
    import { defineComponent, nextTick, ref } from 'vue'
    export default defineComponent({
        setup (props, ctx) {
            const isLoading = ref(false)
            const isShow = ref(false)
            const { proxy } = useInstance()
            const UPGRADE_STRATEGY_ENUM = {
                AUTO: 'AUTO',
                MANUAL: 'MANUAL'
            }
            const strategyConf = ref({
                upgradeStrategy: UPGRADE_STRATEGY_ENUM.AUTO,
                settingSyncStrategy: UPGRADE_STRATEGY_ENUM.AUTO
            })

            async function setStrategy () {
                try {
                    isLoading.value = true
                    const res = await proxy.$store.dispatch('atom/setTemplateStrategy', {
                        projectId: proxy.$route.params.projectId,
                        templateId: proxy.$route.params.templateId,
                        upgradeStrategy: strategyConf.value.upgradeStrategy,
                        syncSettingStrategy: strategyConf.value.settingSyncStrategy
                            ? UPGRADE_STRATEGY_ENUM.MANUAL
                            : UPGRADE_STRATEGY_ENUM.AUTO
                    })
                    if (res) {
                        proxy.$bkMessage({
                            theme: 'success',
                            message: proxy.$t('template.upgradeSettingSuccess')
                        })
                        nextTick(() => {
                            toogleShow(false)
                        })
                    }
                } catch (error) {
                    console.error('Error setting strategy:', error)
                } finally {
                    isLoading.value = false
                }
            }

            function toogleShow (show) {
                isShow.value = show
            }

            ctx.expose({
                show () {
                    toogleShow(true)
                },
                hide () {
                    toogleShow(false)
                }
            })

            return {
                isShow,
                strategyConf,
                isLoading,
                UPGRADE_STRATEGY_ENUM,
                setStrategy,
                toogleShow
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
        margin-bottom: 12px !important;
    }
}
</style>
