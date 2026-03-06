<template>
    <bk-sideslider
        :is-show.sync="value"
        ext-cls="release-detail-sideslider"
        :title="$t('publicVar.releaseVarGroup')"
        width="640"
        :quick-close="false"
        @hidden="handleHideReleaseSlider"
    >
        <template>
            <div
                slot="content"
                class="release-detail-wrapper"
            >
                <bk-form
                    label-width="auto"
                    form-type="vertical"
                    :model="releaseParams"
                    ref="releaseForm"
                    class="release-variable-pac-setting"
                    error-display-type="normal"
                >
                    <div class="release-pipeline-pac-submit-conf">
                        <header class="release-pac-variable-form-header">
                            {{ $t("submitSetting") }}
                        </header>

                        <bk-form-item
                            :label="$t('versionDesc')"
                            property="description"
                        >
                            <bk-input
                                type="textarea"
                                maxlength="512"
                                v-model="releaseParams.versionDesc"
                                :placeholder="$t('versionDescPlaceholder')"
                            />
                        </bk-form-item>
                    </div>
                </bk-form>
            </div>
        </template>
        <div slot="footer">
            <bk-button
                class="ml20"
                theme="primary"
                :loading="releasing"
                @click="handleRelease"
            >
                {{ $t('release') }}
            </bk-button>
            <bk-button
                theme="default"
                :loading="releasing"
                @click="handleHideReleaseSlider"
            >
                {{ $t('cancel') }}
            </bk-button>
        </div>
    </bk-sideslider>
</template>

<script setup>
    import { ref, computed } from 'vue'
    import UseInstance from '@/hook/useInstance'
    import {
        ADD_VARIABLE,
        EDIT_VARIABLE,
        OPERATE_TYPE,
        VARIABLE
    } from '@/store/modules/publicVar/constants'
    const { proxy } = UseInstance()
    defineProps({
        value: {
            type: Boolean,
            default: false
        }
    })
    const releasing = ref(false)
    const releaseParams = ref({
        versionDesc: ''
    })
    const groupData = computed(() => proxy.$store.state.publicVar.groupData)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const operateType = computed(() => proxy.$store.state.publicVar.operateType)
    function handleHideReleaseSlider () {
        releaseParams.value.versionDesc = ''
        proxy.$emit('update:value', false)
    }
    async function handleRelease () {
        try {
            releasing.value = true
            const publicVars = groupData.value?.publicVars || [].map(i => {
                return {
                    ...i,
                    buildFormProperty: {
                        ...i.buildFormProperty,
                        varGroupName: i.varName
                    }
                }
            })
            const groupName =  await proxy.$store.dispatch('publicVar/saveVariableGroup', {
                projectId: projectId.value,
                type: operateType.value,
                params: {
                    ...groupData.value,
                    publicVars,
                    versionDesc: releaseParams.value.versionDesc
                }
            })
            proxy.$bkMessage({
                theme: 'success',
                message: operateType.value === OPERATE_TYPE.UPDATE
                    ? proxy.$t('publicVar.updateVarGroupSuccess')
                    : proxy.$t('publicVar.newVarGroupSuccess')
            })
            proxy.$emit('success', groupName)
            // proxy.$router.push({
            //     name: 'PublicVarList',
            //     query: {
            //         flagName: groupName
            //     }
            // })
        } catch (e) {
            proxy.$bkMessage({
                theme: 'error',
                message: e.message || e
            })
        } finally {
            releasing.value = false
        }
    }
</script>

<style lang="scss">
.release-detail-sideslider {
    .bk-sideslider-content {
        height: calc(100vh - 60px) !important;
    }
    .release-detail-wrapper {
        height: 100%;
        .release-pac-variable-form-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-weight: 700;
            font-size: 14px;
            border-bottom: 1px solid #dcdee5;
            padding-bottom: 8px;
            margin-bottom: 16px;

        .devops-icon.icon-angle-right {
            transition: all 0.3s;
            display: inline-flex;
            justify-content: center;
            width: 12px;
            height: 12px;
            line-height: 1;
            font-size: 12px;

            &.pac-codelib-form-show {
                display: inline-flex;
                transform: rotate(90deg);
            }
        }
    }
    }
    .release-variable-pac-setting {
        flex: 1;
        padding: 24px;
        display: flex;
        flex-direction: column;
        grid-gap: 24px;
    }
}

</style>