<template>
    <bk-sideslider
        :is-show.sync="isShow"
        ext-cls="manage-var-group-slider"
        width="800"
        :title="$t('publicVar.manageGroup')"
        :quick-close="false"
        :before-close="beforeHiddenFn"
        @hidden="hideSlider"
    >
        <div
            slot="content"
            class="var-group-wrapper"
        >
            <template v-if="editable">
                <bk-button
                    icon="plus"
                    :disabled="showAddComp"
                    @click="handleAddGroup"
                >
                    {{ $t('publicVar.addVarGroup') }}
                </bk-button>
                <div
                    v-if="showAddComp"
                    class="group-select-wrapper"
                >
                    <div class="header-top">
                        <span
                            class="label"
                            :title="$t('publicVar.varGroup')"
                        >
                            {{ $t('publicVar.varGroup') }}
                        </span>
                        <bk-select
                            class="group-select"
                            @selected="chooseGroup"
                            @scroll-end="fetchVarGroupList"
                        >
                            <bk-option
                                v-for="item in renderVarGroupList"
                                :key="item.groupName"
                                :id="item.groupName"
                                :name="item.groupName"
                                :disabled="item.disabled"
                            >
                                <div class="manage-var-page-group-option">
                                    <span class="group-name">{{ item.groupName }}</span>
                                    <span
                                        v-bk-overflow-tips
                                        class="manage-var-page-group-desc"
                                    >
                                        {{ item.desc }}
                                    </span>
                                </div>
                            </bk-option>
                        </bk-select>
                        <bk-button
                            class="mr10"
                            text
                            :disabled="!selectedVarGroupData.groupName"
                            @click="handleAppend"
                        >
                            {{ $t('publicVar.append') }}
                        </bk-button>
                        <bk-button
                            text
                            @click="handelCancelAppend"
                        >
                            {{ $t('cancel') }}
                        </bk-button>
                    </div>
                    <div v-if="selectedVarGroupData.groupName">
                        <div
                            v-bk-overflow-tips
                            class="group-desc"
                        >
                            {{ selectedVarGroupData.desc }}
                        </div>
                        <div
                            class="variable-list"
                            v-for="data in renderSelectedVariableList"
                            :key="data.key"
                        >
                            <variable-table
                                :data="data"
                            />
                        </div>
                    </div>
                </div>
            </template>
            <render-var-group
                v-for="(data, index) in allProjectVarGroup"
                :key="data.groupName"
                class="mt10"
                :data="data"
                :index="index"
                :editable="editable"
                :global-params="globalParams"
                @delete="handleDeleteVarGroup"
                @updateData="handleUpdataVarGroup"
            />
        </div>
        <footer
            v-if="editable"
            slot="footer"
            class="var-group-footer"
        >
            <bk-button
                theme="primary"
                :disabled="showAddComp"
                @click="handleConfirmAdd"
            >
                {{ $t('confirm') }}
            </bk-button>
            <bk-button
                @click="beforeHiddenFn"
            >
                {{ $t('cancel') }}
            </bk-button>
        </footer>
    </bk-sideslider>
</template>

