<template>
    <div class="bk-pipeline-matrix-group">
        <header class="bk-pipeline-matrix-group-header" @click="showMatrixPanel">
            <div class="matrix-name" @click.stop="toggleMatrixOpen">
                <i :class="matrixToggleCls"></i>
                <span :class="matrixTitleCls">
                    {{$t('editPage.jobMatrix')}}
                </span>
            </div>
            <div class="matrix-status">
                <status-icon
                    type="matrix"
                    :status="matrix.status"
                    :depend-on-value="dependOnValue"
                >
                </status-icon>
                <span v-if="statusDesc" :title="statusDesc" :class="matrixStatusDescCls">{{statusDesc}}</span>
            </div>
        </header>
        <section class="matrix-body" v-if="isMatrixOpen && hasMatrixJob">
            <Job
                v-for="(job, jobIndex) in computedJobs"
                :key="job.containerId"
                :container="job"
                :stage-index="stageIndex"
                :container-index="containerIndex"
                :container-group-index="jobIndex"
                :container-length="containerLength"
                :stage-disabled="stageDisabled"
                :editable="editable"
                :is-preview="isPreview"
                :handle-change="handleChange"
                :user-name="userName"
                :match-rules="matchRules"
                :can-skip-element="canSkipElement"
            >
            </Job>
        </section>
    </div>
    
</template>

<script>
    import StatusIcon from './StatusIcon'
    import Job from './Job'
    import { STATUS_MAP, CLICK_EVENT_NAME } from './constants'
    import { getDependOnDesc, eventBus } from './util'
    export default {
        components: {
            StatusIcon,
            Job
        },
        props: {
            matrix: {
                type: Object,
                required: true
            },
            disabled: Boolean,
            stageIndex: Number,
            containerIndex: Number,
            containerLength: Number,
            stageDisabled: Boolean,
            editable: {
                type: Boolean,
                default: true
            },
            isPreview: {
                type: Boolean,
                default: false
            },
            canSkipElement: {
                type: Boolean,
                default: false
            },
            handleChange: {
                type: Function,
                required: true
            },
            userName: {
                type: String,
                default: 'unknow'
            },
            updateCruveConnectHeight: Function,
            matchRules: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                isMatrixOpen: false
            }
        },
        computed: {
            matrixToggleCls () {
                return `matrix-fold-icon devops-icon icon-angle-down ${this.isMatrixOpen ? 'open' : ''}`
            },
            matrixTitleCls () {
                return {
                    'skip-name': this.disabled || this.matrix.status === STATUS_MAP.SKIP
                }
            },
            matrixStatusDescCls () {
                return {
                    'status-desc': true,
                    [this.matrix.status]: !!this.matrix.status
                }
            },
            statusDesc () {
                try {
                    if (this.matrix.status === STATUS_MAP.RUNNING) {
                        const { matrixControlOption = {} } = this.matrix
                        const { finishCount = 0, totalCount } = matrixControlOption
                        const progress = totalCount === 0 ? 0 : (finishCount / totalCount).toFixed(2) * 100
                        return `${progress}% (${finishCount}/${totalCount})`
                    }
                    return ''
                } catch (e) {
                    return ''
                }
            },
            computedJobs () {
                return this.matrix.groupContainers.map(container => {
                    container.elements = container.elements.map((element, index) => {
                        const eleItem = this.matrix.elements[index] || {}
                        return Object.assign(element, eleItem, element, {
                            '@type': eleItem['@type'],
                            classType: eleItem.classType,
                            atomCode: eleItem.atomCode
                        })
                    })
                    return container
                })
            },
            hasMatrixJob () {
                return this.computedJobs.length > 0
            },
            dependOnValue () {
                const val = getDependOnDesc(this.matrix)
                return `${this.$t('storeMap.dependOn')} 【${val}】`
            }
        },
        methods: {
            toggleMatrixOpen () {
                this.isMatrixOpen = !this.isMatrixOpen
                this.updateCruveConnectHeight()
            },
            showMatrixPanel () {
                eventBus.$emit(CLICK_EVENT_NAME, {
                    stageIndex: this.stageIndex,
                    containerIndex: this.containerIndex
                })
            }
        }
    }
</script>

<style lang="scss">
  @import "./index";
  .bk-pipeline-matrix-group {
    border: 1px solid $borderNormalColor;
    padding: 10px;
    background: #fff;
    .bk-pipeline-matrix-group-header {
      display: flex;
      align-items: center;
      cursor: pointer;
      justify-content: space-between;
      height: 20px;
      .matrix-name {
        display: flex;
        align-items: center;
        font-size: 14px;
        color: #222;
        min-width: 0;
    
        .matrix-fold-icon {
          display: block;
          margin-right: 10px;
          transition: all 0.3s ease;
          &.open {
            transform: rotate(-180deg);
          }
        }
        > span {
          @include ellipsis();
        }
      }
      .matrix-status {
        color: $primaryColor;
        display: flex;
        align-items: center;
        .status-desc {
            font-size: 12px;
            @include ellipsis(110px);
        }
      }
    }
    .matrix-body {
      margin-top: 12px;
    }
  }
</style>
