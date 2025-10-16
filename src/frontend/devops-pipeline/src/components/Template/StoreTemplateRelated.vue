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
                                v-if="row?.link?.show"
                                theme="primary"
                                :href="row.link.url"
                                target="_blank"
                                :icon="row.link.icon"
                                icon-placement="right"
                                @click="row.link.handler"
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
        <UpgradeFromStoreDialog
            v-model="showTemplateUpgradeDialog"
            :template-id="templateId"
            :project-id="projectId"
            @confirm="upgradeTemplate"
            @cancel="hideUpgradeDialog"
        />
    </section>
</template>
<script>
    import useInstance from '@/hook/useInstance'
    import { STRATEGY_ENUM } from '@/utils/pipelineConst'
    import dayjs from 'dayjs'
    import { computed, defineComponent, ref } from 'vue'
    import TemplateUpgradeStrategyDialog from './TemplateUpgradeStrategyDialog.vue'
    import UpgradeFromStoreDialog from '@/views/Template/List/UpgradeFromStoreDialog.vue'

    export default defineComponent({
        components: {
            TemplateUpgradeStrategyDialog,
            UpgradeFromStoreDialog
        },
        setup () {
            const { proxy, t } = useInstance()
            const upgradeStrategyDialog = ref(null)
            const showTemplateUpgradeDialog = ref(false)
            const projectId = computed(() => proxy.$route.params.projectId)
            const templateId = computed(() => proxy.$route.params.templateId)
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
                                key: 'srcMarketTemplateLatestVersionName',
                                value: t('template.latestVersionTitle', [srcMarketTemplateLatestVersionName]),
                                grayDesc: t(`template.${upgradeStrategy}-UPGRADE`),
                                link: srcMarketTemplateLatestVersionName === latestInstalledVersionName ? {
                                    show: true,
                                    text: t('template.goStore'),
                                    url: storeTemplateUrl.value,
                                    icon: 'devops-icon icon-jump-link'
                                } : {
                                    show: upgradeStrategy === STRATEGY_ENUM.MANUAL,
                                    text: t('template.install'),
                                    handler: hanleShowTemplateUpgradeDialog
                                }
                            },
                            {
                                key: 'latestInstalledVersionName',
                                value: t('template.latestInstallVersionTitle', [
                                    latestInstalledVersionName,
                                    isAutoUpgrade.value ? '' : `${t('editPage.by')} ${latestInstaller} `,
                                    dayjs(latestInstalledTime).format('YYYY-MM-DD HH:mm:ss')
                                ])
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
                            ...(upgradeStrategy === STRATEGY_ENUM.AUTO ? [{
                                key: 'settingSyncStrategy',
                                value: t(`template.${settingSyncStrategy}-SYNC`),
                                grayDesc: settingSyncStrategy === STRATEGY_ENUM.AUTO ? t('template.syncSettingStrategyDesc') : ''
                            }] : [])
                        ]
                    }
                ]
            })
            const activeName = ref(panels.value.map(panel => panel.name))

            function showUpgradeStrategyDialog () {
                upgradeStrategyDialog.value?.show?.()
            }

            function hanleShowTemplateUpgradeDialog () {
                showTemplateUpgradeDialog.value = true
            }
            function upgradeTemplate () {

            }
            function hideUpgradeDialog () {
                showTemplateUpgradeDialog.value = false
            }
            async function upgradeTemplate (version, done) {
                try {
                    await proxy.$store.dispatch('templates/importTemplateFromStore', {
                        projectId: projectId.value,
                        templateId: templateId.value,
                        params: {
                            marketTemplateId: relatedInfo.value.srcMarketTemplateId,
                            marketTemplateProjectId: relatedInfo.value.srcMarketProjectId,
                            marketTemplateVersion: version,
                            copySettings: true
                        }
                    })

                    proxy.$bkMessage({
                        theme: 'success',
                        message: t('template.templateUpgradeSuccess')
                    })
                    hideUpgradeDialog()
                    done()
                    proxy.$store.dispatch('atom/requestTemplateSummary', {
                        projectId: projectId.value,
                        templateId: templateId.value
                    })
                } catch (error) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                    done()
                    hideUpgradeDialog()
                }
            }
            return {
                showTemplateUpgradeDialog,
                upgradeStrategyDialog,
                showUpgradeStrategyDialog,
                hanleShowTemplateUpgradeDialog,
                projectId,
                templateId,
                panels,
                activeName,
                relatedInfo,
                storeTemplateUrl,
                upgradeTemplate,
                hideUpgradeDialog
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
