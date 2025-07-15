<template>
    <section
        :class="['variable-version-wrapper', {
            'variable-panel-show': showVariable
        }]"
    >
        <div
            class="variable-entry"
            :class="{ 'is-close': !showVariable }"
            @click="toggleOpenVar"
        >
            <i class="bk-icon icon-angle-double-right"></i>
            {{ $t('newui.variable') }}
        </div>
        <div
            v-show="showVariable"
            class="variable-version-container"
        >
            <div class="select-tab-container">
                <div class="tab-content">
                    <div
                        v-for="(panel, index) in panels"
                        class="tab-item"
                        v-bk-overflow-tips
                        :key="index"
                        :class="{ actived: active === panel.name }"
                        @click="selectTab(panel.name)"
                    >
                        {{ panel.label }}
                    </div>
                </div>
            </div>
            <div class="content-wrapper">
                <pipeline-param
                    v-if="active === 'pipeline'"
                    :editable="editable"
                    :params="params"
                    :update-container-params="handleContainerChange"
                />
                <atom-output-var
                    :stages="stages"
                    :editable="editable"
                    v-else-if="active === 'atomOutput'"
                />
                <system-var
                    :container="container"
                    :editable="editable"
                    v-else-if="active === 'system'"
                />
                <pipeline-version
                    v-else
                    :params="params"
                    :disabled="!editable"
                    :pipeline-model="pipelineModel"
                    :container="container"
                    :update-container-params="handleContainerChange"
                    :is-direct-show-version="isDirectShowVersion"
                />
            </div>
        </div>
    </section>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import PipelineParam from './components/pipeline-param'
    import AtomOutputVar from './components/atom-output-var'
    import PipelineVersion from './components/pipeline-version'
    import SystemVar from './components/system-var'

    export default {
        components: {
            PipelineParam,
            PipelineVersion,
            AtomOutputVar,
            SystemVar
        },
        props: {
            pipeline: {
                type: Object,
                required: true
            },
            editable: {
                type: Boolean,
                default: true
            },
            isDirectShowVersion: {
                type: Boolean,
                default: false
            },
            pipelineModel: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                panels: [
                    { name: 'pipeline', label: this.$t('newui.pipelineVar') },
                    { name: 'atomOutput', label: this.$t('newui.atomVar') },
                    { name: 'system', label: this.$t('newui.sysVar') },
                    { name: 'version', label: this.$t('newui.versions') }
                ],
                active: 'pipeline'
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineWithoutTrigger',
                'showVariable'
            ]),
            stages () {
                return this.pipelineWithoutTrigger?.stages || []
            },
            container () {
                return this.pipeline?.stages[0]?.containers[0] || {}
            },
            params () {
                return this.container?.params || []
            },
            buildNo () {
                return this.container?.buildNo || {}
            }
        },
        created () {
            this.requestCommonParams()
        },
        mounted () {
            this.setShowVariable(true)
            if (this.isDirectShowVersion) {
                this.active = 'version'
            }
        },
        beforeDestroy () {
            this.setShowVariable(false)
        },
        methods: {
            ...mapActions('atom', [
                'setShowVariable',
                'toggleAtomSelectorPopup',
                'updateContainer',
                'requestCommonParams'
            ]),
            handleContainerChange (name, value) {
                this.updateContainer({
                    container: this.container,
                    newParam: {
                        [name]: value
                    }
                })
            },
            toggleOpenVar () {
                // 打开变量面板的同时关闭插件选择
                if (!this.showVariable) {
                    this.toggleAtomSelectorPopup(false)
                }
                this.setShowVariable(!this.showVariable)
            },
            selectTab (tab) {
                this.active = tab
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/mixins/ellipsis.scss";
@import "@/scss/mixins/scroller.scss";
.variable-version-wrapper {
    &.variable-panel-show {
        width: 480px;
    }

    &:not(.variable-panel-show) {
        &:after {
            z-index: 2020;
            content: '';
            position: absolute;
            top: 0;
            right: 0;
            width: 2px;
            height: 100%;
            background: #699df4;
        }
    }
}

.variable-entry {
  z-index: 2020;
  position: absolute;
  right: 460px;
  top: calc(50% - 50px);
  writing-mode: vertical-lr;
  padding: 30px 2px;
  background: #a3c5fd;
  cursor: pointer;
  border-radius: 0 4px 4px 0;
  font-size: 12px;
  line-height: 16px;
  color: #fff;
  .bk-icon {
    font-size: 14px;
  }
  &.is-close {
    right: 0;
    border-radius: 4px 0 0 4px;
    background: #699df4;
    .bk-icon {
      display: inline-block;
      transform: rotate(180deg);
    }
  }
}
.variable-version-container {
  z-index: 2018;
  position: absolute;
  top: 0;
  right: 0;
  height: 100%;
  width: 480px;
  background-color: #fafbfd;
  border: 1px solid #dcdee5;
  border-top: 0;
  .select-tab-container {
    border-bottom: 1px solid #DCDEE5;
    .tab-content {
      height: 40px;
      padding: 0 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      .tab-item {
        line-height: 40px;
        cursor: pointer;
        font-size: 14px;
        color: #63656e;
        &.actived {
          color: #3a84ff;
          border-bottom: 2px solid #3a84ff;
        }
        &:last-child {
          margin-right: 16px;
        }
      }
    }
  }
  .content-wrapper {
    height: calc(100% - 40px);
    padding: 8px 24px 20px;
    overflow-y: auto;
    @include scroller(#a5a5a5, 4px);
  }
}
.current-edit-param-item {
  z-index: 11;
  width: 480px;
  position: absolute;
  top: 0;
  right: 0;
  height: 100%;
  background-color: #fff;
  border-left: 1px solid #dcdee5;
  .edit-var-header {
    display: flex;
    align-items: center;
    height: 40px;
    font-size: 16px;
    color: #313238;
    border-bottom: 1px solid #dcdee5;
    padding: 0 4px;
    .back-icon {
      cursor: pointer;
      font-size: 28px;
      color: #3a84ff;
    }
  }
  .edit-var-content {
    height: calc(100% - 90px);
    overflow-y: auto;
    padding: 24px 40px;
  }
  .edit-var-footer {
    position: absolute;
    bottom: 0;
    width: 100%;
    display: flex;
    align-items: center;
    height: 48px;
    padding: 0 24px;
    border-top: 1px solid #dcdee5;
  }
}
</style>
