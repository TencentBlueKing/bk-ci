<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in fieldsMap">
            <form-field
                v-if="!obj.hidden && rely(obj, element)"
                :class="{ 'changed-prop': atomVersionChangedKeys.includes(key) }"
                :key="key"
                v-bind="obj"
                :is-error="errors.has(key)"
                :error-msg="errors.first(key)"
            >
                <component
                    :is="obj.component"
                    v-validate.initial="Object.assign({}, { crontabArrayRule: obj.required && obj.component === 'timer-cron-tab' })"
                    :name="key"
                    :value="element[key]"
                    :element="element"
                    :disabled="disabled || !checkCanOverride(obj)"
                    :handle-change="handleChange"
                    v-bind="obj"
                />
            </form-field>
        </template>
        <yaml-branch-field
            :element="element"
            :disabled="disabled"
            :errors="errors"
            :handle-update-element="handleUpdateElement"
        />
    </div>
</template>

<script>
    import TimerCronTab from '@/components/atomFormField/TimerCrontab/'
    import BranchParameterArray from '../../AtomFormComponent/BranchParameterArray/index'
    import validMixins from '../../validMixins'
    import atomMixin from '../atomMixin'
    import CodelibSelector from './CodelibSelector'
    import YamlBranchField from './YamlBranchField'
    import yamlBranchMixin from './yamlBranchMixin'
    export default {
        components: {
            TimerCronTab,
            BranchParameterArray,
            CodelibSelector,
            YamlBranchField
        },
        mixins: [atomMixin, validMixins, yamlBranchMixin],
        computed: {
            disabled () {
                // $props.disabled 为面板只读态（atomMixin 中声明的 prop，被本 computed 遮蔽）；element.disabled 为触发器停用开关
                return this.$props.disabled || this.element?.disabled === true
            },
            fieldsMap () {
                return Object.keys(this.atomPropsModel).reduce((acc, key) => {
                    // exclude ['agentType', 'agentHashIdList']
                    if (!['nodeType', 'nodes'].includes(key) && !(key === 'branches' && this.showYamlBranch)) {
                        acc[key] = this.atomPropsModel[key]
                    }
                    return acc
                }, {})
            }
        },
        watch: {
            element: {
                handler (newVal, oldVal) {
                    this.$nextTick(() => {
                        if (this.$validator) {
                            this.$validator.validateAll()
                        }
                    })
                },
                deep: true
            }
        },
        methods: {
            updateProps (newParam) {
                this.updateAtom({
                    element: this.element,
                    newParam
                })
            },
            handleChange (name, value) {
                this.updateProps({
                    [name]: value
                })
            }
        }
    }
</script>

<style lang="scss">

</style>
