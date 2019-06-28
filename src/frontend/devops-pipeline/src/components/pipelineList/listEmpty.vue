<template>
    <section>
        <empty-tips v-if="hasFilter"
            :title="filterNoResultTipsConfig.title"
            :desc="filterNoResultTipsConfig.desc"
            :btns="filterNoResultTipsConfig.btns">
        </empty-tips>

        <empty-tips
            v-else-if="pageType === 'collect'"
            :img-type="collectEmptyTipsConfig.imgType"
            :title="collectEmptyTipsConfig.title"
            :desc="collectEmptyTipsConfig.desc"
            :btns="collectEmptyTipsConfig.btns">
        </empty-tips>

        <empty-tips
            v-else-if="pageType === 'myPipeline'"
            :title="mypipelineEmptyTipsConfig.title"
            :desc="mypipelineEmptyTipsConfig.desc"
            :btns="mypipelineEmptyTipsConfig.btns">
        </empty-tips>

        <empty-tips
            v-else-if="pageType === 'allPipeline'"
            :title="allPipelineEmptyTipsConfig.title"
            :desc="allPipelineEmptyTipsConfig.desc"
            :btns="allPipelineEmptyTipsConfig.btns">
        </empty-tips>

        <empty-tips
            v-else
            :title="otherViewsEmptyTipsConfig.title"
            :desc="otherViewsEmptyTipsConfig.desc"
            :btns="otherViewsEmptyTipsConfig.btns">
        </empty-tips>
    </section>
</template>

<script>
    import { mapState } from 'vuex'
    import emptyTips from '@/components/pipelineList/imgEmptyTips'
    export default {
        components: {
            emptyTips
        },
        props: {
            hasPipeline: {
                type: Object,
                default: {}
            },
            hasFilter: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                filterNoResultTipsConfig: {
                    title: '搜索结果为空',
                    desc: '找不到满足当前过滤条件的流水线，请重新设置过滤条件',
                    imgType: '',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.showSlide(true),
                            text: '重新过滤'
                        }
                    ]
                },
                collectEmptyTipsConfig: {
                    title: '您尚未添加任何流水线至收藏夹',
                    desc: '将鼠标悬浮到更多ICON上点击收藏',
                    imgType: 'noCollect'
                },
                mypipelineEmptyTipsConfig: {
                    title: '创建自己的第一条流水线',
                    desc: '你还没有拥有任务流水线，可以点击下方 “创建流水线” 按钮，进行创建',
                    imgType: '',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.createPipeline(),
                            text: '创建流水线'
                        },
                        {
                            theme: 'default',
                            size: 'normal',
                            handler: () => this.tutorial(),
                            text: '了解更多'
                        }
                    ]
                },
                allPipelineEmptyTipsConfig: {
                    title: '创建项目的第一条流水线',
                    desc: '该项目下还没有拥有任务流水线，可以点击下方 “创建流水线” 按钮，进行创建',
                    imgType: '',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.createPipeline(),
                            text: '创建流水线'
                        },
                        {
                            theme: 'default',
                            size: 'normal',
                            handler: () => this.tutorial(),
                            text: '了解更多'
                        }
                    ]
                },
                otherViewsEmptyTipsConfig: {
                    title: '该视图下没有任何流水线',
                    desc: '该视图下还没有拥有任务流水线，可以点击下方 “创建流水线” 按钮，进行创建，或切换到其它视图',
                    imgType: '',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.createPipeline(),
                            text: '创建流水线'
                        },
                        {
                            theme: 'default',
                            size: 'normal',
                            handler: () => this.tutorial(),
                            text: '了解更多'
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapState('pipelines', [
                'currentViewId'
            ]),
            pageType () {
                return this.currentViewId
            }
        },
        methods: {
            showSlide (val) {
                this.$emit('showSlide', val)
            },
            createPipeline () {
                this.$emit('showCreate', true)
            },
            tutorial () {
                window.open(`${DOCS_URL_PREFIX}/所有服务/流水线/什么是流水线/summary.html`, '_blank')
            }
        }
    }
</script>
