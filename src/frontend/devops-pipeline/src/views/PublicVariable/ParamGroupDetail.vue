<template>
    <section>
        <bk-sideslider
            :is-show.sync="isShow"
            :width="sidesliderWidth"
            :show-mask="false"
            :transfer="false"
            :ext-cls="{
                'param-group-sideslider': true,
                'not-tips': !showTips
            }"
            @hidden="handleHidden"
            :before-close="handleBeforeClose"
        >
            <template v-if="readOnly">
                <div slot="header">
                    <bk-tab
                        :active.sync="viewTab"
                        ext-cls="view-variable-tabs"
                        type="unborder-card"
                    >
                        <bk-tab-panel
                            v-for="(panel, index) in panels"
                            v-bind="panel"
                            :key="index"
                        >
                        </bk-tab-panel>
                    </bk-tab>
                </div>
                <div
                    class="sideslider-content"
                    slot="content"
                >
                    <component
                        :is="tabComponent"
                        :group-data="groupData"
                        :read-only="readOnly"
                    />
                </div>
                <div
                    v-if="showFooter"
                    class="sideslider-footer"
                    slot="footer"
                >
                    <bk-button
                        theme="primary"
                        v-perm="{
                            hasPermission: groupData?.permission?.canEdit,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: RESOURCE_TYPE.VARIABLE,
                                resourceCode: groupData.groupName,
                                action: VARIABLE_RESOURCE_ACTION.EDIT
                            }
                        }"
                        @click="handleEditGroup(groupData)"
                    >
                        {{ $t('publicVar.editParamGroup') }}
                    </bk-button>
                </div>
            </template>
            <template v-else>
                <div slot="header">
                    {{ title }}
                    <span
                        v-if="groupData.groupName"
                        class="group-name"
                    >
                        | {{ groupData.groupName }}
                    </span>
                </div>
                <div
                    class="sideslider-content"
                    slot="content"
                >
                    <basic-info
                        ref="basicInfoRef"
                        :read-only="readOnly"
                        :group-data="groupData"
                        :show-type="showType"
                        :new-param-id="newParamId"
                        :handle-add-param="handleAddParam"
                        :handle-edit-param="handleEditParam"
                        :handle-delete-param="handleDeleteParam"
                        :handle-copy-param="handleCopyParam"
                    />
                </div>
                <div
                    class="sideslider-footer"
                    slot="footer"
                >
                    <bk-button
                        :disabled="releaseDisabled"
                        @click="handlePreview"
                        :loading="releasing"
                        theme="primary"
                    >
                        {{ $t('publicVar.release') }}
                    </bk-button>
                    <bk-button
                        @click="handleHidden"
                        :loading="releasing"
                    >
                        {{ $t('cancel') }}
                    </bk-button>
                </div>
            </template>
        </bk-sideslider>

        <bk-sideslider
            :is-show.sync="showAddParamSlider"
            ext-cls="public-param-from-slider"
            :width="640"
            :title="paramTitle"
            :transfer="false"
            :before-close="hideAddParamSlider"
        >
            <div slot="content">
                <pipeline-param-form
                    ref="pipelineParamFormRef"
                    is-public-var
                    :edit-index="editIndex"
                    :param-type="paramType"
                    :global-params="publicVars"
                    :edit-item="sliderEditItem"
                    :update-param="updateEditParma"
                    :reset-edit-item="resetEditParam"
                />
            </div>
            <footer slot="footer">
                <bk-button
                    theme="primary"
                    @click="handleSaveVar"
                >
                    {{ $t('confirm') }}
                </bk-button>
                <bk-button
                    @click="hideAddParamSlider"
                >
                    {{ $t('cancel') }}
                </bk-button>
            </footer>
        </bk-sideslider>
        <VariableGroupPreviewDialog
            :is-show.sync="showPreView"
            :preview-data="previewData"
            :group-data="groupData"
            @confirm="handleShowReleaseSlider"
        />
        <release-variable-slider
            :value.sync="showReleaseSlider"
            @success="handleReleaseSuccess"
        />
    </section>
</template>

