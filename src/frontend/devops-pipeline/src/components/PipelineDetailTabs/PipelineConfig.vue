<template>
    <div class="pipeline-config-wrapper" v-bkloading="{ isLoading }">
        <bk-alert
            v-if="isActiveDraft"
            :title="
                $t('draftInfoTips', [draftCreator, draftLastUpdateTime])
            "
        />
        <bk-alert
            v-if="hasUnResolveEvent"
            type="warning"
        >
            <i class="bk-icon icon-info-circle" />
            <i18n :path="unResolveEventTooltips" tag="p" slot="title">
                <span class="text-link" @click="showVersionSideSlider">{{ $t('goVersionSideslider') }}</span>
            </i18n>
        </bk-alert>
        <header class="pipeline-config-header">
            <mode-switch read-only />
            <VersionSideslider
                v-model="activePipelineVersion"
                ref="versionSideslider"
                @change="handleVersionChange"
            />
            <RollbackEntry
                :version="activePipelineVersion"
                :pipeline-id="pipelineId"
                :project-id="projectId"
                :version-name="activePipelineVersionName"
                :draft-base-version-name="draftBaseVersionName"
                :is-active-draft="isActiveDraft"
            >
                <template v-if="isCurrentVersion || isActiveDraft">
                    <i style="font-size: 16px" class="devops-icon icon-pipeline-edit" />
                    {{$t('edit')}}
                </template>
                <template v-else>
                    <i style="font-size: 16px" class="devops-icon icon-rollback" />
                    {{ $t("rollback") }}
                </template>
            </RollbackEntry>
            <VersionDiffEntry
                :version="activePipelineVersion"
                :latest-version="releaseVersion"
            >
                <i style="font-size: 16px" class="devops-icon icon-diff" />
                {{ $t("diff") }}
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
    import { convertTime } from '@/utils/util'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
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
                RESOURCE_ACTION,
                isLoading: false,
                yaml: '',
                activePipelineVersion: null,
                activePipelineVersionModel: null,
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

            ...mapState([
                'pipelineMode'
            ]),
            ...mapGetters({
                isCodeMode: 'isCodeMode',
                getPipelineSubscriptions: 'atom/getPipelineSubscriptions',
                draftBaseVersionName: 'atom/getDraftBaseVersionName'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            canEdit () {
                return this.pipelineInfo?.permissions?.canEdit ?? true
            },
            pipelineType () {
                return this.$route.params.type
            },
            releaseVersion () {
                return this.pipelineInfo?.releaseVersion
            },
            releaseVersionName () {
                return this.pipelineInfo?.releaseVersionName
            },
            hasUnResolveEvent () {
                return ['DELETED', 'UN_MERGED'].includes(this.pipelineInfo?.yamlInfo?.status)
            },
            unResolveEventTooltips () {
                switch (this.pipelineInfo?.yamlInfo?.status) {
                    case 'DELETED':
                        return 'ymlDeletedTips'
                    case 'UN_MERGED':
                        return 'unMergedTips'
                    default:
                        return ''
                }
            },
            isCurrentVersion () {
                return this.activePipelineVersion === this.pipelineInfo?.releaseVersion
            },
            isActiveDraft () {
                return this.activePipelineVersionModel?.isDraft ?? false
            },
            draftLastUpdateTime () {
                return convertTime(this.activePipelineVersionModel?.updateTime)
            },
            draftCreator () {
                return this.activePipelineVersionModel?.creator ?? '--'
            },
            dynamicComponentConf () {
                switch (this.pipelineType) {
                    case 'pipeline':
                        return {
                            is: PipelineModel,
                            props: {}
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
                            is: BaseConfig,
                            props: {
                                basicInfo: {
                                    ...this.pipelineInfo,
                                    ...this.pipelineSetting
                                }
                            }
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
            pipelineId (id) {
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
                this.$nextTick(() => {
                    this.init()
                })
            }
        },

        beforeDestroy () {
            this.$refs.editor?.destroy()
            this.setPipelineYaml('')
            this.setPipeline(null)
            this.setPipelineWithoutTrigger(null)
            this.setPipelineSetting(null)
        },
        methods: {
            ...mapActions('atom', [
                'requestPipeline',
                'setPipeline',
                'setPipelineYaml',
                'setPipelineSetting',
                'setPipelineWithoutTrigger'
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
                this.activePipelineVersionModel = version
                if (versionId !== this.activePipelineVersion) {
                    this.$nextTick(() => {
                        this.init()
                    })
                }
            },
            showVersionSideSlider () {
                this.$refs.versionSideslider?.showVersionSideSlider?.()
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
  grid-gap: 16px;
  .pipeline-config-header {
    display: flex;
    align-items: center;
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
