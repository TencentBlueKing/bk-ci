<template>
    <bk-sideslider
        :is-show.sync="props.isShow"
        :width="640"
        :title="$t('store.注册应用')"
        quick-close
        :before-close="handleClose"
    >
        <div slot="content">
            <bk-form
                ref="formRef"
                :model="application"
                :label-width="110"
                class="operation-app-form"
            >
                <bk-form-item
                    :label="$t('store.应用名称')"
                    property="baseInfo.name"
                    :rules="rules.name"
                    :required="true"
                    error-display-type="normal"
                >
                    <bk-input
                        v-model="application.baseInfo.name"
                        clearable
                    />
                </bk-form-item>

                <bk-form-item
                    :label="$t('store.标识')"
                    property="baseInfo.storeCode"
                    :rules="rules.storeCode"
                    :required="true"
                    error-display-type="normal"
                >
                    <bk-input
                        v-model="application.baseInfo.storeCode"
                        clearable
                    />
                </bk-form-item>

                <bk-form-item
                    :label="$t('store.应用类型')"
                    property="baseInfo.baseFeatureInfo.type"
                    :required="true"
                >
                    <bk-radio-group v-model="application.baseInfo.baseFeatureInfo.type">
                        <bk-radio-button value="LOCAL">
                            {{ $t('store.本地应用') }}
                        </bk-radio-button>
                        <bk-radio-button value="AI">
                            {{ $t('store.AI应用') }}
                        </bk-radio-button>
                    </bk-radio-group>
                </bk-form-item>

                <bk-form-item
                    :label="$t('store.应用来源')"
                    property="baseInfo.baseFeatureInfo.extBaseFeatureInfo.sourceType"
                    :required="true"
                >
                    <bk-radio-group v-model="application.baseInfo.baseFeatureInfo.extBaseFeatureInfo.sourceType">
                        <bk-radio-button value="SELF_UPLOAD">
                            {{ $t('store.自行上传') }}
                        </bk-radio-button>
                        <bk-radio-button value="OFFICIAL_HOSTING">
                            {{ $t('store.官方CI托管') }}
                        </bk-radio-button>
                    </bk-radio-group>
                </bk-form-item>

                <template v-if="application.baseInfo.baseFeatureInfo.extBaseFeatureInfo.sourceType === 'OFFICIAL_HOSTING'">
                    <bk-form-item
                        :label="$t('store.模板应用')"
                        property="baseInfo.baseFeatureInfo.extBaseFeatureInfo.frameworkCode"
                        :required="true"
                    >
                        <div class="framework-tag">
                            {{ $t('store.蓝盾开发框架') }}
                        </div>
                        <bk-radio-group
                            v-model="application.baseInfo.baseFeatureInfo.extBaseFeatureInfo.frameworkCode"
                            class="framework-radio-group"
                        >
                            <bk-radio
                                value="NODEJS_FRAMEWORK"
                                class="framework-radio"
                            >
                                <div class="framework-radio-content">
                                    <span>{{ $t('store.NodeJS开发框架') }}</span>
                                    <i class="devops-icon icon-info-circle info-icon" />
                                    <span class="info-text">
                                        {{ $t('store.自动初始化代码库，并提供开发框架的脚手架代码') }}
                                    </span>
                                </div>
                            </bk-radio>
                            <bk-radio
                                value="CUSTOM_FRAMEWORK"
                                class="framework-radio"
                            >
                                <div class="framework-radio-content">
                                    <span>{{ $t('store.自主框架') }}</span>
                                    <i class="devops-icon icon-info-circle info-icon" />
                                    <span class="info-text">
                                        {{ $t('store.使用自己的个人框架自由发挥，需手动选择代码库地址。项目下需给平台账号 devops 开通开发者权限，用于自动化构建流程。') }}
                                    </span>
                                </div>
                            </bk-radio>
                        </bk-radio-group>
                    </bk-form-item>

                    <bk-form-item
                        :label="$t('store.授权方式')"
                        property="baseInfo.baseFeatureInfo.extBaseFeatureInfo.authType"
                        :required="true"
                    >
                        <bk-radio
                            v-model="application.baseInfo.baseFeatureInfo.extBaseFeatureInfo.authType"
                            :checked="true"
                            value="OAUTH"
                        >
                            {{ $t('store.工蜂OAUTH') }}
                        </bk-radio>
                    </bk-form-item>

                    <bk-form-item
                        :label="$t('store.代码源')"
                        property="baseInfo.baseFeatureInfo.extBaseFeatureInfo.codeSource"
                        :required="true"
                    >
                        <div class="framework-tag">
                            {{ $t('store.腾讯工蜂服务') }}
                        </div>
                    </bk-form-item>

                    <bk-form-item
                        v-if="application.baseInfo.baseFeatureInfo.extBaseFeatureInfo.frameworkCode === 'CUSTOM_FRAMEWORK'"
                        :label="$t('store.代码仓库')"
                        property="baseInfo.baseFeatureInfo.extBaseFeatureInfo.repositoryHttpUrl"
                        :required="true"
                    >
                        <bk-select
                            v-model="application.baseInfo.baseFeatureInfo.extBaseFeatureInfo.repositoryHttpUrl"
                            :loading="repositoryLoading"
                            filterable
                            searchable
                            @search-change="handleRepositorySearch"
                        >
                            <bk-option
                                v-for="item in repositoryList"
                                :key="item.id"
                                :id="item.httpUrl"
                                :name="item.httpUrl"
                            />
                        </bk-select>
                    </bk-form-item>

                    <bk-form-item
                        :label="$t('store.构建目录')"
                        property="baseInfo.baseFeatureInfo.extBaseFeatureInfo.buildDir"
                    >
                        <bk-input
                            v-model="application.baseInfo.baseFeatureInfo.extBaseFeatureInfo.buildDir"
                            clearable
                            :placeholder="$t('store.请输入应用所在的子目录，默认为根目录')"
                        />
                    </bk-form-item>
                </template>

                <bk-form-item>
                    <bk-button
                        theme="primary"
                        class="w88 mr10"
                        :loading="isSubmiting"
                        :disabled="isSubmiting"
                        @click="handleConfirm"
                    >
                        {{ $t('store.提交') }}
                    </bk-button>
                    <bk-button
                        class="w88"
                        :loading="isSubmiting"
                        :disabled="isSubmiting"
                        @click="handleClose"
                    >
                        {{ $t('store.取消') }}
                    </bk-button>
                </bk-form-item>
            </bk-form>
        </div>
    </bk-sideslider>
