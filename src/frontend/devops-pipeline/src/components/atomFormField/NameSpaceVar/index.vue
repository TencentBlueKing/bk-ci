<template>
    <div class="namespace-vars">
        <ul>
            <template v-if="paramList.length">
                <li class="param-item" key="th">
                    <!--<div><vuex-input value='namespace名称' disabled='true'/></div>
                    <div><vuex-input value='参数key' disabled='true'/></div>
                    <div><vuex-input value='参数value' disabled='true' /></div>-->
                    <span>namespace名称</span>
                    <span>参数key</span>
                    <span>参数value</span>
                    <i style="visibility:hidden" class="bk-icon icon-minus" />
                </li>
                <li class="param-item" v-for="(param, index) in paramList" :key="index">
                    <div><vuex-input name="namespace" :value="param.namespace" :handle-change="(name, value) => handleParamChange(name, value, index)" :data-vv-scope="`param-${index}`" v-validate.initial="`required`" /></div>
                    <div><vuex-input name="varKey" :value="param.varKey" :handle-change="(name, value) => handleParamChange(name, value, index)" :data-vv-scope="`param-${index}`" v-validate.initial="`required`" /></div>
                    <div><vuex-input name="varValue" :value="param.varValue" :handle-change="(name, value) => handleParamChange(name, value, index)" /></div>
                    <i @click.stop.prevent="editParam(index, false)" class="bk-icon icon-minus hover-click" />
                </li>
            </template>
            <a class="text-link hover-click" v-if="!disabled" @click.stop.prevent="editParam(paramList.length, true)">
                <i class="bk-icon icon-plus-circle" />
                <span>新增变量</span>
            </a>
        </ul>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import VuexInput from '@/components/atomFormField/VuexInput'

    export default {
        components: {
            VuexInput
        },
        mixins: [atomFieldMixin],
        props: {
            value: {
                type: Object,
                default: []
            }
        },
        data () {
            return {
                paramList: []
            }
        },
        watch: {
            value (val) {
                this.paramList = val
            }
        },
        async created () {
            this.paramList = this.value
            // this.paramList.push({namespace: 111, key: 1, value: 222})
        },
        methods: {
            editParam (index, isAdd) {
                if (isAdd) {
                    const param = {
                        namespace: `namespace${this.paramList.length + 1}`,
                        varKey: `param${this.paramList.length + 1}`,
                        varValue: ''
                    }
                    this.paramList.splice(index + 1, 0, param)
                } else {
                    this.paramList.splice(index, 1)
                }
                this.handleChange(this.name, this.paramList)
            },
            handleParamChange (key, value, paramIndex) {
                const param = this.paramList[paramIndex]
                if (param) {
                    Object.assign(param, {
                        [key]: value
                    })
                    this.handleChange(this.name, this.paramList)
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../scss/conf.scss';
    .namespace-vars {
        .param-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
            > span {
                flex: 1;
                margin-right: 0 10px;
            }
            > div {
                flex: 1;
                margin-right: 10px;
            }
        }
        .param-item-empty {
            text-align: center;
            color: $fontLigtherColor;
        }
        .hover-click {
            cursor: pointer;
        }
    }
</style>
