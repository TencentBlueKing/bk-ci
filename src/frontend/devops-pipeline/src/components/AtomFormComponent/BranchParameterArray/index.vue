<template>
    <ul class="param-main">
        <li
            class="param-item"
            v-for="(item, index) in curValue"
            :key="index"
        >
            <select-input
                class="select-custom"
                :value="item"
                name
                :disabled="disabled || isLoading || (!['SELF', 'NAME'].includes(repositoryType) && !repoHashId)"
                type="text"
                :options="branchesList"
                :handle-change="(name, value) => handleChangeBranch(value, index)"
            >
            </select-input>
            <i
                class="bk-icon icon-plus-circle"
                @click="plusParam()"
            />
            <i
                :class="{
                    'bk-icon icon-minus-circle': true,
                    'disabled': curValue.length <= 1
                }"
                @click="minusParam(index)"
            />
        </li>
    </ul>
</template>

<script>
    import mixins from '../mixins'
    import SelectInput from '@/components/AtomFormComponent/SelectInput'
    import {
        PROCESS_API_URL_PREFIX
    } from '@/store/constants'
    export default {
        name: 'branch-parameter-array',
        components: {
            SelectInput
        },
        mixins: [mixins],
        props: {
            element: {
                type: Object
            }
        },
        data () {
            return {
                defaultValue: '',
                isLoading: false,
                branchesList: [],
                curValue: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            repoHashId () {
                return this.element?.repoHashId || ''
            },
            repositoryType () {
                return this.element?.repositoryType || ''
            }
        },
        watch: {
            /**
             * 切换项目
             * 1.清空已选分支
             * 2.获取分支列表
             */
            repoHashId: {
                handler (val, oldVal) {
                    if (val === oldVal) return
                    this.getBranchesList()
                    this.handleChange(this.name, [])
                    this.curValue = ['']
                },
                deep: true
            },

            repositoryType: {
                handler (val, oldVal) {
                    if (val === oldVal) return
                    this.handleChange(this.name, [])
                    this.curValue = ['']
                },
                deep: true
            }
        },
        created () {
            this.getBranchesList()
            if (this.value.length) {
                this.curValue = JSON.parse(JSON.stringify(this.value))
            } else {
                this.curValue = ['']
            }
        },
        methods: {
            handleChangeBranch (val, index) {
                this.$set(this.curValue, index, val)
                const params = this.curValue.filter(i => !!i)
                this.handleChange(this.name, params)
            },
            getBranchesList () {
                if (!this.repoHashId) return
                this.isLoading = true
                this.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/buildParam/${this.projectId}/${this.repoHashId}/gitRefs`).then(res => {
                    this.branchesList = res.data.map(i => {
                        return {
                            ...i,
                            id: i.value,
                            name: i.key
                        }
                    })
                }).finally(() => {
                    this.isLoading = false
                })
            },
            /**
             * 添加一行参数
             */
            plusParam () {
                this.curValue.push('')
                this.handleChange(this.name, this.curValue)
            },
            /**
             * 删除一行参数
             */
            minusParam (index) {
                if (this.curValue.length <= 1) return
                this.curValue.splice(index, 1)
                this.handleChange(this.name, this.curValue)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .param-main {
        .param-item {
            margin-bottom: 10px;
            display: flex;
            align-items: center;
            .select-custom {
                flex: 1;
                margin-right: 10px;
                &.last-child {
                    margin-right: 0;
                }
            }
        }
        .bk-icon {
            margin-left: 5px;
            font-size: 14px;
            cursor: pointer;
            &.disabled {
                cursor: not-allowed;
            }
        }
    }
</style>
