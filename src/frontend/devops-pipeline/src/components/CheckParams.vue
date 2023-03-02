<template>
    <div>
        <section
            class="check-params-item"
            v-for="(param, paramIndex) in params" :key="paramIndex"
            :is-error="!isMetadataVar && errors.any(`param-${paramIndex}`)"
        >
            <form-field class="form-field" :is-error="!isMetadataVar && errors.has(`param-${paramIndex}.key`)" :error-msg="errors.first(`param-${paramIndex}.key`)">
                <vuex-input
                    :data-vv-scope="`param-${paramIndex}`"
                    :disabled="true"
                    :desc-tooltips="param.desc"
                    :handle-change="(name, value) => handleParamChange(name, value, paramIndex)"
                    v-validate.initial="`required|unique:${params.map(p => p.key).join(',')}|max: 50|${snonVarRule}`"
                    name="key"
                    :placeholder="isMetadataVar ? $t('view.key') : 'Key'"
                    :value="param.chineseName ? param.chineseName : param.key" />
            </form-field>
            <span :class="{ 'default-required': true ,'is-required': param.required }" />
            <div :class="{ 'bk-form-item': true, 'required-error-item': param.required && !param.value.length && isShowReuired && !isBooleanParam(param.valueType) }">
                <!-- 自定义变量展示 -->
                <define-param-show
                    edit-value-only
                    :param="param"
                    :global-params="data"
                    :param-index="paramIndex"
                    @handleParamChange="handleParamChange"
                />
            </div>
            <i v-if="param.required && !param.value.length && isShowReuired && !isBooleanParam(param.valueType)" v-bk-tooltips="paramRequiredTips" class="bk-icon icon-exclamation-circle-shape top-middle is-required-icon" />
        </section>
    </div>
</template>

<script>
    import {
        isBooleanParam
    } from '@/store/modules/atom/paramsConfig'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import DefineParamShow from '@/components/AtomFormComponent/DefineParam/show.vue'

    export default {
        components: {
            VuexInput,
            FormField,
            DefineParamShow
        },
        props: {
            params: {
                type: Array,
                default: () => []
            },
            isSupportVar: {
                type: Boolean,
                default: false
            },
            isMetadataVar: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            snonVarRule () {
                return !this.isSupportVar ? 'nonVarRule' : ''
            }
        },
        methods: {
            isBooleanParam,
            handleParamChange (key, value, paramIndex) {
                this.$emit('handleParamChange', key, value, paramIndex)
            }
        }
    }
</script>

<style lang="scss">
    .check-params-item {
        display: flex;
        margin-bottom: 10px;
        .form-field {
            width: 286px;
            margin-right: 10px;
        }
        > .bk-form-item {
            width: 286px;
            height: 32px;
            margin-top: 0px !important;
        }
        .is-required-icon {
            color: red;
            position: relative;
            top: 10px;
            right: -6px;
        }
        .default-required {
            width: 8px;
            height: 8px;
        }
        .is-required:after {
            height: 8px;
            line-height: 1;
            content: "*";
            color: #ea3636;
            font-size: 12px;
            position: relative;
            left: -6px;
            top: 4px;
            display: inline-block;
        }
    }
    .required-error-item {
        .bk-select,
        .bk-form-input,
        .bk-form-textarea {
            border: 1px solid red;
        }
    }
</style>
