<template>
    <bk-dialog
        :value="isShow"
        :width="560"
        :auto-close="false"
        :show-footer="false"
        :title="$t('newlist.chooseExport')"
        @cancel="handleCancel"
    >
        <ul class="export-list">
            <li
                v-for="exportItem in exportList"
                :key="exportItem.exportUrl"
                class="export-item"
            >
                <logo
                    :name="exportItem.icon"
                    class="export-icon"
                />
                <h5 class="export-title">{{ exportItem.title }}</h5>
                <p class="export-tip">
                    {{ exportItem.tips }}<a
                        :href="exportItem.tipsLink"
                        v-if="exportItem.tipsLink"
                        target="_blank"
                    >{{ $t('newlist.knowMore') }}</a>
                </p>
                <bk-button
                    class="export-button"
                    @click="downLoadFromApi(exportItem)"
                    :loading="isDownLoading"
                >
                    {{ $t('newlist.exportPipelineJson') }}
                </bk-button>
            </li>
        </ul>
    </bk-dialog>
</template>

<script>
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import Logo from '@/components/Logo'
    import { mapActions, mapState } from 'vuex'
    import { CODE_MODE, UI_MODE } from '@/utils/pipelineConst'

    export default {
        components: {
            Logo
        },
        props: {
            isShow: Boolean
        },

        data () {
            return {
                isDownLoading: false
            }
        },

        computed: {
            ...mapState('atom', [
                'pipeline',
                'pipelineSetting',
                'pipelineInfo'
            ]),

            projectId () {
                return this.$route.params.projectId
            },

            pipelineId () {
                return this.$route.params.pipelineId
            },

            pipelineName () {
                return this.pipelineInfo?.pipelineName ?? '--'
            },

            exportList () {
                const archiveFlag = this.$route.query?.archiveFlag
                const queryString = archiveFlag ? `?archiveFlag=${encodeURIComponent(archiveFlag)}` : ''
                return [
                    {
                        type: UI_MODE,
                        title: 'Pipeline Json',
                        icon: 'export-pipeline',
                        name: `${this.pipelineName}.json`,
                        tips: this.$t('newlist.exportJsonTip'),
                        exportUrl: `${API_URL_PREFIX}/${PROCESS_API_URL_PREFIX}/user/pipelines/${this.pipelineId}/projects/${this.projectId}/export${queryString}`
                    },
                    {
                        type: CODE_MODE,
                        title: 'Pipeline YAML',
                        icon: 'export-pipeline',
                        name: `${this.pipelineName}.yml`,
                        tips: this.$t('newlist.exportPipelineYamlTip'),
                        exportUrl: `${API_URL_PREFIX}/${PROCESS_API_URL_PREFIX}/user/transfer/projects/${this.projectId}?pipelineId=${this.pipelineId}&actionType=FULL_MODEL2YAML`,
                        tipsLink: this.BKCI_DOCS?.PAC_GUIDE_DOC,
                        params: {
                            modelAndSetting: {
                                model: this.pipeline,
                                setting: this.pipelineSetting
                            }
                        }
                    }
                ]
            }
        },

        methods: {
            ...mapActions('atom', ['download']),

            handleCancel () {
                this.$emit('update:isShow', false)
            },

            downLoadFromApi (exportItem) {
                const { exportUrl: url, name, params, type } = exportItem
                this.isDownLoading = true
                this.download({ url, name, params, type }).catch((e) => {
                    this.handleError(
                        e,
                        {
                            projectId: this.projectId,
                            resourceCode: this.pipelineId,
                            action: this.$permissionResourceAction.EDIT
                        }
                    )
                }).finally(() => {
                    this.isDownLoading = false
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .export-list {
        padding: 0px 18px 16px;
        display: flex;
        justify-content: space-around;
        font-size: 12px;
        .export-item {
            width: 240px;
            height: 360px;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 37px 30px 30px;
            border-radius: 2px;
            .export-icon {
                width: 64px;
                height: 64px;
                margin-bottom: 16px;
            }
            .export-title {
                font-weight: 600;
                font-size: 16px;
                line-height: 24px;
                margin-bottom: 8px;
                margin-top: 0;
            }
            .export-tip {
                flex: 1;
                color: #63656e;
                line-height: 20px;
                a {
                    color: #3a84ff;
                }
            }
            .export-button {
                width: 180px;
                height: 32px;
                text-align: center;
                line-height: 30px;
                border: 1px solid #c4c6cc;
                color: #63656e;
                border-radius: 2px;
            }
        }
        .export-item:hover {
            background: #f5f6fa;
            .export-button {
                background: #3a84ff;
                border: 1px solid #3a84ff;
                color: #ffffff;
            }
        }
    }
</style>