</template>

<script setup>
    import { ref, reactive, watch } from 'vue'
    import { STORE_TYPE } from '@/utils/constants'
    import leaveConfirm from '@/utils/leave-confirm'
    import UseInstance from '@/hook/useInstance'

    const { proxy } = UseInstance()
    const { $store, $bkMessage, $bkInfo, $t } = proxy

    const props = defineProps({
        isShow: {
            type: Boolean,
            default: false
        }
    })

    const emit = defineEmits(['confirm', 'cancel'])

    const formRef = ref(null)
    const isModified = ref(false)
    const isSubmiting = ref(false)
    const repositoryLoading = ref(false)
    const repositoryList = ref([])

    // 应用表单数据
    const application = reactive(initForm())

    // 表单验证规则
    const rules = {
        storeCode: [{
            required: true,
            validator: (val) => /^[a-z_][a-z\d_]{0,31}$/i.test(val),
            message: $t('store.标识只能由大小写字母、数字和下划线组成，首字母必须为字母或者下划线，长度在1-32之间'),
            trigger: 'blur'
        }],
        name: [{
            required: true,
            validator: (val) => /^[\u4e00-\u9fa5a-zA-Z0-9\-_.]{1,40}$/.test(val),
            message: $t('store.由汉字、英文字母、数字、连字符(-)、下划线(_)或点(.)组成，不超过40个字符'),
            trigger: 'blur'
        }]
    }

    // 监听 props.isShow 的变化来控制侧边栏显示
    watch(() => props.isShow, (newVal) => {
        isModified.value = false
        if (newVal) {
            formRef.value?.clearError()
            fetchRepositoryList()
        }
    })
    
    // 监听表单变化
    watch(
        () => application,
        () => {
            if (props.isShow) {
                isModified.value = true
            }
        },
        { deep: true }
    )
    
    function initForm () {
        return {
            baseInfo: {
                storeCode: '',
                storeType: STORE_TYPE,
                name: '',
                baseFeatureInfo: {
                    type: 'LOCAL',
                    extBaseFeatureInfo: {
                        sourceType: 'SELF_UPLOAD',
                        frameworkCode: 'NODEJS_FRAMEWORK',
                        authType: 'OAUTH',
                        codeSource: 'GIT',
                        repositoryHttpUrl: '',
                        buildDir: ''
                    }
                }
            }
        }
    }

    // 获取代码仓库列表
    async function fetchRepositoryList (searchKey = '') {
        repositoryLoading.value = true
        try {
            const res = await $store.dispatch('store/getRepositoryList', searchKey)
            repositoryList.value = res.project || []
        } catch (error) {
            console.error(error)
            repositoryList.value = []
        } finally {
            repositoryLoading.value = false
        }
    }

    // 仓库搜索
    function handleRepositorySearch (value) {
        fetchRepositoryList(value)
    }

    /**
     * 移除对象中的空值
     */
    function removeEmptyValues (obj) {
        return Object.fromEntries(
            Object.entries(obj)
                .filter(([_, value]) => value !== '' && value !== null && value !== undefined)
                .map(([key, value]) => {
                    if (typeof value === 'object' && !Array.isArray(value)) {
                        return [key, removeEmptyValues(value)]
                    }
                    return [key, value]
                })
        )
    }

    /**
     * 侧边栏关闭前的钩子函数
     * 如用户改变数据，关闭侧滑框时二次确认提醒
     */
    async function handleClose () {
        let result = true
        if (isModified.value) {
            result = await leaveConfirm($bkInfo, $t)
        }
        if (result) {
            resetValue()
            formRef.value?.clearError()
            emit('cancel')
        }
    }

    /**
     * 侧边栏提交按钮
     * 校验通过,提交数据创建应用
     */
    async function handleConfirm () {
        const isValid = await formRef.value?.validate()
        if (isValid) {
            isSubmiting.value = true
            try {
                const result = await handleSubmit()
                if (result) {
                    $bkMessage({
                        theme: 'success',
                        message: $t('store.创建应用成功')
                    })
                    resetValue()
                    isModified.value = false
                    emit('confirm', result)
                }
            } catch (error) {
                $bkMessage({
                    theme: 'error',
                    message: error.message || error
                })
            } finally {
                isSubmiting.value = false
            }
        }
    }

    /**
     * 提交表单
     */
    async function handleSubmit () {
        const params = removeEmptyValues(application)
        const baseFeatureInfo = params.baseInfo.baseFeatureInfo
        const extBaseFeatureInfo = baseFeatureInfo.extBaseFeatureInfo

        if (baseFeatureInfo) {
            if (extBaseFeatureInfo?.repositoryHttpUrl) {
                const repositoryId = repositoryList.value.find(item => item.httpUrl === extBaseFeatureInfo.repositoryHttpUrl)?.id
                extBaseFeatureInfo.repositoryId = repositoryId
            }
            if (baseFeatureInfo?.extBaseFeatureInfo.sourceType === 'SELF_UPLOAD') {
                delete baseFeatureInfo.extBaseFeatureInfo
            } else if (extBaseFeatureInfo?.frameworkCode === 'NODEJS_FRAMEWORK') {
                delete extBaseFeatureInfo.repositoryHttpUrl
                delete extBaseFeatureInfo.repositoryId
            }
        }

        try {
            await $store.dispatch('store/createApp', params)
            return true
        } catch (e) {
            return false
        }
    }

    // 重置表单
    function resetValue () {
        Object.assign(application, initForm())
    
        if (formRef.value) {
            formRef.value.clearError()
        }
    }


</script>

<style lang="scss" scoped>
.operation-app-form {
    padding: 20px 40px;
    min-height: 500px;
}

.framework-tag {
    display: inline-block;
    border: 1px solid #2a82e4;
    border-radius: 2px;
    text-align: center;
    font-size: 12px;
    color: #2a82e4;
    cursor: pointer;
    padding: 0 16px;
    height: 24px;
    line-height: 24px;
}

.framework-radio-group {
    display: flex;
    flex-direction: column;
    margin-top: 20px;
}

.framework-radio {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: pre-line;
  margin: 6px 0;
}

.framework-radio-content {
  display: flex;
  align-items: center;
  width: 430px;
  white-space: normal;
  line-height: 2;
  
  .info-icon {
    margin-left: 8px;
    padding: 0 5px;
    font-size: 12px;
    color: #808080;
    vertical-align: middle;
  }
  
  .info-text {
    color: #808080;
    font-size: 12px;
    vertical-align: middle;
    flex: 1;
  }
}

.w88 {
  width: 88px;
}

.mr10 {
  margin-right: 10px;
}
</style>
