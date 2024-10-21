<template>
    <div class="staff-input">
        <div
            v-if="prependText"
            class="group-text prepend-box"
        >
            {{ prependText }}
        </div>
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
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'

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
            },
            prependText: {
                type: String,
                default: ''
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
            this.getList()
        },
        methods: {
            getList () {
                // 默认是拥有流水线权限的人员
                const url = this.listUrl || `/project/api/user/users/projectUser/${this.$route.params.projectId}/${this.$route.params.pipelineId}/map`
                this.$ajax.get(`${url}`).then(res => {
                    this.list = res.data
                }).catch(e => {
                    console.log(e)
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

<style lang="scss">
    .staff-input {
        display: flex;
        .prepend-box {
            border: 1px solid #c4c6cc;
            border-right: none;
            padding: 0 15px;
        }
        .bk-tag-selector {
            flex: 1;
        }
    }
</style>
