<template>
    <span class="version-diff-entry-wrapper">
        <bk-button
            :text="text"
            :outline="outline"
            :theme="theme"
            :disabled="disabled"
            :size="size"
            @click="initDiff"
        >
            <slot>
                {{ $t('diff') }}
            </slot>
        </bk-button>
        <bk-dialog
            render-directive="if"
            v-model="showVersionDiffDialog"
            header-position="left"
            :draggable="false"
            ext-cls="diff-version-dialog"
            width="90%"
            :title="$t('diff')"
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
    import { mapActions } from 'vuex'
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
            archiveFlag: Boolean
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

        methods: {
            ...mapActions('atom', [
                'fetchPipelineByVersion'
            ]),
            async fetchPipelineYaml (version) {
                try {
                    const res = await this.fetchPipelineByVersion({
                        projectId: this.$route.params.projectId,
                        pipelineId: this.$route.params.pipelineId,
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
                const [activeYaml, currentYaml] = await Promise.all([
                    this.fetchPipelineYaml(this.activeVersion),
                    this.fetchPipelineYaml(this.currentVersion)
                ])
                this.activeYaml = activeYaml
                this.currentYaml = currentYaml
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
