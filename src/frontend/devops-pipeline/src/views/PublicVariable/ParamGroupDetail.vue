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
                            hasPermission: isManage,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: 'project',
                                resourceCode: projectId,
                                action: PROJECT_RESOURCE_ACTION.MANAGE
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
        PROJECT_RESOURCE_ACTION
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
        saveSuccessFn: Function
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
    const published = ref(false)
    const showPreView = ref(false)
    const showReleaseSlider = ref(false)
    const previewData = ref([])
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
    const showAddParamSlider = ref(false)
    const releasing = ref(false)
    const newParamId = ref('') // 用于记录新增的变量，新增变量后高亮table当前行
    const editIndex = ref(-1)
    const paramType = ref('var')
    const publicVarType = ref(VARIABLE)
    const paramTitle = ref('')
    const sliderEditItem = ref({})
    const releaseDisabled = computed(() => {
        return !groupData.value?.groupName
    })
    const sidesliderWidth = computed(() => {
        // 250 表格第一列宽度
        // 100 表格第二列宽度
        // 20 padding样式
        return window.innerWidth - 250 - 120 - 20
    })
    watch(() => props.isShow, (val) => {
        if (!val) viewTab.value = 'basicInfo'
    })
    watch(() => groupData.value.groupName, (newVal, oldVal) => {
        if (!oldVal) return
        console.log('groupName')
    })
    function handleHidden () {
        proxy.$emit('update:isShow', false)
    }
    function handleBeforeClose () {
        return navConfirm(
            {
                content: proxy.$t('editPage.closeConfirmMsg'),
                type: 'warning',
                cancelText: proxy.$t('cancel')
            }
        )
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
                    type: publicVarType.value,
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
</script>

<style lang="scss">
    .param-group-sideslider {
        top: 155px !important;
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
