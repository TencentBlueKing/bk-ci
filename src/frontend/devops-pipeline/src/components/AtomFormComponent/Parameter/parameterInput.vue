<template>
    <bk-input :clearable="!disabled" v-model="value" v-if="type === 'input'" :disabled="disabled"></bk-input>
    <bk-select v-model="value" searchable v-else :disabled="disabled" :multiple="multiple" :loading="loading" :clearable="!disabled">
        <bk-option v-for="(option, index) in paramList"
            :key="index"
            :id="option.id"
            :name="option.name">
        </bk-option>
    </bk-select>
</template>

<script>
    export default {
        props: {
            type: {
                type: String
            },
            value: {
                type: String
            },
            disabled: {
                type: Boolean,
                default: false
            },
            listType: {
                type: String
            },
            list: {
                type: Array,
                default: () => []
            },
            url: {
                type: Array,
                default: () => []
            },
            urlQuery: {
                type: String
            },
            multiple: {
                type: Boolean,
                default: false
            },
            paramValues: {
                type: Object,
                default: () => ({})
            }
        },

        data () {
            return {
                paramList: [],
                loading: false,
                queryKey: []
            }
        },

        watch: {
            value (newValue) {
                this.$emit('updateValue', newValue)
            },
            paramValues: {
                handler (value, oldValue) {
                    const index = this.queryKey.findIndex((key) => value[key] !== oldValue[key])
                    if (index > -1) {
                        this.$emit('updateValue', [])
                        this.initList()
                    }
                },
                deep: true
            }
        },

        created () {
            this.initList()
        },

        methods: {
            initList () {
                if (this.listType === 'list') {
                    this.paramList = this.list
                    return
                }

                let url = this.url || ''
                let isErrorParam = false
                url = url.replace(/{([^\{\}]+)}/g, (str, key) => {
                    const value = this.paramValues[key]
                    if (typeof value === 'undefined') isErrorParam = true
                    return value
                })

                const urlQuery = this.urlQuery || {}
                this.queryKey = []
                Object.keys(urlQuery).forEach((key, index) => {
                    this.queryKey.push(key)
                    const value = typeof this.paramValues[key] === 'undefined' ? urlQuery[key] : this.paramValues[key]
                    url += `${index <= 0 ? '?' : '&'}${key}=${value}`
                })

                if (isErrorParam) return
                this.loading = true
                this.$ajax.get(url).then((res) => {
                    this.paramList = res.data || []
                }).catch(e => this.$showTips({ message: e.message, theme: 'error' })).finally(() => (this.loading = false))
            }
        }
    }
</script>
