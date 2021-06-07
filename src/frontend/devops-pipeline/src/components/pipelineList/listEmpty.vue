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
                    title: this.$t('newlist.knowMore'),
                    desc: this.$t('newlist.knowMore'),
                    imgType: '',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.showSlide(true),
                            text: this.$t('newlist.filterAgain')
                        }
                    ]
                },
                collectEmptyTipsConfig: {
                    title: this.$t('newlist.coloectEmptyTitle'),
                    desc: this.$t('newlist.collectEmptyDesc'),
                    imgType: 'noCollect'
                },
                mypipelineEmptyTipsConfig: {
                    title: this.$t('newlist.myEmptyTitle'),
                    desc: this.$t('newlist.myEmptyDesc'),
                    imgType: '',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.createPipeline(),
                            text: this.$t('newlist.createPipeline')
                        },
                        {
                            theme: 'default',
                            size: 'normal',
                            handler: () => this.importPipeline(),
                            text: this.$t('newPipelineFromJSONLabel')
                        }
                    ]
                },
                allPipelineEmptyTipsConfig: {
                    title: this.$t('newlist.allEmptyTitle'),
                    desc: this.$t('newlist.allEmptyDesc'),
                    imgType: '',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.createPipeline(),
                            text: this.$t('newlist.createPipeline')
                        },
                        {
                            theme: 'default',
                            size: 'normal',
                            handler: () => this.importPipeline(),
                            text: this.$t('newPipelineFromJSONLabel')
                        }
                    ]
                },
                otherViewsEmptyTipsConfig: {
                    title: this.$t('newlist.otherEmptyTitle'),
                    desc: this.$t('newlist.otherEmptyDesc'),
                    imgType: '',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.createPipeline(),
                            text: this.$t('newlist.createPipeline')
                        },
                        {
                            theme: 'default',
                            size: 'normal',
                            handler: () => this.importPipeline(),
                            text: this.$t('newPipelineFromJSONLabel')
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
            importPipeline () {
                this.$emit('showImport', true)
            }
        }
    }
</script>
