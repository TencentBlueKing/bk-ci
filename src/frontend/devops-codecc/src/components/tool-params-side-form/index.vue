<!-- deprecated -->
<template>
    <div class="params-side" :class="{ 'mini': !isParamsSideFormShow }">
        <div v-show="isParamsSideFormShow">
            <div class="main-top">
                {{$t('工具配置')}}
                <span class="cc-splite">|</span>
                <span class="fs12" v-if="hasSave">{{$t('已接入x个工具', { num: toolsNum })}}</span>
                <span class="fs12" v-else>{{$t('已选x个工具', { num: toolsNum })}}</span>
                <span class="fr bk-icon icon-angle-right toggle-button" @click="isParamsSideFormShow = false"></span>
            </div>
            <bk-form class="side-card" v-model="toolConfigParams">
                <toolparams>
                    <div class="side-card-item" v-for="toolParam in toolConfigParams" :key="toolParam.toolName">
                        <tool>{{toolParam.displayName}}</tool>
                        <tool-params-form
                            v-for="param in toolParam.paramsList"
                            @handleFactorChange="handleFactorChange"
                            :key="param.key"
                            :param="param"
                            :tool="toolParam.toolName">
                        </tool-params-form>
                        <bk-form-item v-if="toolParam.toolName !== 'CCN' && toolParam.toolName !== 'DUPC'"
                            :label="$t('规则集')"
                            :property="toolParam.tool"
                            :rules="formRules.ruleSet">
                            <bk-select v-model="toolParam.checkerSetId">
                                <bk-option-group
                                    v-for="(group, index) in toolParam.checkerSetsList"
                                    :name="group.name"
                                    :key="index">
                                    <bk-option v-for="option in group.children"
                                        :key="option.checkerSetId"
                                        :id="option.checkerSetId"
                                        :name="option.checkerSetName">
                                    </bk-option>
                                </bk-option-group>
                            </bk-select>
                        </bk-form-item>
                    </div>
                </toolparams>
                <bk-form-item v-if="hasSave" class="txac pd20 main-bottom">
                    <bk-button theme="primary" :title="$t('保存配置')" @click.stop.prevent="submitData">{{$t('保存配置')}}</bk-button>
                </bk-form-item>
            </bk-form>
        </div>
        <div v-show="!isParamsSideFormShow">
            <div class="main-top">
                {{$t('工具配置')}}
                <span class="fl bk-icon icon-angle-left toggle-button mr10" @click="isParamsSideFormShow = true"></span>
            </div>
        </div>
    </div>
</template>

<script>
    import ToolParamsForm from '@/components/tool-params-form'

    export default {
        name: 'tool-params-side-form',
        components: {
            ToolParamsForm
        },
        props: {
            toolConfigParams: {
                type: Object
            },
            toolsNum: {
                type: String
            },
            hasSave: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                paramsValue: {},
                formRules: {
                    ruleSet: [
                        {
                            required: true,
                            message: this.$t('必填项'),
                            trigger: 'change'
                        }
                    ]
                },
                isParamsSideFormShow: true
            }
        },
        computed: {},
        created () {},
        methods: {
            handleFactorChange (factor, toolName) {
                this.paramsValue[toolName] = Object.assign({}, this.paramsValue[toolName], factor)
                this.$emit('handleFactorChange', factor, toolName)
            },
            getParamsValue () {
                const taskId = this.$route.params.taskId
                const tools = this.toolConfigParams.map(item => {
                    const { toolName, checkerSetId, paramsList } = item
                    const paramObj = {}
                    paramsList.forEach(param => {
                        const key = param.varName
                        paramObj[key] = (this.paramsValue[toolName] && this.paramsValue[toolName][key]) || param.varDefault
                    })
                    const paramJson = Object.keys(paramObj).length ? JSON.stringify(paramObj) : ''
                    return { taskId, toolName, checkerSet: { checkerSetId }, paramJson }
                })
                return tools
            },
            submitData () {
                const taskId = this.$route.params.taskId
                const toolConfig = this.getParamsValue()
                const postData = { taskId, toolConfig }
                this.$store.dispatch('tool/updateParamsAndCheckerSets', postData).then(res => {
                    if (res === true) {
                        this.$bkMessage({ theme: 'success', message: this.$t('保存成功') })
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('保存失败') })
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .main-top, .main-bottom {
        background: #fafbfd;
        border-bottom: 1px solid #dcdee5;
        height: 56px;
        line-height: 56px;
        padding: 0 30px;
    }
    .main-bottom {
        border-top: 1px solid #dcdee5;
        >>>.bk-form-content {
            line-height: 56px;
        }
    }
    .params-side {
        position: fixed;
        top: 60px;
        right: 9px;
        width: 354px;
        max-height: calc(100vh - 102px);
        background: white;
        box-shadow: 0 2px 12px 0 hsla(0, 0%, 87%, 0.5), 0 2px 13px 0 hsla(0, 0%, 87%, 0.5);
        margin-top: 20px;

        &.mini {
            width: 160px;
        }
        .toggle-button {
            height: 30px;
            width: 23px;
            background: white;
            line-height: 30px;
            margin-top: 13px;
            font-weight: bolder;
            padding-left: 3px;
            border: 1px solid #d9e2e8;
            border-radius: 2px;
            box-shadow: 0 2px 12px 0 hsla(0, 0%, 87%, 0.5), 0 2px 13px 0 hsla(0, 0%, 87%, 0.5);
            cursor: pointer;
        }
        toolparams {
            display: block;
            padding: 0 20px 25px;
            max-height: calc(100vh - 158px);
            overflow-y: scroll;
            
            .side-card-item {
                padding: 25px 0 15px;
                border-bottom: 1px solid #dcdee5;

                tool {
                    font-size: 18px;
                    color: #54cad1;
                    line-height: 35px;
                }
                &:last-of-type {
                    border-bottom: none;
                }
            }
        }
    }
</style>
