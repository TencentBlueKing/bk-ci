<template>
    <bk-dialog
        v-model="isShow"
        ext-cls="create-env-dialog"
        width="800"
        theme="primary"
        header-position="left"
        :title="$t('environment.createEnvTitle')"
    >
        <bk-form
            ref="envForm"
            class="create-env-form"
            :model="envParams"
            :rules="formRules"
            :label-width="100"
        >
            <bk-form-item
                :label="$t('environment.envInfo.name')"
                required
                property="name"
                error-display-type="normal"
            >
                <bk-input
                    class="env-name-input"
                    name="env_name"
                    maxlength="30"
                    :placeholder="$t('environment.pleaseEnter')"
                    v-model="envParams.name"
                >
                </bk-input>
            </bk-form-item>
            <bk-form-item
                v-if="!isCreateResType"
                :label="$t('environment.envInfo.envType')"
                class="env-type-item"
                required
                property="envType"
            >
                <bk-radio-group v-model="envParams.envType">
                    <bk-radio
                        v-for="envType in envTypeEnums"
                        :key="envType"
                        :value="envType"
                    >
                        <div class="mr10">{{ $t(`environment.envInfo.${envType}EnvType`) }}</div>
                    </bk-radio>
                </bk-radio-group>
            </bk-form-item>
            <bk-form-item
                v-if="isCreateResType"
                label="OS"
                required
                property="os"
                error-display-type="normal"
            >
                <bk-radio-group
                    v-model="envParams.os"
                    class="os-radio-group"
                >
                    <bk-radio
                        v-for="item in osTypeOptions"
                        :key="item.value"
                        :value="item.value"
                        :disabled="item.disabled"
                    >
                        <div class="mr10">{{ item.label }}</div>
                    </bk-radio>
                </bk-radio-group>
            </bk-form-item>
            <bk-form-item
                :label="$t('environment.envInfo.envRemark')"
                property="desc"
            >
                <bk-input
                    class="env-desc-input"
                    :placeholder="$t('environment.pleaseEnter')"
                    type="textarea"
                    :rows="3"
                    :maxlength="100"
                    v-model="envParams.desc"
                />
            </bk-form-item>
            <!-- <bk-form-item
                :label="$t('environment.chargePerson')"
                property="createdUser"
                required
                error-display-type="normal"
            >
                <bk-input
                    class="env-desc-input"
                    :placeholder="$t('environment.pleaseEnter')"
                    v-model="envParams.createdUser"
                />
            </bk-form-item> -->
        </bk-form>
        <template #footer>
            <bk-button
                theme="primary"
                :loading="isLoading"
                :title="$t('environment.submit')"
                @click="handleConfirm"
            >
                {{ $t('environment.submit') }}
            </bk-button>
            <bk-button
                theme="default"
                :loading="isLoading"
                :title="$t('environment.cancel')"
                @click="closeCreateEnvDialog"
            >
                {{ $t('environment.cancel') }}
            </bk-button>
        </template>
    </bk-dialog>
</template>

<script>
    import { computed, watch } from 'vue'
    import useCreateEnv from '@/hooks/useCreateEnv'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import useInstance from '@/hooks/useInstance'
    import { ENV_TYPE_MAP, OS_LABEL_MAP, SERVICE_RESOURCE_TYPE } from '@/store/constants'
    
    export default {
        name: 'CreateEnvDialog',
        setup () {
            const { proxy } = useInstance()
            const { isPersonalProject } = useEnvDetail()

            const {
                isShow,
                isLoading,
                envParams,
                createNewEnv,
                closeCreateEnvDialog
            } = useCreateEnv(onSuccess, onError)

            const isCreateResType = computed(() => proxy.$route.params.resType === SERVICE_RESOURCE_TYPE.CREATE)
            const envTypeEnums = computed(() => ([
                ENV_TYPE_MAP.BUILD,
                // ENV_TYPE_MAP.PROD,
                // ENV_TYPE_MAP.DEV,
                // ENV_TYPE_MAP.DEVX
            ]))
            const enabledOsType = computed(() => (
                isPersonalProject.value ? 'WINDOWS' : 'LINUX'
            ))
            const osTypeOptions = computed(() => (
                Object.entries(OS_LABEL_MAP).map(([value, label]) => ({
                    label,
                    value,
                    disabled: enabledOsType.value !== value
                }))
            ))
            const formRules = computed(() => {
                const rules = {
                    name: [
                        {
                            required: true,
                            message: proxy.$t('environment.fieldCannotEmpty'),
                            trigger: 'blur'
                        }
                    ]
                }
                if (isCreateResType.value) {
                    rules.os = [
                        {
                            required: true,
                            message: proxy.$t('environment.fieldCannotEmpty'),
                            trigger: 'change'
                        }
                    ]
                }
                return rules
            })
            const handleConfirm = async () => {
                const valid = await proxy.$refs.envForm.validate()
                if (valid) {
                    await createNewEnv()
                }
            }

            function onSuccess (envId, envType) {
                proxy.$bkMessage({
                    theme: 'success',
                    message: proxy.$t('environment.successfullyAdded')
                })
                proxy.$emit('success', envId, envType)
            }
            function onError (err) {
                proxy.$bkMessage({
                    theme: 'error',
                    message: err.message || err
                })
                proxy.$emit('success')
            }
            
            watch(isShow, (val) => {
                if (val && isCreateResType.value) {
                    envParams.value.envType = ENV_TYPE_MAP.CREATE
                    envParams.value.os = ''
                }
            })

            watch(enabledOsType, (enabledOs) => {
                if (isCreateResType.value && envParams.value.os && envParams.value.os !== enabledOs) {
                    envParams.value.os = ''
                }
            })
            return {
                isShow,
                isLoading,
                envParams,
                formRules,
                envTypeEnums,
                osTypeOptions,
                isCreateResType,

                // function
                onError,
                handleConfirm,
                closeCreateEnvDialog
            }
        }
    }
</script>

<style lang="scss">
    .create-env-form {
        .form-error-tip {
            text-align: left !important;
        }
        .os-radio-group {
            .bk-form-radio {
                margin-right: 14px;
            }
        }
    }
</style>
