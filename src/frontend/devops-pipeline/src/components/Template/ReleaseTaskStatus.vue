<template>
    <div class="release-status-main">
        <section class="content-wrapper">
            <template v-if="showReleasePage">
                <bk-loading
                    class="loading-icon"
                    is-loading
                    mode="spin"
                    theme="primary"
                    :size="16"
                />
                <i18n
                    class="release-status-title"
                    tag="p"
                    path="template.releasing.title"
                >
                    <span class="bold">{{ instanceNum }}</span>
                </i18n>
                <p class="sub-message">{{ $t('template.releasing.tips') }}</p>
            </template>
            <template v-else-if="showPartOfMrPage">
                <!-- 合并请求：部分成功 -->
                <template v-if="isPartialSuccess">
                    <span class="part-of-mr" />
                    <i18n
                        class="release-status-title"
                        tag="p"
                        path="template.partOfMr.partialSuccessTitle"
                    >
                        <span class="success bold">{{ releaseRes?.successItemNum }}</span>
                        <span class="failed bold">{{ releaseRes?.failItemNum }}</span>
                    </i18n>
                    <span class="sub-message">
                        {{ $t('template.partOfMr.partialSuccessTip1') }}
                    </span>
                </template>
                <!-- 合并请求：全部成功 -->
                <template v-else>
                    <span class="part-of-mr" />
                    <p class="release-status-title">
                        {{ $t('template.partOfMr.title') }}
                    </p>
                    <p
                        class="sub-message pending"
                    >
                        {{ $t('template.partOfMr.tips1') }}
                        <span>
                            {{ $t('template.partOfMr.tips2') }}
                        </span>
                    </p>
                    <p class="pac-mode-message">
                        {{ $t('template.partOfMr.tips3') }}
                    </p>
                </template>
            </template>
            <!-- 全部成功 -->
            <template v-else-if="showSuccessPage">
                <i class="bk-icon bk-dialog-mark icon-check-1 release-status-icon success-icon" />
                <i18n
                    class="release-status-title"
                    tag="p"
                    path="template.releaseSuc.title"
                >
                    <span class="success bold">{{ releaseRes?.successItemNum }}</span>
                </i18n>
                <p class="sub-message">
                    {{ $t('template.releaseSuc.tips') }}
                </p>
            </template>
            <!-- 全部失败 -->
            <template v-else-if="showFailedPage">
                <i class="bk-icon bk-dialog-mark icon-close release-status-icon failed-icon" />
                <i18n
                    class="release-status-title"
                    tag="p"
                    path="template.releaseFail.title"
                >
                    <span class="failed bold">{{ releaseRes?.failItemNum }}</span>
                </i18n>
                <p class="sub-message">
                    {{ $t('template.releaseFail.tips') }}
                </p>
            </template>
            <template v-else-if="isPartialSuccess && !showPartOfMrPage">
                <i class="bk-icon bk-dialog-mark icon-check-1 release-status-icon partial-success-icon" />
                <i18n
                    class="release-status-title"
                    tag="p"
                    path="template.releasePartialSuccess.title"
                >
                    <span class="success bold">{{ releaseRes?.successItemNum }}</span>
                    <span class="failed bold">{{ releaseRes?.failItemNum }}</span>
                </i18n>
                <p class="sub-message">
                    {{ $t('template.releasePartialSuccess.tip1') }}
                </p>
            </template>
            <div class="release-status-btn">
                <template v-if="showFailedPage || (isPartialSuccess && !showPartOfMrPage)">
                    <!-- 失败 / 部分成功 -->
                    <bk-button
                        theme="primary"
                        @click="handleRetryRelease"
                    >
                        {{ $t('retry') }}
                    </bk-button>
                    <bk-button
                        @click="handleModifyConfig"
                    >
                        {{ $t('template.modifyConfig') }}
                    </bk-button>
                </template>
                <template v-if="showPartOfMrPage">
                    <!-- 有 MR 合并连接 -->
                    <!-- 全部成功 / 部分成功 -->
                    <bk-button
                        theme="primary"
                        class="mr10"
                        @click="handleClick"
                    >
                        {{ $t('template.partOfMr.dealMR') }}
                    </bk-button>
                    <bk-button
                        v-if="isPartialSuccess"
                        @click="handleModifyConfig"
                    >
                        {{ $t('template.modifyConfig') }}
                    </bk-button>
                    <bk-button
                        v-if="isPartialSuccess"
                        @click="handleRetryRelease"
                    >
                        {{ $t('template.partOfMr.failedRetry') }}
                    </bk-button>
                </template>
                
                <bk-button
                    @click="handleToInstanceList"
                >
                    {{ $t('template.returnInstanceList') }}
                </bk-button>
            </div>

            <template v-if="isPartialSuccess && showPartOfMrPage">
                <p class="sub-message pending">
                    ( 1 ) {{ $t('template.partOfMr.tips1') }}
                    <span>
                        {{ $t('template.partOfMr.tips2') }}
                    </span>
                </p>
                <p class="pac-mode-message">
                    {{ $t('template.partOfMr.tips3') }}
                </p>
                <p class="sub-message pending mt20">
                    ( 2 ) {{ $t('template.partOfMr.partialSuccessTip2', [releaseRes?.failItemNum]) }}
                </p>
            </template>
            <release-failed-message
                class="mt20"
                v-if="Object.keys(releaseRes?.errorMessages)?.length"
                :data="releaseRes?.errorMessages"
            />
        </section>
    </div>
