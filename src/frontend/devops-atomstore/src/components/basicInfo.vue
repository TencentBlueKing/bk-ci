<template>
    <div class="basic-info-component">
        <div class="flex-container">
            <div class="flex-1">
                <bk-form-item
                    :label="$t('store.应用名称')"
                    property="name"
                    error-display-type="normal"
                    :required="true"
                >
                    <bk-input
                        v-model="basicInfo.name"
                        :clearable="true"
                        @input="val => updateField('name', val)"
                    />
                </bk-form-item>
                
                <bk-form-item
                    :label="$t('store.应用分类')"
                    property="classifyCode"
                    error-display-type="normal"
                    :required="true"
                >
                    <bk-select
                        v-model="basicInfo.classifyCode"
                        :loading="classifyLoading"
                        @change="val => updateField('classifyCode', val)"
                    >
                        <bk-option
                            v-for="item in classifyList"
                            :key="item.classifyCode"
                            :id="item.classifyCode"
                            :name="item.classifyName"
                        />
                    </bk-select>
                </bk-form-item>
            </div>
            
            <bk-form-item
                :label="$t('store.应用LOGO')"
                property="logoUrl"
                error-display-type="normal"
                :required="true"
            >
                <bk-upload
                    :theme="'picture'"
                    :with-credentials="true"
                    :multiple="false"
                    :limit="1"
                    :files="logoFiles"
                    :name="'logo'"
                    :handle-res-code="handleResCode"
                    :url="logoUploadUrl"
                    @on-delete="handleDelete"
                    @on-error="handleLogoError"
                    @on-exceed="handleLogoExceed"
                >
                    <template #default>
                        <div class="upload-trigger">
                            <i class="bk-icon icon-plus" />
                            <p>{{ $t('store.点击上传') }}</p>
                        </div>
                    </template>
                </bk-upload>
            </bk-form-item>
        </div>
        
        <bk-form-item
            :label="$t('store.应用简介')"
            property="summary"
            error-display-type="normal"
            :required="true"
        >
            <bk-input
                v-model="basicInfo.summary"
                :placeholder="$t('store.应用简介不超过256个字符')"
                :clearable="true"
                @input="val => updateField('summary', val)"
            />
        </bk-form-item>
        
        <bk-form-item
            :label="$t('store.详细描述')"
            property="description"
        >
            <mavon-editor
                v-model="basicInfo.description"
                :toolbars="toolbars"
                :external-link="false"
                :box-shadow="false"
                preview-background="#fff"
                :language="$i18n.locale === 'en-US' ? 'en' : $i18n.locale"
                @change="handleDescriptionChange"
            />
        </bk-form-item>
    </div>
</template>

<script setup name="BasicInfos">
    import { computed, onMounted, ref } from 'vue'
    import UseInstance from '@/hook/useInstance.js'
    import { LOGO_UPLOAD_URL } from '@/utils/constants'
    import { toolbars } from '@/utils/editor-options'

    const props = defineProps({
        basicInfo: {
            type: Object,
            default: () => ({})
        }
    })

    const emit = defineEmits(['update:basicInfo'])

    const { proxy } = UseInstance()
    const { $store, $bkMessage, $t } = proxy

    const classifyList = ref([])
    const classifyLoading = ref(false)

    const logoUploadUrl = LOGO_UPLOAD_URL

    const logoFiles = computed(() => {
        return props.basicInfo?.logoUrl ? [{ url: props.basicInfo.logoUrl }] : []
    })

    onMounted(() => {
        fetchClassifyList()
    })

    async function fetchClassifyList () {
        try {
            try {
                if (classifyLoading.value || classifyList.value.length > 0) return
                classifyLoading.value = true
                classifyList.value = await $store.dispatch('store/fetchClassifyList')
            } catch (error) {
                classifyList.value = []
            } finally {
                classifyLoading.value = false
            }
        } catch (error) {
            console.error(error)
        }
    }
    
    // 更新字段的辅助函数
    function updateField (field, value) {
        emit('update:basicInfo', {
            ...props.basicInfo,
            [field]: value
        })
    }

    // mavon-editor 的 change 事件处理
    function handleDescriptionChange (value) {
        updateField('description', value)
    }

    function handleResCode (res) {
        if (res.data?.logoUrl) {
            updateField('logoUrl', res.data.logoUrl)
            return true
        }
        if (res.message) {
            $bkMessage({
                theme: 'error',
                message: res.message
            })
        }
        return false
    }

    function handleDelete () {
        updateField('logoUrl', '')
    }

    function handleLogoError (_, __, error) {
        $bkMessage({
            theme: 'error',
            message: error.message
        })
    }

    function handleLogoExceed () {
        $bkMessage({
            theme: 'error',
            message: $t('store.请先删除当前错误Logo图片')
        })
    }
</script>

<style lang="scss" scoped>
.basic-info-component {
    display: flex;
    flex-direction: column;

    .flex-container {
        display: flex;
    }
    
    .flex-1 {
        flex: 1;
        margin-bottom: 24px;
    }
    
    .upload-trigger {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        color: #63656e;
        
        .icon-plus {
            font-size: 25px;
        }
    }

    ::v-deep .v-note-wrapper {
        height: 262px;
        border: 1px solid #c4c6cc;

        .v-note-panel {
            box-shadow: none;
        }
    }
}
</style>
