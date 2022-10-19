<template>

    <section class="bk-pipeline-list-empty-tips">
        <Logo name="empty" size="188"></Logo>
        <p class="bk-pipeline-list-empty-tips-desc">{{ emptyTipsConfig.desc }}</p>
        <p class="bk-pipeline-list-empty-tips-btns" v-if="emptyTipsConfig.btns.length">
            <bk-button
                v-for="(btn, index) of emptyTipsConfig.btns"
                v-bind="btn.btnProps"
                :key="index"
                @click="btn.handler"
            >
                {{ btn.text }}
            </bk-button>
        </p>
    </section>
</template>

<script>
    import Logo from '@/components/Logo'
    import { bus, ADD_TO_PIPELINE_GROUP } from '@/utils/bus'
    export default {
        components: {
            Logo
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
        computed: {
            emptyTipsConfig () {
                const btnProps = {
                    text: true,
                    theme: 'primary',
                    size: 'normal'
                }
                return this.hasFilter
                    ? {
                        desc: this.$t('newlist.knowMore'),
                        imgType: '',
                        btns: [
                            {
                                btnProps,
                                handler: () => this.showSlide(true),
                                text: this.$t('newlist.filterAgain')
                            }
                        ]
                    }
                    : {
                        desc: this.$t('newlist.otherEmptyDesc'),
                        imgType: '',
                        btns: [
                            {
                                btnProps,
                                handler: () => bus.$emit(ADD_TO_PIPELINE_GROUP, this.$route.params.viewId),
                                text: this.$t('newlist.addPipelineToGroup')
                            }
                        ]
                    }
            }
        }
    }
</script>

<style lang="scss">
    .bk-pipeline-list-empty-tips {
        .bk-pipeline-list-empty-tips-desc {
            color: #979BA5;
            margin: 8px 0;
        }
    }
</style>
