<template>
    <ul class="param-main">
        <li
            class="param-item"
            v-for="(item, index) in curValue"
            :key="index"
        >
            <bk-select
                :disabled="disabled || !repoHashId"
                :placeholder="placeholder"
                :loading="isLoading"
                :value="item"
                @change="(item) => handleChangeBranch(item, index)"
                ext-cls="select-custom"
                searchable>
                <bk-option
                    v-for="option in branchesList"
                    :key="option.key"
                    :disabled="curValue.includes(option.key)"
                    :id="option.key"
                    :name="option.value">
                </bk-option>
            </bk-select>
            <i
                class="bk-icon icon-plus-circle"
                @click="plusParam()" />
            <i
                :class="{
                    'bk-icon icon-minus-circle': true,
                    'disabled': curValue.length <= 1
                }"
                @click="minusParam(index)" />
        </li>
    </ul>
</template>

<script>
    import mixins from '../mixins'
    import {
        PROCESS_API_URL_PREFIX
    } from '@/store/constants'
    export default {
        name: 'branch-parameter-array',
        mixins: [mixins],
        props: {
            repoHashId: {
                type: String
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
            }
        },
        watch: {
            /**
             * 切换项目
             * 1.清空已选分支
             * 2.获取分支列表
             */
            repoHashId: {
                handler (val) {
                    this.handleChange(this.name, [])
                    this.curValue = ['']
                    if (val) {
                        this.getBranchesList()
                    }
                }
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
                this.curValue[index] = val
                const params = this.curValue.filter(i => !!i)
                this.handleChange(this.name, params)
            },
            getBranchesList () {
                if (!this.repoHashId) return
                this.isLoading = true
                this.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/buildParam/${this.projectId}/${this.repoHashId}/gitRefs`).then(res => {
                    this.branchesList = res.data
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
