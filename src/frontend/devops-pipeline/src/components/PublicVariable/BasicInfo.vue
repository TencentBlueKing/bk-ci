<template>
    <section class="public-var-basic-info-main">
        <div class="header-wrapper">
            <mode-switch
                class="mode-switch"
                @change="handleChangeMode"
            />
        </div>
        <template
            v-if="isCodeMode"
        >
            <div class="public-variable-yaml-editor">
                <YamlEditor
                    ref="editor"
                    :read-only="readOnly"
                    :value="publicVarYaml"
                    @change="handleYamlChange"
                />
            </div>
        </template>
        <template v-else>
            <div
                class="content-wrapper"
                v-bkloading="{ isLoading }"
            >
                <section class="left-box">
                    <p class="basic-info-title">
                        {{ $t('publicVar.basicInfo') }}
                    </p>
                    <template v-if="readOnly">
                        <div class="render-content">
                            <div
                                class="info-item"
                                v-for="item in renderInfoList"
                                :key="item.key"
                            >
                                <label class="info-label">
                                    {{ item.label }}
                                </label>
                                <div class="info-value">
                                    {{ item.value }}
                                </div>
                            </div>
                        </div>
                    </template>
                    <template v-else>
                        <bk-form
                            :label-width="200"
                            form-type="vertical"
                        >
                            <bk-form-item
                                :label="$t('publicVar.paramGroupId')"
                                :required="true"
                            >
                                <vuex-input
                                    :value="groupData.groupName"
                                    :disabled="!!groupData.groupName && isEditPage"
                                    name="groupName"
                                    :handle-change="(name, val) => handleChangeGroupData(name, val)"
                                />
                            </bk-form-item>
                            <bk-form-item
                                :label="$t('publicVar.paramGroupDesc')"
                            >
                                <vuex-textarea
                                    type="textarea"
                                    name="desc"
                                    :value="groupData.desc"
                                    :handle-change="(name, val) => handleChangeGroupData(name, val)"
                                />
                            </bk-form-item>
                        </bk-form>
                    </template>
                </section>
                <section class="right-box">
                    <div
                        v-if="!readOnly"
                        class="right-aside-header-wrapper"
                    >
                        <bk-button
                            icon="plus"
                            class="mr10"
                            @click="handleAddParam(VARIABLE)"
                        >
                            {{ $t('publicVar.addParam') }}
                        </bk-button>
                        <bk-button
                            icon="plus"
                            class="mr10"
                            @click="handleAddParam(CONSTANT)"
                        >
                            {{ $t('publicVar.addConst') }}
                        </bk-button>
                    </div>
                    <p
                        v-else
                        class="basic-info-title"
                    >
                        {{ $t('publicVar.variableList') }}
                    </p>
                    <div
                        class="variable-list"
                        v-for="data in variableList"
                        :key="data.key"
                    >
                        <render-variable-table
                            :read-only="readOnly"
                            :data="data"
                            :new-param-id="newParamId"
                        />
                    </div>
                </section>
            </div>
        </template>
    </section>
</template>

