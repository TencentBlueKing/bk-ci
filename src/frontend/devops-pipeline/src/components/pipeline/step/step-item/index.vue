<template>
    <skeleton
        :class="[
            config.rightIcon ? config.rightIcon.status : '',
            {
                'step-item-checked': config.stageType === 'check' || config.stageType === 'status' ? config.rightIcon.checked : ''
            }
        ]"
        :extCls="type === 'create' ? 'create' : ''"
        :stepIndex="config.stepIndex"
        :stageIndex="config.stageIndex"
        :has-error="config.hasError"
        @item-enter="itemEnterHandler"
        @item-leave="itemLeaveHandler"
        @item-click="itemClickHandler">
        <template slot='icon'>
            <i class="bk-icon"
                :class="'icon-' + (type === 'create' ? 'plus-circle' : 'order')">
            </i>
        </template>

        <template slot='title'>
            <span class="step-item__title text-overflow"
                :class="{
                    skipped: config.stepStatus === 'skipped'
                }">
                {{ title }}
                <span class="warning-text" v-if="config.rightIcon && config.rightIcon.status === 'warning'">(已暂停)</span>
            </span>
        </template>

        <template slot='right'>
            <!-- 可勾选状态 start -->
            <template v-if="config.stageIndex !== 0 && config.stageType === 'check' && config.rightIcon">
                <label class="bk-form-checkbox">
                    <input type="checkbox" data-type="check"
                        v-model="localConfig.rightIcon.checked">
                        <!-- 如果直接用config，会导致打包后点击checkbox时没反应，其它地方没改成config是因为防止引入其它bug -->
                </label>
            </template>
            <!-- 可勾选状态 end -->

            <!-- 编辑状态 start -->
            <template v-if="config.stageType === 'edit' && config.rightIcon">
                <i class="bk-icon edit-icon"
                    v-show="config.rightIcon.show"
                    :class="'icon-' + config.rightIcon.icon"
                    :title="config.rightIcon.alt"
                    @click.stop="rightIconClickHandler($event)">
                </i>
            </template>
            <!-- 编辑状态 end -->

            <!-- 状态展示 start -->
            <template v-if="config.stageType === 'status' && config.rightIcon">
                <span class="step-status">
                    <template v-if="config.rightIcon.status === 'running'">
                        <span class="status-loading">
                            <img src="./../../../../images/step_loading.png" alt="loading">
                        </span>
                    </template>
                    <i class="bk-icon"
                        v-if="config.rightIcon.status !== 'running'"
                        :class="['icon-' + statusMap[config.rightIcon.status], config.rightIcon.status]"
                        :title="config.rightIcon.alt">
                    </i>
                </span>
            </template>
            <!-- 状态展示 end -->
        </template>
    </skeleton>
</template>

<script>
    import skeleton from './skeleton'
    import { mapGetters } from 'vuex'

    export default {
        props: {
            // 当前step-item的类型，可选的值有serial or parallel or create
            type: {
                type: String,
                default: 'serial'
            },
            // 当前step-item的title
            title: {
                type: [String, Number],
                default: ''
            },
            /** 当前step-item的配置选项
             *  stageIndex：step所在的stage在stage列表中的索引值
             *  stepIndex：item在stage中的索引值
             *  rightIcon：右侧icon的配置
             */
            config: {
                type: Object,
                default () {
                    return {
                        stageIndex: 0,
                        stepIndex: 0,
                        rightIcon: {},
                        hasError: false
                    }
                }
            }
        },
        data () {
            return {
                localConfig: JSON.parse(JSON.stringify(this.config))
            }
        },
        watch: {
            config: {
                handler (val) {
                    this.localConfig = JSON.parse(JSON.stringify(this.config))
                },
                deep: true
            }
        },
        computed: {
            ...mapGetters({
                'statusMap': 'pipeline/getStatusToIconMap'
            })
        },
        methods: {
            itemEnterHandler (indexObj) {
                this.$emit('item-enter', indexObj)
            },
            itemLeaveHandler (indexObj) {
                this.$emit('item-leave', indexObj)
            },
            itemClickHandler (indexObj) {
                this.$emit('item-click', indexObj)
            },
            rightIconClickHandler (e) {
                let {
                    config
                } = this

                config.rightIcon.handler({
                    stageIndex: config.stageIndex,
                    stepIndex: config.stepIndex
                })
                e.stopPropagation()
            }
        },
        components: {
            skeleton
        }
    }
</script>
