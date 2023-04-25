<template>
    <section>
        <bk-form>
            <bk-form-item
                v-for="(reviewGroup, index) in copyReviewGroups"
                :key="index"
                :class="{ 'is-error': errorIndexs.includes(index), 'review-form': true }"
            >
                <section class="review-group">
                    <bk-input
                        class="review-name"
                        :placeholder="'Flow ' + (index + 1)"
                        :disabled="disabled"
                        :value="reviewGroup.name"
                        @change="modifyReviewName(reviewGroup, ...arguments)"
                    ></bk-input>

                    <staff-input
                        clearable
                        class="review-user"
                        :placeholder="$t('stageReview.userInputTips')"
                        :value="reviewGroup.reviewers"
                        :disabled="disabled"
                        :handle-change="(name, value) => addReviewUser(reviewGroup, name, value)"
                    ></staff-input>

                    <bk-button text title="primary" @click="deleteReviewGroup(index)" :disabled="disabled" class="review-opt">{{ $t('delete') }}</bk-button>
                </section>
                <span v-if="errorIndexs.includes(index)" class="bk-form-tip is-danger">{{ $t('stageReview.reviewRequire') }}</span>
            </bk-form-item>
        </bk-form>
        <bk-button text title="primary" @click="addReviewGroup" :disabled="disabled || reviewGroups.length >= 5" class="review-opt mt3">
            <i class="bk-icon icon-plus-circle"></i>{{ $t('stageReview.addFlow') }}
        </bk-button>
    </section>
</template>

<script>
    import StaffInput from '@/components/atomFormField/StaffInput'

    export default {
        components: {
            StaffInput
        },

        props: {
            disabled: Boolean,
            reviewGroups: Array
        },

        data () {
            return {
                copyReviewGroups: JSON.parse(JSON.stringify(this.reviewGroups)),
                errorIndexs: []
            }
        },

        watch: {
            errorIndexs (val) {
                const isReviewError = val.length > 0 || this.copyReviewGroups.length <= 0
                this.$emit('change', 'isReviewError', isReviewError)
            }
        },

        created () {
            this.valideReviewGroups()
        },

        methods: {
            modifyReviewName (reviewGroup, value) {
                reviewGroup.name = value
                this.triggleChange()
            },

            addReviewUser (reviewGroup, name, value) {
                reviewGroup.reviewers = value
                this.triggleChange()
            },

            deleteReviewGroup (index) {
                this.copyReviewGroups.splice(index, 1)
                this.triggleChange()
            },

            addReviewGroup () {
                const newItem = { name: '', reviewers: [] }
                this.copyReviewGroups.push(newItem)
                this.triggleChange()
            },

            triggleChange () {
                this.valideReviewGroups()
                this.$emit('change', 'reviewGroups', this.copyReviewGroups)
            },

            valideReviewGroups () {
                this.errorIndexs = []
                this.copyReviewGroups.forEach((reviewGroup, index) => {
                    if (reviewGroup.name === '' || !reviewGroup.reviewers || reviewGroup.reviewers.length <= 0) {
                        this.errorIndexs.push(index)
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .review-group {
        display: flex;
        align-items: center;
        .review-name {
            width: 270px;
        }
        .review-user {
            width: 487px;
            margin: 0 8px 0 10px;
        }
    }
    .review-form {
        margin-bottom: 10px;
        &:last-child, &.is-error {
            margin-bottom: 0px;
        }
    }
    .is-error {
        ::v-deep .devops-staff-input {
            border-color: #ff5656;
            color: #ff5656;
        }
    }
    .review-opt {
        display: flex;
        align-items: center;
        font-size: 12px;
        ::v-deep .bk-icon {
            top: -1px;
            margin-right: 3px;
        }
    }
    .mt3 {
        margin-top: 3px;
    }
</style>
