<template>
    <section>
        <span class="review-subtitle">审核流</span>
        <bk-timeline :list="computedReviewGroups"></bk-timeline>
    </section>
</template>

<script>
    export default {
        props: {
            reviewGroups: Array
        },

        data () {
            return {
            }
        },

        computed: {
            computedReviewGroups () {
                return this.reviewGroups.map((item, index) => this.getReviewItem(item, index))
            }
        },

        methods: {
            getReviewItem (item, index) {
                console.log(item)
                const typeMap = {
                    PROCESS: 'success',
                    ABORT: 'danger',
                    REVIEWING: 'default'
                }
                const type = typeMap[item.status] || 'default'

                const paramStr = (item.params || []).map(({ key, value }) => {
                    return `${key}=${JSON.stringify(value)}`
                }).join(' | ')

                let content
                switch (item.status) {
                    case 'PROCESS':
                        content = (
                            <section class="stage-review-content">
                                <p class="review-title">
                                    {item.name}
                                    <span class="review-normal"> 由 {item.operator} 审批：</span>
                                    <span class="review-process"> 同意（继续执行流水线） </span>
                                </p>
                                <p v-show={paramStr}>变更参数：{paramStr}</p>
                                <p>审批意见：{item.suggest || '无'}</p>
                                <p>2021-12-13</p>
                            </section>
                        )
                        break
                    case 'ABORT':
                        content = (
                            <section class="stage-review-content">
                                <p class="review-title">
                                    {item.name}
                                    <span class="review-normal"> 由 {item.operator} 审批：</span>
                                    <span class="review-abort"> 驳回（取消执行，立即标记为Stage成功状态） </span>
                                </p>
                                <p>审批意见：{item.suggest || '无'}</p>
                                <p>2021-12-13</p>
                            </section>
                        )
                        break
                    default:
                        content = (
                            <section class="stage-review-content">
                                <p class="review-title">
                                    {item.name}
                                    <span class="review-normal"> 处理人：{item.reviewers.join(',')}</span>
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

<style lang="scss" scoped>
    /deep/ .bk-timeline {
        margin-left: 5px;
        margin-top: 24px;
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
        line-height: 20px;
        font-size: 14px;
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
    }
</style>