</template>
<script setup>
    import { ref, computed, watch, onUnmounted } from 'vue'
    import UseInstance from '@/hook/useInstance'
    import {
        RELEASE_STATUS,
        SET_RELEASE_BASE_ID,
        SET_RELEASE_ING
    } from '@/store/modules/templates/constants'
    import ReleaseFailedMessage from './ReleaseFailedMessage'
    defineProps({
        instanceNum: Boolean
    })
    const { proxy } = UseInstance()
    const releaseRes = ref({})
    const releaseStatus = ref(RELEASE_STATUS.INIT)
    const releaseBaseId = computed(() => proxy?.$store?.state?.templates?.releaseBaseId)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    const isPartialSuccess = computed(() => releaseStatus.value === RELEASE_STATUS.PARTIAL_SUCCESS)
    const showReleasePage = computed(() => [RELEASE_STATUS.INIT, RELEASE_STATUS.INSTANCING].includes(releaseStatus.value))
    const showSuccessPage = computed(() => [RELEASE_STATUS.SUCCESS].includes(releaseStatus.value))
    const showFailedPage = computed(() => [RELEASE_STATUS.FAILED].includes(releaseStatus.value))
    const showPartOfMrPage = computed(() => !!(releaseRes.value?.pullRequestUrl))
    const currentVersionId = computed(() => proxy?.$route.params?.version)
    const timer = ref(null)
    watch(() => releaseBaseId.value, (val) => {
        if (val && showReleasePage.value) {
            fetchReleaseTaskStatus()
        }
    }, {
        immediate: true
    })
    function handleToInstanceList () {
        proxy.$router.push({
            name: 'TemplateOverview',
            params: {
                type: 'instanceList',
                version: currentVersionId.value
            }
        })
        
        proxy.$store.commit(`templates/${SET_RELEASE_ING}`, false)
    }
    async function fetchReleaseTaskStatus () {
        try {
            const res = await proxy.$store.dispatch('templates/fetchReleaseTaskStatus', {
                projectId: projectId.value,
                templateId: templateId.value,
                baseId: releaseBaseId.value
            })
            releaseRes.value = res.data
            releaseStatus.value = res.data.status
            if ([RELEASE_STATUS.INIT, RELEASE_STATUS.INSTANCING].includes(releaseStatus.value)) {
                timer.value = setTimeout(() => {
                    fetchReleaseTaskStatus()
                }, 3000)
            } else {
                clearTimeout(timer.value)
            }
        } catch (e) {
            console.error(e)
        }
    }
    async function handleRetryRelease () {
        try {
            releaseStatus.value = RELEASE_STATUS.INIT
            const baseId = await proxy.$store.dispatch('templates/retryReleaseInstance', {
                projectId: projectId.value,
                templateId: templateId.value,
                baseId: releaseBaseId.value
            })
            proxy.$store.commit(`templates/${SET_RELEASE_BASE_ID}`, baseId)
        } catch (e) {
            console.error(e)
        }
    }

    async function handleModifyConfig () {
        try {
            await proxy.$store.dispatch('templates/fetchTaskDetailParams', {
                projectId: projectId.value,
                templateId: templateId.value,
                baseId: releaseBaseId.value,
                status: RELEASE_STATUS.FAILED
            })
            
            proxy.$emit('cancel')
        } catch (e) {
            console.error(e)
        }
    }
    onUnmounted(() => {
        proxy.$store.commit(`templates/${SET_RELEASE_BASE_ID}`, '')
        proxy.$store.commit(`templates/${SET_RELEASE_ING}`, false)
        clearTimeout(timer.value)
    })
</script>
<style lang="scss">
.release-status-main {
    display: flex;
    align-items: center;
    justify-content: space-evenly;
    width: 100%;
    height: 100%;
    text-align: center;
    .content-wrapper {
        display: flex;
        flex-direction: column;
        align-items: center;
    }
    .loading-icon {
        display: flex;
        height: 150px;
    }
    
    .release-status-icon {
        margin-top: 122px;
        margin-bottom: 40px;
        color: #fff;
        border-radius: 50%;
        font-size: 50px;
        &.success-icon {
            background: #2caf5e;
        }
        &.failed-icon {
            background: #ea3636;
        }
        &.partial-success-icon {
            background: #F59500;
        }
    }

    .release-status-title {
        font-size: 24px;
        color: #313238;
        line-height: 32px;
        margin-bottom: 16px;
         .bold {
            font-weight: bold;
        }
         .success {
            color: #40b771;
        }
         .failed {
            color: #eb3333;
        }
    }

    .sub-message {
        margin: auto;
        width: 280px;
        font-size: 14px;
        color: #4D4F56;
        text-align: center;
    }

    .pending {
        width: 552px;
        margin-bottom: 16px;
        text-align: left;
        span {
            cursor: pointer;
            font-weight: 700;
        }
    }

    .pac-mode-message {
        margin: auto;
        text-align: left;
        padding-left: 16px;
        width: 552px;
        height: 46px;
        line-height: 46px;
        background: #F5F7FA;
        border-radius: 2px;
        font-size: 14px;
        color: #4D4F56;
    }

    .release-status-content {
        margin: auto;
        margin-top: 22px;
        width: 294px;
        height: 294px;
        border: 1px solid #DCDEE5;
        border-radius: 50%;
        box-shadow: inset 0 1px 13px 0 #0000001a;
    }

    .release-status-btn {
        margin-top: 28px;
    }
    .part-of-mr {
        position: relative;
        width: 42px;
        height: 42px;
        background-color: #E1ECFF;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 20px;
        &:before {
            position: absolute;
            content: '';
            width: 0;
            height: 0;
            border: 14px solid #3A84FF;
            border-top-color: transparent;
            position: absolute;
            transform: rotate(-45deg);
            border-radius: 50%;
        }
        &:after {
            content: '';
            position: absolute;
            border: 2px solid #3A84FF;
            width: 28px;
            height: 28px;
            border-radius: 50%;

        }
    }
}

</style>
