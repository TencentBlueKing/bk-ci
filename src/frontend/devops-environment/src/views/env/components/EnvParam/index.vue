<template>
    <div class="env-param-container">
        <bk-alert
            theme="info"
            :title="$t('environment.envVariableJobTips')"
            :closable="false"
            class="env-alert"
        />

        <div class="operation-area">
            <bk-button
                theme="primary"
                @click="handleAddVariable"
            >
                {{ $t('environment.addVariable') }}
            </bk-button>
            <search-select
                ref="searchSelect"
                class="search-input"
                v-model="searchValue"
                :data="searchList"
                clearable
                :show-condition="false"
                :placeholder="filterTips"
            />
        </div>
        <bk-table
            v-bkloading="{ isLoading }"
            :data="renderData"
            :max-height="tableMaxHeight"
            :pagination="pagination"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
        >
            <bk-table-column
                :label="$t('environment.variableName')"
                prop="name"
                min-width="120"
            />
            <bk-table-column
                :label="$t('environment.variableValue')"
                prop="value"
                min-width="120"
            >
                <template slot-scope="{ row }">
                    {{ row.secure ? '******' : row.value }}
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('environment.envInfo.type')"
                prop="secure"
                min-width="80"
            >
                <template slot-scope="{ row }">
                    {{ row.secure ? $t('environment.envInfo.cipherText') : $t('environment.envInfo.clearText') }}
                </template>
            </bk-table-column>

            <bk-table-column
                :label="$t('environment.operation')"
                fixed="right"
                min-width="120"
            >
                <template slot-scope="{ row, $index }">
                    <bk-button
                        text
                        class="mr5"
                        theme="primary"
                        @click="handleEdit(row)"
                    >
                        {{ $t('environment.edit') }}
                    </bk-button>
                    <bk-button
                        text
                        theme="primary"
                        @click="handleDelete($index)"
                    >
                        {{ $t('environment.delete') }}
                    </bk-button>
                </template>
            </bk-table-column>
        </bk-table>

        <bk-sideslider
            :is-show.sync="sliderConfig.isShow"
            :title="sliderConfig.title"
            :width="640"
            :quick-close="true"
            @hidden="handleSliderHidden"
        >
            <div
                slot="content"
                class="slider-content"
            >
                <bk-form
                    ref="envVarForm"
                    :model="formData"
                    :rules="formRules"
                    :label-width="80"
                >
                    <bk-form-item
                        :label="$t('environment.variableName')"
                        property="name"
                        required
                        error-display-type="normal"
                    >
                        <bk-input
                            v-model="formData.name"
                            :maxlength="50"
                        />
                    </bk-form-item>
                    
                    <bk-form-item
                        :label="$t('environment.variableValue')"
                        property="value"
                        required
                        error-display-type="normal"
                    >
                        <bk-input
                            v-model="formData.value"
                            :type="formData.secure ? 'password' : 'text'"
                            :maxlength="200"
                        />
                    </bk-form-item>
                    
                    <bk-form-item
                        :label="$t('environment.envInfo.type')"
                        property="secure"
                        required
                    >
                        <bk-radio-group
                            v-model="formData.secure"
                        >
                            <bk-radio
                                class="mr10"
                                :value="false"
                                :disabled="sliderConfig.isEdit"
                            >
                                {{ $t('environment.envInfo.clearText') }}
                            </bk-radio>
                            <bk-radio
                                :value="true"
                                :disabled="sliderConfig.isEdit"
                            >
                                {{ $t('environment.envInfo.cipherText') }}
                            </bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    
                    <bk-form-item>
                        <bk-button
                            theme="primary"
                            :loading="sliderConfig.isSubmitting"
                            @click="handleSubmit"
                        >
                            {{ $t('environment.confirm') }}
                        </bk-button>
                        <bk-button
                            @click="handleCancel"
                            :loading="sliderConfig.isSubmitting"
                        >
                            {{ $t('environment.cancel') }}
                        </bk-button>
                    </bk-form-item>
                </bk-form>
            </div>
        </bk-sideslider>
    </div>
</template>

