<template>
    <bk-input v-model="form.value" v-if="isStringParam(form.valueType)" :disabled="disabled"></bk-input>

    <bk-input v-model="form.value" type="textarea" v-else-if="isTextareaParam(form.valueType)" :disabled="disabled"></bk-input>

    <bk-select v-model="form.value" :multiple="isMultipleParam(form.valueType)" searchable v-else-if="isSelectorParam(form.valueType)" :disabled="disabled">
        <bk-option v-for="option in form.options"
            :key="option.value"
            :id="option.value"
            :name="option.key">
        </bk-option>
    </bk-select>

    <bk-checkbox v-model="form.value" v-else-if="isCheakboxParam(form.valueType)"></bk-checkbox>

    <bk-radio-group v-model="form.value" v-else-if="isBooleanParam(form.valueType)">
        <bk-radio :value="true" :disabled="disabled">
            true
        </bk-radio>
        <bk-radio :value="false" :disabled="disabled" style="marginLeft:44px">
            false
        </bk-radio>
    </bk-radio-group>
</template>

<script>
    import {
        isEnumParam,
        isMultipleParam,
        isTextareaParam,
        isStringParam,
        isBooleanParam,
        isCheakboxParam
    } from '@/store/modules/atom/paramsConfig'

    export default {
        props: {
            form: Object,
            disabled: Boolean
        },

        methods: {
            isBooleanParam,
            isStringParam,
            isTextareaParam,
            isMultipleParam,
            isCheakboxParam,

            isSelectorParam (type) {
                return isMultipleParam(type) || isEnumParam(type)
            }
        }
    }
</script>
