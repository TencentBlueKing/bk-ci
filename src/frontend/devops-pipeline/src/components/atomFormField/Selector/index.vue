<template>
    <bk-select @toggle="toggleVisible" @change="onChange" v-bind="selectProps">
        <bk-option
            v-for="item in listData"
            :key="item[settingKey]"
            :id="item[settingKey]"
            :name="item[displayKey]"
            :disabled="item.disabled"
        >
            <slot name="option-item" v-bind="item"></slot>
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
            showSelectAll: {
                type: Boolean,
                default: false
            },
            searchUrl: String,
            replaceKey: String,
            dataPath: String
        },
        data () {
            return {
                listData: []
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
                    list: this.listData,
                    'id-key': this.settingKey,
                    'display-key': this.displayKey,
                    'show-select-all': this.showSelectAll
                }
                if (this.searchUrl) props['remote-method'] = this.remoteMethod
                return props
            }
        },
        watch: {
            list: {
                handler (list) {
                    this.listData = list
                },
                immediate: true
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
            remoteMethod (name) {
                return new Promise((resolve, reject) => {
                    clearTimeout(this.remoteMethod.timeId)
                    this.remoteMethod.timeId = setTimeout(async () => {
                        try {
                            const regExp = new RegExp(this.replaceKey, 'g')
                            const url = this.searchUrl.replace(regExp, name)
                            const data = await this.$ajax.get(url)
                            this.listData = this.getResponseData(data)
                            resolve()
                        } catch (error) {
                            console.error(error)
                            reject(error)
                        }
                    }, 500)
                })
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
