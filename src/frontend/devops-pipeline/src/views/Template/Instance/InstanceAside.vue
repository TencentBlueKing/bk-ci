<template>
    <div class="instance-aside">
        <div class="operate">
            <bk-button
                v-if="isInstanceCreateType"
                icon="plus"
                :disabled="!currentVersion || editingIndex > -1"
                @click="handleShowInstanceCreate"
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
                    {{ $t('template.batchEditParams') }}
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
                    <div class="edit-input-main">
                        <bk-input
                            ref="nameInputRef"
                            name="pipelineName"
                            :class="{
                                'instance-empty-input is-danger': isErrorName || errors.has('pipelineName')
                            }"
                            v-validate="{
                                required: true,
                                max: 128
                            }"
                            :value="instanceName"
                            @change="checkPipelineName"
                            @blur="(value) => handleEnterChangeName(value, instanceIndex)"
                            @enter="(value) => handleEnterChangeName(value, instanceIndex)"
                        >
                        </bk-input>
                        <i
                            v-if="isErrorName || errors.has('pipelineName')"
                            class="bk-icon icon-exclamation-circle-shape tooltips-icon"
                            v-bk-tooltips="errors.has('pipelineName') ? errors.first('pipelineName') : $t('template.nameExists')"
                        />
                    </div>
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
                            <bk-icon
                                type="copy"
                                class="operate-icon copy-icon"
                                v-bk-tooltips="$t('template.copyInstance')"
                                @click.stop="handleCopyInstance(instance, instanceIndex)"
                            />
                            <bk-popconfirm
                                :title="$t('template.deleteInstanceTitleTips')"
                                :content="$t('template.deleteInstanceContentTips')"
                                width="288"
                                trigger="click"
                                @confirm="handleDeleteInstance(instanceIndex)"
                            >
                                <bk-icon
                                    type="delete"
                                    class="operate-icon delete-icon"
                                    v-bk-tooltips="$t('delete')"
                                />
                            </bk-popconfirm>
                        </template>
                    </div>
                </template>
            </li>
        </ul>
        <instance-pipeline-name
            :show-instance-create.sync="showInstanceCreate"
            :pipeline-list="renderInstanceList"
            @confirm="handleConfirmCreateInstance"
        />
    </div>
</template>