<script>
    import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue'
    import useInstance from '@/hooks/useInstance'
    import usePagination from '@/hooks/usePagination'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    export default {
        name: 'EnvParam',
        components: {
            SearchSelect
        },
        setup () {
            const { proxy } = useInstance()
            const {
                pagination,
                resetPage,
                resetPagination,
                updateCount,
                handlePageChange,
                handlePageLimitChange
            } = usePagination()
            const {
                envHashId,
                envParamsList,
                fetchEnvDetail,
                updateEnvDetail
            } = useEnvDetail()
            const isLoading = ref(false)
            const curEnvDetail = ref({})
            const searchValue = ref([])
            const envVarForm = ref(null)
            const tableMaxHeight = ref(565)
            
            // 侧滑配置
            const sliderConfig = ref({
                isShow: false,
                isEdit: false,
                title: '',
                isSubmitting: false,
                editIndex: -1
            })
            
            // 表单数据
            const formData = ref({
                name: '',
                value: '',
                secure: false
            })
            
            // 表单校验规则
            const formRules = {
                name: [
                    {
                        required: true,
                        message: proxy.$t('environment.pleaseInputVariableName'),
                        trigger: 'blur'
                    }
                ],
                value: [
                    {
                        required: true,
                        message: proxy.$t('environment.pleaseInputVariableValue'),
                        trigger: 'blur'
                    }
                ]
            }
            
            const searchList = computed(() => {
                return [
                    {
                        name: proxy.$t('environment.variableName'),
                        id: 'name'
                    },
                    {
                        name: proxy.$t('environment.variableValue'),
                        id: 'value'
                    },
                    {
                        name: proxy.$t('environment.envInfo.type'),
                        id: 'type',
                        children: [
                            { id: 'string', name: proxy.$t('environment.envInfo.clearText') },
                            { id: 'password', name: proxy.$t('environment.envInfo.cipherText') }
                        ]
                    },
                    {
                        name: proxy.$t('environment.lastModifiedUser'),
                        id: 'lastModifiedUser'
                    }
                ]
            })
            watch(() => envHashId.value, (val) => {
                fetchData()
            })
            const filterTips = computed(() => {
                return searchList.value.map(item => item.name).join(' / ')
            })
            
            // 前端分页：根据 pagination 和 envParamsList 计算当前页显示的数据
            const renderData = computed(() => {
                const start = (pagination.value.current - 1) * pagination.value.limit
                const end = start + pagination.value.limit
                return envParamsList.value.slice(start, end)
            })
            const fetchData = async () => {
                try {
                    isLoading.value = true
                    const res = await fetchEnvDetail()
                    curEnvDetail.value = res
                    if (res?.envVars) {
                        updateCount(res?.envVars.length)
                    }
                } catch (e) {
                    console.error(e)
                } finally {
                    isLoading.value = false
                }
            }

            const handleDelete = (index) => {
                const confirmFn = async () => {
                    try {
                        // 计算真实的索引（考虑分页）
                        const realIndex = (pagination.value.current - 1) * pagination.value.limit + index
                        const params = {
                            ...curEnvDetail.value,
                            envVars: curEnvDetail.value.envVars.filter((item, i) => i !== realIndex)
                        }
                        await updateEnvDetail(params)
                        proxy.$bkMessage({
                            theme: 'success',
                            message: proxy.$t('environment.successfullyDeleted')
                        })
                        await fetchData()
                    } catch (e) {
                        console.error(e)
                        proxy.$bkMessage({
                            theme: 'error',
                            message: e.message
                        })
                    }
                }
                proxy.$bkInfo({
                    title: proxy.$t('environment.confirmDeleteEnvParams'),
                    confirmFn
                })
            }
            
            // 重置表单
            const resetForm = () => {
                formData.value = {
                    name: '',
                    value: '',
                    secure: false
                }
                if (envVarForm.value) {
                    envVarForm.value.clearError()
                }
            }
            
            // 添加变量
            const handleAddVariable = () => {
                resetForm()
                sliderConfig.value = {
                    isShow: true,
                    isEdit: false,
                    title: proxy.$t('environment.addVariable'),
                    isSubmitting: false,
                    editIndex: -1
                }
            }
            
            // 编辑变量
            const handleEdit = (row) => {
                resetForm()
                // 找到当前行在完整列表中的索引
                const realIndex = curEnvDetail.value.envVars.findIndex(item =>
                    item.name === row.name && item.value === row.value && item.secure === row.secure
                )
                formData.value = {
                    name: row.name,
                    value: row.value,
                    secure: row.secure
                }
                sliderConfig.value = {
                    isShow: true,
                    isEdit: true,
                    title: proxy.$t('environment.editVariable'),
                    isSubmitting: false,
                    editIndex: realIndex
                }
            }
            
            // 提交表单
            const handleSubmit = async () => {
                try {
                    await envVarForm.value.validate()
                    sliderConfig.value.isSubmitting = true
                    
                    const newEnvVars = [...(curEnvDetail.value.envVars || [])]
                    
                    if (sliderConfig.value.isEdit) {
                        // 编辑模式：更新指定索引的数据
                        newEnvVars[sliderConfig.value.editIndex] = {
                            name: formData.value.name,
                            value: formData.value.value,
                            secure: formData.value.secure
                        }
                    } else {
                        // 新增模式：添加到数组末尾
                        newEnvVars.push({
                            name: formData.value.name,
                            value: formData.value.value,
                            secure: formData.value.secure
                        })
                    }
                    
                    const params = {
                        ...curEnvDetail.value,
                        envVars: newEnvVars
                    }
                    
                    await updateEnvDetail(params)
                    proxy.$bkMessage({
                        theme: 'success',
                        message: sliderConfig.value.isEdit
                            ? proxy.$t('environment.successfullyModified')
                            : proxy.$t('environment.successfullyAdded')
                    })
                    
                    sliderConfig.value.isShow = false
                    await fetchData()
                } catch (e) {
                    console.error(e)
                } finally {
                    sliderConfig.value.isSubmitting = false
                }
            }
            
            const handleCancel = () => {
                sliderConfig.value.isShow = false
            }
            
            // 侧滑关闭
            const handleSliderHidden = () => {
                resetForm()
            }
            
            // 动态计算表格最大高度
            const calculateTableHeight = () => {
                // 获取容器高度
                const container = document.querySelector('.env-param-container')
                if (container) {
                    const containerHeight = container.clientHeight
                    // 减去头部高度（按钮和搜索框区域）和表格上边距
                    // 头部高度约 32px（按钮高度）+ 20px（margin-top）+ 52px (bk-alter-32px, margin-bottom-20px) = 104px
                    const headerHeight = 104
                    const calculatedHeight = containerHeight - headerHeight
                    // 确保计算出的高度大于最小值
                    if (calculatedHeight > 200) {
                        tableMaxHeight.value = calculatedHeight
                    }
                }
            }
            
            // 窗口大小变化时重新计算
            const handleResize = () => {
                calculateTableHeight()
            }
            
            onMounted(() => {
                fetchData()
                calculateTableHeight()
                window.addEventListener('resize', handleResize)
            })
            
            onBeforeUnmount(() => {
                window.removeEventListener('resize', handleResize)
            })
            return {
                // data
                isLoading,
                searchValue,
                searchList,
                filterTips,
                pagination,
                envParamsList,
                envHashId,
                renderData,
                envVarForm,
                sliderConfig,
                formData,
                formRules,
                tableMaxHeight,
                
                // function
                resetPage,
                resetPagination,
                handlePageChange,
                handlePageLimitChange,
                handleAddVariable,
                handleEdit,
                handleDelete,
                handleSubmit,
                handleCancel,
                handleSliderHidden
            }
        }
        
    }
</script>

<style lang="scss" scoped>
.env-param-container {
    height: calc(100% - 90px);
    .search-input {
        width: 480px;
        background: white;
        ::placeholder {
            color: #c4c6cc;
        }
    }
    .env-alert {
        margin-bottom: 20px;
    }

    .operation-area {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;

        .search-select {
            width: 400px;
        }
    }
}

.slider-content {
    padding: 20px 40px;
}
</style>
