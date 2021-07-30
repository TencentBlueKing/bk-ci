<template>
    <section>
        <ul>
            <li v-for="(reviewGroup, index) in reviewGroups" :key="index" class="review-group">
                <bk-input
                    class="review-name"
                    :disabled="disabled"
                    :value="reviewGroup.name"
                    @change="modifyReviewName(reviewGroup, ...arguments)"
                ></bk-input>

                <user-input
                    clearable
                    class="review-user"
                    :value="reviewGroup.reviewers"
                    :disabled="disabled"
                    :handle-change="(name, value) => addReviewUser(reviewGroup, name, value)"
                ></user-input>

                <bk-button text title="primary" @click="deleteReviewGroup(index)" :disabled="disabled" class="review-opt">删除</bk-button>
            </li>
        </ul>
        <bk-button text title="primary" @click="addReviewGroup" :disabled="disabled" class="review-opt">
            <i class="bk-icon icon-plus-circle"></i>添加审批步骤
        </bk-button>
    </section>
</template>

<script>
    import UserInput from '@/components/atomFormField/UserInput'

    export default {
        components: {
            UserInput
        },

        props: {
            disabled: Boolean,
            reviewGroups: Array
        },

        data () {
            return {
                copyReviewGroups: JSON.parse(JSON.stringify(this.reviewGroups))
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

            addReviewGroup () {
                const newItem = { name: '', reviewers: [] }
                this.copyReviewGroups.push(newItem)
                this.triggleChange()
            },

            triggleChange () {
                this.$emit('change', 'reviewGroups', this.copyReviewGroups)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .review-group {
        display: flex;
        align-items: center;
        margin-bottom: 10px;
        .review-name {
            width: 270px;
        }
        .review-user {
            width: 487px;
            margin: 0 8px 0 10px;
        }
    }
    .review-opt {
        display: flex;
        align-items: center;
        font-size: 12px;
        /deep/ .bk-icon {
            top: -1px;
            margin-right: 3px;
        }
    }
</style>
