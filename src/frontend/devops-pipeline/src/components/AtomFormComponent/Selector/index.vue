<template>
    <bk-select v-bind="dropdownConf"
        :name="name"
        :loading="isLoading"
        :placeholder="isLoading ? &quot;获取数据中...&quot; : placeholder"
        :value="value"
        :disabled="disabled || isLoading"
        @edit="edit"
        @selected="handleSelect"
        @toggle="toggleVisible"
        @clear="handleClear"
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
                    <i class="bk-icon icon-plus-circle" />
                    {{ mergedOptionsConf.itemText }}
                </a>
            </div>
        </template>
    </bk-select>
</template>

<script>
    import mixins from '../mixins'
    export default {
        mixins: [mixins],
        props: {
            placeholder: {
                type: String,
                default: '请选择'
            },
            options: {
                type: Array,
                default: []
            },
            optionsConf: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                isLoading: false,
                list: this.options,
                webUrl: WEB_URL_PIRFIX
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            mergedOptionsConf () {
                return Object.assign({}, {
                    hasAddItem: false,
                    itemText: '关联代码库',
                    itemTargetUrl: `/codelib/{projectId}/`,
                    url: '',
                    paramId: 'id',
                    paramName: 'name',
                    tools: { 'edit': false, 'del': false },
                    searchable: false,
                    clearable: false,
                    multiple: false
                }, this.optionsConf)
            },
            hasUrl () {
                return this.mergedOptionsConf && this.mergedOptionsConf.url && typeof this.mergedOptionsConf.url === 'string'
            },
            dropdownConf () {
                const { searchable, tools, multiple, clearable } = this.mergedOptionsConf
                return {
                    tools,
                    searchable,
                    multiple,
                    clearable
                }
            },
            addItemUrl () {
                const { webUrl, urlParse, mergedOptionsConf: { itemTargetUrl }, $route: { params } } = this
                const originUrl = /^http:\/\//.test(itemTargetUrl) ? itemTargetUrl : webUrl + itemTargetUrl

                return urlParse(originUrl, params)
            }
        },
        watch: {
            atomValue: {
                deep: true,
                handler: function (params) {
                    // this.freshList()
                }
            },
            options (newOptions) {
                this.list = newOptions
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
            edit (index) {
                const hashId = this.list[index].repositoryHashId || ''
                const type = this.list[index].type || ''
                const groupId = this.list[index].groupHashId || ''
                if (hashId) {
                    const href = `${WEB_URL_PIRFIX}/codelib/${this.projectId}/${hashId}/${type}`
                    window.open(href, '_blank')
                } else if (groupId) {
                    const groupHref = `${WEB_URL_PIRFIX}/experience/${this.projectId}/setting/?groupId=${groupId}`
                    window.open(groupHref, '_blank')
                }
            },
            toggleVisible (open) {
                open && this.hasUrl && this.freshList()
            },
            urlParse (originUrl, query) {
                /* eslint-disable */
                return new Function('ctx', `return '${originUrl.replace(/\{(.*?)\}/g, '\'\+ ctx.$1 \+\'')}'`)(query)
                /* eslint-enable */
            },
            getUrlParamKey (url) {
                if (this.hasUrl) {
                    const paramKey = url.match(/\{(.*?)\}/g)
                    return paramKey || []
                }
                return []
            },
            transformList (res) {
                // 正常情况
                return (res.data.records || res.data || []).map(item => {
                    if (typeof item === 'string') {
                        return {
                            id: item,
                            name: item
                        }
                    }
                    return {
                        ...item,
                        id: item[this.mergedOptionsConf.paramId],
                        name: item[this.mergedOptionsConf.paramName]
                    }
                })
            },
            async freshList () {
                try {
                    const { atomValue = {}, transformList, $route: { params = {} }, mergedOptionsConf } = this
                    const changeUrl = this.urlParse(mergedOptionsConf.url, {
                        ...params,
                        ...atomValue
                    })
                    this.isLoading = true

                    const res = await this.$ajax.get(changeUrl)

                    this.list = transformList(res)
                    // 添加无权限查看项
                    const valueArray = mergedOptionsConf.multiple && Array.isArray(this.value) ? this.value : [this.value]
                    const listMap = this.list.reduce((listMap, item) => {
                        listMap[item.id] = item
                        return listMap
                    }, {})

                    valueArray.map(value => {
                        if (typeof value !== 'undefined' && value !== '' && !listMap[value]) {
                            this.list.splice(0, 0, {
                                id: value,
                                name: '******（无权限查看）'
                            })
                        }
                    })

                    this.$emit('change', this.list)
                } catch (e) {
                    this.$showTips({
                        message: e.message,
                        theme: 'error'
                    })
                } finally {
                    this.isLoading = false
                }
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
