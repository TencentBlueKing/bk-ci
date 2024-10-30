<template>
    <ul class="param-main">
        <li
            class="param-item"
            v-for="(param, index) in parameters"
            :key="index"
        >
            <bk-input
                :value="param"
                @change="(val) => handleChangeInput(val, index)"
            >
            </bk-input>
            <i
                class="bk-icon icon-plus-circle"
                @click="plusParam()"
            />
            <i
                :class="{
                    'bk-icon icon-minus-circle': true,
                    'disabled': parameters.length <= 1
                }"
                @click="minusParam(index)"
            />
        </li>
    </ul>
</template>

<script>
    export default {
        name: 'InputParameterArray',
        props: {
            name: {
                type: String,
                default: ''
            },
            value: {
                type: Array,
                default: () => []
            },
            handleChange: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                parameters: []
            }
        },
        created () {
            this.parameters = this.value
            if (!this.parameters.length) this.plusParam()
        },
        methods: {
            handleChangeInput (val, index) {
                this.parameters[index] = val
                this.handleChange(this.name, this.parameters)
            },
            plusParam () {
                this.parameters.push('')
            },
            minusParam (index) {
                if (this.parameters.length <= 1) return
                this.parameters.splice(index, 1)
                this.handleChange(this.name, this.parameters)
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
