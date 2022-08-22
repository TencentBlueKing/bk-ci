<template>
    <custom-selector name="value" type="text"
        :value="curInsertVal"
        :disabled="disabled"
        :placeholder="placeholder"
        :config="dataInputConfig"
        :selected-list="value"
        :set-value="setValue"
        :delete-item="deleteItem"
    />
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import customSelector from '@/components/common/custom-selector'

    export default {
        name: 'company-staff-input',
        components: {
            customSelector
        },
        mixins: [atomFieldMixin],
        props: {
            value: {
                type: Array,
                required: true,
                default: () => []
            },
            disabled: {
                type: Boolean,
                default: false
            },
            placeholder: {
                type: String,
                default: ''
            },
            inputType: {
                type: String,
                default: 'rtx'
            }
        },

        data () {
            return {
                isLoading: false,
                curInsertVal: '',
                list: [],
                initData: []
            }
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            dataInputConfig () {
                return {
                    initData: this.initData || [],
                    data: this.list || [],
                    onChange: this.onChange
                }
            }
        },

        watch: {
            value (value) { // 选中列表发生变化时更新list数据
                this.list = this.initData.filter(val => !value.includes(val))
            }
        },

        created () {
            this.requestList()
        },

        methods: {
            onChange (name, value) {
                this.handleValueChange('value', value)
            },

            requestList () {
                const self = this
                const cookie = document.cookie.match(/bk_uid=\S*/)
                if (!cookie) {
                    console.error('人员选择器，由于cookie无法拉取人员信息')
                    return false
                }

                const host = location.host
                const prefix = `${host.indexOf('o.ied.com') > -1 ? OIED_URL : OPEN_URL}/component/compapi/tof3`

                const config = {
                    url: '',
                    data: {}
                }

                switch (this.inputType) {
                    case 'rtx':
                        config.url = `${prefix}/get_all_staff_info/`
                        config.data = {
                            query_type: 'simple_data',
                            app_code: 'workbench'
                        }
                        break
                    case 'email':
                        config.url = `${prefix}/get_all_ad_groups/`
                        config.data.query_type = undefined
                        config.data = {
                            app_code: 'workbench'
                        }
                        break
                    case 'all':
                        config.url = `${prefix}/get_all_rtx_and_mail_group/`
                        config.data = {
                            app_code: 'workbench'
                        }
                        break
                    default:
                        break
                }

                this.ajaxRequest({
                    url: config.url,
                    jsonp: 'callback' + this.uuid(),
                    data: config.data,
                    success: function (res) {
                        if (res.result) {
                            const key = self.inputType === 'email' ? 'Name' : 'english_name'
                            self.initData = self.list = res.data.map(val => val[key])
                            let valueMap = {}
                            if (Array.isArray(self.value)) {
                                valueMap = self.arrayToHashMap(self.value)
                            } else if (typeof self.value === 'string') {
                                valueMap = self.arrayToHashMap(self.value.split(';'))
                            }
                            self.list = self.list.filter(val => valueMap[val.toLowerCase()] !== 1)
                        } else {
                            console.error(res.message)
                        }
                    },
                    error: function (error) {
                        console.error(error)
                    }
                })
            },
            arrayToHashMap (arr) {
                return arr.reduce((acc, item) => {
                    acc[item.toLowerCase()] = 1
                    return acc
                }, {})
            },
            ajaxRequest (params) {
                params = params || {}
                params.data = params.data || {}

                const callbackName = params.jsonp
                const head = document.getElementsByTagName('head')[0]
                params.data.callback = callbackName

                const data = this.formatParams(params.data)
                const script = document.createElement('script')
                head.appendChild(script)

                window[callbackName] = function (res) {
                    head.removeChild(script)
                    clearTimeout(script.timer)
                    window[callbackName] = null
                    params.success && params.success(res)
                }

                script.src = params.url + '?' + data
            },

            // 格式化参数
            formatParams (data) {
                const arr = []
                for (const name in data) {
                    arr.push(encodeURIComponent(name) + '=' + encodeURIComponent(data[name]))
                }
                return arr.join('&')
            },

            handleValueChange (name, value) {
                this.curInsertVal = value
            },

            setValue (name, index, item, type) {
                let newVal = []
                if (type) {
                    newVal = [...this.value.slice(0, index), ...item, ...this.value.slice(index, this.value.length)]
                } else {
                    newVal = [...this.value.slice(0, index), item, ...this.value.slice(index, this.value.length)]
                }
                this.handleChange(this.name, newVal)
                this.curInsertVal = ''
            },

            deleteItem (name, index, item) {
                if (!item.isBkVar()) this.list.push(item)
                const updateVal = [...this.value.slice(0, index - 1), ...this.value.slice(index, this.value.length)]
                this.handleChange(this.name, updateVal)
            },

            uuid () {
                let id = ''
                const randomNum = Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1)

                for (let i = 0; i < 7; i++) {
                    id += randomNum
                }
                return id
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../../scss/conf';
</style>
