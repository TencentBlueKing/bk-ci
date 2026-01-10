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
    import { computed } from 'vue'
    import useCreateEnv from '@/hooks/useCreateEnv'
    import useInstance from '@/hooks/useInstance'
    
    export default {
        name: 'CreateEnvDialog',
        setup () {
            const { proxy } = useInstance()

            const {
                isShow,
                isLoading,
                envParams,
                createNewEnv,
                closeCreateEnvDialog
            } = useCreateEnv(onSuccess, onError)

            const envTypeEnums = computed(() => (['BUILD', 'DEV', 'PROD', 'DEVX']))
            const formRules = computed(() => ({
                name: [
                    {
                        required: true,
                        message: proxy.$t('environment.fieldCannotEmpty'),
                        trigger: 'blur'
                    }
                ]
                // createdUser: [
                //     {
                //         required: true,
                //         message: proxy.$t('environment.fieldCannotEmpty'),
                //         trigger: 'blur'
                //     }
                // ]
            }))
            const handleConfirm = async () => {
                const valid = await proxy.$refs.envForm.validate()
                if (valid) {
                    await createNewEnv()
                }
            }

            function onSuccess (envId) {
                proxy.$bkMessage({
                    theme: 'success',
                    message: proxy.$t('environment.successfullyAdded')
                })
                proxy.$emit('success', envId)
            }
            function onError (err) {
                proxy.$bkMessage({
                    theme: 'error',
                    message: err.message || err
                })
                proxy.$emit('success')
            }
            return {
                isShow,
                isLoading,
                envParams,
                formRules,
                envTypeEnums,

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
    }
</style>
