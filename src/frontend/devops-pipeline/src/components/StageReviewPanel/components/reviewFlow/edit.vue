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
                    <div
                        class="review-warpper"
                    >
                        <bk-select
                            v-model="reviewGroup.reviewType"
                            class="review-type-select"
                            @change="() => handleChangeReviewType(index)"
                        >
                            <bk-option
                                v-for="option in reviewTypeList"
                                :key="option.id"
                                :id="option.id"
                                :name="option.name"
                            >
                            </bk-option>
                        </bk-select>
                        <user-group-input
                            v-if="reviewGroup.reviewType === 'group'"
                            clearable
                            class="review-user"
                            :placeholder="reviewGroup.reviewType ? $t('stageReview.userGroupInputTips') : ''"
                            :value="reviewGroup.groups"
                            :disabled="disabled"
                            :handle-change="(name, value) => addReviewGroup(reviewGroup, name, value)"
                        >
                        </user-group-input>
                        <user-input
                            v-else
                            clearable
                            class="review-user"
                            :placeholder="reviewGroup.reviewType ? $t('stageReview.userInputTips') : ''"
                            :value="reviewGroup.reviewers"
                            :disabled="disabled || !reviewGroup.reviewType"
                            :handle-change="(name, value) => addReviewUser(reviewGroup, name, value)"
                        ></user-input>
                    </div>

                    <bk-button
                        text
                        title="primary"
                        @click="deleteReviewGroup(index)"
                        :disabled="disabled"
                        class="review-opt"
                    >
                        {{ $t('delete') }}
                    </bk-button>
                </section>
                <span
                    v-if="errorIndexs.includes(index)"
                    class="bk-form-tip is-danger"
                >{{
                    $t('stageReview.reviewRequire') }}</span>
            </bk-form-item>
        </bk-form>
        <bk-button
            text
            title="primary"
            @click="addReviewItem"
            :disabled="disabled || reviewGroups.length >= 5"
            class="review-opt mt3"
        >
            <i class="bk-icon icon-plus-circle"></i>{{ $t('stageReview.addFlow') }}
        </bk-button>
    </section>
</template>

<script>
    import UserInput from '@/components/atomFormField/UserInput'
    import UserGroupInput from '@/components/atomFormField/UserGroupInput'

    export default {
        components: {
            UserInput,
            UserGroupInput
        },

        props: {
            disabled: Boolean,
            reviewGroups: Array
        },

        data () {
            return {
                copyReviewGroups: JSON.parse(JSON.stringify(this.reviewGroups)),
                reviewTypeList: [
                    {
                        id: 'user',
                        name: this.$t('stageReview.reviewer')
                    },
                    {
                        id: 'group',
                        name: this.$t('stageReview.groups')
                    }
                ],
                reviewType: 'user'
            }
        },

        computed: {
            errorIndexs () {
                return this.copyReviewGroups.map((reviewGroup, index) => {
                    if (!reviewGroup.name) return index
                    if (reviewGroup.reviewers.length <= 0 && reviewGroup.groups.length <= 0) {
                        return index
                    }
                    return -1
                }).filter(item => item > -1)
            }
        },

        watch: {
            errorIndexs (val) {
                const isReviewError = val.length > 0 || this.copyReviewGroups.length <= 0
                this.$emit('change', 'isReviewError', isReviewError)
            }
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

            addReviewItem () {
                const newItem = { name: '', reviewers: [], groups: [] }
                this.copyReviewGroups.push(newItem)
                this.triggleChange()
            },

            triggleChange () {
                this.$emit('change', 'reviewGroups', this.copyReviewGroups)
            },

            handleChangeReviewType (index) {
                this.copyReviewGroups[index].reviewers = []
                this.copyReviewGroups[index].groups = []
                this.$emit('change', 'reviewGroups', this.copyReviewGroups)
            },

            addReviewGroup (reviewGroup, name, value) {
                reviewGroup.groups = value
                this.triggleChange()
            }
        }
    }
</script>

<style lang="scss" scoped>
.review-group {
    display: flex;
    align-items: center;

    .review-name {
        width: 240px;
    }

    .review-warpper {
        width: 520px;
        margin-left: 10px;
        display: flex;
        .review-type-select {
            width: 120px;
            border-right: none;
        }
        .review-user {
            flex: 1;
            margin-right: 5px;
        }
    }
}

.review-form {
    margin-bottom: 10px;

    &:last-child,
    &.is-error {
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
