<template>
    <div class="instance-aside">
        <div class="operate">
            <bk-button
                v-if="isInstanceCreateType"
                icon="plus"
                @click="handleAddInstance"
            >
                {{ $t('new') }}
            </bk-button>
            <i18n
                v-else
                class="select-pipeline"
                tag="div"
                path="template.selectPipelineNum"
            >
                <span class="select-num">
                    {{ renderInstanceList.length }}
                </span>
            </i18n>
            <div
                v-if="renderInstanceList.length"
                class="batch-edit-btn"
                @click="handleBatchEdit"
            >
                <Logo
                    name="batch-edit"
                    size="14"
                    style="fill:#3c96ff;position:relative;top:2px;"
                />
                <span>
                    {{ $t('template.batchEdit') }}
                </span>
            </div>
        </div>
        <ul class="instance-list">
            <li
                v-for="(instance, instanceIndex) in renderInstanceList"
                :class="[
                    'item',
                    {
                        active: instanceActiveIndex === instanceIndex,
                        editing: instanceIndex === editingIndex
                    }
                ]"
                :key="instance.id"
                @click="handleInstanceClick(instanceIndex)"
            >
                <template v-if="instanceIndex === editingIndex">
                    <bk-input
                        ref="nameInputRef"
                        v-model="instanceName"
                        @blur="(value) => handleEnterChangeName(value, instanceIndex)"
                        @enter="(value) => handleEnterChangeName(value, instanceIndex)"
                    >
                    </bk-input>
                </template>
                <template v-else>
                    <div
                        :class="['pipeline-name', {
                            'upgrade': !isInstanceCreateType
                        }]"
                        v-bk-overflow-tips
                    >
                        {{ instance.pipelineName }}
                    </div>
                    <div class="instance-operate">
                        <bk-icon
                            type="edit2"
                            class="operate-icon edit-icon"
                            v-bk-tooltips="$t('template.editInstanceName')"
                            @click.stop="handleEditName(instanceIndex)"
                        />
                        <template v-if="isInstanceCreateType">
                            <bk-popconfirm
                                :title="$t('template.deleteInstanceTitleTips')"
                                :content="$t('template.deleteInstanceContentTips')"
                                width="288"
                                trigger="click"
                                @confirm="handelDeleteInstance(instanceIndex)"
                            >
                                <bk-icon
                                    type="delete"
                                    class="operate-icon delete-icon"
                                    v-bk-tooltips="$t('delete')"
                                />
                            </bk-popconfirm>
                            <bk-icon
                                type="copy"
                                class="operate-icon copy-icon"
                                v-bk-tooltips="$t('template.copyInstance')"
                                @click.stop="handleCopyInstance(instance, instanceIndex)"
                            />
                        </template>
                    </div>
                </template>
            </li>
        </ul>
    </div>
</template>

