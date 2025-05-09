<template>
    <div class="release-status-main">
        <section
            class="content-wrapper"
            v-if="showReleasePage"
        >
            <bk-loading
                class="loading-icon"
                is-loading
                mode="spin"
                theme="primary"
                :size="16"
            />
           
            <p class="release-status-title">{{ $t('template.releasing.title', [instanceNum]) }}</p>
            <p class="sub-message">{{ $t('template.releasing.tips') }}</p>
        </section>
        <section
            class="content-wrapper"
            v-if="showPartOfMrPage"
        >
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
            <div class="release-status-btn">
                <bk-button
                    theme="primary"
                    @click="handleClick"
                >
                    {{ $t('template.partOfMr.dealMR') }}
                </bk-button>
                <bk-button
                    @click="handleToInstanceList"
                >
                    {{ $t('template.returnInstanceList') }}
                </bk-button>
            </div>
        </section>
        <section
            class="content-wrapper"
            v-else-if="showSuccessPage"
        >
            <Logo
                size="64"
                name="success"
                class="release-status-icon"
            />
            <p
                class="release-status-title"
            >
                {{ $t('template.releaseSuc.title', [releaseRes.successItemNum]) }}
            </p>
            <p class="sub-message">
                {{ $t('template.releaseSuc.tips') }}
            </p>
            <bk-button
                @click="handleToInstanceList"
                class="release-status-btn"
            >
                {{ $t('template.returnInstanceList') }}
            </bk-button>
        </section>
        <section
            class="content-wrapper"
            v-else-if="showFailedPage"
        >
            <Logo
                size="64"
                name="failure"
                class="release-status-icon"
            />
            <p class="release-status-title">
                {{ $t('template.releaseFail.title', [releaseRes.failItemNum]) }}
            </p>
            <p class="sub-message">
                {{ $t('template.releaseFail.tips') }}
            </p>
            <div class="release-status-btn">
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
                <bk-button
                    @click="handleCancelRelease"
                >
                    {{ $t('close') }}
                </bk-button>
            </div>
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
    import { TARGET_ACTION_ENUM } from '@/utils/pipelineConst'
    const props = defineProps({
        instanceNum: Boolean,
        targetAction: String
    })
    const { proxy } = UseInstance()
    const releaseRes = ref({})
    const releaseStatus = ref(RELEASE_STATUS.INIT)
    const releaseBaseId = computed(() => proxy?.$store?.state?.templates?.releaseBaseId)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    const showReleasePage = computed(() => [RELEASE_STATUS.INIT, RELEASE_STATUS.INSTANCING].includes(releaseStatus.value))
    const showSuccessPage = computed(() => [RELEASE_STATUS.SUCCESS].includes(releaseStatus.value))
    const showFailedPage = computed(() => [RELEASE_STATUS.FAILED].includes(releaseStatus.value))
    const showPartOfMrPage = computed(() => showSuccessPage.value && props.targetAction === TARGET_ACTION_ENUM.CHECKOUT_BRANCH_AND_REQUEST_MERGE)
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
                taskId: releaseBaseId.value
            })
            releaseRes.value = res.data
            releaseStatus.value = res.data.status
            if ([RELEASE_STATUS.INIT, RELEASE_STATUS.INSTANCING].includes(releaseStatus.value)) {
                timer.value = setTimeout(() => {
                    fetchReleaseTaskStatus()
                }, 3000)
            } else {
                proxy.$store.commit(`templates/${SET_RELEASE_BASE_ID}`, '')
                clearTimeout(timer.value)
            }
        } catch (e) {
            console.error(e)
        }
    }
    async function handleRetryRelease () {
        try {
            releaseStatus.value = RELEASE_STATUS.INIT
            await proxy.$store.dispatch('templates/retryReleaseInstance', {
                projectId: projectId.value,
                templateId: templateId.value,
                taskId: releaseBaseId.value
            })
        } catch (e) {
            console.error(e)
        }
    }
    function handleCancelRelease () {
        proxy.$emit('cancel')
    }
    async function handleModifyConfig () {
        try {
            const res = await proxy.$store.dispatch('templates/fetchTaskDetailParams', {
                projectId: projectId.value,
                templateId: templateId.value,
                taskId: releaseBaseId.value,
                status: RELEASE_STATUS.FAILED
            })
            proxy.$emit('cancel')
            console.log(res, 123)
        } catch (e) {
            console.error(e)
        }
    }
    onUnmounted(() => {
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
    }

    .release-status-title {
        font-size: 24px;
        color: #313238;
        line-height: 32px;
        margin-bottom: 16px;
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
