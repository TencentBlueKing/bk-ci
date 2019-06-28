<template>
    <bk-select
        :value="value"
        :loading="isLoading"
        :disabled="disabled"
        :searchable="searchable"
        :multiple="multiSelect"
        :clearable="clearable"
        @selected="onSelect"
        @toggle="toggleVisible"
        :placeholder="placeholder"
        :search-key="displayKey"
        @edit="editItem"
    >
        <bk-option
            v-for="item in list"
            :key="item[settingKey]"
            :id="item[settingKey]"
            :name="item[displayKey]"
        >
        </bk-option>
        <div slot="extension">
            <slot name="props"></slot>
        </div>
    </bk-select>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'selector',
        mixins: [atomFieldMixin],
        props: {
            value: [String, Number, Array, Boolean],
            searchable: {
                type: Boolean,
                default: true
            },
            clearable: {
                type: Boolean,
                default: true
            },
            isLoading: {
                type: Boolean,
                default: false
            },
            hasCreateItem: {
                type: Boolean,
                default: false
            },
            toggleVisible: {
                type: Function,
                default: () => () => {}
            },
            list: {
                type: Array,
                default: []
            },
            multiSelect: {
                type: Boolean,
                default: false
            },
            placeholder: String,
            displayKey: {
                type: String,
                default: 'name'
            },
            settingKey: {
                type: String,
                default: 'id'
            },
            tools: {
                type: Boolean,
                default: false
            },
            edit: {
                type: Function,
                default: () => () => {}
            }
        },
        methods: {
            onSelect (id, data) {
                const { name } = this
                this.handleChange(name, id)
            },
            editItem (index) {
                this.edit(index)
            }
        }
    }
</script>
