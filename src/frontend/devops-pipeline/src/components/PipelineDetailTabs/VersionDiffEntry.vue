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
                {{ !isTemplateInstance ? $t('diff') : $t('template.diff') }}
            </slot>
        </bk-button>
        <bk-dialog
            render-directive="if"
            v-model="showVersionDiffDialog"
            header-position="left"
            :draggable="false"
            ext-cls="diff-version-dialog"
            width="90%"
            :title="!isTemplateInstance ? $t('diff') : $t('template.diff')"
        >
            <div
                class="diff-version-dialog-content"
                v-bkloading="{ isLoading: isLoadYaml, color: '#1d1d1d' }"
            >
                <header
                    class="diff-version-header"
                    v-if="showButton"
                >
                    <VersionSelector
                        ext-cls="dark-theme-select-trigger"
                        ext-popover-cls="dark-theme-select-menu"
                        :editable="canSwitchVersion"
                        :show-draft-tag="!canSwitchVersion"
                        :show-extension="false"
                        v-model="activeVersion"
                        @change="diffActiveVersion"
                    />
                    <VersionSelector
                        ext-cls="dark-theme-select-trigger"
                        ext-popover-cls="dark-theme-select-menu"
                        :editable="canSwitchVersion"
                        :show-draft-tag="!canSwitchVersion"
                        :show-extension="false"
                        v-model="currentVersion"
                        @change="diffCurrentVersion"
                    />
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
                required: true
            },
            latestVersion: {
                type: Number,
                required: true
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
            pipelineId: String
        },
        data () {
            return {
                isLoadYaml: false,
                showVersionDiffDialog: false,
                activeVersion: '',
                currentVersion: '',
                activeYaml: '',
                currentYaml: '',
                pipelineVersionList: []
            }
        },
        computed: {
            ...mapGetters('atom', ['isTemplate']),
            isTemplateInstance () {
                return this.type === 'templateInstance' && this.isTemplate
            }
        },

        methods: {
            ...mapActions('atom', [
                'fetchPipelineByVersion',
                'fetchTemplateByVersion'
            ]),
            ...mapActions('templates', ['requestVersionCompare']),
            async fetchPipelineYaml (version) {
                try {
                    const fn = this.isTemplate ? this.fetchTemplateByVersion : this.fetchPipelineByVersion
                    const res = await fn({
                        ...this.$route.params,
                        version
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
            async fetchTemplateInstanceYaml (versions) {
                try {
                    const res = await this.requestVersionCompare({
                        ...this.$route.params,
                        ...versions
                    })
                    return res.data
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
                if (this.isTemplateInstance) {
                    const { baseVersionYaml, comparedVersionYaml } = await this.fetchTemplateInstanceYaml({
                        pipelineId: this.pipelineId,
                        comparedVersion: this.currentVersion
                    })
                    this.activeYaml = comparedVersionYaml
                    this.currentYaml = baseVersionYaml
                } else {
                    const [activeYaml, currentYaml] = await Promise.all([
                        this.fetchPipelineYaml(this.activeVersion),
                        this.fetchPipelineYaml(this.currentVersion)
                    ])
                    this.activeYaml = activeYaml
                    this.currentYaml = currentYaml
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
    .diff-version-dialog.bk-dialog-wrapper {
        transition: none;
        .bk-dialog {
            transition: all .3s;
            margin: 0 auto;
            top: 10%;
            .bk-dialog-content {
                height: 80vh;
                .bk-dialog-body {
                    height: calc(100% - 100px);
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
</style>
