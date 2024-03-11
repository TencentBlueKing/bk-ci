<template>
    <span>
        <bk-button
            :text="text"
            :outline="outline"
            :theme="theme"
            @click="diffVersion(version)"
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
            <div class="diff-version-dialog-content" v-bkloading="{ isLoading: isLoadYaml, color: '#1d1d1d' }">
                <header class="diff-version-header">
                    <span>
                        {{ latestVersionName }}
                        <bk-tag theme="info">{{ $t('template.current') }}</bk-tag>
                    </span>
                    <span>
                        <bk-select
                            ext-cls="dark-theme-select-trigger"
                            ext-popover-cls="dark-theme-select-menu"
                            v-model="activeVersion"
                            @change="diffVersion"
                            enable-scroll-load
                            :scroll-loading="bottomLoadingOptions"
                            :clearable="false"
                            @scroll-end="loadMore()"
                        >
                            <bk-option
                                v-for="item in pipelineVersionList"
                                :key="item.version"
                                :id="item.version"
                                :name="item.versionName"
                            />
                        </bk-select>
                    </span>
                </header>
                <div class="pipeline-yaml-diff-wrapper">
                    <yaml-diff
                        :old-yaml="currentYaml"
                        height="100%"
                        :new-yaml="activeYaml"
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
    import { mapActions } from 'vuex'
    import YamlDiff from '@/components/YamlDiff'
    export default {
        components: {
            YamlDiff
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
            version: {
                type: Number,
                required: true
            },
            latestVersion: {
                type: Number,
                required: true
            }
        },
        data () {
            return {
                isLoadYaml: false,
                showVersionDiffDialog: false,
                activeVersion: '',
                activeYaml: '',
                currentYaml: '',
                pipelineVersionList: [],
                page: 1,
                hasNext: true,
                bottomLoadingOptions: {
                    size: 'small',
                    isLoading: false
                }
            }
        },
        computed: {
            latestVersionName () {
                return this.pipelineVersionList.find(item => item.version === this.latestVersion)?.versionName ?? '--'
            }
        },

        methods: {
            ...mapActions('atom', [
                'fetchPipelineByVersion'
            ]),
            ...mapActions('pipelines', [
                'requestPipelineVersionList'
            ]),
            async loadMore (page) {
                try {
                    if (!this.hasNext) return
                    const { projectId, pipelineId } = this.$route.params
                    const pageSize = 20
                    this.bottomLoadingOptions.isLoading = true
                    const res = await this.requestPipelineVersionList({
                        projectId,
                        pipelineId,
                        page: page ?? this.page + 1,
                        pageSize
                    })
                    this.page = res.page
                    this.hasNext = res.count > res.page * pageSize
                    if (res.records.length > 0) {
                        this.pipelineVersionList.push(...res.records.map(item => {
                            return {
                                ...item,
                                versionName: item.versionName || this.$t('editPage.draftVersion', [item.baseVersionName])
                            }
                        }))
                    }
                } catch (error) {
                    console.log(error)
                } finally {
                    this.bottomLoadingOptions.isLoading = false
                }
            },
            async diffVersion (version) {
                try {
                    this.isLoadYaml = true
                    this.showVersionDiffDialog = true
                    this.loadMore(this.page)
                    this.activeVersion = version
                    const [activePipeline, currentPipeline] = await Promise.all([
                        this.fetchPipelineByVersion({
                            projectId: this.$route.params.projectId,
                            pipelineId: this.$route.params.pipelineId,
                            version: this.activeVersion
                        }),
                        this.fetchPipelineByVersion({
                            projectId: this.$route.params.projectId,
                            pipelineId: this.$route.params.pipelineId,
                            version: this.latestVersion
                        })
                    ])

                    if (activePipeline?.yamlSupported && currentPipeline?.yamlSupported) {
                        this.activeYaml = activePipeline.yamlPreview.yaml
                        this.currentYaml = currentPipeline.yamlPreview.yaml
                        return
                    }
                    console.log(activePipeline, 'hahaha', currentPipeline)
                    throw new Error(activePipeline?.yamlInvalidMsg)
                } catch (error) {
                    console.log(error)
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.isLoadYaml = false
                }
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/select-dark-theme.scss";
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
