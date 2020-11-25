<template>
    <bk-select
        v-model="selectedList"
        :placeholder="placeholder"
        :is-loading="isLoading"
        :disabled="disabled"
        :multiple="multiSelect"
        :searchable="searchable"
        :clearable="clearable"
        @selected="itemSelected"
        @toggle="visibleToggle">
        <bk-option v-for="(option, index) in list"
            :key="index"
            :id="option[settingKey]"
            :option="option"
            :name="option[displayKey]">
        </bk-option>
        <div slot="extension">
            <slot name="extension"></slot>
        </div>
    </bk-select>
</template>

<script>
    export default {
        name: 'vv-selector',
        props: {
            value: {
                type: [Array, String],
                default: null
            },
            list: {
                type: Array,
                default: []
            },
            isLoading: {
                type: Boolean,
                default: false
            },
            disabled: {
                type: Boolean,
                default: false
            },
            placeholder: {
                type: String
            },
            displayKey: {
                type: String,
                default: 'name'
            },
            settingKey: {
                type: String,
                default: 'id'
            },
            multiSelect: {
                type: Boolean,
                default: false
            },
            searchable: {
                type: Boolean,
                default: false
            },
            clearable: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                selectedList: this.value
            }
        },
        watch: {
            value (value) {
                this.selectedList = value
            }
        },
        methods: {
            visibleToggle (open) {
                this.$emit('visibleToggle', open)
            },
            itemSelected (value, item) {
                this.$emit('input', value)
                this.$emit('item-selected', value, item.$attrs.option)
            }
        }
    }
</script>
