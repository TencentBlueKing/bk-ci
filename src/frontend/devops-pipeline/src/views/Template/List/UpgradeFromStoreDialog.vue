<template>
    <bk-dialog
        :value="value"
        :title="$t('template.upgradeTemplate')"
        :width="480"
        :auto-close="false"
        :loading="submiting"
        :mask-close="false"
        :close-icon="false"
        @confirm="confirm"
        @cancel="cancel"
    >
        <bk-form
            form-type="vertical"
            ref="formRef"
            :model="formModel"
            :rules="formRules"
        >
            <bk-form-item
                required
                property="version"
                :label="$t('version')"
            >
                <bk-select
                    v-model="formModel.version"
                    :placeholder="$t('template.selectTemplateUpgradedVersion')"
                    :loading="fetching"
                >
                    <bk-option
                        v-for="v in upgradeVersions"
                        :key="v.id"
                        v-bind="v"
                    >
                    </bk-option>
                </bk-select>
            </bk-form-item>
        </bk-form>
    </bk-dialog>
</template>

<script>
    import { defineComponent, getCurrentInstance, reactive, ref, watch } from 'vue'

    export default defineComponent({
        props: {
            templateId: {
                type: String,
                required: true
            },
            projectId: {
                type: String,
                required: true
            },
            value: {
                type: Boolean,
                default: false
            }
        },
        setup (props, ctx) {
            const { proxy } = getCurrentInstance()
            const fetching = ref(false)
            const formRef = ref()
            const upgradeVersions = ref([])
            const submiting = ref(false)
            const formRules = {
                version: [{
                    required: true,
                    message: proxy.$t('editPage.requiredTips', [proxy.$t('template.upgradedVersion')]),
                    trigger: 'change'
                }]
            }
            const formModel = reactive({
                version: ''
            })

            watch(() => props.value, (newValue) => {
                newValue && fetchVersions()
                if (!newValue) {
                    formRef.value?.clearError()
                }
            })

            async function fetchVersions () {
                try {
                    fetching.value = true
                    const res = await proxy.$store.dispatch('templates/requestTemplateVersionList', {
                        templateId: props.templateId,
                        projectId: props.projectId,
                        upgradableVersionsQuery: true
                    })
                    upgradeVersions.value = res.records.map(version => ({
                        id: version.version,
                        name: version.versionName
                    }))
                    formModel.version = upgradeVersions.value?.[0]?.id
                } catch (error) {
                    console.error('Error fetching versions:', error)
                    return []
                } finally {
                    fetching.value = false
                }
            }

            async function confirm () {
                try {
                    if (submiting.value) return
                    submiting.value = true
                    await formRef.value?.validate()
                    ctx.emit('confirm', formModel.version, () => {
                        submiting.value = false
                    })
                } catch (error) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: error.content ?? error.message ?? error
                    })
                    submiting.value = false
                }
            }

            function cancel () {
                ctx.emit('cancel')
            }

            return {
                fetching,
                upgradeVersions,
                formModel,
                submiting,
                formRules,
                formRef,
                confirm,
                cancel
            }
        }
    })
</script>
