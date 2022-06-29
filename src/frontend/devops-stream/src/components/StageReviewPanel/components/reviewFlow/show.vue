<template>
    <section>
        <span class="review-subtitle">{{$t('pipeline.approveFlow')}}</span>
        <bk-timeline :list="computedReviewGroups"></bk-timeline>
    </section>
</template>

<script>
    import { convertTime } from '@/utils/util'

    export default {
        props: {
            reviewGroups: {
                type: Array,
                default: () => ([])
            }
        },

        computed: {
            computedReviewGroups () {
                return this.reviewGroups.map((item, index) => this.getReviewItem(item, index))
            }
        },

        methods: {
            getReviewItem (item, index) {
                const typeMap = {
                    PROCESS: 'success',
                    ABORT: 'danger',
                    REVIEWING: 'default'
                }
                const type = typeMap[item.status] || 'default'

                const paramStr = (item.params || []).map(({ key, value }) => {
                    return `${(key || '').replace(/^variables\./, '')}=${JSON.stringify(value)}`
                }).join(' | ')

                let content
                switch (item.status) {
                    case 'PROCESS':
                        content = (
                            <section class="stage-review-content">
                                <p class="review-title">
                                    <span class="content-subtitle">{item.name}</span>
                                    <span class="review-normal"> {this.$t('pipeline.approvedBy', [item.operator])}: </span>
                                    <span class="review-process"> {this.$t('pipeline.approveContinue')} </span>
                                </p>
                                <p v-show={paramStr}><span class="mr8 max-width">{this.$t('pipeline.editVariables')}:</span>{paramStr}</p>
                                <p><span class="mr8 max-width">{this.$t('pipeline.approvalOpinion')}:</span>{item.suggest}</p>
                                <p><span class="mr8 max-width">{this.$t('pipeline.approvalTime')}:</span>{convertTime(item.reviewTime)}</p>
                            </section>
                        )
                        break
                    case 'ABORT':
                        content = (
                            <section class="stage-review-content">
                                <p class="review-title">
                                    <span class="content-subtitle">{item.name}</span>
                                    <span class="review-normal"> {this.$t('pipeline.approvedBy', [item.operator])}: </span>
                                    <span class="review-abort"> {this.$t('pipeline.rejectStop')} </span>
                                </p>
                                <p><span class="mr8 max-width">{this.$t('pipeline.approvalOpinion')}:</span>{item.suggest}</p>
                                <p><span class="mr8 max-width">{this.$t('pipeline.approvalTime')}:</span>{convertTime(item.reviewTime)}</p>
                            </section>
                        )
                        break
                    default:
                        content = (
                            <section class="stage-review-content">
                                <p class="review-title">
                                    <span class="content-subtitle">{item.name}</span>
                                    <span class="review-normal"> {this.$t('pipeline.approver')}: {item.reviewers.join(',')}</span>
                                </p>
                            </section>
                        )
                        break
                }

                return {
                    content,
                    type
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    /deep/ .bk-timeline {
        margin-left: 5px;
        margin-top: 24px;
        margin-bottom: -24px;
        .bk-timeline-dot {
            padding-bottom: 0;
            .bk-timeline-section {
                top: -23px;
            }
            .bk-timeline-content {
                max-width: inherit;
            }
        }
    }
    .stage-review-content {
        line-height: 16px;
        font-size: 12px;
        color: #777981;
        p {
            margin-bottom: 4px;
        }
        .review-title {
            margin-bottom: 7px;
            color: #777981;
        }
        .review-normal {
            display: inline-block;
            margin-left: 12px;
            color: #777981;
        }
        .review-process {
            color: #56d577;
        }
        .review-abort {
            color: #ff7979;
        }
        .content-subtitle {
            color: black;
        }
        .mr8 {
            margin-right: 8px;
        }
        .max-width {
            display: inline-block;
            min-width: 110px;
        }
    }
</style>
