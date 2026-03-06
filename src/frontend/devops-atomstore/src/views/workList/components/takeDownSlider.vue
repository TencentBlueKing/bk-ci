<template>
    <bk-sideslider
        :is-show="isShow"
        :title="$t('store.下架应用')"
        :width="640"
        @update:isShow="handleUpdateShow"
    >
        <template slot="content">
            <div class="takedown-container">
                <ul class="info-list">
                    <li class="info-item">
                        <label class="info-label">{{ $t('store.应用名称') }}</label>
                        <span class="info-value">{{ infos?.name || '--' }}</span>
                    </li>
                    <li class="info-item">
                        <label class="info-label">{{ $t('store.应用标识') }}</label>
                        <span class="info-value">{{ infos?.storeCode || '--' }}</span>
                    </li>
                    <li
                        v-if="showVersion"
                        class="info-item"
                    >
                        <label class="info-label">{{ $t('store.版本') }}</label>
                        <span class="info-value">{{ infos?.version || '--' }}</span>
                    </li>
                </ul>
                
                <bk-form
                    ref="formRef"
                    :model="takeDownForm"
                    :label-width="100"
                    label-position="left"
                >
                    <bk-form-item
                        :label="$t('store.下架原因')"
                        property="reason"
                        :required="true"
                        :rules="[{ required: true, message: $t('store.请输入下架原因'), trigger: 'blur' }]"
                    >
                        <bk-input
                            v-model="takeDownForm.reason"
                            type="textarea"
                            :placeholder="$t('store.请输入下架原因')"
                            :rows="3"
                        />
                    </bk-form-item>
                </bk-form>
                
                <bk-button
                    theme="primary"
                    :loading="isSubmitting"
                    :disabled="isSubmitting"
                    class="submit-btn"
                    @click="takeDownVersion"
                >
                    {{ $t('store.下架') }}
                </bk-button>
            </div>
        </template>
    </bk-sideslider>
</template>

<script setup>
    import { ref, watch } from 'vue'
    import { STORE_TYPE } from '@/utils/constants'
    import UseInstance from '@/hook/useInstance.js'

    const props = defineProps({
        isShow: {
            type: Boolean,
            default: false
        },
        infos: {
            type: Object,
            default: () => ({})
        },
        showVersion: {
            type: Boolean,
            default: true
        },
        storeCode: {
            type: String,
            default: ''
        }
    })

    const emit = defineEmits(['takedown-success'])

    const { proxy } = UseInstance()
    const { $store, $bkMessage, $bkInfo, $t } = proxy

    const formRef = ref(null)
    const isSubmitting = ref(false)

    const takeDownForm = ref({
        storeCode: props.storeCode,
        storeType: STORE_TYPE,
        version: props.infos?.version ?? '',
        reason: '',
    })

    watch(() => props.infos, (newVal) => {
        takeDownForm.value.storeCode = newVal.storeCode
        takeDownForm.value.version = newVal.version
    })

    // 处理 isShow 更新
    function handleUpdateShow (val) {
        if (!val) {
            resetForm()
        }
    }

    function hideTakedownSideslider () {
        emit('update:modelValue', false)
        takeDownForm.value = {
            storeCode: props.storeCode,
            storeType: STORE_TYPE,
            version: '',
            reason: '',
        }
    }

    // 提交下架
    async function takeDownVersion () {
        try {
            const isValid = await formRef.value.validate()
            if (!isValid) return

            isSubmitting.value = true
        
            const { version, ...resetParams } = takeDownForm.value
            const params = props.showVersion ? takeDownForm.value : resetParams
        
            await $store.dispatch('store/takeDownVersion', params)

            $bkMessage({
                theme: 'success',
                message: $t('store.下架成功')
            })

            resetForm()
            emit('takedownSuccess')
        } catch (error) {
            $bkMessage({
                theme: 'error',
                message: error.message || error
            })
        } finally {
            isSubmitting.value = false
        }
    }

    // 重置表单
    function resetForm () {
        Object.assign(takeDownForm.value, {
            reason: ''
        })
        if (formRef.value) {
            formRef.value.clearError()
        }
    }
</script>

<style lang="scss" scoped>
.takedown-container {
    padding: 24px;
}

.info-list {
    margin-bottom: 20px;
}

.info-item {
    display: flex;
    align-items: center;
    margin-bottom: 20px;
}

.info-label {
    flex-shrink: 0;
    width: 100px;
    padding-right: 30px;
    text-align: right;
}

.info-value {
    flex: 1;
}

.submit-btn {
    margin-left: 100px;
    margin-top: 24px;
}
</style>
