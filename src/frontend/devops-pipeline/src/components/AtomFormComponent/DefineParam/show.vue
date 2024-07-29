<template>
    <div class="define-params-show">
        <selector
            :popover-min-width="250"
            v-if="isSelectorParam(param.valueType)"
            :handle-change="(name, value) => handleParamChange(name, value, paramIndex)"
            :list="transformOpt(param.options)"
            :multi-select="isMultipleParam(param.valueType)"
            name="value"
            :data-vv-scope="`param-${param.key}`"
            :placeholder="$t('editPage.selectParamValueTips')"
            :disabled="disabled && !editValueOnly"
            :key="param.valueType"
            :value="getSelectorDefaultVal(param)"
        >
        </selector>
        <enum-input
            v-if="isBooleanParam(param.valueType)"
            name="value"
            :list="boolList"
            :disabled="disabled && !editValueOnly"
            :data-vv-scope="`param-${param.key}`"
            :handle-change="(name, value) => handleParamChange(name, value, paramIndex)"
            :value="param.value"
        >
        </enum-input>
        <vuex-input
            v-if="isStringParam(param.valueType)"
            :disabled="disabled && !editValueOnly"
            :handle-change="(name, value) => handleParamChange(name, value, paramIndex)"
            name="value"
            :click-unfold="true"
            :data-vv-scope="`param-${param.key}`"
            :placeholder="$t('editPage.paramValueTips')"
            :value="param.value"
        />
        <vuex-textarea
            v-if="isTextareaParam(param.valueType)"
            :click-unfold="true"
            :disabled="disabled && !editValueOnly"
            :handle-change="(name, value) => handleParamChange(name, value, paramIndex)"
            name="value"
            :data-vv-scope="`param-${param.key}`"
            :placeholder="$t('editPage.paramValueTips')"
            :value="param.value"
        />
    </div>
</template>

<script>
    import atomFieldMixin from '../../atomFormField/atomFieldMixin'
    import Selector from '@/components/atomFormField/Selector'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'

    import {
        isTextareaParam,
        isStringParam,
        isBooleanParam,
        isEnumParam,
        isMultipleParam
    } from '@/store/modules/atom/paramsConfig'
    
    const BOOLEAN = [
        {
            value: true,
            label: true
        },
        {
            value: false,
            label: false
        }
    ]

    export default {
        name: 'define-param-show',
        components: {
            Selector,
            VuexInput,
            EnumInput,
            VuexTextarea
        },
        mixins: [atomFieldMixin],
        props: {
            param: {
                type: Object,
                default: () => ({})
            },
            paramIndex: {
                type: Number,
                default: 0
            },
            globalParams: {
                type: Array,
                default: () => []
            },
            // 只允许修改值，不允许增减项和修改key
            editValueOnly: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            boolList () {
                return BOOLEAN
            }
        },
        methods: {
            isBooleanParam,
            isStringParam,
            isTextareaParam,
            isMultipleParam,
            isSelectorParam (type) {
                return isMultipleParam(type) || isEnumParam(type)
            },

            transformOpt (opts) {
                const uniqueMap = {}
                opts = opts.filter(opt => opt.key.length)
                return Array.isArray(opts)
                    ? opts.filter(opt => {
                        if (!uniqueMap[opt.key]) {
                            uniqueMap[opt.key] = 1
                            return true
                        }
                        return false
                    }).map(opt => ({ id: opt.key, name: opt.value }))
                    : []
            },

            getSelectorDefaultVal ({ valueType, value = '' }) {
                if (isMultipleParam(valueType)) {
                    const isString = typeof value === 'string'
                    const isArray = Array.isArray(value)
                    return isString ? (value.split(',').filter(i => i.trim() !== '')) : (isArray ? value : [])
                }
                return value
            },

            handleParamChange (key, value, paramIndex) {
                this.$emit('handleParamChange', key, value, paramIndex)
            }
        }
    }
</script>
