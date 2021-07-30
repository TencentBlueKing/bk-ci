<template>
    <section>
        <span class="review-subtitle">
            审核流
            <i class="bk-icon icon-clock"></i>
        </span>

        <bk-steps
            controllable
            class="review-steps"
            :steps="computedReviewSteps"
            :cur-step="curStep"
            @step-changed="stepChange"
        ></bk-steps>
        <bk-divider></bk-divider>

        <span class="review-subtitle">
            当前状态<span class="gray-color ml20">审批中，处理人：xuzhan</span>
        </span>
        <bk-radio-group v-model="isCancel" class="review-result">
            <bk-radio :value="false" :disabled="disabled">
                同意 <span class="gray-color">（继续执行流水线）</span>
            </bk-radio>
            <bk-radio :value="true" :disabled="disabled" class="ml135">
                驳回 <span class="gray-color">（取消执行，立即标记为Stage成功状态）</span>
            </bk-radio>
        </bk-radio-group>

        <span class="review-subtitle">审核意见</span>
        <bk-input
            placeholder="请输入审核意见，驳回时必填"
            type="textarea"
            :rows="3"
            :maxlength="200"
            :disabled="disabled"
            v-model="suggest">
        </bk-input>
        <span class="error-message">{{ errMessage }}</span>
    </section>
</template>

<script>
    export default {
        props: {
            reviewGroups: Array,
            showReviewGroup: Object,
            disabled: Boolean
        },

        data () {
            return {
                curStep: this.reviewGroups.findIndex(x => x === this.showReviewGroup) + 1,
                isCancel: false,
                suggest: '',
                errMessage: ''
            }
        },

        computed: {
            computedReviewSteps () {
                return this.reviewGroups.map((item, index) => {
                    return {
                        title: item.name,
                        icon: index + 1
                    }
                })
            }
        },

        watch: {
            showReviewGroup: {
                handler () {
                    this.suggest = this.showReviewGroup.suggest
                    this.isCancel = this.showReviewGroup.status === 'ABORT'
                },
                immediate: true
            }
        },

        methods: {
            stepChange (index) {
                this.curStep = index
                const showReviewGroup = this.reviewGroups[index - 1]
                this.$emit('update:showReviewGroup', showReviewGroup)
            },

            getApproveData () {
                return new Promise((resolve, reject) => {
                    if (this.isCancel && this.suggest === '') {
                        this.errMessage = '驳回时，审核意见必填'
                        reject(new Error(this.errMessage))
                    } else {
                        this.errMessage = ''
                        resolve({
                            isCancel: this.isCancel,
                            suggest: this.suggest,
                            id: this.showReviewGroup.id
                        })
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .review-steps {
        margin: 25px 0;
    }
    .review-result {
        margin-top: 2px;
    }
    .gray-color {
        color: #979BA5;
    }
    .ml135 {
        margin-left: 135px;
    }
    .error-message {
        font-size: 12px;
        color: #ea3636;
        line-height: 18px;
    }
</style>
