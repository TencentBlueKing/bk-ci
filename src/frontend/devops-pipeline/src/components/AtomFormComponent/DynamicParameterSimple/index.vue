<template>
    <ul class="param-main" v-bkloading="{ isLoading }">
        <li class="param-label" style="display: flex; padding-right:30px;">
            <span
                v-for="(item, index) in curParameters[0].rowAttributes"
                :key="index"
                class="input-label"
                :title="item.label">
                {{ item.label }}：
                <i class="bk-icon icon-info-circle label-desc" v-bk-tooltips.top="{ content: item.desc, allowHTML: false }" />
            </span>
        </li>
        <li class="param-com" v-for="(parameter, paramIndex) in curParameters" :key="paramIndex">
            <parameter-com v-for="(model, index) in parameter.rowAttributes"
                :class="[{ 'last-child': index === parameter.rowAttributes.length - 1 }, 'input-com']"
                :style="{ maxWidth: `calc(${100 / parameter.rowAttributes.length}% - ${58 / parameter.rowAttributes.length}px)` }"
                v-bind="model"
                :key="model.id"
                @update-key="(newValue) => updateKey(model, newValue)"
                @update-value="(newValue) => updateValue(model, newValue)"
                :param-values="paramValues"
            ></parameter-com>
            <i class="bk-icon icon-plus-circle" @click="plusParam(parameter, paramIndex)"></i>
            <i class="bk-icon icon-minus-circle" v-if="curParameters.length > 1" @click="minusParam(paramIndex)"></i>
        </li>
    </ul>
</template>

<script>
    import mixins from '../mixins'
    import parameterCom from './parameterCom'

    export default {
        name: 'dynamic-parameter-simple',

        components: {
            parameterCom
        },
        
        mixins: [mixins],

        props: {
            parameters: {
                type: Array
            }
        },

        data () {
            return {
                isLoading: false,
                curParameters: []
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                this.curParameters = JSON.parse(JSON.stringify(this.parameters))
                this.setValue()
            },

            setValue () {
                let values = this.atomValue[this.name] || []
                if (!Array.isArray(values)) values = JSON.parse(values)

                if (values.length) {
                    this.curParameters = values.map((value) => {
                        const originAttr = this.parameters[0]
                        const currentRowAttr = JSON.parse(JSON.stringify(originAttr))
                        const curKeys = Object.keys(value)
                        const curValues = Object.values(value)
                        const rowAttrs = currentRowAttr.rowAttributes
                        rowAttrs.forEach((attr, index) => {
                            attr.id = curKeys[index]
                            attr.value = curValues[index]
                        })
                        return currentRowAttr
                    })
                }

                this.updateParameters()
            },

            /**
             * 添加一行动态参数
             */
            plusParam (parameter, index) {
                this.curParameters.splice(index, 0, JSON.parse(JSON.stringify(parameter)))
                this.updateParameters()
            },

            /**
             * 删除一行动态参数
             */
            minusParam (index) {
                this.curParameters.splice(index, 1)
                this.updateParameters()
            },

            /**
             * key更新
             */
            updateKey (model, newValue) {
                model.id = newValue
                this.updateParameters()
            },

            /**
             * value更新
             */
            updateValue (model, newValue) {
                model.value = newValue
                this.updateParameters()
            },

            /**
             * 更新参数
             */
            updateParameters () {
                const res = this.curParameters.map((parameter) => {
                    const rowAttributes = parameter.rowAttributes
                    const obj = {}
                    rowAttributes.forEach((model) => {
                        obj[model.id] = model.value || ''
                        return obj
                    })
                    return obj
                })
                this.handleChange(this.name, String(JSON.stringify(res)))
            }
        }
    }
</script>

<style lang="scss" scoped>
    .param-main {
        margin-top: 8px;
        .param-title {
            font-size: 12px;
            line-height: 30px;
        }
        .param-com {
            margin-bottom: 10px;
            display: flex;
            align-items: center;
            .input-com {
                flex: 1;
                margin-right: 10px;
                &.last-child {
                    margin-right: 0;
                }
            }
        }
        .param-label {
            display: flex;
            padding-right: 30px;
        }
        .input-label {
            flex: 1;
        }
    }
    .bk-icon {
        margin-left: 5px;
        font-size: 14px;
        cursor: pointer;
    }
    .label-desc {
        position: relative;
        bottom: 1px;
        right: 6px;
        margin-left: 0;
        cursor: auto;
        color: #C3CDD7;
        font-size: 14px;
        vertical-align: middle;
    }
</style>
