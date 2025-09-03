<template>
    <bk-sideslider
        :is-show.sync="isShow"
        ext-cls="manage-var-group-slider"
        width="800"
        :title="$t('publicVar.manageGroup')"
        :quick-close="false"
        @hidden="hideSlider"
    >
        <div
            slot="content"
            class="var-group-wrapper"
        >
            <bk-button
                icon="plus"
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
                            v-for="item in varGroupList"
                            :key="item.groupName"
                            :id="item.groupName"
                            :name="item.groupName"
                            :disabled="item.disabled"
                        >
                            {{ item.groupName }}
                            <span class="manage-var-page-group-desc">{{ item.desc }}</span>
                        </bk-option>
                    </bk-select>
                    <bk-button
                        class="mr10"
                        text
                        @click="handleConfirmAdd"
                    >
                        {{ $t('publicVar.append') }}
                    </bk-button>
                    <bk-button
                        text
                        @click="handelCancelAdd"
                    >
                        {{ $t('cancel') }}
                    </bk-button>
                </div>
                <div v-if="selectedVarGroupData.groupName">
                    <div class="group-desc">
                        {{ selectedVarGroupData.desc }}
                    </div>
                    <div
                        class="variable-list"
                        v-for="data in renderVariableList"
                        :key="data.key"
                    >
                        <variable-table
                            :data="data"
                        />
                    </div>
                </div>
                <render-var-group
                    class="mt10"
                />
            </div>
        </div>
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
        handleSaveVariableByGroup: {
            type: Function,
            default: () => () => {}
        }
    })
    const showAddComp = ref(false)
    const varGroupList = ref([]) // 公共变量组
    const allVarGroup = ref([]) // 项目下使用的变量组
    const pagination = ref({
        page: 1,
        pageSize: 20,
        loadEnd: false
    })
    const selectedVarGroupData = ref({})
    const variablesList = ref([])
    const newGroups = ref({})
    const projectId = computed(() => proxy.$route.params?.projectId)
    const pipelineId = computed(() => proxy.$route.params?.pipelineId)
    const publicVarGroups = computed(() => proxy.$store?.state?.atom?.pipeline?.publicVarGroups)
    const renderVariableList = computed(() => {
        const requiredParam = variablesList.value.filter(i => i.type === VARIABLE && i.buildFormProperty.required)
        const otherParam = variablesList.value.filter(i => i.type === VARIABLE && !i.buildFormProperty.required)
        const constantParam = variablesList.value.filter(i => i.type === CONSTANT)

        return [
            ...(
                requiredParam.length ? [
                    {
                        title: proxy.$t('newui.pipelineParam.buildParam'),
                        data: requiredParam,
                        key: VARIABLE
                    }
                ] : []
            ),
            ...(
                otherParam.length ? [
                    {
                        title: proxy.$t('newui.pipelineParam.otherVar'),
                        data: otherParam,
                        key: VARIABLE
                    }
                ] : []
            ),
            ...(
                constantParam.length ? [
                    {
                        title: proxy.$t('publicVar.constant'),
                        data: constantParam,
                        key: CONSTANT
                    }
                ] : []
            )
        ]
    })
    watch(() => showAddComp.value, (val) => {
        if (val) {
            fetchVarGroupList()
        } else {
            pagination.value.page = 1
            pagination.value.loadEnd = false
            selectedVarGroupData.value = {}
            variablesList.value = []
            varGroupList.value = []
        }
    })
    watch(() => props.isShow, (val) => {
        if (val) {
            fetchAllVarGroupByPipeline()
        } else {
            pagination.value.page = 1
            pagination.value.loadEnd = false
            selectedVarGroupData.value = {}
            variablesList.value = []
            varGroupList.value = []
            showAddComp.value = false
        }
    })
    function hideSlider () {
        proxy.$emit('update:isShow', false)
    }
    async function chooseGroup (id) {
        selectedVarGroupData.value = varGroupList.value.find(i => i.groupName === id)
        newGroups.value = {
            groupName: id
        }
        const res = await proxy.$store.dispatch('publicVar/getVariables', {
            groupName: id
        })
        variablesList.value = res
    }
    async function fetchAllVarGroupByPipeline () {
        try {
            const res = await proxy.$store.dispatch('publicVar/fetchAllVariableGroupByPipeline', {
                pipelineId: pipelineId.value,
                referType: 'PIPELINE'
            })
            allVarGroup.value = res
            console.log(res, 123)
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
            varGroupList.value = res.records.map(i => ({
                ...i,
                disabled: publicVarGroups.value?.findIndex(group => group.groupName === i.groupName) > -1
            }))
            pagination.value.loadEnd = res.page === res.totalPages
            pagination.value.page = res.page + 1
        } catch (e) {
            console.error(e, 'fetchVarGroupList')
        }
    }
    function handleAddGroup () {
        // todo
        showAddComp.value = true
    }
    function handleConfirmAdd () {
        showAddComp.value = false
        try {
            proxy.$store.dispatch('atom/setPipelineEditing', true)
            proxy.$store.dispatch('atom/updatePipelinePublicVarGroups', [
                ...publicVarGroups.value,
                newGroups.value
            ])
            props.handleSaveVariableByGroup(variablesList.value.map(i => ({
                ...i.buildFormProperty
            })))
        } catch (e) {
            console.error(e)
        }
    }
    function handelCancelAdd () {
        showAddComp.value = false
        newGroups.value = {}
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
    .manage-var-page-group-desc {
        color: #979BA5 !important;
        padding-left: 8px;
    }
</style>