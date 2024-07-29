<template>
    <div class="composite-input-warpper">
        <bk-input
            class="composite-input"
            :style="{ 'width': width ? `${width}px` : '100%' }"
            :disabled="disabled"
            :value="value"
            :placeholder="placeholder"
            @change="handleChangeValue"
        >
            <template
                v-if="prependText"
                slot="prepend"
            >
                <div
                    class="group-text"
                    :style="{ 'width': `${labelWidth}px` }"
                    v-bk-overflow-tips="prependText"
                >
                    {{ prependText }}
                </div>
            </template>
            <template
                v-if="appendText"
                slot="append"
            >
                <div
                    class="group-text"
                    :style="{ 'width': labelWidth ? `${labelWidth}px` : 'initial' }"
                    v-bk-overflow-tips="appendText"
                >
                    {{ appendText }}
                </div>
            </template>
        </bk-input>
        <i
            v-if="iconDesc"
            v-bk-tooltips="{
                content: iconDesc
            }"
            class="bk-icon icon-question-circle-shape"
        />
    </div>
</template>

<script>
    import atomFieldMixin from './atomFieldMixin'
    export default {
        name: 'composite-input',
        mixins: [atomFieldMixin],
        props: {
            value: String,
            appendText: String,
            prependText: String,
            disabled: Boolean,
            iconDesc: String,
            placeholder: String,
            labelWidth: String,
            width: String
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
        .bk-icon {
            margin-left: 10px;
            line-height: 32px;
            cursor: pointer;
        }
    }
</style>
