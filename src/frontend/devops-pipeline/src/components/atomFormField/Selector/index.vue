<template>
    <bk-select
        :value="value"
        :loading="isLoading"
        :disabled="disabled"
        :searchable="searchable"
        :multiple="multiSelect"
        :clearable="clearable"
        @toggle="toggleVisible"
        :placeholder="placeholder"
        :search-key="displayKey"
        @change="onChange"
        :popover-options="popoverOptions"
        :enable-virtual-scroll="list.length > 3000"
        :list="list"
        :id-key="settingKey"
        :display-key="displayKey"
    >
        <bk-option
            v-for="item in list"
            :key="item[settingKey]"
            :id="item[settingKey]"
            :name="item[displayKey]"
            :disabled="item.disabled"
        >
        </bk-option>
        <div slot="extension">
            <slot></slot>
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
            }
        },
        computed: {
            popoverOptions () {
                return {
                    popperOptions: {
                        modifiers: {
                            preventOverflow: {
                                boundariesElement: 'window'
                            }
                        }
                    }
                }
            }
        },
        methods: {
            onChange (val, oldVal) {
                if (val !== oldVal) {
                    this.handleChange(this.name, val)
                }
            },
            editItem (index) {
                this.edit(index)
            }
        }
    }
</script>

<style lang="scss">
    @import "../../../scss/conf";
    .bkdevops-option-name {
        width: 100%;
        text-overflow: ellipsis;
        overflow: hidden;
        white-space: nowrap;
        &.selected {
            width: calc(100% - 24px)
        }
    }
    .bk-selector-create-item {
        a {
            display: block;
            color: $fontWeightColor;
        }

        &:hover {
            &, a {
                color: $primaryColor !important;
            }
        }
    }

</style>
