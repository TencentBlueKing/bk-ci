<template>
    <section class="key-options">
        <div class="key-item" v-for="(option, index) in options" :key="option.key">
            <span class="key-index">{{index + 1}}</span>
            <bk-input class="key-val" :value="option.key" @blur="(val) => handleEdit(val, index)" />
            <span class="key-del"><i @click.stop="handleDelete(index)" class="bk-icon icon-minus-circle hover-click" v-if="!disabled" /></span>
        </div>
        <a class="key-add" v-if="!disabled" @click.stop="handleAdd">
            <i class="devops-icon icon-plus-circle" />
            <span>{{$t('newui.pipelineParam.addItem')}}</span>
        </a>
    </section>
</template>

<script>
    export default {
        props: {
            disabled: {
                type: Boolean,
                default: false
            },
            options: {
                type: Array,
                default: () => ([])
            },
            handleChangeOptions: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                list: []
            }
        },
        created () {
            this.list = this.options || []
        },
        methods: {
            handleEdit (val, index) {
                const item = { key: val, value: val }
                this.list.splice(index, 1, item)
                this.handleChangeOptions('options', this.list)
            },
            handleAdd () {
                this.list.push({ key: '', value: '' })
                this.handleChangeOptions('options', this.list)
            },
            handleDelete (index) {
                this.list.splice(index, 1)
                this.handleChangeOptions('options', this.list)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .key-item {
        display: flex;
        align-items: center;
        height: 46px;
        .key-index {
            font-size: 12px;
            color: #979BA5;
            width: 24px;
        }
        .key-val {
            flex: 1;
            background: #FFFFFF;
            /* border: 1px solid #C4C6CC;
            border-radius: 2px; */
        }
        .key-del {
            width: 32px;
            cursor: pointer;
            text-align: right;
            i {
                font-size: 14px;
            }
        }
    }
    .key-add {
        margin-top: 8px;
        font-size: 12px;
        display: flex;
        align-items: center;
        cursor: pointer;
        color: #3A84FF;
        i {
            margin-right: 6px;
        }
    }
</style>
