<template>
    <div class="conditional-input-selector">
        <bk-select
            v-model="value"
            ext-cls="group-box"
            :disabled="disabled"
            :clearable="clearable"
            :searchable="searchable"
            :placeholder="placeholder"
            @change="handleChangeValue"
        >
            <bk-option
                v-for="item in list"
                :key="item[settingKey]"
                :id="item[settingKey]"
                :name="item[displayKey]"
            >
                <slot
                    name="option-item"
                    v-bind="item"
                ></slot>
            </bk-option>
        </bk-select>
        <component
            class="input-selector"
            :is="curComponent.type"
            :name="curComponent.key"
            v-validate.initial="Object.assign({}, { required: !!curComponent.required })"
            :handle-change="handleChange"
            :value="element[curComponent.key]"
            :element="element"
            :disabled="disabled"
            v-bind="curComponent"
        />
    </div>
</template>

<script>
    import RequestSelector from '@/components/atomFormField/RequestSelector'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'conditional-input-selector',
        components: {
            VuexInput,
            RequestSelector
        },
        mixins: [atomFieldMixin],
        props: {
            value: String,
            element: Object,
            disabled: Boolean,
            placeholder: String,
            searchable: {
                type: Boolean,
                default: false
            },
            clearable: {
                type: Boolean,
                default: false
            },
            displayKey: {
                type: String,
                default: 'label'
            },
            settingKey: {
                type: String,
                default: 'value'
            },
            list: {
                type: Array,
                default: () => []
            }
        },
        computed: {
            curComponent () {
                return this.list.find(i => i.value === this.value) || {
                    type: 'request-selector',
                    key: 'repositoryHashId',
                    required: false
                }
            }
        },
        watch: {
            'element.scmCode' () {
                this.handleChange('repositoryHashId', '')
            }
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
    .conditional-input-selector {
        display: flex;
        .group-box {
            width: 110px;
            border-radius: 0px
        }
        .input-selector {
            flex: 1;
            border-left: none;
        }
    }
</style>
