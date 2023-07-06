<template>
    <bk-select v-bind="dropdownConf"
        :name="name"
        :loading="isLoading"
        :value="value"
        :disabled="disabled || isLoading"
        @selected="handleSelect"
        @toggle="toggleVisible"
        @clear="handleClear"
        :popover-options="popoverOptions"
        :enable-virtual-scroll="list.length > 3000"
        :list="list"
    >
        <bk-option
            v-for="item in list"
            :key="item.id"
            :id="item.id"
            :name="item.name"
        >
        </bk-option>
        <template v-if="mergedOptionsConf.hasAddItem">
            <div slot="extension" class="bk-selector-create-item">
                <a :href="addItemUrl" target="_blank">
                    <i class="devops-icon icon-plus-circle" />
                    {{ mergedOptionsConf.itemText }}
                </a>
            </div>
        </template>
    </bk-select>
</template>

<script>
    import mixins from '../mixins'
    import selectorMixins from '../selectorMixins'
    export default {
        mixins: [mixins, selectorMixins],
        props: {
            placeholder: {
                type: String
            }
        },
        data () {
            return {
                isLoading: false,
                list: this.options
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
            dropdownConf () {
                const { searchable, multiple, clearable, placeholder, searchPlaceholder } = this.mergedOptionsConf
                return {
                    searchable,
                    multiple,
                    clearable,
                    placeholder: this.isLoading ? this.$t('editPage.loadingData') : placeholder,
                    searchPlaceholder: searchPlaceholder ?? placeholder
                }
            }
        },
        watch: {
            options (newOptions) {
                this.list = newOptions
            },
            queryParams (newQueryParams, oldQueryParams) {
                if (this.isParamsChanged(newQueryParams, oldQueryParams)) {
                    this.handleClear()
                }
            }
        },
        created () {
            if (this.hasUrl) {
                this.freshList()
            }
        },
        methods: {
            handleSelect (selected, data) {
                this.handleChange(this.name, selected)
            },
            handleClear () {
                const val = this.dropdownConf.multiple ? [] : ''

                this.handleChange(this.name, val)
            },
            toggleVisible (open) {
                open && this.hasUrl && this.freshList()
            },
            transformList (res) {
                const list = this.getResponseData(res, this.mergedOptionsConf.dataPath)
                return list.map(item => {
                    let curItem = {}
                    if (typeof item === 'string') {
                        curItem = {
                            id: item,
                            name: item
                        }
                    }
                    curItem = {
                        ...item,
                        id: item[this.mergedOptionsConf.paramId],
                        name: item[this.mergedOptionsConf.paramName]
                    }
                    return curItem
                })
            },
            freshList () {
                if (this.isLackParam) { // 缺少参数时，选择列表置空
                    this.list = []
                    return
                }
                const { atomValue = {}, transformList, $route: { params = {} }, mergedOptionsConf } = this
                const changeUrl = this.urlParse(mergedOptionsConf.url, {
                    bkPoolType: this?.container?.dispatchType?.buildType,
                    ...params,
                    ...atomValue
                })
                this.isLoading = true
                this.$ajax.get(changeUrl).then((res) => {
                    this.list = transformList(res)
                    // 添加无权限查看项
                    const valueArray = mergedOptionsConf.multiple && Array.isArray(this.value) ? this.value : [this.value]
                    const listMap = this.list.reduce((listMap, item) => {
                        listMap[item.id] = item
                        return listMap
                    }, {})

                    valueArray.forEach(value => {
                        if (typeof value !== 'undefined' && value !== '' && !listMap[value]) {
                            this.list.splice(0, 0, {
                                id: value,
                                name: `******（${this.$t('editPage.noPermToView')}）`
                            })
                        }
                    })

                    this.$emit('change', this.list)
                }).catch((e) => {
                    console.log(e)
                    this.$showTips({
                        message: e.message,
                        theme: 'error'
                    })
                }).finally(() => {
                    this.isLoading = false
                })
            }
        }
    }
</script>

<style lang="scss">
    .bk-form .bk-form-content,
    .form-field.bk-form-item {
        position: static;
    }
</style>