<script setup>
    import { ref, computed, watch } from 'vue'
    import {
        VARIABLE,
        CONSTANT
    } from '@/store/modules/publicVar/constants'
    import UseInstance from '@/hook/useInstance'
    import RenderVarGroup from './RenderVarGroup'
    import variableTable from './variableTable'

    const { proxy } = UseInstance()
    const props = defineProps({
        isShow: {
            type: Boolean,
            default: false
        },
        saveVariable: {
            type: Function,
            default: () => () => {}
        },
        globalParams: {
            type: Array,
            default: () => []
        },
        groupName: {
            type: String,
            default: ''
        },
        editable: {
            type: Boolean,
            default: true
        }
    })
    const showAddComp = ref(false)
    const varGroupList = ref([]) // 公共变量组,用于下拉选择数据源
    const allProjectVarGroup = ref([]) // 项目下使用的变量组
    const pagination = ref({
        page: 1,
        pageSize: 100,
        loadEnd: false
    })

    const newGroups = ref({})
    const selectedVarGroupData = ref({})
    const selectedVariableList = ref([])

    const groupsMap = ref({
        // 用于存储新增的变量组数据
        varGroups: [],
        variableList: []
    })

    const projectId = computed(() => proxy.$route.params?.projectId)
    const pipelineId = computed(() => proxy.$route.params?.pipelineId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    // const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)
    const publicVarGroups = computed(() => proxy.$store?.state?.atom?.pipeline?.publicVarGroups || [])
    const allExistingParams = computed(() => {
        // 获取被删除的变量组名称列表
        const deletedGroupNames = publicVarGroups.value
            .filter(group => !groupsMap.value.varGroups.some(g => g.groupName === group.groupName))
            .map(group => group.groupName)
        
        // 从 globalParams 中排除属于已删除变量组的参数
        const filteredGlobalParams = props.globalParams.filter(param =>
            !deletedGroupNames.includes(param.varGroupName)
        )
        
        // 合并过滤后的全局参数和待保存的参数列表，用于检测重复
        return [...filteredGlobalParams, ...groupsMap.value.variableList]
    })
    const renderSelectedVariableList = computed(() => {
        // 新增变量组-选中变量组对应的变量
        const requiredParam = selectedVariableList.value.filter(i => i.type === VARIABLE && i.buildFormProperty.required)
        const otherParam = selectedVariableList.value.filter(i => i.type === VARIABLE && !i.buildFormProperty.required)
        const constantParam = selectedVariableList.value.filter(i => i.type === CONSTANT)
        return [
            ...(
                requiredParam.length ? [
                    {
                        title: proxy.$t('newui.pipelineParam.buildParam'),
                        data: requiredParam,
                        key: VARIABLE,
                        repeatParamTips: getRepeatTips(requiredParam)
                    }
                ] : []
            ),
            ...(
                otherParam.length ? [
                    {
                        title: proxy.$t('newui.pipelineParam.otherVar'),
                        data: otherParam,
                        key: VARIABLE,
                        repeatParamTips: getRepeatTips(otherParam)
                    }
                ] : []
            ),
            ...(
                constantParam.length ? [
                    {
                        title: proxy.$t('publicVar.constant'),
                        data: constantParam,
                        key: CONSTANT,
                        repeatParamTips: getRepeatTips(constantParam)
                    }
                ] : []
            )
        ]
    })
    const renderVarGroupList = computed(() => {
        return varGroupList.value.map(i => ({
            ...i,
            disabled: allProjectVarGroup.value.some(group => group.groupName === i.groupName) || groupsMap.value.varGroups.some(group => group.groupName === i.groupName)
        }))
    })
    watch(() => showAddComp.value, (val) => {
        if (val) {
            fetchVarGroupList()
        } else {
            pagination.value.page = 1
            pagination.value.loadEnd = false
            selectedVarGroupData.value = {}
            selectedVariableList.value = []
            varGroupList.value = []
        }
    })
    watch(() => props.isShow, async (val) => {
        if (val) {
            await fetchVarGroupList()
            groupsMap.value.varGroups = JSON.parse(JSON.stringify(publicVarGroups.value)).map(i => {
                return {
                    ...i,
                    desc: varGroupList.value.find(group => group.groupName === i.groupName)?.desc || ''
                }
            })
            groupsMap.value.variableList = props.globalParams
            await fetchAllVarGroupByGroupName()
        } else {
            pagination.value.page = 1
            pagination.value.loadEnd = false
            selectedVarGroupData.value = {}
            selectedVariableList.value = []
            varGroupList.value = []
            showAddComp.value = false
        }
    })
    function varGroupsEqualByGroupName (refArray, compArray) {
        const referenceSet = new Set(refArray.map(item => item.groupName))
        const comparisonSet = new Set(compArray.map(item => item.groupName))
  
        for (const groupName of comparisonSet) {
            if (!referenceSet.has(groupName)) {
                return true
            }
        }

        for (const groupName of referenceSet) {
            if (!comparisonSet.has(groupName)) {
                return true
            }
        }

        return false
    }
    function beforeHiddenFn () {
        if (varGroupsEqualByGroupName(publicVarGroups.value, groupsMap.value.varGroups)) {
            proxy.$bkInfo({
                title: proxy.$t('确认离开当前页？'),
                subHeader: proxy.$createElement('p', {
                    style: {
                        color: '#63656e',
                        fontSize: '14px',
                        textAlign: 'center'
                    }
                }, proxy.$t('离开将会导致未保存信息丢失')),
                okText: proxy.$t('离开'),
                confirmFn: () => {
                    hideSlider()
                }
            })
        } else {
            hideSlider()
        }
    }
    async function chooseGroup (id) {
        selectedVarGroupData.value = varGroupList.value.find(i => i.groupName === id)
        newGroups.value = {
            groupName: id
        }
        const res = await proxy.$store.dispatch('publicVar/getResourceVarReferenceInfo',  {
            referId: pipelineId.value ?? templateId.value,
            params: {
                referType: proxy.$route.name === 'pipelinesEdit' ? 'PIPELINE' : 'TEMPLATE',
                referVersion: proxy.$route.params.version || proxy.$store.state.atom?.pipelineInfo?.version,
                groupName: id
            }
        })
        selectedVariableList.value = res.map(i => ({
            ...i,
            isRepeat: !!(allExistingParams.value.find(param => param.id === i.buildFormProperty.id))
        }))
    }
    async function fetchAllVarGroupByGroupName () {
        try {
            const list = groupsMap.value.varGroups.map(i => {
                return {
                    ...i,
                    isDeleted: !varGroupList.value.find(group => group.groupName === i.groupName),
                    isRepeat: !!(allExistingParams.value.find(param => param?.id === i?.buildFormProperty?.id))
                }
            })
            allProjectVarGroup.value = list.map((data, index) => ({
                ...data,
                variableList: [],
                isOpen: props.groupName ? data.groupName === props.groupName : index === 0,
                isRequested: false
            }))
        } catch (e) {
            console.error(e)
        }
    }
    async function fetchVarGroupList () {
        try {
            if (pagination.value.loadEnd) return
            const res = await proxy.$store.dispatch('publicVar/fetchVariableGroup', {
                projectId: projectId.value,
                params: {
                    page: pagination.value.page,
                    pageSize: pagination.value.pageSize,
                }
            })
            varGroupList.value = res.records
            pagination.value.loadEnd = res.page === res.totalPages
            pagination.value.page = res.page + 1
        } catch (e) {
            console.error(e, 'fetchVarGroupList')
        }
    }
    function handleUpdataVarGroup (payload) {
        const { index, data } = payload
        allProjectVarGroup.value = allProjectVarGroup.value.map((group, idx) => {
            if (idx === index) {
                return {
                    ...group,
                    ...data
                }
            }
            return group
        })
    }
    function handleDeleteVarGroup (groupName) {
        const curVariableList = groupsMap.value.variableList.filter(group => group?.varGroupName === groupName)
        const ids = new Set(curVariableList.map(item => item.id))
        groupsMap.value.varGroups = groupsMap.value.varGroups.filter(group => group.groupName !== groupName)
        groupsMap.value.variableList = groupsMap.value.variableList.filter(i => !(ids.has(i.id) && i.varGroupName === groupName))
        allProjectVarGroup.value = allProjectVarGroup.value.filter(group => group.groupName !== groupName)
    }
    function handleAddGroup () {
        showAddComp.value = true
    }
    function handleAppend () {
        // 检查是否有重复参数
        const hasRepeat = selectedVariableList.value.some(item => item.isRepeat === true)
        if (hasRepeat) {
            proxy.$bkMessage({
                theme: 'error',
                message: proxy.$t('publicVar.repeatTips')
            })
            return
        }
        
        showAddComp.value = false
        try {
            groupsMap.value.varGroups.push(newGroups.value)
            groupsMap.value.variableList = [...groupsMap.value.variableList, ...selectedVariableList.value.map(i => ({
                ...i.buildFormProperty
            }))]

            const newVarGroupData = varGroupList.value.find(i => i.groupName === newGroups.value.groupName)
            allProjectVarGroup.value.push({
                ...newVarGroupData,
                variableList: selectedVariableList.value,
                isOpen: true,
                isRequested: true
            })
        } catch (e) {
            console.error(e)
        }
    }
    function handelCancelAppend () {
        showAddComp.value = false
        newGroups.value = {}
    }
    function handleConfirmAdd () {
        proxy.$store.dispatch('atom/setPipelineEditing', true)
        proxy.$store.dispatch('atom/updatePipelinePublicVarGroups', groupsMap.value.varGroups)
        props.saveVariable(groupsMap.value.variableList)
        proxy.$emit('update:isShow', false)
    }
    function hideSlider () {
        proxy.$emit('update:isShow', false)
    }

    function getRepeatTips (list = []) {
        const isRepeatParamId = list?.find(param => param.isRepeat)?.buildFormProperty?.id || ''
        const repeatParam = allExistingParams.value.find(param => param.id === isRepeatParamId)
        if (repeatParam) {
            const isRequiredParam = repeatParam.required
            const isConstantParam = repeatParam.constant
            const category = repeatParam.category ? repeatParam.category : proxy.$t('notGrouped')
            let tips = ''
            if (isConstantParam) {
                tips = proxy.$t('publicVar.repeatParamTips', [`${proxy.$t('newui.pipelineParam.constParam')}-${category}`])
            } else {
                if (isRequiredParam) {
                    tips = proxy.$t('publicVar.repeatParamTips', [`${proxy.$t('newui.pipelineParam.buildParam')}-${category}`])
                } else {
                    tips = proxy.$t('publicVar.repeatParamTips', [`${proxy.$t('newui.pipelineParam.otherVar')}-${category}`])
                }
            }
            
            return repeatParam.varGroupName ? proxy.$t('publicVar.repeatParamTips', [proxy.$t('publicVar.publicVarGroup', [repeatParam.varGroupName])])
                : tips
        }
        return ''
    }
</script>

<style lang="scss">
    .manage-var-group-slider {
        .bk-sideslider-content {
            height: calc(100vh - 60px) !important;
        }
        .var-group-wrapper {
            height: 100%;
            padding: 24px;
        }
        .var-group-footer {
            padding-left: 24px;
        }
        .group-select-wrapper {
            width: 100%;
            margin-top: 20px;
            border: 1px solid #DCDEE5;
            border-radius: 2px;
            padding: 15px 24px;
            align-items: center;
            font-size: 12px;
            .header-top {
                display: flex;
                align-items: center;
            }
            .label {
                min-width: 60px;
                max-width: 100px;
                overflow: hidden;
                text-overflow: ellipsis;
                height: 32px;
                line-height: 32px;
                font-size: 12px;
                text-align: center;
                background: #FAFBFD;
                border: 1px solid #C4C6CC;
                border-radius: 2px 0 0 2px;
                border-right: none;
            }
            .bk-button-text {
                font-size: 12px;
            }
        }
        .group-select {
            flex: 1;
            margin-right: 10px;
        }
        .group-desc {
            width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            margin: 8px 0;
            color: #979BA5;
        }
        .variable-list {
            margin-bottom: 16px;
            &:last-child {
                margin-bottom: 0;
            }
        }
    }
    .manage-var-page-group-option {
        display: flex;
        align-items: center;
        width: 100%;
        .group-name {
            flex-shrink: 0;
        }
        .manage-var-page-group-desc {
            color: #979BA5 !important;
            padding-left: 8px;
            flex: 1;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
    }
</style>