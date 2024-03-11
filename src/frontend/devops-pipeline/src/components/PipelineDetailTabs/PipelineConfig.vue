<template>
    <div class="pipeline-config-wrapper" v-bkloading="{ isLoading }">
        <bk-alert
            v-if="hasDraftPipeline"
            :title="
                $t('draftInfoTips', [draftCreator, draftLastUpdateTime])
            "
        />
        <bk-alert
            v-if="hasUnResolveEvent"
        >
            <i class="bk-icon icon-info-circle" />
            <i18n :path="unResolveEventTooltips" tag="p" slot="title">
                <span class="text-link" @click="showVersionSideSlider">{{ $t('goVersionSideslider') }}</span>
            </i18n>
        </bk-alert>
        <header class="pipeline-config-header">
            <mode-switch read-only />
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
    import { mapState, mapGetters } from 'vuex'
    import ModeSwitch from '@/components/ModeSwitch'
    import YamlEditor from '@/components/YamlEditor'
    import { TriggerTab, NotifyTab } from '@/components/PipelineEditTabs/'
    import PipelineModel from './PipelineModel'
    import BaseConfig from './BaseConfig'
    import Logo from '@/components/Logo'
    import { convertTime } from '@/utils/util'
    import { bus, SHOW_VERSION_HISTORY_SIDESLIDER } from '@/utils/bus'
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
            Logo
        },
        data () {
            return {
                RESOURCE_ACTION,
                isLoading: false,
                yaml: ''
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineYaml',
                'pipelineSetting',
                'pipeline',
                'pipelineInfo',
                'activePipelineVersion',
                'yamlHighlightBlockMap'
            ]),
            ...mapGetters({
                isCodeMode: 'isCodeMode',
                hasDraftPipeline: 'atom/hasDraftPipeline',
                getPipelineSubscriptions: 'atom/getPipelineSubscriptions'
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
            draftLastUpdateTime () {
                return convertTime(this.activePipelineVersion?.updateTime)
            },
            draftCreator () {
                return this.activePipelineVersion?.creator ?? '--'
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
            },
            yamlHighlightBlock () {
                return this.isCodeMode ? (this.yamlHighlightBlockMap?.[this.pipelineType] ?? []) : []
            }
        },

        beforeDestroy () {
            this.$refs.editor?.destroy()
        },
        methods: {
            showVersionSideSlider () {
                bus.$emit(SHOW_VERSION_HISTORY_SIDESLIDER)
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
