<template>
    <div class="cascade-request-selector">
        <bk-select
            :class="{
                'is-diff-param': isDiffParam
            }"
            @toggle="toggleVisible"
            @selected="onChange"
            v-bind="selectProps"
        >
            <bk-option
                v-for="item in listData"
                :key="item[settingKey]"
                :id="item[settingKey]"
                :name="item[displayKey]"
                :disabled="item.disabled"
            >
                <slot
                    name="option-item"
                    v-bind="item"
                ></slot>
            </bk-option>
            <div slot="extension">
                <slot></slot>
            </div>
        </bk-select>
        <!-- <div>{{ value[parentKey] }}</div>  -->
        <RequestSelector
            :class="{
                'is-diff-param': isDiffParam
            }"
            v-bind="Object.assign(
                {},
                {
                    ...childrenOptions,
                    id: undefined,
                    key: value[parentKey],
                    paramId: 'key',
                    paramValue: 'value',
                    initRequest: initRequest,
                    options: cascadeProps.children.options,
                    name: childrenKey,
                    value: value[childrenKey],
                    searchUrl: childrenSearchUrl,
                    replaceKey: cascadeProps.children.replaceKey,
                    disabled: disabled,
                    placeholder: placeholder,
                    handleChange: (name, value) => handleUpdateChildrenValue(name, value)
                }
            )"
        />
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import RequestSelector from '@/components/atomFormField/RequestSelector'
    export default {
        name: 'selector',
        components: {
            RequestSelector
        },
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
            zIndex: {
                type: Number,
                default: 2500
            },
            isLoading: {
                type: Boolean,
                default: false
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
            dataPath: String,
            cascadeProps: {
                required: true,
                type: Object
            },
            childrenOptions: {
                type: Object,
                default: () => {}
            },
            isDiffParam: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                listData: [],
                initRequest: false
            }
        },
        computed: {
            parentKey () {
                return this.cascadeProps.id || ''
            },
            childrenKey () {
                return this.cascadeProps.children.id || ''
            },
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
                    value: this.value[this.parentKey],
                    loading: this.isLoading,
                    disabled: this.disabled || this.readOnly,
                    searchable: this.searchable,
                    multiple: this.multiSelect,
                    clearable: this.clearable,
                    placeholder: this.placeholder,
                    zIndex: this.zIndex,
                    'search-key': this.displayKey,
                    'popover-options': this.popoverOptions,
                    'enable-virtual-scroll': this.list.length > 3000,
                    list: this.listData,
                    'id-key': this.settingKey,
                    'display-key': this.displayKey,
                    'show-select-all': this.showSelectAll
                }
                if (this.cascadeProps.searchUrl) props['remote-method'] = this.remoteMethod
                return props
            },

            childrenSearchUrl () {
                return this.cascadeProps?.children?.searchUrl?.replace('{parentValue}', this.value[this.parentKey])
            }
        },
        watch: {
            'cascadeProps.options': {
                handler (list) {
                    this.listData = list
                },
                immediate: true,
                deep: true
            },
            isDiffParam (val) {
                this.toggleVisible(val)
                this.initRequest = true
            }
        },
        methods: {
            onChange (val, oldVal) {
                if (val !== oldVal) {
                    this.initRequest = true
                    this.handleChange(this.name, {
                        [this.parentKey]: val,
                        [this.childrenKey]: ''
                    })
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
                            const regExp = new RegExp(this.cascadeProps.replaceKey, 'g')
                            const url = this.cascadeProps.searchUrl.replace(regExp, name)
                            const data = await this.$ajax.get(url)
                            this.listData = this.getResponseData(data)
                            resolve()
                        } catch (error) {
                            console.error(error)
                            reject(error)
                        }
                    }, 500)
                })
            },

            async toggleVisible (value) {
                if (!value) return
                const regExp = new RegExp(this.cascadeProps.replaceKey, 'g')
                const url = this.cascadeProps.searchUrl.replace(regExp, '')
                const data = await this.$ajax.get(url)
                this.listData = this.getResponseData(data)
            },
            
            handleUpdateChildrenValue (name, value) {
                this.handleChange(this.name, {
                    ...this.value,
                    [this.childrenKey]: value
                })
            }
        }
    }
</script>

<style lang="scss">
    @import "../../../scss/conf";
    .cascade-request-selector {
        display: flex;
        width: 100%;
        :first-child {
            border-right: none;
        }
        .bk-select {
            flex: 1;
        }
    }
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
    .is-diff-param {
        border-color: #FF9C01 !important;
    }
</style>
