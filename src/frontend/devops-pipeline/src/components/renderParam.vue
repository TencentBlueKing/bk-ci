<template>
    <section class="render-param">
        <form-field
            v-bind="param"
            :is-error="errors.has('devops' + param.name)"
            :error-msg="errors.first('devops' + param.name)"
            :label="param.label || param.id"
            :show-operate-btn="showOperateBtn"
            :handle-use-default-value="() => handleUseDefaultValue(param.id)"
            :handle-set-parma-required="() => handleSetParmaRequired(param.id)"
            :handle-follow-template="() => handleFollowTemplate(param.id)"
        >
            <section class="component-row">
                <component
                    :is="param.component"
                    flex
                    click-unfold
                    show-select-all
                    v-bind="Object.assign({}, param, { id: undefined, name: 'devops' + param.id })"
                    :handle-change="handleParamUpdate"
                    :placeholder="param.placeholder"
                    :disabled="disabled || param.isDelete"
                    :random-sub-path="param.latestRandomStringInPath"
                    :enable-version-control="param.enableVersionControl"
                    :is-diff-param="highlightChangedParam && param.isChanged"
                    v-validate="{ required: param.required, objectRequired: isObject(param.value) }"
                    :class="{
                        'is-diff-param': (highlightChangedParam && param.isChanged) || param.affectedChanged,
                        'is-change-param': param.isChange,
                        'is-new-param': param.isNew,
                        'is-delete-param': param.isDelete
                    }"
                />
            </section>
            <span
                v-if="!errors.has('devops' + param.name)"
                :class="['preview-params-desc', param.type === 'TEXTAREA' ? 'params-desc-styles' : '']"
                :title="param.desc"
            >
                {{ param.desc }}
            </span>
            <span
                v-if="param.affectTips"
                class="preview-params-desc affect-warning"
            >
                {{ param.affectTips }}
            </span>
        </form-field>
    </section>
</template>
<script>
    import { isObject } from '@/utils/util'
    import CascadeRequestSelector from '@/components/atomFormField/CascadeRequestSelector'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import FileParamInput from '@/components/atomFormField/FileParamInput'
    import RequestSelector from '@/components/atomFormField/RequestSelector'
    import Selector from '@/components/atomFormField/Selector'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import metadataList from '@/components/common/metadata-list'
    export default {
        components: {
            Selector,
            RequestSelector,
            EnumInput,
            VuexInput,
            VuexTextarea,
            FormField,
            metadataList,
            FileParamInput,
            CascadeRequestSelector
        },
        props: {
            param: {
                type: Object,
                default: () => ({})
            },
            highlightChangedParam: Boolean,
            showOperateBtn: {
                type: Boolean,
                default: false
            },
            handleUseDefaultValue: {
                type: Function,
                default: () => () => {}
            },
            handleSetParmaRequired: {
                type: Function,
                default: () => () => {}
            },
            handleParamUpdate: {
                type: Function,
                default: () => () => {}
            },
            disabled: {
                type: Boolean,
                default: false
            },
            handleFollowTemplate: {
                type: Function,
                default: () => () => {}
            }
        },
        methods: {
            isObject
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/conf';
    @import '@/scss/mixins/ellipsis';
     
    .component-row {
        display: flex;
        position: relative;
        .metadata-box {
            position: relative;
            display: none;
        }

        .bk-select {
            &:not(.is-disabled) {
                background: white;
            }
            width: 100%;
        }
        .meta-data {
            align-self: center;
            margin-left: 10px;
            font-size: 12px;
            color: $primaryColor;
            white-space: nowrap;
            cursor: pointer;
        }
        .meta-data:hover {
            .metadata-box {
                display: block;
            }
        }
    }
    .preview-params-desc {
        color: #999;
        width: 100%;
        font-size: 12px;
        @include ellipsis();
         &.affect-warning {
            color: #FF9C01;
        }
    }
    .params-desc-styles {
        margin-top: 32px;
    }
    .is-diff-param {
        border-color: #FF9C01 !important;
    }
    .is-new-param {
        background: #EBFAF0 !important;
    }
        
    .is-change-param {
        background: #FDF4E8 !important;
    }
        
    .is-delete-param {
        background: #FFF0F0 !important;
    }
    .is-new-param,
    .is-delete-param,
    .is-change-param {
        &:focus {
            background: #FFF !important;
        }
    }
</style>
