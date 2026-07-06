<template>
    <div class="test-info-container">
        <div
            v-bkloading="{ isLoading: isLoading }"
            class="test-info-wrapper"
        >
            <div class="test-content">
                <progress-info
                    :list="tagTestList"
                    :label-text="$t('store.调试云桌面')"
                    :tooltips="$t('store.添加一个或多个云桌面，用于测试当前应用。')"
                    :alert-title="$t('store.在调试云桌面内，测试应用功能，测试通过后回到此页面继续发布流程')"
                    :is-edit="isEdit"
                    :current-step="currentStep"
                    @add="handleAdd"
                    @goStep="goStep"
                >
                    <template #default>
                        <bk-tag
                            v-for="project in tagTestList"
                            :key="project.instanceId"
                            closable
                            @close="removeTag(project.instanceId)"
                            class="test-tag"
                        >
                            {{ project.instanceName }}
                        </bk-tag>
                    </template>
                </progress-info>
            </div>
            <div
                v-if="isEdit"
                class="edit-mask"
            ></div>
        </div>

        <bk-dialog
            v-model="addDialog"
            :title="$t('store.添加调试云桌面')"
            quick-close
            header-position="left"
            @confirm="handleConfirm"
            @closed="handleCancel"
        >
            <div>
                <p class="dialog-label">{{ $t('store.云桌面') }}</p>
                <bk-select
                    v-model="selectTestList"
                    multiple
                >
                    <bk-option
                        v-for="option in allTestList"
                        :key="option.workspaceName"
                        :id="option.workspaceName"
                        :name="option.displayName || option.workspaceName"
                    />
                </bk-select>
            </div>
        </bk-dialog>
    </div>
</template>

<script setup name="TestInfoProgress">
    import { ref, computed, onMounted } from 'vue'
    import UseInstance from '@/hook/useInstance.js'
    import ProgressInfo from '@/components/progress/progressInfo.vue'

    const props = defineProps({
        appDetail: {
            type: Object,
            required: true
        },
        currentStep: {
            type: Object,
            required: true
        },
        isEdit: {
            type: Boolean,
            required: true
        }
    })

    const emit = defineEmits(['goStep'])

    const { proxy } = UseInstance()
    const { $store, $route } = proxy

    const storeCode = computed(() => $route.params.storeCode)
    const storeId = computed(() => $route.params.storeId)

    const tagTestList = ref([])
    const allTestList = ref([])
    const selectTestList = ref([])
    const addDialog = ref(false)
    const isLoading = ref(false)

    onMounted(() => {
        fetchTagTestList()
    })

    async function fetchTagTestList () {
        try {
            isLoading.value = true
            const [testInfoResult, workspacesResult] = await Promise.all([
                $store.dispatch('store/testInfoGet', storeCode.value),
                $store.dispatch('store/workspacesSearch')
            ])

            allTestList.value = workspacesResult.records || []
            tagTestList.value = (testInfoResult || []).map(item => ({
                ...item,
                instanceName: item.instanceName || item.instanceId
            }))
        } catch (error) {
            console.log(error)
            allTestList.value = []
            tagTestList.value = []
        } finally {
            isLoading.value = false
        }
    }

    async function removeTag (instanceId) {
        const tagTestParam = tagTestList.value.filter(item => item.instanceId !== instanceId)
        const flag = await saveInfo(tagTestParam)
        if (flag) {
            tagTestList.value = tagTestParam
        }
    }

    async function goStep (step, status) {
        try {
            if (status === 'next') {
                await $store.dispatch('store/passComponent', storeId.value)
            }
            emit('goStep', step, status)
        } catch (error) {
            console.log(error)
        }
    }

    function handleAdd () {
        if (!props.isEdit) {
            addDialog.value = true
            const allTestIds = new Set(allTestList.value.map(item => item.workspaceName))
            selectTestList.value = tagTestList.value.map(item => item.instanceId).filter(id => allTestIds.has(id))
        }
    }

    async function saveInfo (tagTestParam) {
        try {
            const result = tagTestParam.map(({ projectName, ...rest }) => rest)
            const params = { testItems: result }
            return await $store.dispatch('store/testInfoSave', {
                storeCode: storeCode.value,
                params
            })
        } catch (error) {
            console.log(error)
            return false
        }
    }

    async function handleConfirm () {
        const selectedIds = new Set(selectTestList.value)
        tagTestList.value.forEach(tag => {
            const matchedTest = allTestList.value.find(test => test.workspaceName === tag.instanceId)
            if (matchedTest) {
                tag.instanceName = matchedTest.displayName
            }
        })
        tagTestList.value = tagTestList.value.filter(tag => {
            const isInAllTestList = allTestList.value.some(test => test.workspaceName === tag.instanceId)
            const isInSelect = selectedIds.has(tag.instanceId)
            // 如果项目不在 allTestList 中，或者在 selectTestList 中时选中状态时保留
            return !isInAllTestList || isInSelect
        })
        
        allTestList.value.forEach(test => {
            // 仅当项目被选择且当前 tagTestList 中不包含时添加
            if (selectedIds.has(test.workspaceName) && !tagTestList.value.some(tag => tag.instanceId === test.workspaceName)) {
                tagTestList.value.push({
                    projectCode: test.projectId,
                    instanceId: test.workspaceName,
                    instanceName: test.displayName
                })
            }
        })
        const flag = await saveInfo(tagTestList.value)
        if (flag) {
            addDialog.value = false
        }
    }

    function handleCancel () {
        addDialog.value = false
    }
</script>

<style lang="scss" scoped>
.test-info-container {
    height: 100%;
    position: relative;

    .test-info-wrapper {
        height: 100%;
        position: relative;
    }

    .test-content {
        height: 100%;
        overflow-y: auto;
    }

    .edit-mask {
        width: 100%;
        height: 100%;
        position: absolute;
        inset: 0;
        cursor: not-allowed;
    }

    .test-tag {
        font-size: 14px;
        color: #222;
        height: 32px;
        border-radius: 2px;
    }
}

.dialog-label {
    margin-bottom: 10px;
    text-align: left;
}
</style>
