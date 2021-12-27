<template>
    <section>
        <div class="matrix-container">
            <div class="matrix-header" @click="showContainerPanel">
                <div class="matrix-name" @click.stop="toggleMatrixOpen">
                    <i class="fold-icon devops-icon" :class="container.isOpen !== false ? 'icon-angle-up' : 'icon-angle-down'" style="display:block"></i>
                    <span :class="{ 'skip-name': containerDisabled || container.status === 'SKIP' }" :title="container.name">{{$t('editPage.jobMatrix')}}</span>
                </div>
                <div class="matrix-status">
                    <matrix-status type="container" :editable="editable" :container-disabled="containerDisabled" :status="container.status" :depend-on-value="dependOnValue">
                    </matrix-status>
                    <span :title="statusDesc" class="status-desc" :class="container.status">{{statusDesc}}</span>
                </div>
            </div>
            <section class="matrix-body" v-if="container.isOpen !== false && computedJobs.length">
                <Job v-for="(job, jobIndex) in computedJobs"
                    :key="job.containerId"
                    :stage-index="stageIndex"
                    :container-index="containerIndex"
                    :container-group-index="jobIndex"
                    :stage-length="stageLength"
                    :container-length="computedJobs.length"
                    :container="job"
                    :editable="false"
                    :stage-disabled="stageDisabled">
                </Job>
            </section>
        </div>

    </section>
</template>

<script>
    import Vue from 'vue'
    import { mapActions } from 'vuex'
    import Job from './Job'
    import MatrixStatus from './MatrixStatus'
    import { bus } from '@/utils/bus'

    export default {
        components: {
            MatrixStatus,
            Job
        },
        props: {
            preContainer: Object,
            container: Object,
            stageIndex: Number,
            containerIndex: Number,
            stageLength: Number,
            containerLength: Number,
            stageDisabled: Boolean,
            editable: {
                type: Boolean,
                default: true
            }
        },
        computed: {
            statusDesc () {
                let desc = ''
                if (this.container.status) {
                    if (this.container.status === 'RUNNING') {
                        const option = this.container.matrixControlOption || {}
                        if (option && option.totalCount) {
                            const finishCount = option.finishCount || 0
                            desc = `${(finishCount / option.totalCount).toFixed(2) * 100}% (${finishCount}/${option.totalCount})`
                        } else {
                            // desc = this.$t(`details.statusMap.${this.container.status}`)
                        }
                    } else {
                        // desc = this.$t(`details.statusMap.${this.container.status}`)
                    }
                }
                return desc
            },
            computedJobs () {
                this.container.groupContainers.forEach(container => {
                    if (container.isOpen === undefined) {
                        Vue.set(container, 'isOpen', false)
                    }
                    container.elements.forEach((element, index) => {
                        const eleItem = this.container.elements[index] || {}
                        const mergeEle = Object.assign({}, eleItem, element, { '@type': eleItem['@type'], classType: eleItem.classType, atomCode: eleItem.atomCode })
                        Object.assign(element, mergeEle)
                    })
                })
                return this.container.groupContainers
            },
            containerDisabled () {
                return !!(this.container.jobControlOption && this.container.jobControlOption.enable === false) || this.stageDisabled
            },
            dependOnValue () {
                if (this.container.status !== 'DEPENDENT_WAITING') return ''
                let val = ''
                if (this.container.jobControlOption && this.container.jobControlOption.dependOnType) {
                    if (this.container.jobControlOption.dependOnType === 'ID') {
                        val = this.container.jobControlOption.dependOnId || []
                    } else {
                        val = this.container.jobControlOption.dependOnName || ''
                    }
                }
                return `${this.$t('storeMap.dependOn')} 【${val}】`
            }
        },
        // websocket重新推送会覆盖上一次的折叠和打开状态, 记住上一次状态
        watch: {
            'container.groupContainers' (val, oldVal) {
                if (val.length > 0 && val.length === oldVal.length) {
                    for (let i = 0; i < val.length; i++) {
                        if (val[i].isOpen === undefined && oldVal[i] !== undefined) {
                            Vue.set(val[i], 'isOpen', oldVal[i].isOpen)
                        }
                    }
                }
            },
            'container.isOpen' (val, oldVal) {
                if (val === undefined && oldVal !== undefined) {
                    Vue.set(this.container, 'isOpen', !!oldVal)
                }
            }
        },
        created () {
            if (this.containerDisabled) {
                this.container.runContainer = false
            }
            Vue.set(this.container, 'isOpen', true)
        },
        methods: {
            ...mapActions('atom', [
                'togglePropertyPanel'
            ]),
            
            showContainerPanel () {
                const { stageIndex, containerIndex } = this
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: {
                        stageIndex,
                        containerIndex
                    }
                })
            },

            toggleMatrixOpen  () {
                this.container.isOpen = !this.container.isOpen
                bus.$emit('update-container-line', {
                    stageIndex: this.stageIndex,
                    containerIndex: this.containerIndex + 1
                })
            }
        }
    }
</script>

<style lang="scss">
    @import "./Stage";
    .devops-stage-container {
        .matrix-container {
            border: 1px solid #B5C0D5;
            padding: 10px;
            background: #fff;
        }
        .matrix-header {
            display: flex;
            align-items: center;
            cursor: pointer;
            justify-content: space-between;
            height: 20px;
            .matrix-name {
                display: flex;
                align-items: center;
                font-size: 14px;
                color: #222222;
                .fold-icon {
                    margin-right: 10px;
                }
            }
            .matrix-status {
                display: flex;
                align-items: center;
                color: $primaryColor;
                .status-desc {
                    font-size: 12px;
                    max-width: 110px;
                    overflow: hidden;
                    text-overflow:ellipsis;
                    white-space: nowrap;
                }
            }
        }

        .matrix-body {
            margin-top: 12px;
        }

        .container-title {
            display: flex;
            height: $itemHeight;
            background: #33333f;
            color: white;
            font-size: 14px;
            align-items: center;
            position: relative;
            margin: 0 0 16px 0;
            // width: 240px;
            z-index: 3;
            > .container-name {
                @include ellipsis();
                flex: 1;
                padding: 0 12px;
                span:hover {
                    color: $primaryColor;
                }
            }

            .atom-canskip-checkbox {
                margin-right: 6px;
                &.is-disabled .bk-checkbox {
                    background-color: transparent;
                    border-color: #979BA4;
                }

            }
            input[type=checkbox] {
                border-radius: 3px;
            }
            .debug-btn {
                position: absolute;
                height: 100%;
                right: 0;
            }
            .close {
                @include add-plus-icon(#2E2E3A, #2E2E3A, #c4c6cd, 16px, true);
                @include add-plus-icon-hover($dangerColor, $dangerColor, white);
                border: none;
                display: none;
                margin-right: 10px;
                transform: rotate(45deg);
                cursor: pointer;
                &:before, &:after {
                    left: 7px;
                    top: 4px;
                }
            }
        }
    }
</style>
