<template>
    <section class="stage-review-flow-home">
        <bk-timeline :list="computedReviewGroups"></bk-timeline>
        <bk-link
            theme="primary"
            icon="devops-icon icon-plus-circle"
            icon-placement="left"
            v-if="!disabled"
            @click.native="addReviewFlow"
        >新增审核组</bk-link>
    </section>
</template>

<script>
    import UserInput from '@/components/atomFormField/UserInput'

    export default {
        props: {
            disabled: {
                type: Boolean,
                default: false
            },
            reviewGroups: {
                type: Array,
                default: () => ([])
            },
            showReviewOpt: {
                type: Boolean,
                default: false
            }
        },

        data () {
            return {
                copyReviewGroups: []
            }
        },

        computed: {
            computedReviewGroups () {
                return this.copyReviewGroups.map((item, index) => this.getReviewItem(item, index))
            },

            // 当前审核项索引
            currentReviewIndex () {
                return this.copyReviewGroups.findIndex(item => item.status === 'REVIEWING')
            }
        },

        created () {
            this.copyReviewGroups = JSON.parse(JSON.stringify(this.reviewGroups))
        },

        methods: {
            getReviewItem (item, index) {
                const userProps = {
                    clearable: true,
                    disabled: this.disabled,
                    value: item.reviewers,
                    handleChange: (name, value) => this.addReviewUser(item, name, value)
                }

                const suggestProps = {
                    placeholder: '请输入审批意见，非必填',
                    value: item.suggest,
                    disabled: !this.disabled
                }
                const suggestEvents = {
                    change: (value) => this.addReviewSuggest(item, value)
                }

                const iconEvents = {
                    click: () => this.minusReviewFlow(index)
                }

                const typeMap = {
                    PROCESS: 'success',
                    ABORT: 'danger',
                    REVIEWING: 'default'
                }
                const type = typeMap[item.status]

                const showReviewOpt = this.showReviewOpt && index === this.currentReviewIndex
                const showOptInfo = index < this.currentReviewIndex

                return {
                    content: (
                        <section class="stage-review-content">
                            <bk-form label-width="200" form-type="vertical">
                                <bk-form-item label="审核组" required>
                                    <UserInput {...{ props: userProps }} />
                                </bk-form-item>
                                <bk-form-item label="操作信息" v-show={showOptInfo}>
                                    <span>审核人：{item.operator}</span>
                                    <span>审核意见：{item.suggest}</span>
                                </bk-form-item>
                                <bk-form-item label="审核意见" v-show={showReviewOpt}>
                                    <bk-input {...{ props: suggestProps }} {...{ on: suggestEvents }} />
                                </bk-form-item>
                            </bk-form>
                            <i class="devops-icon icon-minus-circle" {...{ on: iconEvents }} v-show={!this.disabled}></i>
                        </section>
                    ),
                    type
                }
            },

            addReviewFlow () {
                const defaultFlow = {
                    params: [],
                    reviewers: [],
                    suggest: ''
                }
                this.copyReviewGroups.push(defaultFlow)
                this.triggleChange()
            },

            minusReviewFlow (index) {
                this.copyReviewGroups.splice(index, 1)
                this.triggleChange()
            },

            addReviewUser (item, name, value) {
                item.reviewers = value
                this.triggleChange()
            },

            addReviewSuggest (item, value) {
                item.suggest = value
                this.triggleChange()
            },

            triggleChange () {
                this.$emit('change', 'reviewGroups', this.copyReviewGroups)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .stage-review-flow-home {
        margin-top: 50px;
        /deep/ .bk-timeline {
            margin-left: 5px;
            .bk-timeline-dot .bk-timeline-section {
                top: -30px;
            }
        }
        /deep/ .bk-link {
            margin-top: 28px;
        }
    }
    .stage-review-content {
        display: flex;
        justify-content: flex-start;
        margin-bottom: -30px;
        .icon-minus-circle {
            cursor: pointer;
            padding-top: 12px;
            margin-left: 15px;
        }
    }
</style>