<script setup>
    import { ref, defineProps, computed, watch, onMounted, onBeforeUnmount } from 'vue'
    import {
        SET_TEMPLATE_DETAIL,
        SET_INSTANCE_LIST,
        UPDATE_USE_TEMPLATE_SETTING,
        INSTANCE_OPERATE_TYPE
    } from '@/store/modules/templates/constants'
    import { deepClone } from '@/utils/util'
    import Logo from '@/components/Logo'
    import UseInstance from '@/hook/useInstance'
    import InstancePipelineName from '@/components/Template/instance-pipeline-name'
    const props = defineProps({
        isEditing: Boolean
    })
    const { proxy } = UseInstance()
    const instanceActiveIndex = ref(0)
    const editingIndex = ref(-1)
    const nameInputRef = ref(null)
    const newIndex = ref(1)
    const isErrorName = ref(false)
    const showInstanceCreate = ref(false)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    const currentVersion = computed(() => proxy?.$route.params?.version)
    const pipelineName = computed(() => proxy?.$route.query?.pipelineName)
    const useTemplateSettings = computed(() => proxy?.$route.query?.useTemplateSettings)
    const instanceViewType = computed(() => proxy.$route.params?.type)
    const isInstanceCreateType = computed(() => instanceViewType.value === INSTANCE_OPERATE_TYPE.CREATE)
    const renderInstanceList = computed(() => {
        return instanceList.value
    })
    const instanceList = computed(() => proxy.$store?.state?.templates?.instanceList)
    const curTemplateDetail = computed(() => proxy.$store?.state?.templates?.templateDetail)
    const instanceName = computed(() => {
        return renderInstanceList.value[editingIndex.value]?.pipelineName ?? ''
    })
    watch(() => curTemplateDetail.value, (val) => {
        if (instanceList.value.length) return
        if (pipelineName.value && isInstanceCreateType.value) {
            handleAddInstance()
            proxy.$store.commit(`templates/${UPDATE_USE_TEMPLATE_SETTING}`, useTemplateSettings.value)
            handleInstanceClick()
        }
    })
    async function checkPipelineName (newVal) {
        const curInstanceName = instanceList.value[editingIndex.value]?.pipelineName
        if (!newVal || curInstanceName === newVal) {
            isErrorName.value = false
            return
        }
        if (renderInstanceList.value.find(i => i.pipelineName === newVal)) {
            isErrorName.value = true
            return
        }
        try {
            isErrorName.value = await proxy.$store.dispatch('pipelines/checkPipelineName', {
                projectId: projectId.value,
                pipelineName: newVal.trim()
            })
        } catch(e) {
            console.error(e)
            isErrorName.value = false
        }
    }
    function handleInstanceClick (index = -1) {
        if (editingIndex.value !== -1) return
        instanceActiveIndex.value = index
        proxy.$router.replace({
            query: {
                ...proxy.$route.query,
                index: instanceActiveIndex.value + 1
            }
        })
    }
    function handleEnterChangeName (value, index) {
        if (!value.trim()) return
        if (isErrorName.value) return
        if (props.isInstanceCreateType && !value) {
            instanceList.value.splice(index, 1)
            editingIndex.value = -1
            const newIndex = instanceList.value.length - 1
            handleInstanceClick(newIndex)
            instanceActiveIndex.value = newIndex
            proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, {
                list: index === 0 ? [] : instanceList.value
            })
            return
        }
        if (!props.isInstanceCreateType && !value) return
        proxy.$set(instanceList.value[index], 'pipelineName', value.trim())
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, { list: instanceList.value })
        editingIndex.value = -1
        proxy.$emit('update:isEditing', false)
    }
    function handleEditName (index) {
        editingIndex.value = index
        proxy?.$nextTick(() => {
            nameInputRef.value && nameInputRef.value[0]?.focus()
        })
        proxy.$emit('update:isEditing', true)
    }
    function handleDeleteInstance (index) {
        instanceList.value.splice(index, 1)
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, { list: instanceList.value })
        handleInstanceClick(instanceList.value.length - 1)
    }
    function handleCopyInstance (instance, index) {
        const newInstance = deepClone(instance)
        newInstance.pipelineName += `_copy${newIndex.value}`
        newIndex.value += 1
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, { list: [...instanceList.value, newInstance] })
        handleInstanceClick(instanceList.value.length - 1)
    }
    async function fetchPipelinesDetails () {
        try {
            proxy.$store.dispatch('templates/updateInstancePageLoading', true)
            const pipelineIds = renderInstanceList.value.map(i => i.pipelineId)
            const res = await proxy.$store.dispatch('templates/fetchPipelineDetailById', {
                pipelineIds,
                projectId: projectId.value,
                templateId: templateId.value
            })
            const list = renderInstanceList.value.map(i => {
                const triggerElements = res[i.pipelineId]?.triggerElements
                const overrideTemplateField = res[i.pipelineId]?.overrideTemplateField ?? []
                return {
                    ...i,
                    ...res[i.pipelineId],
                    pipelineName: proxy.$route.query.pipelineName || i.pipelineName,
                    ...(
                        triggerElements?.length ? {
                            triggerConfigs: triggerElements.map(trigger => {
                                return {
                                    id: trigger.id,
                                    atomCode: trigger.atomCode,
                                    stepId: trigger.stepId ?? '',
                                    disabled: Object.hasOwnProperty.call(trigger?.additionalOptions ?? {}, 'enable') ? !trigger?.additionalOptions?.enable : false,
                                    cron: trigger.advanceExpression,
                                    name: trigger.name,
                                    version: trigger.version,
                                    isFollowTemplate: !(overrideTemplateField?.triggerStepIds?.includes(trigger.stepId)),
                                    isDelete: false,
                                    isNew: false,
                                    ...(
                                        trigger.startParams ? {
                                            variables: JSON.parse(trigger.startParams)
                                        } : {}
                                    )
                                }
                            })
                        }
                        : undefined
                    )
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
                    proxy.$set(item?.buildNo, 'isFollowTemplate', !overrideTemplateField?.paramIds?.includes('BK_CI_BUILD_NO'))
                }
            })
            proxy.$store.dispatch('templates/updateInstancePageLoading', false)
            proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, { list })
        } catch (e) {
            console.error(e)
        }
    }
    function handleShowInstanceCreate () {
        showInstanceCreate.value = true
    }
    function handleConfirmCreateInstance (name) {
        showInstanceCreate.value = false
        handleAddInstance(name)
    }
    function handleAddInstance (name) {
        const instanceParams = deepClone(curTemplateDetail.value)
        const newInstance = {
            ...instanceParams,
            pipelineName: name ?? pipelineName.value ?? ''
        }
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, { list: [...instanceList.value, newInstance] })
        proxy?.$nextTick(() => {
            const index = instanceList.value.length - 1
            handleInstanceClick(index)
            proxy.$store.dispatch('templates/updateInstancePageLoading', false)
        }, 3000)
    }
    async function init () {
        if ([INSTANCE_OPERATE_TYPE.UPGRADE, INSTANCE_OPERATE_TYPE.COPY].includes(instanceViewType.value)) {
            const { pipelineId, pipelineName } = proxy.$route.query
            if (pipelineId && pipelineName) {
                proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, {
                    list: [
                        {
                            pipelineName: pipelineName?.substring(0, 128),
                            pipelineId
                        }
                    ]
                })
            }

            if (instanceViewType.value === INSTANCE_OPERATE_TYPE.UPGRADE  && !instanceList.value.length) {
                proxy.$router.push({
                    name: 'TemplateOverview',
                    params: {
                        type: 'instanceList',
                        version: currentVersion.value
                    }
                })
                return
            }
            await fetchPipelinesDetails()
            proxy.$nextTick(() => {
                handleInstanceClick(instanceActiveIndex.value)
            })
        }
        if (instanceViewType.value === INSTANCE_OPERATE_TYPE.CREATE && !pipelineName.value) {
            handleShowInstanceCreate()
        }
       
    }
    function handleBatchEdit () {
        if (editingIndex.value > -1) return
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
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, { list: [] })
        proxy.$store.commit(`templates/${UPDATE_USE_TEMPLATE_SETTING}`, false)
    })
</script>

<style lang="scss" scoped>
    .instance-aside {
        height: 100%;
        font-size: 12px;
        padding: 24px;
        background-color: #fff;
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
                border: none;
            }
            &.active {
                color: #3A84FF;
                font-weight: 700;
                background: #E1ECFF !important;
            }
            &:hover {
                background: #EAEBF0;
                .instance-operate {
                    visibility: visible;
                }
            }
        }
        .edit-input-main {
            position: relative;
            display: inline-block;
            vertical-align: middle;
            width: 100%;
            .tooltips-icon {
                position: absolute;
                z-index: 1000;
                right: 8px;
                top: 8px;
                color: #ea3636;
                cursor: pointer;
                font-size: 16px;
            }
        }
        .pipeline-name {
            max-width: 75%;
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

<style lang="scss">
    .instance-empty-input {
        .bk-form-input {
            border-color: red;
        }
    }
</style>
