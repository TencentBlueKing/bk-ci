<template>
    <section>
        <bk-collapse
            v-model="activeName"
            class="store-template-related info-collapse-panel"
        >
            <bk-collapse-item
                hide-arrow
                v-for="panel in panels"
                ext-cls="no-animation-collapse"
                :key="panel.name"
                :name="panel.name"
            >
                <header class="pipeline-base-config-panel-header">
                    {{ $t(`template.${panel.name}`) }}
                </header>
                <div
                    class="base-info-panel-content"
                    slot="content"
                >
                    <p
                        v-for="row in panel.rows"
                        :key="row.key"
                    >
                        <label class="base-info-block-row-label">
                            {{ $t(`template.${row.key}`) }}
                        </label>

                        <span class="base-info-block-row-value">
                            <span v-html="row.value || '--'" />
                            <span
                                v-if="row.grayDesc"
                                class="base-info-block-row-value-gray"
                            >
                                ({{ row.grayDesc }})
                            </span>
                            <bk-link
                                v-if="row.link"
                                theme="primary"
                                :href="row.link.url"
                                target="_blank"
                                icon="devops-icon icon-jump-link"
                                icon-placement="right"
                            >
                                {{ row.link.text }}
                            </bk-link>
                            <span
                                v-if="row.handler"
                                class="text-link"
                                @click.stop="row.handler"
                            >
                                <i
                                    class="devops-icon icon-edit-line"
                                />
                            </span>
                        </span>
                    </p>
                </div>
            </bk-collapse-item>
        </bk-collapse>
        <TemplateUpgradeStrategyDialog ref="upgradeStrategyDialog" />
    </section>
</template>
<script>
    import useInstance from '@/hook/useInstance'
import { STRATEGY_ENUM } from '@/utils/pipelineConst'
import dayjs from 'dayjs'
import { computed, defineComponent, ref } from 'vue'
import TemplateUpgradeStrategyDialog from './TemplateUpgradeStrategyDialog.vue'

    export default defineComponent({
        components: {
            TemplateUpgradeStrategyDialog
        },
        setup () {
            const { proxy, t } = useInstance()
            const upgradeStrategyDialog = ref(null)
            const relatedInfo = computed(() => {
                return proxy.$store.state.atom.pipelineInfo?.pipelineTemplateMarketRelatedInfo ?? {}
            })
            const isAutoUpgrade = computed(() => {
                return relatedInfo.value.upgradeStrategy === STRATEGY_ENUM.AUTO
            })
            const storeTemplateUrl = computed(() => {
                const { srcMarketTemplateId } = relatedInfo.value
                return `${WEB_URL_PREFIX}/store/atomStore/detail/template/${srcMarketTemplateId}`
            })
            const panels = computed(() => {
                const {
                    srcMarketTemplateName,
                    latestInstalledVersionName,
                    latestInstaller,
                    latestInstalledTime,
                    srcMarketTemplateLatestVersionName,
                    settingSyncStrategy,
                    upgradeStrategy
                } = relatedInfo.value
                return [
                    {
                        name: 'sourceInfo',
                        rows: [
                            {
                                key: 'srcMarketTemplateName',
                                value: srcMarketTemplateName
                            },
                            {
                                key: 'latestInstalledVersionName',
                                value: t('template.latestInstallVersionTitle', [
                                    latestInstalledVersionName,
                                    isAutoUpgrade.value ? '' : `${t('editPage.by')} ${latestInstaller} `,
                                    dayjs(latestInstalledTime).format('YYYY-MM-DD HH:mm:ss')
                                ])
                            },
                            {
                                key: 'srcMarketTemplateLatestVersionName',
                                value: t('template.latestVersionTitle', [srcMarketTemplateLatestVersionName]),
                                grayDesc: t(`template.${upgradeStrategy}-UPGRADE`),
                                link: {
                                    text: t('template.goStore'),
                                    url: storeTemplateUrl.value
                                }
                            }
                        ]
                    },
                    {
                        name: 'upgradeSetting',
                        rows: [
                            {
                                key: 'upgradeStrategy',
                                value: t(`template.${upgradeStrategy}-UPGRADE`),
                                grayDesc: t(`template.${upgradeStrategy}-upgradeStrategyDesc`),
                                handler: showUpgradeStrategyDialog
                            },
                            {
                                key: 'settingSyncStrategy',
                                value: t(`template.${settingSyncStrategy}-SYNC`),
                                grayDesc: t('template.syncSettingStrategyDesc')
                            }
                        ]
                    }
                ]
            })
            const activeName = ref(panels.value.map(panel => panel.name))

            function showUpgradeStrategyDialog () {
                upgradeStrategyDialog.value?.show?.()
            }
            return {
                upgradeStrategyDialog,
                showUpgradeStrategyDialog,
                panels,
                activeName,
                relatedInfo,
                storeTemplateUrl
            }
        }
    })
</script>
<style lang="scss">
    @import url('@/scss/info-collapsed.scss');
    .store-template-related {
        padding: 24px;
        .bk-link {
            line-height: 1;
            .bk-link-text {
                font-size: 12px;
            }
        }
    }
</style>
