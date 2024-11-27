<template>
    <bk-tag-input
        v-model="tagValue"
        allow-create
        clearable
        :placeholder="placeholder"
        :search-key="['id', 'name']"
        separator=","
        :disabled="disabled"
        :create-tag-validator="checkVariable"
        :paste-fn="paste"
        :list="list"
    >
    </bk-tag-input>
</template>

<script>
    import atomFieldMixin from './atomFieldMixin'

    export default {
        name: 'staff-input',
        mixins: [atomFieldMixin],
        props: {
            value: {
                type: Array,
                required: true,
                default: () => []
            },
            placeholder: {
                type: String,
                default: ''
            },
            listUrl: {
                type: String,
                default: ''
            },
            disabled: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                list: []
            }
        },
        computed: {
            tagValue: {
                get () {
                    return this.value
                },
                set (value) {
                    this.handleChange(this.name, value)
                }
            }
        },
        created () {
            // this.getList()
        },
        methods: {
            getList () {
                // 默认是拥有流水线权限的人员
                const url = this.listUrl || `/project/api/user/users/projectUser/${this.$route.params.projectId}/${this.$route.params.pipelineId}/map`
                this.$ajax.get(`${url}`).then(res => {
                    this.list = res.data
                })
            },
            // 检验变量
            checkVariable (val) {
                return /^\$\{(.*)\}$/.test(val)
            },
            paste (val) {
                const newArr = val.split(',').filter(v => !this.tagValue.find(w => w === v))
                this.tagValue = [...this.tagValue, ...newArr]
                return []
            }
        }
    }
</script>
