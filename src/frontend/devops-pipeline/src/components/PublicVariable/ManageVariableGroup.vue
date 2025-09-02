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
                    />
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
        </div>
    </bk-sideslider>
</template>

<script setup>
    import { ref, computed, watch } from 'vue'
    import UseInstance from '@/hook/useInstance'
    const { proxy } = UseInstance()
    const props = defineProps({
        isShow: {
            type: Boolean,
            default: false
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
    const projectId = computed(() => proxy.$route.params?.projectId)
    const pipelineId = computed(() => proxy.$route.params?.pipelineId)
    watch(() => showAddComp.value, (val) => {
        if (val) {
            fetchVarGroupList()
        } else {
            pagination.value.page = 1
            varGroupList.value = []
        }
    })
    watch(() => props.isShow, (val) => {
        if (val) fetchAllVarGroupByPipeline()
    })
    function hideSlider () {
        proxy.$emit('update:isShow', false)
    }
    function chooseGroup (id) {
        console.log(id, proxy.$route.params, 1111)
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
            varGroupList.value = res.records
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
    }
    function handelCancelAdd () {
        showAddComp.value = false
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
            height: 62px;
            margin-top: 20px;
            border: 1px solid #DCDEE5;
            border-radius: 2px;
            padding: 15px 24px;
            display: flex;
            align-items: center;
            font-size: 12px;
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
            .group-select {
                flex: 1;
                margin-right: 10px;
            }
            .bk-button-text {
                font-size: 12px;
            }
        }
    }
</style>