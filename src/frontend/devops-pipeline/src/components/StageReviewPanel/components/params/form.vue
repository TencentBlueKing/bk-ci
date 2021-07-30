<template>
    <bk-dialog
        theme="primary"
        header-position="center"
        width="629"
        :mask-close="false"
        :value="show"
        :title="computedTitle"
        @confirm="confirm"
        @cancel="cancel">
        <bk-form form-type="vertical">
            <bk-form-item label="参数名称" :required="true">
                <bk-input v-model="copyForm.key"></bk-input>
            </bk-form-item>
            <bk-form-item label="中文名">
                <bk-input v-model="copyForm.chineseName"></bk-input>
            </bk-form-item>
            <bk-form-item label="参数类型" :required="true">
                <bk-select v-model="copyForm.valueType" @selected="changeValueType" searchable>
                    <bk-option v-for="option in paramTypeList"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item label="默认值" v-if="copyForm.valueType" :key="copyForm.valueType">
                <param-value :form="copyForm"></param-value>
            </bk-form-item>
            <bk-form-item label="是否必填">
                <bk-radio-group v-model="copyForm.required">
                    <bk-radio :value="true">
                        是
                    </bk-radio>
                    <bk-radio :value="false" style="marginLeft:55px">
                        否
                    </bk-radio>
                </bk-radio-group>
            </bk-form-item>
            <bk-form-item label="下拉选项" v-if="isSelectorParam(copyForm.valueType)">
                <bk-input type="textarea" :value="getTextAreaValue()" @blur="changeOption" :placeholder="$t('editPage.optionTips')"></bk-input>
            </bk-form-item>
            <bk-form-item label="描述">
                <bk-input type="textarea" v-model="copyForm.desc"></bk-input>
            </bk-form-item>
        </bk-form>
    </bk-dialog>
</template>

<script>
    import paramValue from './param-value.vue'
    import {
        CHECK_PARAM_LIST,
        isEnumParam,
        isMultipleParam,
        isBooleanParam
    } from '@/store/modules/atom/paramsConfig'

    const paramTypeList = CHECK_PARAM_LIST.map((item) => ({
        id: item.id,
        name: global.pipelineVue.$t(`storeMap.${item.name}`)
    }))

    const defaultValue = {
        required: false,
        options: [],
        value: ''
    }

    export default {
        components: {
            paramValue
        },

        props: {
            show: Boolean,
            param: Object
        },

        data () {
            return {
                copyForm: {},
                paramTypeList
            }
        },

        computed: {
            computedTitle () {
                const keys = Object.keys(this.param)
                return keys.length ? '编辑参数' : '添加参数'
            }
        },

        watch: {
            show (val) {
                if (val) {
                    this.copyForm = JSON.parse(JSON.stringify({ ...defaultValue, ...this.param }))
                }
            }
        },

        methods: {
            isBooleanParam,
            isMultipleParam,

            isSelectorParam (type) {
                return isMultipleParam(type) || isEnumParam(type)
            },

            getTextAreaValue () {
                return (this.copyForm.options || []).map(x => x.key).join('\n')
            },

            changeValueType (type) {
                this.copyForm.value = ''
                this.copyForm.options = []

                if (isMultipleParam(type)) this.copyForm.value = []

                if (isBooleanParam(type)) this.copyForm.value = false
            },

            changeOption (val) {
                this.copyForm.options = (val || '').split('\n').filter(v => v).map(x => ({ key: x, value: x }))
                this.copyForm.value = isMultipleParam(this.copyForm.valueType) ? [] : ''
            },

            confirm () {
                this.$emit('confirm', this.copyForm)
            },

            cancel () {
                this.$emit('cancel')
            }
        }
    }
</script>
