<template>
    <div :class="['bk-pipeline-matrix-group', {
        'un-exec-this-time': reactiveData.isExecDetail && isUnExecThisTime
    }]">
        <header class="bk-pipeline-matrix-group-header" @click="showMatrixPanel">
            <div class="matrix-name" @click.stop="toggleMatrixOpen()">
                <Logo name="angle-down" size="12" :class="matrixToggleCls"></Logo>
                <span :class="matrixTitleCls">
                    {{matrix.name || t('jobMatrix')}}
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
                :key="job.id"
                :container="job"
                :container-group-index="jobIndex"
                v-bind="restProps"
                :ref="job.id"
            >
            </Job>
        </section>
    </div>
</template>

<script>
    import StatusIcon from './StatusIcon'
    import Job from './Job'
    import Logo from './Logo'
    import { STATUS_MAP, CLICK_EVENT_NAME } from './constants'
    import { getDependOnDesc, eventBus, isTriggerContainer } from './util'
    import { localeMixins } from './locale'
    
    export default {
        components: {
            StatusIcon,
            Job,
            Logo
        },
        mixins: [localeMixins],
        props: {
            stage: {
                type: Object,
                required: true
            },
            matrix: {
                type: Object,
                required: true
            },
            disabled: Boolean,
            stageIndex: Number,
            containerIndex: Number,
            containerLength: Number,
            stageDisabled: Boolean,
            handleChange: {
                type: Function,
                required: true
            },
            stageLength: Number,
            updateCruveConnectHeight: Function
        },
        inject: [
            'reactiveData'
        ],
        data () {
            return {
                isMatrixOpen: false
            }
        },
        computed: {
            restProps () {
                const { matrix, ...restProps } = this.$props
                return restProps
            },
            matrixToggleCls () {
                return `matrix-fold-icon ${this.isMatrixOpen ? 'open' : ''}`
            },
            matrixTitleCls () {
                return {
                    'skip-name': this.disabled || this.matrix.status === STATUS_MAP.SKIP
                }
            },
            isUnExecThisTime () {
                return this.matrix?.executeCount < this.reactiveData.currentExecCount
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
                        const progress = totalCount === 0 ? 0 : Math.round((finishCount / totalCount) * 100)
                        return `${progress}% (${finishCount}/${totalCount})`
                    }
                    return ''
                } catch (e) {
                    return ''
                }
            },
            computedJobs () {
                return this.matrix.groupContainers.map(container => {
                    return {
                        ...container,
                        elements: container.elements.map((element, index) => {
                            const eleItem = this.matrix.elements[index] || {}
                            return Object.assign(element, {
                                ...eleItem,
                                ...element,
                                '@type': eleItem['@type'],
                                classType: eleItem.classType,
                                atomCode: eleItem.atomCode
                            })
                        })
                    }
                })
            },
            hasMatrixJob () {
                return this.computedJobs.length > 0
            },
            dependOnValue () {
                if (isTriggerContainer(this.matrix)) return ''
                const val = getDependOnDesc(this.matrix)
                return `${this.t('dependOn')} 【${val}】`
            }
        },
        methods: {
            toggleMatrixOpen (open) {
                this.isMatrixOpen = open ?? !this.isMatrixOpen
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
  @import "./conf";
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
          flex-shrink: 0;
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
      > div {
        margin-bottom: 34px;
      }
    }
  }
</style>
