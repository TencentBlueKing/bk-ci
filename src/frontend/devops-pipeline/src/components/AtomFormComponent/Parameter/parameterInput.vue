<template>
    <bk-input :clearable="!disabled" v-model="value" v-if="type === 'input'" :disabled="disabled"></bk-input>
    <section v-else class="parameter-select" v-bk-clickoutside="toggleShowList">
        <bk-input ref="inputItem"
            :clearable="!disabled"
            :value="Array.isArray(value) ? value.join(',') : value"
            :disabled="disabled"
            @change="inputManually"
            @focus="toggleShowList(true)">
        </bk-input>
        <ul v-if="showList && paramList.length" class="parameter-list">
            <li v-for="(option, index) in paramList"
                :key="index"
                @click="chooseOption(option)"
                :class="{ 'is-active': isActive(option.id) }"
            >{{option.name}}</li>
        </ul>
    </section>
</template>

<script>
    import mixins from '../mixins'
    export default {
        mixins: [mixins],
        props: {
            type: {
                type: String
            },
            value: {
                type: [String, Array]
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
                showList: false,
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
            chooseOption (option) {
                if (!this.multiple) {
                    this.$emit('updateValue', option.id)
                    this.$refs.inputItem.$refs.input.blur()
                } else {
                    if (Array.isArray(this.value)) {
                        const index = this.value.findIndex(x => x === option.id)
                        if (index > -1) this.value.splice(index, 1)
                        else this.value.push(option.id)
                    } else {
                        this.$emit('updateValue', [option.id])
                    }
                }
            },

            toggleShowList (value = false) {
                value = value === true ? value : false
                this.showList = value
            },

            inputManually (value) {
                if (this.multiple) {
                    if (value === '') value = []
                    else value = value.split(',')
                }
                this.$emit('updateValue', value)
            },

            isActive (id) {
                if (Array.isArray(this.value)) {
                    return this.value.includes(id)
                } else {
                    return this.value === id
                }
            },

            initList () {
                if (this.listType === 'list') {
                    this.paramList = this.list
                    return
                }

                if (typeof this.url === 'string' && this.url !== '') { // 只有存在url字段时才去请求
                    // let url = this.url
                    // let isErrorParam = false
                    // url = url.replace(/{([^\{\}]+)}/g, (str, key) => {
                    //     const value = this.paramValues[key]
                    //     if (typeof value === 'undefined') isErrorParam = true
                    //     return value
                    // })

                    let [url] = this.generateReqUrl(this.url, this.paramValues)
                    if (!url) return

                    const urlQuery = this.urlQuery || {}
                    this.queryKey = []
                    Object.keys(urlQuery).forEach((key, index) => {
                        this.queryKey.push(key)
                        const value = typeof this.paramValues[key] === 'undefined' ? urlQuery[key] : this.paramValues[key]
                        url += `${index <= 0 ? '?' : '&'}${key}=${value}`
                    })

                    this.loading = true
                    this.$ajax.get(url).then((res) => {
                        this.paramList = res.data || []
                    }).catch(e => this.$showTips({ message: e.message, theme: 'error' })).finally(() => (this.loading = false))
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .parameter-select {
        position: relative;
        .parameter-list {
            position: absolute;
            top: 32px;
            left: 0;
            right: 0;
            padding: 6px 0;
            list-style: none;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            line-height: 32px;
            background: #fff;
            color: #63656e;
            overflow: auto;
            max-height: 216px;
            z-index: 2;
            li {
                padding: 0 16px;
                position: relative;
                &:hover {
                    color: #3a84ff;
                    background-color: #eaf3ff;
                }
            }
            .is-active {
                color: #3a84ff;
                background-color: #f4f6fa;
                &:after {
                    content: '';
                    position: absolute;
                    right: 16px;
                    top: 11px;
                    height: 7px;
                    width: 3px;
                    transform: rotate(45deg);
                    border-bottom: 1px solid #3a84ff;
                    border-right: 1px solid #3a84ff;
                }
            }
        }
    }
</style>
