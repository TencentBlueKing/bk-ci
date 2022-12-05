<template>
    <bk-dialog
        theme="primary"
        header-position="center"
        width="629"
        :mask-close="false"
        :value="show"
        :title="computedTitle"
        :auto-close="false"
        @confirm="confirm"
        @cancel="cancel">
        <bk-form form-type="vertical" :model="copyForm" ref="paramForm">
            <bk-form-item :label="$t('name')" :rules="[requireRule($t('name'))]" property="key" :required="true" error-display-type="normal">
                <bk-input v-model="copyForm.key"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('pipeline.alias')">
                <bk-input v-model="copyForm.chineseName"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('type')" :rules="[requireRule($t('type'))]" property="valueType" :required="true" error-display-type="normal">
                <bk-select v-model="copyForm.valueType" @selected="changeValueType" searchable>
                    <bk-option v-for="option in paramTypeList"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item :label="$t('pipeline.options')" v-if="isSelectorParam(copyForm.valueType)" :desc="$t('editPage.optionsDesc')">
                <bk-input type="textarea" :value="getTextAreaValue()" @blur="changeOption" :placeholder="$t('editPage.optionTips')"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('pipeline.defaultValue')" v-if="copyForm.valueType" :key="copyForm.valueType">
                <param-value :form="copyForm"></param-value>
            </bk-form-item>
            <bk-form-item :label="$t('pipeline.required')">
                <bk-radio-group v-model="copyForm.required">
                    <bk-radio :value="true">
                        true
                    </bk-radio>
                    <bk-radio :value="false" style="marginLeft:55px">
                        false
                    </bk-radio>
                </bk-radio-group>
            </bk-form-item>
            <bk-form-item :label="$t('description')">
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
                return keys.length ? 'Edit variable' : 'Create variables'
            }
        },

        watch: {
            show (val) {
                if (val) {
                    this.copyForm = JSON.parse(JSON.stringify({ ...defaultValue, ...this.param }))
                    this.$refs.paramForm.clearError()
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

            requireRule (label) {
                return {
                    validator (val) {
                        return val.length
                    },
                    message: label + 'is required',
                    trigger: 'blur'
                }
            },

            changeValueType (type) {
                this.copyForm.value = ''
                this.copyForm.options = []

                if (isMultipleParam(type)) this.copyForm.value = []

                if (isBooleanParam(type)) this.copyForm.value = false
            },

            changeOption (val) {
                let opts = []
                if (val && typeof val === 'string') {
                    opts = val.split('\n').map(opt => {
                        const v = opt.trim()
                        const res = v.match(/^([\w\.\\\/]+)=(\S+)$/) || [v, v, v]
                        const [, key, value] = res
                        return {
                            key,
                            value
                        }
                    })
                }
                this.copyForm.options = opts
                this.copyForm.value = isMultipleParam(this.copyForm.valueType) ? [] : ''
            },

            confirm () {
                return this.$refs.paramForm.validate(() => {
                    this.$emit('confirm', this.copyForm)
                }).catch((err) => {
                    this.$bkMessage({ message: err.content, theme: 'error' })
                })
            },

            cancel () {
                this.$emit('cancel')
            }
        }
    }
</script>
