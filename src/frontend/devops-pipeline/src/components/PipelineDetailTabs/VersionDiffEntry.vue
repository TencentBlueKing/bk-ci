<template>
    <span class="version-diff-entry-wrapper">
        <bk-button
            :text="text"
            :outline="outline"
            :theme="theme"
            :loading="loading"
            :disabled="disabled"
            :size="size"
            @click="initDiff"
        >
            <slot>
                {{ isTemplate ? $t('template.diff') : $t('diff') }}
            </slot>
        </bk-button>
        <bk-dialog
            render-directive="if"
            v-model="showVersionDiffDialog"
            header-position="left"
            :draggable="false"
            ext-cls="diff-version-dialog"
            width="90%"
        >
            <template #header>
                <span>{{ isTemplate ? $t('template.diff') : $t('diff') }}</span>
                <span
                    v-if="instanceCompareWithTemplate"
                    class="compare-with-template-tips"
                >
                    <i class="bk-icon icon-info-circle"></i>
                    {{ $t('template.instanceCompareWithTemplate') }}
                </span>
            </template>
            <div
                class="diff-version-dialog-content"
                v-bkloading="{ isLoading: isLoadYaml, color: '#1d1d1d' }"
            >
                <header
                    class="diff-version-header"
                    v-if="showButton"
                >
                    <p
                        v-if="instanceCompareWithTemplate"
                        class="base-version-selector-left-part"
                    >
                        {{ $t('template.instance') }}
                        {{ instanceName || '--' }}
                        <span class="from-template-version-span">
                            ｜
                            {{ $t('template.fromTemplateVersion', [templateVersionName || '--']) }}
                        </span>
                        <bk-tag
                            theme="info"
                            type="stroke"
                        >
                            {{ $t('template.parsedYaml') }}
                        </bk-tag>
                    </p>
                    <div v-else>
                        <span
                            v-if="!version && draftYaml !== ''"
                            class="base-version-selector-left-part"
                        >
                            <i class="devops-icon icon-edit-line" />
                            {{ $t('draft') }}： {{ draftInfo?.updater }} {{ formatDate(draftInfo?.updateTime) }}
                        </span>
                        <VersionSelector
                            v-else
                            ext-cls="dark-theme-select-trigger"
                            ext-popover-cls="dark-theme-select-menu"
                            :editable="canSwitchVersion"
                            :show-draft-tag="!canSwitchVersion"
                            :show-extension="false"
                            v-model="activeVersion"
                            @change="diffActiveVersion"
                            v-bind="baseVersionSelectorConf"
                        />
                    </div>
                    <div class="latest-version-selector-right-part">
                        <span
                            v-if="!latestVersion && baseYaml !== ''"
                            class="base-version-selector-left-part"
                        >
                            <i class="devops-icon icon-edit-line" />
                            {{ $t('draft') }}： {{ $userInfo.username }} {{ formatDate() }}
                            <span class="draft-editing">{{ $t('editing') }}</span>
                        </span>
                        <p v-else>
                            <span v-if="instanceCompareWithTemplate">{{ $t('template.template') }}</span>
                            <VersionSelector
                                ext-cls="dark-theme-select-trigger"
                                ext-popover-cls="dark-theme-select-menu"
                                :editable="canSwitchVersion"
                                :show-draft-tag="!canSwitchVersion"
                                :show-extension="false"
                                v-model="currentVersion"
                                @change="diffCurrentVersion"
                                v-bind="versionSelectorConf"
                            />
                        </p>
                    </div>
                    <bk-checkbox
                        v-if="instanceCompareWithTemplate"
                        class="use-template-settings-checkbox"
                        v-model="useTemplateSettings"
                        v-bk-tooltips="$t('template.withSettingCompareTips')"
                        @change="initDiff"
                    >{{ $t('template.useTemplateSettings') }}
                    </bk-checkbox>
                </header>
                <div class="pipeline-yaml-diff-wrapper">
                    <yaml-diff
                        :old-yaml="activeYaml"
                        height="100%"
                        :new-yaml="currentYaml"
                    />
                </div>
            </div>

            <footer slot="footer">
                <bk-button
                    @click="showVersionDiffDialog = false"
                >
                    {{ $t('close') }}
                </bk-button>
            </footer>
        </bk-dialog>
    </span>
