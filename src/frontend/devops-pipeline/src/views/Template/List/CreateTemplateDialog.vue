<template>
    <bk-dialog
        v-model="value"
        width="700"
        header-position="left"
        :title="$t('template.addTemplate')"
        :ok-text="$t('template.create')"
        :confirm-fn="handConfirmCreateTemplate"
        :on-close="handleCancelCreateTemplate"
    >
        <bk-form
            ref="templateFormRef"
            :rules="rules"
            :label-width="100"
            ext-cls="create-template-form"
            :model="templateFormData"
        >
            <bk-form-item
                required
                property="name"
                :label="$t('template.name')"
                error-display-type="normal"
            >
                <bk-input
                    v-model="templateFormData.name"
                    :maxlength="30"
                />
            </bk-form-item>
            <bk-form-item
                required
                :label="$t('template.type')"
                property="type"
            >
                <ul class="template-type-selector">
                    <li
                        v-for="item in templateTypeMap"
                        :class="['template-type-item', {
                            'active': item.isActive
                        }]"
                        :key="item.value"
                        @click="handleChangeTemplateType(item.value)"
                    >
                        {{ item.name }}
                    </li>
                </ul>
                <div
                    v-if="templateTypeTips"
                    class="template-type-tips"
                >
                    {{ templateTypeTips }}
                </div>
            </bk-form-item>
            <bk-form-item
                property="desc"
                :label="$t('template.desc')"
            >
                <bk-input
                    v-model="templateFormData.desc"
                    type="textarea"
                    :maxlength="200"
                    :rows="3"
                />
            </bk-form-item>
        </bk-form>
    </bk-dialog>
</template>

<script setup name='CreateTemplateDialog'>
    import { onMounted, ref, computed, defineProps } from 'vue'
    import UseInstance from '@/hook/useInstance'
    const { proxy, i18n, bkMessage, userInfo } = UseInstance()
    defineProps({
        value: {
            type: Boolean,
            default: false
        }
    })
    const emit = defineEmits(['update:value', 'confirm'])

    const projectId = computed(() => proxy.$route.params.projectId)
    const userName = computed(() => userInfo?.username || window?.userInfo?.username || '')
    const rules = computed(() => {
        return {
            name: [
                {
                    required: true,
                    message: i18n.t('template.pleaseEnterTemplateName'),
                    trigger: 'blur'
                },
                {
                    message: i18n.t('template.nameTips'),
                    validator: (val) => {
                        console.log(val)
                        return val
                    },
                    trigger: 'blur'
                }
            ]
        }
    })
    function getDefaultFormData () {
        return {
            projectId: projectId.value,
            creator: userName.value,
            type: 'PIPELINE',
            source: 'CUSTOM',
            name: '',
            desc: ''
        }
    }
    const templateFormRef = ref(null)
    const templateFormData = ref({
        projectId: projectId.value,
        creator: userName.value,
        type: 'PIPELINE',
        source: 'CUSTOM',
        name: '',
        desc: ''
    })
    const templateTypeMap = computed(() => [
        {
            name: i18n.t('template.pipelineTemplate'),
            value: 'PIPELINE',
            isActive: templateFormData.value.type === 'PIPELINE'
        }
    ])
    const templateTypeTips = computed(() => {
        const tipsMap = {
            PIPELINE: i18n.t('template.pipelineTypeTips')
        }
        return tipsMap[templateFormData.value.type] || ''
    })

    function handleChangeTemplateType (type) {
        templateFormData.value.type = type
    }

    function handConfirmCreateTemplate () {
        templateFormRef.value.validate().then(async () => {
            await proxy.$store.dispatch('templates/createTemplate', {
                projectId: projectId.value,
                params: templateFormData.value
            })
            bkMessage({
                theme: 'success',
                message: i18n.t('创建模板成功')
            })
            templateFormData.value = getDefaultFormData()
            emit('update:value', false)
            emit('confirm')
        })
    }

    function handleCancelCreateTemplate () {
        emit('update:value', false)
        templateFormRef.value.clearError()
        templateFormData.value = getDefaultFormData()
    }

    onMounted(() => {
        templateFormData.value = getDefaultFormData()
    })
</script>

<style lang="scss">
    .create-template-form {
        .bk-label .bk-label-text {
            font-weight: normal;
        }
    }
</style>
<style lang="scss" scoped>
    .template-type-selector {
        .template-type-item {
            display: inline-block;
            width: 152px;
            height: 44px;
            line-height: 44px;
            background: #FFFFFF;
            border: 1px solid #C4C6CC;
            border-radius: 2px;
            align-items: center;
            text-align: center;
            margin-right: 8px;
            margin-bottom: 8px;
            cursor: pointer;
            &.active {
                color: #3A84FF;
                background: #F0F5FF;
                border: 1px solid #3A84FF;
            }
        }
    }
    .template-type-tips {
        font-size: 12px;
        color: #979BA5;
        line-height: 20px;
        background: #F5F7FA;
        padding: 8px 16px;
    }
</style>
