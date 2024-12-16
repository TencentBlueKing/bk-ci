<template>
    <form-field
        v-if="paramVisible"
        :class="{ 'changed-prop': hasChanged }"
        :desc="param.desc"
        :desc-link="param.descLink"
        :desc-link-text="param.descLinkText"
        :required="param.required"
        :label="param.label"
        :is-error="errors.has(paramKey)"
        :error-msg="errors.first(paramKey)"
    >
        <component
            :is="param.type"
            :container="container"
            :atom-value="fullAtomValue"
            :disabled="disabled"
            :name="paramKey"
            v-validate.initial="paramRule"
            :handle-change="handleUpdateAtomInput"
            :value="fullAtomValue[paramKey]"
            v-bind="relatedProps"
            :get-atom-key-modal="getAtomKeyModal"
            :placeholder="paramPlaceholder"
        />
        <route-tips v-bind="paramRouteTips"></route-tips>
    </form-field>
</template>

<script>

    import DynamicParameter from '@/components/AtomFormComponent/DynamicParameter'
    import DynamicParameterSimple from '@/components/AtomFormComponent/DynamicParameterSimple'
    import Parameter from '@/components/AtomFormComponent/Parameter'
    import TimePicker from '@/components/AtomFormComponent/TimePicker'
    import Tips from '@/components/AtomFormComponent/Tips'
    import Accordion from '@/components/atomFormField/Accordion'
    import NameSpaceVar from '@/components/atomFormField/NameSpaceVar'
    import Selector from '../AtomFormComponent/Selector'
    import validMixins from '../validMixins'
    import AtomOutput from './AtomOutput'
    import atomMixin from './atomMixin'

    export default {
        name: 'atom-param-row',
        components: {
            Selector,
            Accordion,
            TimePicker,
            Parameter,
            Tips,
            NameSpaceVar,
            DynamicParameter,
            DynamicParameterSimple,
            AtomOutput
        },
        mixins: [atomMixin, validMixins],
        props: {
            element: {
                type: Object,
                required: true
            },
            paramKey: {
                type: String,
                required: true
            },
            param: {
                type: Object,
                required: true
            },
            fullAtomValue: {
                type: Object,
                required: true
            },
            getAtomKeyModal: {
                type: Function,
                required: true
            }
        },
        computed: {
            paramVisible () {
                return !this.isHidden(this.param, this.fullAtomValue) && this.rely(this.param, this.fullAtomValue)
            },
            paramRule () {
                const { type, rule, required } = this.param
                return Object.assign(
                    {},
                    { max: this.getMaxLengthByType(type) },
                    rule,
                    { required: !!required }
                )
            },
            hasChanged () {
                return this.atomVersionChangedKeys.includes(this.paramKey)
            },
            paramPlaceholder () {
                return this.getPlaceholder(this.param, this.fullAtomValue)
            },
            paramRouteTips () {
                return this.getComponentTips(this.param, this.fullAtomValue)
            },
            relatedProps () {
                return {
                    ...this.param,
                    ...Object.keys(this.param).reduce((acc, key) => {
                        const item = this.param[key]
                        if (typeof item === 'string') {
                            const res = item.match(/\{(\S+)\}/)
                            if (res !== null && this.fullAtomValue[res[1]]) {
                                acc[key] = this.fullAtomValue[res[1]]
                            }
                        }
                        return acc
                    }, {})
                }
            }
        }
    }
</script>

<style lang="scss">
</style>
