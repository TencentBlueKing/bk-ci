<template>
    <div class="composite-input-warpper">
        <bk-input class="composite-input" :disabled="disabled" :value="value" @change="handleChangeValue">
            <template v-if="prependText" slot="prepend">
                <div class="group-text">{{ prependText }}</div>
            </template>
            <template v-if="appendText" slot="append">
                <div class="group-text">{{ appendText }}</div>
            </template>
        </bk-input>
        <i
            v-if="iconDesc"
            v-bk-tooltips="{
                content: iconDesc
            }"
            class="bk-icon icon-question-circle-shape" />
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'composite-input',
        mixins: [atomFieldMixin],
        props: {
            value: String,
            appendText: String,
            prependText: String,
            disabled: Boolean,
            iconDesc: String
        },
        methods: {
            handleChangeValue (val) {
                const { name, handleChange } = this
                handleChange(name, val)
            }
        }
    }
</script>

<style lang="scss">
    .composite-input-warpper {
        display: flex;
        .composite-input {
            width: 150px;
        }
        .bk-icon {
            margin-left: 10px;
            line-height: 32px;
            cursor: pointer;
        }
    }
</style>
