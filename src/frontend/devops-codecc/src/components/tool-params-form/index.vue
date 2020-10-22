<template>
    <div>
        <div class="disf" v-if="(taskDetail.atomCode && taskDetail.createFrom === 'bs_pipeline') || taskDetail.createFrom === 'gongfeng_scan'">
            <span class="pipeline-label">{{ param.labelName }}</span>
            <span class="fs14">{{ param.varDefault }}</span>
        </div>
        <div v-else>
            <bk-form-item
                :label="param.labelName"
                class="pb20"
            >
                <bk-input
                    v-bk-tooltips="{
                        content: param.varTips,
                        width: isToolManage ? 400 : 300,
                        placement: isToolManage ? 'bottom' : 'right'
                    }"
                    @change="handleChange"
                    v-model.trim="param.varDefault"
                    v-if="param.varType === 'STRING'">
                </bk-input>

                <bk-input
                    type="number"
                    @change="handleChange"
                    v-model.trim="param.varDefault"
                    v-if="param.varType === 'NUMBER'">
                </bk-input>

                <bk-radio-group @change="handleChange" v-model="param.varDefault" v-else-if="param.varType === 'BOOLEAN'" class="radio-param">
                    <bk-radio :value="true" class="item">{{$t('是')}}</bk-radio>
                    <bk-radio :value="false" class="item">{{$t('否')}}</bk-radio>
                </bk-radio-group>

                <bk-radio-group @change="handleChange" v-else-if="param.varType === 'RADIO'" v-model="param.varDefault" class="radio-param">
                    <bk-radio v-for="(option, index) in param.varOptionList" :value="option.id" :key="index" class="item pr20">{{option.name}}</bk-radio>
                </bk-radio-group>

                <bk-checkbox-group @change="handleChange" v-model="param.varDefault" v-else-if="param.varType === 'CHECKBOX'" class="checkbox-param">
                    <bk-checkbox v-for="(option, index) in param.varOptionList" :value="option.id" :key="index" class="item">{{option.name}}</bk-checkbox>
                </bk-checkbox-group>
            </bk-form-item>

        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'

    export default {
        name: 'tool-params-form',
        props: {
            param: {
                type: Object
            },
            tool: {
                type: String
            },
            isToolManage: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                formRules: {
                    inputValue: [
                        {
                            max: 50,
                            message: this.$t('不能多于x个字符', { num: 50 }),
                            trigger: 'blur'
                        }
                    ],
                    chooseValue: [
                        {
                            required: true,
                            message: this.$t('必填项'),
                            trigger: 'change'
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapState('task', {
                taskDetail: 'detail'
            })
        },
        created () {
        },
        methods: {
            handleChange (value) {
                const factor = {}
                factor[this.param.varName] = value
                this.$emit('handleFactorChange', factor, this.tool)
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .pipeline-label {
        display: inline-block;
        width: 104px;
        text-align: left;
        font-size: 14px;
        line-height: 14px;
        height: 46px;
        font-weight: 600;
    }
</style>
