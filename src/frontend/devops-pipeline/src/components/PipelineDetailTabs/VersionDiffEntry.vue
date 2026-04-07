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
                    <VersionSelector
                        v-else
                        ext-cls="dark-theme-select-trigger"
                        ext-popover-cls="dark-theme-select-menu"
                        :editable="canSwitchVersion"
                        :show-draft-tag="!canSwitchVersion"
                        :show-extension="false"
                        :draft-version="(diffMode && currentEditingData) ? draftVersion : undefined"
                        :need-draft-list="!!draftVersion"
                        v-model="activeVersion"
                        @change="diffActiveVersion"
                        v-bind="baseVersionSelectorConf"
                    />
                    <p class="latest-version-selector-right-part">
                        <span v-if="instanceCompareWithTemplate">{{ $t('template.template') }}</span>
                        <VersionSelector
                            ext-cls="dark-theme-select-trigger"
                            ext-popover-cls="dark-theme-select-menu"
                            :editable="canSwitchVersion"
                            :show-draft-tag="!canSwitchVersion"
                            :show-extension="false"
                            :draft-version="draftVersion"
                            v-model="currentVersion"
                            @change="diffCurrentVersion"
                            v-bind="versionSelectorConf"
                        />
                    </p>
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
                        read-only
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
    import { DRAFT_STATUS } from '@/utils/pipelineConst'
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
            // 左侧最新版本
            version: {
                type: Number,
            },
            // 右侧版本
            latestVersion: {
                type: Number,
            },
            // 草稿版本
            draftVersion: {
                type: Number,
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
            // 当前正在编辑的数据（用于对比）
            currentEditingData: {
                type: Object,
                default: null
            },
            // 差异对比模式：CONFLICT（冲突）或 PUBLISHED（已发布）
            diffMode: {
                type: String,
                default: null,
                validator: (value) => !value || [DRAFT_STATUS.CONFLICT, DRAFT_STATUS.PUBLISHED].includes(value)
            }
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

        methods: {
            ...mapActions('atom', [
                'fetchPipelineByVersion',
                'fetchTemplateByVersion',
                'canSwitchToYaml',
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
            async fetchPipelineYaml (version, draftVersion) {
                try {
                    const isTemplate = this.isTemplate || !!this.templateId
                    const fn = isTemplate ? this.fetchTemplateByVersion : this.fetchPipelineByVersion
                    const res = await fn({
                        ...this.$route.params,
                        ...(isTemplate ? {templateId: this.uniqueId} : {}),
                        version,
                        ...(draftVersion ? {draftVersion} : {}),
                        archiveFlag: this.archiveFlag,
                        source: 'COMPARE'
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
            // 获取当前正在编辑的内容的 YAML
            async fetchCurrentEditingYaml () {
                try {
                    if (!this.currentEditingData) {
                        return
                    }
                    
                    // 调用 canSwitchToYaml 接口将当前编辑中的 modelAndSetting 转换为 YAML
                    const res = await this.canSwitchToYaml({
                        projectId: this.$route.params.projectId,
                        pipelineId: this.uniqueId,
                        actionType: 'FULL_MODEL2YAML',
                        modelAndSetting: this.currentEditingData
                    })
                    
                    if (res?.newYaml) {
                        return res.newYaml
                    }
                    
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                    return
                }
            },
            async initDiff (version) {
                this.activeVersion = this.version
                // 如果存在 draftVersion，传递父草稿版本号，VersionSelector 会自动选中最新的子草稿
                this.currentVersion = this.draftVersion || this.latestVersion
                this.showVersionDiffDialog = true
                this.isLoadYaml = true
                
                // 处理特殊的差异对比模式（CONFLICT 或 PUBLISHED）
                if (this.diffMode && this.currentEditingData) {
                    // CONFLICT冲突状态：对比最后保存的草稿 vs 当前编辑内容
                    // PUBLISHED已发布状态：对比最新发布版本 vs 当前编辑内容
                    const draftVersion = this.diffMode === DRAFT_STATUS.CONFLICT ? (this.draftVersion || undefined) : undefined
                    const [activeYaml, currentYaml] = await Promise.all([
                        this.fetchPipelineYaml(this.version, draftVersion),
                        this.fetchCurrentEditingYaml()
                    ])
                    this.activeYaml = activeYaml
                    this.currentYaml = currentYaml
                    this.isLoadYaml = false
                    return
                }

                if (this.instanceCompareWithTemplate) {
                    try {
                        const { templateVersionName, instanceName, baseVersionYaml, comparedVersionYaml } = await this.compareYamlWithTemplate({
                            projectId: this.$route.params.projectId,
                            templateId: this.$route.params.templateId,
                            pipelineId: this.pipelineId,
                            templateVersion: ['string', 'number'].includes(typeof version) ? version : this.latestVersion,
                            pipelineVersion: this.activeVersion,
                            useTemplateSettings: this.useTemplateSettings
                        })
                        this.activeYaml = baseVersionYaml
                        this.currentYaml = comparedVersionYaml
                        this.instanceName = instanceName
                        this.templateVersionName = templateVersionName
                    } catch (error) {
                        this.$bkMessage({
                            theme: 'error',
                            message: error.message,
                            zIndex: 3000
                        })
                    }
                } else {
                    try {
                        const [activeYaml, currentYaml] = await Promise.all([
                            this.fetchPipelineYaml(this.activeVersion),
                            this.fetchPipelineYaml(this.latestVersion, this.draftVersion || undefined)
                        ])
                        this.activeYaml = activeYaml
                        this.currentYaml = currentYaml
                    } catch (error) {
                        this.$bkMessage({
                            theme: 'error',
                            message: error.message,
                            zIndex: 3000
                        })
                    }
                }
                this.isLoadYaml = false
            },
            async diffActiveVersion (versionId, versionData) {
                // versionId 是唯一标识（草稿时为 "draft-1"，发布时为版本号）
                if (versionId !== this.activeVersion) {
                    this.activeVersion = versionId
                    this.isLoadYaml = true
                    // 如果是草稿，传递 initialVersion 和 draftVersion；否则只传 versionId
                    const version = versionData.isDraftVersion ? versionData.initialVersion : versionId
                    this.activeYaml = await this.fetchPipelineYaml(version, versionData.draftVersion || undefined)
                    this.isLoadYaml = false
                }
            },
            async diffCurrentVersion (versionId, versionData) {
                if (versionId === this.currentVersion) return
                if (this.instanceCompareWithTemplate) {
                    this.initDiff(versionId)
                } else {
                    this.currentVersion = versionId
                    this.isLoadYaml = true
                    const version = versionData.isDraftVersion ? versionData.initialVersion : versionId
                    this.currentYaml = await this.fetchPipelineYaml(version, versionData.draftVersion || undefined)
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
