<template>
    <div class="pipeline-config-wrapper" v-bkloading="{ isLoading }">
        <bk-alert
            v-if="isActiveDraftVersion"
            :title="
                $t('draftInfoTips', [draftCreator, draftLastUpdateTime])
            "
        />
        <p v-show="isBranchVersion" id="branch-version-guide-tooltips">
            <span v-for="i in [1,2,3]" :key="i" v-html="$t(`branchVersionTips${i}`)" />
        </p>
        <bk-alert v-if="isBranchVersion">
            <i class="bk-icon icon-info-circle" />
            <i18n path="branchVersionNotMergeYet" tag="p" slot="title">
                <span class="branch-version-operate-btn text-link" v-bk-tooltips="MRGuideTooltips">
                    <i class="devops-icon icon-helper" />
                    {{ $t('helpGuide') }}
                </span>
                <span class="branch-version-operate-btn text-link" @click="commitMR">
                    <i class="devops-icon icon-jump-link" />
                    {{ $t('goTgitCommitMR') }}
                </span>
            </i18n>
        </bk-alert>

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
    import Logo from '@/components/Logo'
    import ModeSwitch from '@/components/ModeSwitch'
    import { NotifyTab, TriggerTab } from '@/components/PipelineEditTabs/'
    import YamlEditor from '@/components/YamlEditor'
    import { SHOW_VERSION_HISTORY_SIDESLIDER, bus } from '@/utils/bus'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { pipelineTabIdMap } from '@/utils/pipelineConst'
    import { convertTime } from '@/utils/util'
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
                isActiveDraftVersion: 'atom/isActiveDraftVersion',
                getPipelineSubscriptions: 'atom/getPipelineSubscriptions',
                isBranchVersion: 'atom/isBranchVersion'
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
