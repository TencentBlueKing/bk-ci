<template>
    <bk-select @toggle="toggleVisible" @change="onChange" v-bind="selectProps">
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
            },
            searchUrl: String,
            replaceKey: String
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
            },

            selectProps () {
                const props = {
                    value: this.value,
                    loading: this.isLoading,
                    disabled: this.disabled,
                    searchable: this.searchable,
                    multiple: this.multiSelect,
                    clearable: this.clearable,
                    placeholder: this.placeholder,
                    'search-key': this.displayKey,
                    'popover-options': this.popoverOptions,
                    'enable-virtual-scroll': this.list.length > 3000,
                    list: this.list,
                    'id-key': this.settingKey,
                    'display-key': this.displayKey
                }
                if (this.searchUrl) props['remote-method'] = this.remoteMethod
                return props
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
            },
            async remoteMethod (name) {
                try {
                    const regExp = new RegExp(this.replaceKey, 'g')
                    const url = this.searchUrl.replace(regExp, name)
                    const data = this.$http.get(url)
                    return data
                } catch (error) {
                    console.error(error)
                }
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