<script setup>
    import { ref, defineProps, computed, onMounted, onBeforeUnmount } from 'vue'
    import {
        SET_TEMPLATE_DETAIL,
        SET_INSTANCE_LIST,
        UPDATE_USE_TEMPLATE_SETTING
    } from '@/store/modules/templates/constants'
    import { deepClone } from '@/utils/util'
    import Logo from '@/components/Logo'
    import UseInstance from '@/hook/useInstance'
    const props = defineProps({
        isInstanceCreateType: Boolean
    })
    const { proxy } = UseInstance()
    const instanceActiveIndex = ref(0)
    const editingIndex = ref(null)
    const nameInputRef = ref(null)
    const newIndex = ref(1)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    const instanceList = computed(() => proxy.$store?.state?.templates?.instanceList)
    const currentVersionId = computed(() => proxy?.$route.params?.version)
    const renderInstanceList = computed(() => {
        return instanceList.value
    })
    const instanceName = computed(() => {
        return renderInstanceList.value[editingIndex.value].pipelineName
    })
    const templateTriggerConfigs = computed(() => {
        return curTemplateDetail.value?.resource?.model?.stages[0]?.containers[0]?.elements?.map(i => ({
            atomCode: i.atomCode,
            stepId: i.stepId ?? '',
            disabled: i.additionalOptions?.enable ?? true,
            cron: i.advanceExpression,
            variables: i.startParams,
            name: i.name,
            version: i.version,
            isFollowTemplate: false
        }))
    })
    const curTemplateDetail = computed(() => proxy.$store?.state?.templates?.templateDetail)
    function handleInstanceClick (index) {
        if (editingIndex.value) return
        instanceActiveIndex.value = index
        proxy.$router.replace({
            query: {
                index: instanceActiveIndex.value + 1
            }
        })
    }
    function handleEnterChangeName (value, index) {
        if (props.isInstanceCreateType && !value) {
            instanceList.value.splice(index, 1)
            editingIndex.value = null
            const newIndex = instanceList.value.length - 1
            handleInstanceClick(newIndex)
            instanceActiveIndex.value = newIndex
            proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, index === 0 ? [] : instanceList.value)
            return
        }
        if (!props.isInstanceCreateType && !value) return
        proxy.$set(instanceList.value[index], 'pipelineName', value.trim())
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, instanceList.value)
        editingIndex.value = null
    }
    function handleEditName (index) {
        editingIndex.value = index
        proxy?.$nextTick(() => {
            nameInputRef.value && nameInputRef.value[0]?.focus()
        })
    }
    function handelDeleteInstance (index) {
        instanceList.value.splice(index, 1)
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, instanceList.value)
        handleInstanceClick(instanceList.value.length - 1)
    }
    function handleCopyInstance (instance, index) {
        const newInstance = deepClone(instance)
        newInstance.pipelineName += `_copy${newIndex.value}`
        newIndex.value += 1
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, [...instanceList.value, newInstance])
        handleInstanceClick(instanceList.value.length - 1)
    }
    async function fetchPipelinesDetails () {
        try {
            const pipelineIds = renderInstanceList.value.map(i => i.pipelineId)
            const res = await proxy.$store.dispatch('templates/fetchPipelineDetailById', {
                pipelineIds,
                projectId: projectId.value,
                templateId: templateId.value
            })
            const list = renderInstanceList.value.map(i => {
                return {
                    ...i,
                    ...res[i.pipelineId]
                }
            })
            list.forEach(item => {
                const overrideTemplateField = res[item.pipelineId]?.overrideTemplateField ?? {}
                item.param.forEach(p => {
                    proxy.$set(p, 'isRequiredParam', p.required)
                    proxy.$set(p, 'isFollowTemplate', !overrideTemplateField?.paramIds?.includes(p.id))
                })
                if (item.buildNo) {
                    proxy.$set(item.buildNo, 'isRequiredParam', item?.buildNo?.required)
                }
            })
            proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, list)
        } catch (e) {
            console.error(e)
        }
    }
    function handleAddInstance () {
        if (!curTemplateDetail.value?.params) return
        const { params, buildNo } = deepClone(curTemplateDetail.value)
        const newInstance = {
            enablePac: false,
            param: params.map(p => {
                return {
                    ...p,
                    isRequiredParam: p.required,
                    isFollowTemplate: false
                }
            }),
            buildNo: {
                ...buildNo,
                isRequiredParam: buildNo.required,
                isFollowTemplate: false
            },
            triggerConfigs: templateTriggerConfigs.value
        }
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, [...instanceList.value, newInstance])
        proxy?.$nextTick(() => {
            const index = instanceList.value.length - 1
            handleInstanceClick(index)
            handleEditName(index)
            nameInputRef.value && nameInputRef.value[0]?.focus()
        }, 3000)
    }
    function init () {
        if (!props.isInstanceCreateType && !instanceList.value.length) {
            proxy.$router.push({
                name: 'TemplateOverview',
                params: {
                    type: 'instanceList',
                    version: currentVersionId.value
                }
            })
            return
        }
        if (!props.isInstanceCreateType) {
            fetchPipelinesDetails()
            proxy.$nextTick(() => {
                handleInstanceClick(instanceActiveIndex.value)
            })
        }
    }
    function handleBatchEdit () {
        proxy.$emit('batchEdit')
    }
    onMounted(() => {
        init()
    })
    
    onBeforeUnmount(() => {
        proxy.$store.commit(`templates/${SET_TEMPLATE_DETAIL}`, {
            templateVersion: '',
            templateDetail: {}
        })
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, [])
        proxy.$store.commit(`templates/${UPDATE_USE_TEMPLATE_SETTING}`, false)
    })
</script>

<style lang="scss" scoped>
    .instance-aside {
        height: 100%;
        font-size: 12px;
        padding: 24px;
        .operate {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .select-pipeline {
            color: #4d4f56;
        }
        .select-num {
            font-weight: 700;
        }
        .batch-edit-btn {
            cursor: pointer;
            color: #3A84FF;
        }
    }
    .instance-list {
        list-style: none;
        margin-top: 20px;
        overflow: auto;
        height: calc(100% - 140px);
        .item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            height: 32px;
            padding: 0 15px;
            border-radius: 2px;
            background: #F5F7FA;
            margin-bottom: 10px;
            cursor: pointer;
            &.editing {
                padding: 0;
                font-weight: 400 !important;
            }
            &.active,
            &:hover {
                color: #3A84FF;
                font-weight: 700;
                background: #E1ECFF;
            }
            &:hover {
                .instance-operate {
                    visibility: visible;
                }
            }
        }
        .pipeline-name {
            max-width: 55%;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            &.upgrade {
                max-width: 75%;
            }
        }
        .instance-operate {
            visibility: hidden;
            color: #989ca7;
            .edit-icon {
                position: relative;
                top: 1px;
                font-size: 20px !important;
            }
            .copy-icon,
            .delete-icon {
                font-size: 14px !important;
            }
            .operate-icon {
                &:hover {
                    color: #3A84FF;
                }
            }
        }
    }
</style>
