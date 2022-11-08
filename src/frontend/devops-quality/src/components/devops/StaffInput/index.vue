<template>
    <custom-selector name="value" type="text"
        :value="curInsertVal"
        :disabled="disabled"
        :placeholder="placeholder"
        :config="dataInputConfig"
        :selected-list="value"
        :set-vaule="setVaule"
        :delete-item="deleteItem" />
</template>

<script>
    // import atomFieldMixin from '../atomFieldMixin'
    import customSelector from '@/components/common/custom-selector'
    import Vue from 'vue'
    // import ajax from '@/utils/ajax'

    const vue = new Vue()

    export default {
        // mixins: [ atomFieldMixin ],
        name: 'staff-input',
        components: {
            customSelector
        },
        props: {
            name: {
                type: String,
                default: ''
            },
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
            handleChange: {
                type: Function,
                default: () => () => {}
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
        async created () {
            await this.getList()
        },
        methods: {
            async getList () {
                const url = `/project/api/user/users/projects/${this.projectId}/list`

                try {
                    const res = await vue.$ajax.get(`${url}`).then(data => {
                        return data
                    })

                    res.forEach(item => {
                        this.initData.push(item)
                        this.list.push(item)
                    })

                    this.$nextTick(() => {
                        this.value.forEach(item => {
                            this.list = this.list.filter(val => val.indexOf(item) === -1)
                        })
                    })
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            onChange (name, value) {
                this.handleValueChange('value', value)
            },
            handleValueChange (name, value) {
                this.curInsertVal = value
            },
            setVaule (name, index, item, type) {
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
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../scss/conf';

</style>
