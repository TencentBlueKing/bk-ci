<template>
    <section>
        <form-field
            :hide-colon="true"
            :desc="$t('editPage.batchAddTips')"
            :label="$t('editPage.batchAdd')"
        >
            <bk-input
                class="key-val"
                :type="'textarea'"
                v-model="batchInput"
                :row="3"
                :placeholder="$t('editPage.optionTips')"
            />
        </form-field>
        <div class="batch-confirm-div">
            <span @click="handleBatchInput">{{ $t('editPage.batchAddBtn') }}</span>
        </div>
    </section>
</template>

<script>
    export default {
        props: {
            submitBatchAdd: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                batchInput: ''
            }
        },
        methods: {
            handleBatchInput () {
                this.submitBatchAdd(this.batchInput)
                this.batchInput = ''
            }
        }
    }
</script>

<style lang="scss" scoped>
    .batch-confirm-div {
        width: 100%;
        padding: 8px 0 16px 0;
        border-bottom: 1px solid #DCDEE5;
        color: #3A84FF;
        cursor: pointer;
        font-size: 12px;
    }
</style>
