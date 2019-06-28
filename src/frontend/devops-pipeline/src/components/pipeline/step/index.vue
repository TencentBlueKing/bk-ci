<template>
    <transition name="fade">
        <section class="pipeline-step"
            :class="[config.type, config.stageStatus]"
            :style="{
                width: config.stageIndex === config.stageCount - 1 ? '316px' : '348px'
            }">
            <div class="step-mouseover-wrapper">
                <div class="step-mouseover-main"
                    @mouseenter="stepMouseEnterMainHandler"
                    @mouseleave="stepMouseLeaveMainHandler">
                </div>
                <div class="step-mouseover-aside"></div>
            </div>

            <!-- stage头部 -->
            <step-header
                :config="calcHeaderConfig"
                @title-click="titleClickHandler">
            </step-header>
            <!-- stage头部 -->

            <!-- 分隔icon start -->
            <div class="step-divider">
                <div class="pre-round">
                    <!-- <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary">
                        <div class="rotate rotate1"></div>
                        <div class="rotate rotate2"></div>
                        <div class="rotate rotate3"></div>
                        <div class="rotate rotate4"></div>
                        <div class="rotate rotate5"></div>
                        <div class="rotate rotate6"></div>
                        <div class="rotate rotate7"></div>
                        <div class="rotate rotate8"></div>
                    </div> -->
                    <i class="bk-icon static-icon"
                        v-if="config.stageStatus !== 'running'"
                        :class="`icon-${config.stageStatus ? (config.stageStatus === 'not_built' ? 'angle-right' : statusToIconMap[config.stageStatus]) : 'angle-right'}`">
                    </i>

                    <img src="./../../../images/stage_loading.png" alt="loading" class="stage-loading-icon"
                        v-else
                    >
                </div>
                <p class="main-divider"></p>
                <div class="post-round" title="新增"
                    v-show="config.stageType === 'edit'"
                    :style="{
                        right: config.stageIndex !== config.stageCount - 1 ? '30px' : '0'
                    }"
                    @click="handleCreate">
                     <!-- && (config.stageIndex === config.stageCount - 1 || showCreate) -->
                    +
                </div>
                <div class="post-round end-round"
                    v-if="config.stageType !== 'edit' && config.stageIndex === config.stageCount - 1">
                    <!-- <div class="post-round-finish"
                        v-if="config.stageType === 'status'">
                        完成
                    </div> -->
                </div>
            </div>
            <!-- 分隔icon end -->

            <div class="step-list">
                <!-- 数据列表 start -->
                <step-item
                    v-if="list.length > 0"
                    v-for="(item, index) of list"
                    :key="`step-item${index}`"
                    :title="calcTitle(item)"
                    :config="calcItemConfig('option', index)"
                    @item-enter="itemEnterHandler"
                    @item-leave="itemLeaveHandler"
                    @item-click="itemClickHandler">
                </step-item>
                <!-- 数据列表 end -->

                <!-- 添加行 start -->
                <step-item
                    v-if="config.stageType === 'edit'"
                    :type="'create'"
                    :config="calcItemConfig('create')"
                    :title="config.type === 'start' ? '添加参数' : '添加插件'"
                    @item-click="itemClickHandler">
                </step-item>
                <!-- 添加行 end -->

                <!-- 浮动新增icon start -->
                <template v-if="config.stageType === 'edit'">
                <div class="step-divider-create"
                    v-show="floatCreate.show"
                    :style="floatCreate.style"
                    @mouseenter="floatCreateEnterHandler"
                    @mouseleave="floatCreateLeaveHandler"
                    @click="floatCreateClickHandler">
                    <img src="./../../../images/pipeline_create.png" alt="create" :title="`添加${config.type === 'start' ? '参数' : '原子'}`">
                </div>
                </template>
                <!-- 浮动新增icon end -->
            </div>
        </section>
    </transition>
</template>

