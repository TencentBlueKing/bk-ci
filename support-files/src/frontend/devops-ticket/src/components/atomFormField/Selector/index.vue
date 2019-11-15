<template>
    <bk-select
        :value="value"
        :loading="isLoading"
        :disabled="disabled"
        :searchable="searchable"
        :clearable="clearable"
        :multiple="multiSelect"
        :placeholder="placeholder"
        :readonly="readonly"
        @selected="onSelect"
        @toggle="toggle"
    >
        <bk-option v-for="(option, index) in list"
            :key="index"
            :id="option[settingKey]"
            :name="option[displayKey]"
        >
        </bk-option>
        <div slot="extension" v-if="hasAddItem" class="bk-selector-create-item">
            <a :href="itemUrl" target="_blank">
                <i class="bk-icon icon-plus-circle" />
                {{ itemText }}
            </a>
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
                default: false
            },
            clearable: {
                type: Boolean,
                default: false
            },
            readonly: {
                type: Boolean,
                default: false
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
            itemSelected: {
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
            hasAddItem: {
                type: Boolean,
                default: false
            },
            itemText: String,
            itemUrl: String,
            src: Object
        },
        methods: {
            onSelect (id, data) {
                this.itemSelected(id)
            },
            editItem (index) {
                this.edit(index)
            },
            toggle (val) {
                if (val) {
                    this.toggleVisible(val)
                }
            }
        }
    }
</script>

<style lang="scss">
    .bk-selector-create-item a {
        color: #737987;
        display: block;
    }
</style>