<script setup>
    import { computed, watch, ref } from 'vue'
    import {
        RESOURCE_TYPE,
        VARIABLE_RESOURCE_ACTION
    } from '@/utils/permission'
    import { deepClone, randomString } from '@/utils/util'
    import {
        VARIABLE,
        CONSTANT,
        OTHER,
        OPERATE_TYPE
    } from '@/store/modules/publicVar/constants'
    import { navConfirm } from '@/utils/util'
    import UseInstance from '@/hook/useInstance'
    import BasicInfo from '@/components/PublicVariable/BasicInfo'
    import ReferenceList from '@/components/PublicVariable/ReferenceList'
    import ReleaseHistory from '@/components/PublicVariable/ReleaseHistory'
    import PipelineParamForm from '@/components/PipelineEditTabs/components/pipeline-param-form'
    import ReleaseVariableSlider from '@/components/PublicVariable/ReleaseVariableSlider'
    import VariableGroupPreviewDialog from '@/components/PublicVariable/VariableGroupPreviewDialog'

    const { proxy } = UseInstance()
    const props = defineProps({
        isShow: Boolean,
        title: String,
        showType: String,
        showTips: Boolean,
        readOnly: Boolean,
        handleEditGroup: Function,
        saveSuccessFn: Function,
        defaultTab: {
            type: String,
            default: 'basicInfo'
        }
    })
    const viewTab = ref('basicInfo')
    const panels = ref([
        {
            name: 'basicInfo',
            label: proxy.$t('publicVar.basicInfo')
        },
        {
            name: 'referenceList',
            label: proxy.$t('publicVar.referenceList')
        },
        {
            name: 'releaseHistory',
            label: proxy.$t('publicVar.releaseHistory')
        }
    ])
    const basicInfoRef = ref(null)
    const published = ref(false)
    const showPreView = ref(false)
    const showReleaseSlider = ref(false)
    const previewData = ref([])
    const initialGroupData = ref(null) // 保存初始的 groupData 状态
    const shouldSaveInitialData = ref(false) // 标记是否需要保存初始数据
    const groupData = computed(() => proxy.$store.state.publicVar.groupData)
    const isManage = computed(() => proxy.$store.state.pipelines.isManage)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const showFooter = computed(() => viewTab.value === 'basicInfo')
    const publicVars = computed(() => groupData.value?.publicVars ?? [])
    const operateType = computed(() => proxy.$store.state.publicVar.operateType)
    const tabComponent = computed(() => {
        const comMap = {
            'basicInfo': BasicInfo,
            'referenceList': ReferenceList,
            'releaseHistory': ReleaseHistory
        }
        return comMap[viewTab.value]
    })
    const publicVarTypeMap = computed(() => ({
        [VARIABLE]: VARIABLE,
        [CONSTANT]: CONSTANT,
        [OTHER]: VARIABLE
    }))
    const showAddParamSlider = ref(false)
    const releasing = ref(false)
    const newParamId = ref('') // 用于记录新增的变量，新增变量后高亮table当前行
    const editIndex = ref(-1)
    const paramType = ref('var')
    const publicVarType = ref(VARIABLE)
    const paramTitle = ref('')
    const sliderEditItem = ref({})
    const releaseDisabled = computed(() => {
        return !groupData.value?.groupName || !/^[a-zA-Z][a-zA-Z0-9_]{2,31}$/.test(groupData.value?.groupName) || !groupData.value?.publicVars?.length
    })
    const sidesliderWidth = computed(() => {
        // 250 表格第一列宽度
        // 100 表格第二列宽度
        // 20 padding样式
        return window.innerWidth - 250 - 120 - 20
    })
    watch(() => (props.isShow), (val) => {
        if (!val) {
            viewTab.value = 'basicInfo'
            initialGroupData.value = null
            shouldSaveInitialData.value = false
        } else {
            init()
        }
    })
    // 监听 publicVars 变化，在数据加载完成后保存初始状态
    watch(() => publicVars.value, (newVars, oldVars) => {
        // 只在需要保存初始数据且 publicVars 从空变为有数据时保存
        if (shouldSaveInitialData.value && newVars?.length > 0 && (!oldVars || oldVars.length === 0)) {
            proxy.$nextTick(() => {
                initialGroupData.value = deepClone(groupData.value)
                shouldSaveInitialData.value = false
            })
        }
    }, { deep: true })
    watch(() => props.defaultTab, (val) => {
        viewTab.value = val
    })
    function init () {
        // 设置初始 tab
        viewTab.value = props.defaultTab || 'basicInfo'
        // 侧边栏打开且非只读模式时，标记需要保存初始状态
        shouldSaveInitialData.value = true
            
        // 如果是新建模式（没有 groupName），立即保存初始状态
        if (operateType.value === OPERATE_TYPE.CREATE || !groupData.value?.groupName) {
            proxy.$nextTick(() => {
                console.log(123)
                initialGroupData.value = deepClone(groupData.value)
                shouldSaveInitialData.value = false
            })
        } else if (publicVars.value?.length > 0) {
            // 如果是编辑模式且 publicVars 已经存在（第二次打开的情况），直接保存
            proxy.$nextTick(() => {
                initialGroupData.value = deepClone(groupData.value)
                shouldSaveInitialData.value = false
            })
        }
    }
    // 检查 groupData 是否发生变化
    function hasGroupDataChanged () {
        if (!initialGroupData.value || props.readOnly) {
            return false
        }
        
        const current = groupData.value
        const initial = initialGroupData.value
        
        // 检查基本信息是否变化
        if (current.groupName !== initial.groupName || current.desc !== initial.desc) {
            return true
        }
        
        // 检查 publicVars 是否变化
        const currentVars = current.publicVars || []
        const initialVars = initial.publicVars || []
        
        // 数量不同
        if (currentVars.length !== initialVars.length) {
            return true
        }
        
        // 深度对比每个变量
        return JSON.stringify(currentVars) !== JSON.stringify(initialVars)
    }
    
    async function handleHidden () {
        // 检查数据是否变化
        const res = hasGroupDataChanged()
        if (res) {
            try {
                const leave = await navConfirm({
                    content: proxy.$t('editPage.closeConfirmMsg'),
                    type: 'warning',
                    cancelText: proxy.$t('cancel')
                })
                if (leave) {
                    proxy.$emit('update:isShow', false)
                }
            } catch (e) {
                // 用户取消
                return
            }
        } else {
            proxy.$emit('update:isShow', false)
        }
    }
    
    async function handleBeforeClose () {
        // 检查数据是否变化
        if (hasGroupDataChanged()) {
            try {
                const leave = await navConfirm({
                    content: proxy.$t('editPage.closeConfirmMsg'),
                    type: 'warning',
                    cancelText: proxy.$t('cancel')
                })
                return leave
            } catch (e) {
                // 用户取消
                return false
            }
        }
        return true
    }
    async function validParamOptions () {
        let optionValid = true
        if ((sliderEditItem.value?.type === 'ENUM' || sliderEditItem.value?.type === 'MULTIPLE') && sliderEditItem.value?.payload?.type !== 'remote') {
            // value为空， 则默认等于key
            sliderEditItem.value.options?.forEach(item => {
                if (!item.value) {
                    item.value = item.key
                }
            })
            for (const index of sliderEditItem.value?.options?.keys()) {
                optionValid = await proxy.$validator.validate(`option-${index}.*`)
                if (!optionValid) return optionValid
            }
        }
        return optionValid
    }
    async function handleSaveVar () {
        // 单选、复选类型， 需要先校验options
        const optionValid = await validParamOptions()
        proxy.$validator.validate('pipelineParam.*').then((result) => {
            if (result && optionValid) {
                const newPublicVars = deepClone(publicVars.value)
                const { id, name, type, defaultValue, desc } = sliderEditItem.value
                const newVarData = {
                    varName: id,
                    alias: name,
                    type: publicVarTypeMap.value[publicVarType.value],
                    valueType: type,
                    defaultValue,
                    desc,
                    buildFormProperty: {
                        ...sliderEditItem.value,
                        published: published.value
                    }
                }
                if (editIndex.value > -1) {
                    newPublicVars[editIndex.value] = newVarData
                } else {
                    newPublicVars.unshift(newVarData)
                    newParamId.value = id
                    setTimeout(() => {
                        newParamId.value = ''
                    }, 5000)
                }
                hideAddParamSlider(false)
                proxy.$store.dispatch('publicVar/updateGroupData', {
                    ...groupData.value,
                    publicVars: newPublicVars
                })
            }
        })
    }
    function hideAddParamSlider (needCheckChange = true) {
        const hasChange = proxy.$refs.pipelineParamFormRef?.isParamChanged()

        const close = () => {
            showAddParamSlider.value = false
            editIndex.value = -1
            sliderEditItem.value = {}
        }
        if (needCheckChange && hasChange) {
            navConfirm(
                {
                    content: proxy.$t('editPage.closeConfirmMsg'),
                    type: 'warning',
                    cancelText: proxy.$t('cancel')
                }
            ).then((leave) => {
                leave && close()
            })
        } else {
            close()
        }
    }
    function handleAddParam (type = VARIABLE) {
        const typeMap = {
            VARIABLE: {
                type: 'var',
                title: proxy.$t('publicVar.addParam')
            },
            CONSTANT: {
                type: 'constant',
                title: proxy.$t('publicVar.addConst')
            },
            OTHER: {
                type: 'other',
                title: proxy.$t('publicVar.addParam')
            }
        }
        editIndex.value = -1
        showAddParamSlider.value = true
        published.value = false
        publicVarType.value = type
        sliderEditItem.value = {}
        paramType.value = typeMap[type]?.type
        paramTitle.value = typeMap[type]?.title
    }
    function handleEditParam (type = VARIABLE, varName) {
        showAddParamSlider.value = true
        publicVarType.value = type
        paramType.value = type === VARIABLE ? 'var' : 'constant'
        editIndex.value = publicVars.value.findIndex(item => item.varName === varName)
        sliderEditItem.value = deepClone(publicVars.value.find(item => item.varName === varName)?.buildFormProperty)
        published.value = sliderEditItem.value.published
        paramTitle.value = type === VARIABLE ? proxy.$t('publicVar.editParam') : proxy.$t('publicVar.editConst')
    }
    function handleDeleteParam (varName) {
        proxy.$store.dispatch('publicVar/updateGroupData', {
            ...groupData.value,
            publicVars: publicVars.value.filter(i => i.varName !== varName)
        })
    }
    function handleCopyParam (type = VARIABLE, data) {
        editIndex.value = -2
        published.value = false
        publicVarType.value = type
        paramType.value = type === VARIABLE ? 'var' : 'constant'
        const randomStr = randomString(5)
        sliderEditItem.value = {
            ...data,
            id: `${data.id}${data.constant ? `_${randomStr.toUpperCase()}` : `_${randomStr}`}`,
            name: `${data.name}_copy`,
            published: false
        }
        paramTitle.value = type === VARIABLE ? proxy.$t('publicVar.addParam') : proxy.$t('publicVar.addConst')
        showAddParamSlider.value = true
    }

    function updateEditParma (name, value) {
        sliderEditItem.value[name] = value
    }
    function resetEditParam (param = {}) {
        sliderEditItem.value = param
    }
    function getOperateVarTitle (operate, type) {
        const operateMap = {
            [OPERATE_TYPE.CREATE]: proxy.$t('publicVar.create'),
            [OPERATE_TYPE.DELETE]: proxy.$t('publicVar.delete'),
            [OPERATE_TYPE.UPDATE]: proxy.$t('publicVar.update')
        }
        const typeMap = {
            [VARIABLE]: proxy.$t('publicVar.params'),
            [CONSTANT]: proxy.$t('publicVar.constant'),
            [OTHER]: proxy.$t('publicVar.params')
        }
        return `${operateMap[operate]}${typeMap[type]}`
    }
    function getChangesByField (data, field) {
        if (data?.content?.changes?.[field]) {
            return data?.content?.changes?.[field]
        }
        return undefined
    }
    async function fetchPreviewData () {
        try {
            const res = await proxy.$store.dispatch('publicVar/getChangePreview', groupData.value)
            previewData.value = res.map(i => {
                const parseContent = JSON.parse(i.content)
                return {
                    ...i,
                    varName: parseContent.varName,
                    content: parseContent,
                    type: parseContent.type,
                    getChangesByField,
                    operateTitle: getOperateVarTitle(parseContent.operate, parseContent.type)
                }
            })

            if (res.length) {
                showPreView.value = true
            } else {
                showReleaseSlider.value = true
            }
        } catch (e) {
            console.error(e)
        }
    }
    function handleShowReleaseSlider () {
        showPreView.value = false
        showReleaseSlider.value = true
    }
    async function handlePreview () {
        if (releasing.value) return
        
        const isValid = await basicInfoRef.value?.validateForm()
        if (!isValid) return
        
        if (operateType.value === OPERATE_TYPE.CREATE) {
            showReleaseSlider.value = true
        } else {
            fetchPreviewData()
        }
    }
    function handleReleaseSuccess (groupName) {
        showReleaseSlider.value = false
        proxy.$emit('release-success', groupName)
    }

    function resetInitialGroupData () {
        initialGroupData.value = null
        shouldSaveInitialData.value = false
    }

    defineExpose({
        hasGroupDataChanged,
        resetInitialGroupData,
        init
    })
</script>

<style lang="scss">
    .param-group-sideslider {
        top: 155px !important;
        left: 390px !important;
        bottom: 24px !important;
        &.not-tips {
            top: 120px !important;
        }
        .bk-sideslider-content {
            max-height: none !important;
            height: calc(100% - 100px) !important;
        }
        .sideslider-content {
            height: calc(100% - 85px);
        }
        .sideslider-footer {
            padding: 0 20px;
        }
        .group-name {
            color: #979BA5;
        }
    }
    .view-variable-tabs {
        height: 52px !important;
        .bk-tab-header {
            height: 52px !important;
            background-image: none !important;
        }
    }
    .public-param-from-slider {
        .bk-sideslider-content {
            height: 100%;
            padding: 20px 24px;
        }
        .bk-sideslider-footer {
            padding: 0 24px;
        }
    }
</style>