</template>

<script>
    import VersionSelector from '@/components/PipelineDetailTabs/VersionSelector'
    import YamlDiff from '@/components/YamlDiff'
    import { mapActions, mapGetters } from 'vuex'
    import dayjs from 'dayjs'
    export default {
        components: {
            YamlDiff,
            VersionSelector
        },
        props: {
            text: {
                type: Boolean,
                default: true
            },
            outline: Boolean,
            theme: {
                type: String,
                default: 'primary'
            },
            size: {
                type: String,
                default: 'normal'
            },
            version: {
                type: Number,
            },
            latestVersion: {
                type: Number,
            },
            // 直接传入的 YAML 内容（用于编辑页面，当前编辑内容还未保存的情况）
            baseYaml: {
                type: String,
                default: ''
            },
            draftYaml: {
                type: String,
                default: ''
            },
            draftInfo: {
                type: Object,
                default: () => ({})
            },
            canSwitchVersion: {
                type: Boolean,
                default: true
            },
            showButton: {
                type: Boolean,
                default: true
            },
            disabled: {
                type: Boolean,
                default: false
            },
            loading: {
                type: Boolean,
                default: false
            },
            type: String,
            pipelineId: String,
            templateId: String,
            archiveFlag: Boolean,
            instanceCompareWithTemplate: {
                type: Boolean,
                default: false
            },
            
        },
        data () {
            return {
                isLoadYaml: false,
                showVersionDiffDialog: false,
                activeVersion: '',
                currentVersion: '',
                activeYaml: '',
                currentYaml: '',
                pipelineVersionList: [],
                useTemplateSettings: false,
                templateVersionName: '',
                instanceName: ''
            }
        },
        computed: {
            // isTemplate代表是一个模板，而不是说是模板实例
            ...mapGetters('atom', ['isTemplate']),
            
            uniqueId () {
                const { pipelineId, templateId } = this.$route.params
                if (this.isTemplate || !!this.templateId) {
                    return this.templateId || templateId
                }
                return this.pipelineId || pipelineId
            },
            versionSelectorConf () {
                return {
                    isTemplate: (this.isTemplate || !!this.templateId),
                    uniqueId: this.uniqueId
                }
            },
            baseVersionSelectorConf () {
                return this.instanceCompareWithTemplate ? {
                    isTemplate: false,
                    uniqueId: this.pipelineId
                } : this.versionSelectorConf
            }
        },
        watch: {
            // 监听 baseYaml 和 draftYaml 的变化，当父组件传入新数据时重新初始化
            baseYaml (newVal, oldVal) {
                this.handleYamlChange(newVal, oldVal)
            },
            draftYaml (newVal, oldVal) {
                this.handleYamlChange(newVal, oldVal)
            },
        },

        methods: {
            ...mapActions('atom', [
                'fetchPipelineByVersion',
                'fetchTemplateByVersion',
                'compareYamlWithTemplate'
            ]),
            ...mapActions('templates', ['requestVersionCompare']),
            handleYamlChange (newVal, oldVal) {
                if (newVal && newVal !== oldVal && this.showVersionDiffDialog) {
                    this.$nextTick(() => {
                        this.initDiff()
                    })
                }
            },
            formatDate (data) {
                return dayjs(data).format('YYYY-MM-DD HH:mm:ss')
            },
            async fetchPipelineYaml (version) {
                try {
                    const isTemplate = this.isTemplate || !!this.templateId
                    const fn = isTemplate ? this.fetchTemplateByVersion : this.fetchPipelineByVersion
                    const res = await fn({
                        ...this.$route.params,
                        ...(isTemplate ? {templateId: this.uniqueId} : {}),
                        version,
                        archiveFlag: this.archiveFlag
                    })
                    if (res?.yamlSupported) {
                        return res.yamlPreview.yaml
                    }
                    throw new Error(res?.yamlInvalidMsg)
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message,
                        zIndex: 3000
                    })
                    return ''
                }
            },
            async initDiff () {
                this.activeVersion = this.version
                this.currentVersion = this.latestVersion
                this.showVersionDiffDialog = true

                this.isLoadYaml = true
                if (this.instanceCompareWithTemplate) {
                    const { templateVersionName, instanceName, baseVersionYaml, comparedVersionYaml } = await this.compareYamlWithTemplate({
                        projectId: this.$route.params.projectId,
                        templateId: this.$route.params.templateId,
                        pipelineId: this.pipelineId,
                        templateVersion: this.latestVersion,
                        pipelineVersion: this.activeVersion,
                        useTemplateSettings: this.useTemplateSettings
                    })
                    this.activeYaml = baseVersionYaml
                    this.currentYaml = comparedVersionYaml
                    this.instanceName = instanceName
                    this.templateVersionName = templateVersionName
                } else {
                    // 如果传入了 baseYaml 和 draftYaml，直接使用；否则通过版本号获取
                    if (this.latestVersion && this.version) {
                        const [activeYaml, currentYaml] = await Promise.all([
                            this.fetchPipelineYaml(this.activeVersion),
                            this.fetchPipelineYaml(this.currentVersion)
                        ])
                        this.activeYaml = activeYaml
                        this.currentYaml = currentYaml
                    } else {
                        this.currentYaml = this.baseYaml
                        this.activeYaml = this.draftYaml
                    }

                }
                this.isLoadYaml = false
            },
            async diffActiveVersion (version, old) {
                if (version !== this.activeVersion) {
                    this.activeVersion = version
                    this.isLoadYaml = true
                    this.activeYaml = await this.fetchPipelineYaml(this.activeVersion)
                    this.isLoadYaml = false
                }
            },
            async diffCurrentVersion (version, old) {
                if (version !== this.currentVersion) {
                    this.currentVersion = version
                    this.isLoadYaml = true
                    this.currentYaml = await this.fetchPipelineYaml(this.currentVersion)
                    this.isLoadYaml = false
                }
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/select-dark-theme.scss";
    .version-diff-entry-wrapper {
        .bk-button-text.bk-button-small {
            padding: 0;
        }
    }
    .use-template-settings-checkbox {
        position: absolute;
        right: 10px;
        .bk-checkbox-text {
            text-decoration: underline;
            text-decoration-style: dashed;
            text-underline-offset: 5px;
        }
    }
    .diff-version-dialog.bk-dialog-wrapper {
        transition: none;
        .bk-dialog {
            transition: all .3s;
            margin: 0 auto;
            top: 10%;
            .compare-with-template-tips {
                margin-left: 20px;
                font-size: 12px;
                color: #999;
            }
            .bk-dialog-content {
                height: 80vh;
                .bk-dialog-body {
                    height: calc(100% - 100px);
                    .base-version-selector-left-part {
                        color: #E6E6E6;
                        .from-template-version-span {
                            font-weight: normal;
                            color: #999;
                        }
                    }
                    .latest-version-selector-right-part {
                        display: flex;
                        gap: 8px;
                        align-items: center;
                    }
                    .diff-version-dialog-content {
                        display: flex;
                        flex-direction: column;
                        height: 100%;
                        .diff-version-header {
                            flex-shrink: 0;
                            height: 40px;
                            background: #1d1d1d;
                            display: grid;
                            grid-auto-flow: column;
                            grid-template-columns: 1fr 1fr;
                            align-items: center;
                            padding: 0 24px;
                            color: #C4C6CC;
                        }

                        .pipeline-yaml-diff-wrapper {
                            flex: 1;
                            overflow: hidden;
                        }
                    }
                }
            }
        }
    }
    .draft-editing {
        padding: 2px 4px;
        margin-left: 6px;
        line-height: 21px;
        background: #1F472E;
        border: 1px solid #27633D;
        font-size: 10px;
        color: #3FC362;
        border-radius: 2px;
    }
</style>