<script>
    import stepHeader from './step-header'
    import stepItem from './step-item'
    import { mapGetters } from 'vuex'

    export default {
        props: {
            /**
             *  当前stage与数据无关的配置项
             *  type：stage的类型，可选值有start/serial/parallel
             *  index：stage在列表中的索引值
             *  stepHeader：step-header子组件需要的参数
             *  itemRightIcon：当前页面中step-item右侧的icon
             *  stageType：当前组件的类型，可选值有edit/check/status
             *  stageCount：当前列表中共有stage的个数，让组件能感知自己在列表中的位置
             */
            config: {
                type: Object,
                default () {
                    return {
                        type: 'serial',
                        stageIndex: 0,
                        stepHeader: {},
                        stageType: 'edit',
                        stageCount: 0
                    }
                }
            },
            /**
             *  stage中的step列表
             */
            list: {
                type: Array,
                default () {
                    return [
                        {
                            itemRightIcon: {
                                checked: false, // stageType为check时，当前step是否被选中
                                status: 'loading', // stageType为status时，当前step的状态，可选值有loading，success，paused，error
                                alt: '', // icon的title值
                                handler: () => {}, // icon点击的回调函数
                                show: false // icon是否显示
                            },
                            title: '' // step的名字
                        }
                    ]
                }
            }
        },
        data () {
            return {
                floatCreate: { // 浮动新增icon相关参数
                    show: false,
                    style: '',
                    timer: -1,
                    currentStepId: 0
                },
                showCreate: false
            }
        },
        computed: {
            ...mapGetters({
                'statusToIconMap': 'pipeline/getStatusToIconMap'
            }),
            /**
             *  计算step-header需要的config对象
             */
            calcHeaderConfig () {
                let {
                    config
                } = this
                let {
                    stepHeader
                } = config

                return {
                    stageIndex: config.stageIndex,
                    tools: stepHeader.tools,
                    title: stepHeader.title,
                    subtitle: stepHeader.subtitle,
                    stageType: config.stageType,
                    status: stepHeader.status
                }
            }
        },
        methods: {
            /**
             *  计算step-item需要的config对象
             *  @param type 当前step-item的类型，option - 普通选项，create - 新增按钮
             */
            calcItemConfig (type = 'option', index = 0) {
                let {
                    config,
                    list
                } = this
                let obj = {}

                switch (type) {
                    case 'option':
                        let cur = list[index]

                        obj = {
                            stepIndex: index,
                            rightIcon: cur.itemRightIcon,
                            stageIndex: config.stageIndex,
                            stageType: config.stageType,
                            hasError: cur.hasError,
                            stepStatus: cur.stepStatus
                        }
                        break
                    case 'create':
                        obj = {
                            stepIndex: list.length,
                            stageIndex: config.stageIndex
                        }
                }

                return obj
            },
            calcTitle (item) {
                return item.data ? item.data.name : item.title
            },
            /**
             *  鼠标指向step-item时的回调函数
             *  @param indexObj 索引对象，包含stageIndex和stepIndex
             */
            itemEnterHandler (indexObj) {
                let {
                    stageIndex,
                    stepIndex
                } = indexObj
                let {
                    floatCreate,
                    pageType
                } = this

                // 清除计时器并修改浮动新增icon的位置
                clearTimeout(floatCreate.timer)

                // if
                floatCreate.style = `top: ${stepIndex * 52 - 9}px`
                floatCreate.show = true
                floatCreate.currentStepId = stepIndex

                this.$emit('item-enter', indexObj)
            },
            /**
             *  鼠标离开step-item时的回调函数
             *  @param indexObj 索引对象，包含stageIndex和stepIndex
             */
            itemLeaveHandler (indexObj) {
                let {
                    floatCreate
                } = this

                // 延时隐藏浮动新增icon
                floatCreate.timer = setTimeout(() => {
                    floatCreate.show = false
                }, 100)

                this.$emit('item-leave', indexObj)
            },
            /**
             *  点击step-item时的回调函数
             *  @param indexObj 索引对象，包含stageIndex和stepIndex
             */
            itemClickHandler (indexObj) {
                this.$emit('item-click', indexObj)
            },
            /**
             *  鼠标指向浮动新增icon时的回调函数
             */
            floatCreateEnterHandler () {
                let {
                    floatCreate
                } = this

                clearTimeout(floatCreate.timer)
                floatCreate.show = true
            },
            /**
             *  鼠标离开浮动新增icon时的回调函数
             */
            floatCreateLeaveHandler () {
                this.floatCreate.show = false
            },
            /**
             *  点击浮动新增icon时的回调函数
             */
            floatCreateClickHandler () {
                // 向上层组件触发item点击事件
                this.$emit('item-click', {
                    stageIndex: this.config.stageIndex,
                    stepIndex: this.floatCreate.currentStepId
                }, 'floatCreate')
            },
            /**
             *  点击新增按钮的回调函数
             */
            handleCreate () {
                this.$emit('create-stage', this.config.stageIndex)
            },
            /**
             *  点击title的回调函数
             */
            titleClickHandler () {
                this.$emit('title-click', this.config.stageIndex)
            },
            /**
             *  鼠标进入step主体部分
             */
            stepMouseEnterMainHandler () {
                this.showCreate = true
            },
            /**
             *  鼠标离开step主题部分
             */
            stepMouseLeaveMainHandler () {
                this.showCreate = false
            },
            /**
             *  鼠标进入step侧边部分
             */
            stepMouseEnterAsideHandler () {
                this.showCreate = true
            },
            /**
             *  鼠标离开step侧边部分
             */
            stepMouseLeaveAsideHandler () {
                this.showCreate = false
            }
        },
        components: {
            'step-header': stepHeader,
            'step-item': stepItem
        }
    }
</script>

<style lang="scss">
    @import './step.scss';
</style>
