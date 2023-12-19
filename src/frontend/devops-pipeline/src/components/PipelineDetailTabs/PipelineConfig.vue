<template>
    <div class="pipeline-config-wrapper" v-bkloading="{ isLoading }">
        <header class="pipeline-config-header">
            <mode-switch />
            <VersionSideslider
                v-model="activePipelineVersion"
                ref="versionSideslider"
                @change="handleVersionChange"
            />
            <RollbackEntry
                v-if="canRollBack"
                :version="activePipelineVersion"
                :version-name="activePipelineVersionName"
                :draft-version-name="draftVersionName"
            >
                <i class="devops-icon icon-rollback" />
                {{$t('rollback')}}
            </RollbackEntry>
            <VersionDiffEntry
                v-if="!isCurrentVersion"
                :version="activePipelineVersion"
                :latest-version="releaseVersion"
                :current-yaml="pipelineYaml"
            >
                <i class="devops-icon icon-diff" />
                {{$t('diff')}}
            </VersionDiffEntry>
        </header>
        <section class="pipeline-model-content">
            <YamlEditor
                v-if="isCodeMode"
                ref="editor"
                :value="pipelineYaml"
                :highlight-ranges="yamlHighlightBlock"
                read-only
            />

            <component
                class="pipeine-config-content-box"
                v-else-if="dynamicComponentConf"
                v-bind="dynamicComponentConf.props"
                :is="dynamicComponentConf.is"
            />
        </section>

    </div>
</template>

<script>
    import { mapActions, mapState, mapGetters } from 'vuex'
    import ModeSwitch from '@/components/ModeSwitch'
    import YamlEditor from '@/components/YamlEditor'
    import { TriggerTab, NotifyTab } from '@/components/PipelineEditTabs/'
    import PipelineModel from './PipelineModel'
    import BaseConfig from './BaseConfig'
    import VersionSideslider from './VersionSideslider'
    import Logo from '@/components/Logo'
    import VersionDiffEntry from './VersionDiffEntry'
    import RollbackEntry from './RollbackEntry'
    export default {
        components: {
            ModeSwitch,
            PipelineModel,
            TriggerTab,
            BaseConfig,
            YamlEditor,
            Logo,
            VersionSideslider,
            VersionDiffEntry,
            RollbackEntry
        },
        data () {
            return {
                isLoading: false,
                yaml: '',
                activePipelineVersion: null,
                activePipelineVersionName: '',
                yamlHighlightBlockMap: {},
                yamlHighlightBlock: []
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineYaml',
                'pipelineSetting',
                'pipelineWithoutTrigger',
                'pipeline',
                'pipelineInfo'
            ]),
            ...mapGetters({
                isCodeMode: 'isCodeMode',
                getPipelineSubscriptions: 'atom/getPipelineSubscriptions'
            }),
            pipelineType () {
                return this.$route.params.type
            },
            releaseVersion () {
                return this.pipelineInfo?.releaseVersion
            },
            releaseVersionName () {
                return this.pipelineInfo?.releaseVersionName
            },
            canRollBack () {
                return this.activePipelineVersion !== this.pipelineInfo?.releaseVersion && !this.pipelineInfo?.canDebug
            },
            isCurrentVersion () {
                return this.activePipelineVersion === this.pipelineInfo?.releaseVersion
            },
            draftVersionName () {
                return this.$refs?.versionSideslider?.getDraftVersion()
            },
            dynamicComponentConf () {
                switch (this.pipelineType) {
                    case 'pipeline':
                        return {
                            is: PipelineModel
                        }
                    case 'trigger':
                        return {
                            is: TriggerTab,
                            props: {
                                editable: false,
                                pipeline: this.pipeline,
                                isLoading: !this.pipeline
                            }
                        }
                    case 'notice':
                        return {
                            is: NotifyTab,
                            props: {
                                editable: false,
                                failSubscriptionList: this.getPipelineSubscriptions('fail'),
                                successSubscriptionList: this.getPipelineSubscriptions('success')
                            }
                        }
                    case 'setting':
                        return {
                            is: BaseConfig
                        }
                    default:
                        return null
                }
            }
        },
        watch: {
            pipelineType (type) {
                this.yamlHighlightBlock = this.yamlHighlightBlockMap?.[type] ?? []
            },
            isCodeMode (val) {
                if (val) {
                    this.yamlHighlightBlock = this.yamlHighlightBlockMap?.[this.pipelineType] ?? []
                }
            },
            releaseVersion (version) {
                this.activePipelineVersion = version
                this.$nextTick(() => {
                    this.init()
                })
            },
            releaseVersionName (versionName) {
                this.activePipelineVersionName = versionName
            }
        },
        created () {
            if (this.releaseVersion) {
                this.activePipelineVersion = this.releaseVersion
                console.log(this.releaseVersion, this.activePipelineVersion, 'init')
                this.$nextTick(() => {
                    this.init()
                })
            }
        },
        mounted () {
            window.__bk_zIndex_manager.zIndex = 2020
        },
        beforeDestroy () {
            this.$refs.editor?.destroy()
            this.setPipelineYaml('')
            this.setPipeline(null)
            this.setPipelineWithoutTrigger(null)
            this.setPipelineSetting(null)
            window.__bk_zIndex_manager.zIndex = 2000
        },
        methods: {
            ...mapActions('atom', [
                'requestPipeline',
                'setPipeline',
                'setPipelineYaml',
                'setPipelineSetting',
                'setPipelineWithoutTrigger'
            ]),
            ...mapActions('pipelines', [
                'rollbackPipelineVersion'
            ]),
            async init () {
                try {
                    if (this.activePipelineVersion) {
                        this.isLoading = true
                        const yamlHighlightBlockMap = await this.requestPipeline({
                            ...this.$route.params,
                            version: this.activePipelineVersion
                        })
                        this.yamlHighlightBlockMap = yamlHighlightBlockMap
                        if (this.isCodeMode) {
                            this.yamlHighlightBlock = this.yamlHighlightBlockMap[this.pipelineType]
                        }
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.isLoading = false
                }
            },
            handleVersionChange (versionId, version) {
                this.activePipelineVersionName = version.versionName
                this.$nextTick(() => {
                    this.init()
                })
            }
        }
    }
</script>

<style lang="scss">
    .pipeline-config-wrapper {
        padding: 24px;
        display: flex;
        height: 100%;
        flex-direction: column;
        overflow: hidden;
        position: static !important;
        .pipeline-config-header {
            display: flex;
            align-items: center;
            margin-bottom: 24px;
            flex-shrink: 0;
            grid-gap: 16px;
            .text-link {
                display: flex;
                align-items: center;
                font-size: 14px;
                grid-gap: 8px;

                cursor: pointer;
            }
        }
        .pipeline-model-content {
            flex: 1;
            overflow: hidden;
        }
        .pipeine-config-content-box {
            height: 100%;
            overflow: auto;
        }
    }
</style>
