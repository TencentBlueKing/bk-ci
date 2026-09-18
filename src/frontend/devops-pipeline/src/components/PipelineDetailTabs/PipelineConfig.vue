<template>
    <div
        class="pipeline-config-wrapper"
        v-bkloading="{ isLoading }"
    >
        <bk-alert
            v-if="isActiveDraftVersion"
            :title="
                $t('draftInfoTips', [draftCreator, draftLastUpdateTime])
            "
        />
        <p
            v-show="isBranchVersion"
            id="branch-version-guide-tooltips"
        >
            <span
                v-for="i in [1,2,3]"
                :key="i"
                v-html="$t(`branchVersionTips${i}`)"
            />
        </p>
        <bk-alert v-if="isBranchVersion">
            <i class="bk-icon icon-info-circle" />
            <i18n
                path="branchVersionNotMergeYet"
                tag="p"
                slot="title"
            >
                <span
                    class="branch-version-operate-btn text-link"
                    v-bk-tooltips="MRGuideTooltips"
                >
                    <i class="devops-icon icon-helper" />
                    {{ $t('helpGuide') }}
                </span>
                <span
                    class="branch-version-operate-btn text-link"
                    @click="commitMR"
                >
                    <i class="devops-icon icon-jump-link" />
                    {{ $t('goTgitCommitMR') }}
                </span>
            </i18n>
        </bk-alert>

        <bk-alert
            v-if="hasUnResolveEvent"
        >
            <i class="bk-icon icon-info-circle" />
            <i18n
                :path="unResolveEventTooltips"
                tag="p"
                slot="title"
            >
                <span
                    class="text-link"
                    @click="showVersionSideSlider"
                >{{ $t('goVersionSideslider') }}</span>
            </i18n>
        </bk-alert>

        <header class="pipeline-config-header">
            <mode-switch read-only />
            <button
                v-if="showAiInterpretEntry"
                class="pipeline-ai-interpret-entry"
                type="button"
                :aria-label="$t('details.aiInterpretOrchestration')"
                @click="interpretPipeline"
            >
                <span class="pipeline-ai-interpret-entry-inner">
                    <img
                        :src="aiAvatar"
                        alt=""
                    />
                    {{ $t('details.aiInterpretOrchestration') }}
                </span>
            </button>
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
    import Logo from '@/components/Logo'
    import aiAvatar from '@/images/ai-avatar.png'
    import {
        joinLabeledLine,
        requestAiAnalysis,
        summarizePipelineStructure
    } from '@/utils/aiAnalysis'
    import { bus, SHOW_VERSION_HISTORY_SIDESLIDER } from '@/utils/bus'
    import { convertTime } from '@/utils/util'

    import ModeSwitch from '@/components/ModeSwitch'
    import { NotifyTab, TriggerTab } from '@/components/PipelineEditTabs/'
    import YamlEditor from '@/components/YamlEditor'
    import { pipelineTabIdMap } from '@/utils/pipelineConst'
    import { mapGetters, mapState } from 'vuex'
    import BaseConfig from './BaseConfig'
    import PipelineModel from './PipelineModel'
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
                aiAvatar,
                isLoading: false,
                yaml: ''
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineYaml',
                'pipelineSetting',
                'pipeline',
                'pipelineWithoutTrigger',
                'pipelineInfo',
                'activePipelineVersion',
                'switchingVersion',
                'yamlHighlightBlockMap'
            ]),
            ...mapGetters({
                isCodeMode: 'isCodeMode',
                isActiveDraftVersion: 'atom/isActiveDraftVersion',
                getPipelineSubscriptions: 'atom/getPipelineSubscriptions',
                isBranchVersion: 'atom/isBranchVersion',
                isTemplate: 'atom/isTemplate'
            }),
            canEdit () {
                return this.pipelineInfo?.permissions?.canEdit ?? true
            },
            pipelineType () {
                return this.$route.params.type
            },
            showAiInterpretEntry () {
                return !this.isTemplate
                    && this.pipelineType === pipelineTabIdMap.pipeline
                    && !this.switchingVersion
                    && !!this.pipeline
            },
            currentVersion () {
                return this.$route.params.version
                    || this.activePipelineVersion?.version
                    || this.pipelineInfo?.releaseVersion
                    || ''
            },
            MRGuideTooltips () {
                return {
                    placement: 'bottom',
                    trigger: 'click',
                    content: '#branch-version-guide-tooltips',
                    theme: 'light',
                    allowHTML: true
                }
            },
            hasUnResolveEvent () {
                return ['DELETED'].includes(this.pipelineInfo?.yamlInfo?.status)
            },
            unResolveEventTooltips () {
                switch (this.pipelineInfo?.yamlInfo?.status) {
                    case 'DELETED':
                        return 'ymlDeletedTips'
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
                    case pipelineTabIdMap.pipeline:
                        return {
                            is: PipelineModel,
                            props: {}
                        }
                    case pipelineTabIdMap.trigger:
                        return {
                            is: TriggerTab,
                            props: {
                                editable: false,
                                pipeline: this.pipeline,
                                isLoading: !this.pipeline
                            }
                        }
                    case pipelineTabIdMap.notice:
                        return {
                            is: NotifyTab,
                            props: {
                                editable: false,
                                failSubscriptionList: this.getPipelineSubscriptions('fail'),
                                successSubscriptionList: this.getPipelineSubscriptions('success')
                            }
                        }
                    case pipelineTabIdMap.setting:
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
            this.$refs.editor?.destroy?.()
        },
        methods: {
            showVersionSideSlider () {
                bus.$emit(SHOW_VERSION_HISTORY_SIDESLIDER)
            },
            interpretPipeline () {
                const pipelineName = this.pipelineInfo?.pipelineName || this.pipeline?.name || ''
                const composition = summarizePipelineStructure(
                    this.pipelineWithoutTrigger || this.pipeline
                )
                requestAiAnalysis(
                    {},
                    this.$t('details.aiInterpretOrchestrationPrompt'),
                    { version: String(this.currentVersion || '') },
                    {
                        attachment: {
                            title: this.$t('details.aiInterpretOrchestrationAttachmentTitle'),
                            body: [
                                joinLabeledLine(this.$t('pipelineName'), pipelineName),
                                this.$t('details.aiAttachmentComposition'),
                                composition
                            ].filter(Boolean).join('\n')
                        }
                    }
                )
            },
            commitMR () {
                if (this.pipelineInfo?.yamlInfo?.webUrl) {
                    const url = new URL(`${this.pipelineInfo?.yamlInfo?.webUrl}/merge_requests/new`)
                    url.searchParams.append('merge_request[source_branch]', this.activePipelineVersion?.versionName)
                    console.log(url, url.href)
                    window.open(url.href, '_blank')
                }
            }
        }
    }
