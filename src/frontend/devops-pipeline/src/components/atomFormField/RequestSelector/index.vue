<template>
    <selector :popover-min-width="popoverMinWidth"
        :tools="tools"
        :name="name"
        :edit="edit"
        :placeholder="isLoading ? $t('editPage.loadingData') : placeholder"
        :handle-change="handleChange"
        :list="list"
        :toggle-visible="toggleVisible"
        :is-loading="isLoading"
        :value="value"
        :searchable="searchable"
        :multi-select="multiSelect"
        :disabled="disabled || isLoading"
        :search-url="searchUrl"
        :replace-key="replaceKey"
        :data-path="dataPath"
    >
        <template v-if="hasAddItem">
            <div class="bk-selector-create-item">
                <a :href="urlParse(webUrl + itemTargetUrl, { projectId })" target="_blank">
                    <i class="devops-icon icon-plus-circle" />
                    {{ itemText }}
                </a>
            </div>
        </template>
    </selector>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import Selector from '../Selector'
    export default {
        name: 'request-selector',
        components: {
            Selector
        },
        mixins: [atomFieldMixin],
        props: {
            hasAddItem: {
                type: Boolean,
                default: false
            },
            itemText: {
                type: String,
                default: '关联代码库'
            },
            itemTargetUrl: {
                type: String,
                default: '/codelib/{projectId}/'
            },
            searchable: {
                type: Boolean,
                default: true
            },
            url: {
                type: String,
                default: ''
            },
            paramId: {
                type: String,
                default: 'id'
            },
            paramName: {
                type: String,
                default: 'name'
            },
            tools: {
                type: Boolean,
                default: false
            },
            placeholder: {
                type: String
            },
            multiSelect: {
                type: Boolean,
                default: false
            },
            popoverMinWidth: {
                type: Number
            },
            searchUrl: String,
            replaceKey: String,
            dataPath: String
        },
        data () {
            return {
                isLoading: false,
                list: [],
                webUrl: WEB_URL_PREFIX
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        created () {
            this.freshList()
        },
        methods: {
            edit (index) {
                const hashId = this.list[index].repositoryHashId || ''
                const type = this.list[index].type || ''
                const groupId = this.list[index].groupHashId || ''
                if (hashId) {
                    const href = `${WEB_URL_PREFIX}/codelib/${this.projectId}/${hashId}/${type}`
                    window.open(href, '_blank')
                } else if (groupId) {
                    const groupHref = `${WEB_URL_PREFIX}/experience/${this.projectId}/setting/?groupId=${groupId}`
                    window.open(groupHref, '_blank')
                }
            },
            toggleVisible (open) {
                open && this.freshList()
            },
            async freshList () {
                try {
                    const { url, element } = this
                    const query = this.$route.params
                    const changeUrl = this.urlParse(url, {
                        bkPoolType: this?.container?.dispatchType?.buildType,
                        ...query,
                        ...element
                    })
                    this.isLoading = true
                    const res = await this.$ajax.get(changeUrl)

                    // 正常情况
                    this.list = (res.data.resources || res.data.records || res.data || []).map(item => ({
                        ...item,
                        id: item[this.paramId],
                        name: item[this.paramName]
                    }))

                    // 单选selector时处理******
                    if (!this.multiSelect) {
                        if (this.value !== '' && this.list.filter(item => item.id === this.value).length === 0) {
                            this.list.splice(0, 0, {
                                id: this.value,
                                name: `******（${this.$t('editPage.noPermToView')}）`
                            })
                        }
                    } else {
                        // 多选selector时处理******,现在的处理方式是，把多选的数组遍历，看里面的每一项是否在list，若不在则加一项***
                        this.value = this.value.length ? this.value : []
                        this.value.forEach(value => {
                            if (value !== '' && this.list.filter(item => item.id === value).length === 0) {
                                this.list.splice(0, 0, {
                                    id: value,
                                    name: `******（${this.$t('editPage.noPermToView')}）`
                                })
                            }
                        })
                    }
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