<script setup>
    import { ref, computed, onMounted } from 'vue'
    import { CODE_MODE } from '@/utils/pipelineConst'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import UseInstance from '@/hook/useInstance'
    import YamlEditor from '@/components/YamlEditor'
    import ModeSwitch from '@/components/PublicVariable/ModeSwitch'
    import RenderVariableTable from '@/components/PublicVariable/RenderVariableTable'
    import {
        EDIT_VARIABLE,
        VARIABLE,
        CONSTANT
    } from '@/store/modules/publicVar/constants.js'
    import { OPERATE_TYPE } from '../../store/modules/publicVar/constants'

    const props = defineProps({
        isRelease: {
            type: Boolean,
            default: false
        },
        readOnly: {
            type: Boolean,
            default: false
        },
        groupData: {
            type: Object,
            default: () => {}
        },
        showType: {
            type: String,
            default: ''
        },
        newParamId: {
            type: String,
            default: ''
        },
        handleAddParam: {
            type: Function,
            default: () => () => {}
        },
        handleEditParam: {
            type: Function,
            default: () => () => {}
        },
        handleDeleteParam: {
            type: Function,
            default: () => () => {}
        },
        handleCopyParam: {
            type: Function,
            default: () => () => {}
        }
    })
    const { proxy } = UseInstance()
    const isEditPage = computed(() => props.showType === EDIT_VARIABLE)
    const publicVars = computed(() => props.groupData?.publicVars ?? [])
    const publicVarMode = computed(() => proxy.$store.state?.publicVar?.publicVarMode)
    const publicVarYaml = computed(() => proxy.$store.state?.publicVar?.publicVarYaml)
    const isCodeMode = computed(() => publicVarMode.value === CODE_MODE)
    const operateType = computed(() => proxy.$store.state.publicVar.operateType)
    const renderInfoList = computed(() => {
        return [
            {
                label: proxy.$t('publicVar.paramGroupId'),
                value: props.groupData?.groupName ?? '--',
                key: 'id'
            },
            {
                label: proxy.$t('publicVar.paramGroupDesc'),
                value: props.groupData?.desc ?? '--',
                key: 'desc'
            },
            {
                label: proxy.$t('creator'),
                value: props.groupData?.modifier ?? '--',
                key: 'creator'
            },
            {
                label: proxy.$t('createTime'),
                value: props.groupData?.updateTime ?? '--',
                key: 'createTime'
            }
        ]
    })
    const variableList = computed(() => {
        return [
            {
                title: proxy.$t('publicVar.params'),
                data: publicVars.value.filter(i => i.type === VARIABLE),
                emptyBtnText: proxy.$t('publicVar.addParam'),
                emptyBtnFn: () => props?.handleAddParam(VARIABLE),
                handleEditParam: props.handleEditParam,
                handleDeleteParam: props.handleDeleteParam,
                handleCopyParam: props.handleCopyParam,
                key: VARIABLE
            },
            {
                title: proxy.$t('publicVar.constant'),
                data: publicVars.value.filter(i => i.type === CONSTANT),
                emptyBtnText: proxy.$t('publicVar.addConst'),
                emptyBtnFn: () => props?.handleAddParam(CONSTANT),
                handleEditParam: props.handleEditParam,
                handleDeleteParam: props.handleDeleteParam,
                handleCopyParam: props.handleCopyParam,
                key: CONSTANT
            }
        ]
    })

    async function fetchVariablesByGroupName () {
        if (!props.groupData?.groupName || props.isRelease || operateType.value === OPERATE_TYPE.CREATE) return
        try {
            const res = await proxy.$store.dispatch('publicVar/getVariables', {
                groupName: props.groupData?.groupName
            })
            proxy.$store.dispatch('publicVar/updateGroupData', {
                ...props.groupData,
                publicVars: res.map(i => ({
                    ...i,
                    buildFormProperty: {
                        ...i.buildFormProperty,
                        published: true
                    }
                }))
            })
        } catch (e) {
            console.error(e)
        }
    }
    function handleChangeGroupData (name, value) {
        proxy.$store.dispatch('publicVar/updateGroupData', {
            ...props.groupData,
            [name]: value
        })
    }
    function handleChangeMode (mode) {
        // todo
        if (mode === CODE_MODE) {
            console.log(mode)
        } else {
            console.log(mode)
        }
    }
    onMounted(() => {
        fetchVariablesByGroupName()
    })
</script>

<style lang="scss">
    .public-var-basic-info-main {
        height: 100%;
        .header-wrapper {
            display: flex;
            align-items: center;
            padding: 0 20px;
            height: 65px;
            background: #FAFBFD;
            .mode-switch {
                width: 160px;
            }
        }
        .content-wrapper,
        .public-variable-yaml-editor {
            display: flex;
            height: 100%;
            padding: 8px 20px 0;
            background: #fff;
            .left-box {
                width: 260px;
                height: 100%;
                padding-right: 15px;
                border-right: 1px solid #DCDEE5;
            }
            .right-box {
                background: #fff;
                flex: 1;
                padding: 0 20px;
                overflow: scroll;
            }
            .right-aside-header-wrapper {
                margin-bottom: 16px;
            }
        }
        .basic-info-title {
            font-weight: 700;
            font-size: 14px;
            color: #63656E;
            margin-bottom: 10px;
        }
        .variable-list {
            margin-bottom: 16px;
            &:last-child {
                margin-bottom: 0 !important;
            }
        }
        .render-content {
            font-size: 12px;
            margin-top: 24px;
            .info-item {
                margin-bottom: 16px;
            }
            .info-label {
                color: #979BA5;
            }
            .info-value {
                color: #313238;
                margin-top: 6px;
            }
        }
    }
</style>