</script>

<style lang="scss">
#branch-version-guide-tooltips {
    display: grid;
    grid-gap: 6px;
    b {
        color: #FF9C01;
    }
}
.pipeline-config-wrapper {
  padding: 24px;
  display: flex;
  height: 100%;
  flex-direction: column;
  overflow: hidden;
  position: static !important;
  grid-gap: 16px;
  .branch-version-operate-btn {
    margin-left: 16px;
  }
  .pipeline-config-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;
    grid-gap: 16px;
    .pipeline-ai-interpret-entry {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      box-sizing: border-box;
      min-width: 64px;
      height: 24px;
      padding: 0 8px;
      margin: 0;
      border: 1px solid #3a84ff;
      border-radius: 12px;
      background: #e0ebff;
      color: #3a84ff;
      font: inherit;
      font-size: 12px;
      line-height: 1;
      appearance: none;
      cursor: pointer;
      outline: none;
      transition: background-color .15s ease, border-color .15s ease;

      &:hover {
        background: #d4e4ff;
      }

      &:active {
        background: #c8dcff;
      }

      &:focus-visible {
        outline: 2px solid rgba(58, 132, 255, .35);
        outline-offset: 2px;
      }

      .pipeline-ai-interpret-entry-inner {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        line-height: 1;
      }

      img {
        display: block;
        flex: none;
        width: 16px;
        height: 16px;
        object-fit: contain;
      }
    }
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
