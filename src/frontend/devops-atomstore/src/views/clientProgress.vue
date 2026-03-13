<template>
    <div
        v-bkloading="{ isLoading: isLoading, zIndex: 2 }"
        class="client-progress"
    >
        <bread-crumbs
            :bread-crumbs="navList"
            type="devx"
        ></bread-crumbs>
        
        <div class="progress-detail-container">
            <div class="progress-header-card">
                <header class="header-content">
                    <p>{{ $t('store.发布进度') }}</p>
                    <p
                        v-if="!isEnd"
                        class="action-btn"
                        @click="cancelRelease"
                    >
                        <i class="bk-icon icon-pause-circle" />
                        {{ $t('store.取消发布') }}
                    </p>
                    <p
                        v-else
                        class="action-btn"
                        @click="goOverview"
                    >
                        {{ $t('store.查看详情') }}
                    </p>
                </header>
                <ProgressSteps
                    :steps="progress?.processInfos || []"
                    :current-step="currentStep"
                    @jumpStep="handleJumpStep"
                />
            </div>

            <div class="progress-content-card">
                <template v-if="currentStep">
                    <!-- COMMIT 步骤 -->
                    <BasicInfoProgress
                        v-if="currentStep.code === STEP_CODES.COMMIT"
                        :app-detail="appDetail"
                        :show-sites-info="showSitesInfo"
                    />
                    <!-- BUILD 步骤 -->
                    <BuildLogProgress
                        v-else-if="currentStep.code === STEP_CODES.BUILD && progress?.storeBuildInfo"
                        ref="componentRef"
                        :key="buildKey"
                        :is-edit="isEdit"
                        :running-step="runningStep"
                        :current-step="currentStep"
                        v-bind="progress.storeBuildInfo"
                        @rebuild="handleRebuild"
                    />
                    <!-- TEST 步骤 -->
                    <TestInfoProgress
                        v-else-if="currentStep.code === STEP_CODES.TEST"
                        :app-detail="appDetail"
                        :is-edit="isEdit"
                        :current-step="currentStep"
                        @goStep="handleGoStep"
                    />
                    <!-- EDIT 步骤 -->
                    <FillInInformationProgress
                        v-else-if="currentStep.code === STEP_CODES.EDIT"
                        :app-detail="appDetail"
                        :is-edit="isEdit"
                        :current-step="currentStep"
                        @goStep="handleGoStep"
                    />
                    <!-- APPROVE 步骤 -->
                    <div
                        v-else-if="currentStep.code === STEP_CODES.APPROVE"
                        class="approve-container"
                    >
                        <img
                            :src="approveImg"
                            :width="200"
                        />
                        <p class="approve-text">
                            {{ currentStep.status === StepStatus.FAIL ? $t('store.审核不通过') : currentStep.status === StepStatus.SUCCESS ? $t('store.审核通过') : $t('store.应用正在审核中，请耐心等待') }}
                        </p>
                    </div>
                </template>
            </div>
        </div>
    </div>
</template>

<script setup name="ClientProgress">
    import { ref, computed, onMounted, watch } from 'vue'
    import UseInstance from '@/hook/useInstance.js'
    import useProgress from '@/hook/useProgress.js'
    import { STEP_CODES } from '@/utils/constants'
    import leaveConfirm from '@/utils/leave-confirm'
    import breadCrumbs from '@/components/bread-crumbs.vue'
    import ProgressSteps from '@/components/progressSteps.vue'
    import BasicInfoProgress from '@/components/progress/basicInfoProgress.vue'
    import BuildLogProgress from '@/components/progress/buildLogProgress.vue'
    import TestInfoProgress from '@/components/progress/testInfoProgress.vue'
    import FillInInformationProgress from '@/components/progress/fillInInformationProgress.vue'
    import approveImg from '@/assets/images/approve.png'

    const { proxy } = UseInstance()
    const { $store, $router, $route, $t } = proxy

    const isLoading = ref(false)
    const appDetail = ref(null)
    const storeId = computed(() => $route.params.storeId)
    const storeCode = computed(() => $route.params.storeCode)
    const { progress, refreshProgress, isEnd, loopProgress, runningStep, StepStatus } = useProgress()
    const showSitesInfo = computed(() => appDetail.value?.extData?.netPolicyInfo?.needVisitedSiteInfos?.length > 0)
    const buildKey = ref(0)
    const currentStep = ref(null)
    const isProgressClick = ref(false)
    const isEdit = computed(() => isEnd.value || isProgressClick.value)
    const componentRef = ref(null)
    const fillInInformationEditing = ref(false)

    const navList = computed(() => {
        return [
            { name: $t('store.工作台') },
            { name: $t('store.云研发'), to: { name: 'devxWork' } },
            { name: appDetail.value?.name, to: { name: 'statisticData', params: { code: storeCode.value, type: 'devx' } } },
            { name: `${$t('store.发布进度')}` }
        ]
    })

    onMounted(() => {
        fetchVersionDetail()
    })

    watch(runningStep, (nv) => {
        if (nv) {
            currentStep.value = nv
        }
    }, { deep: true })

    function setLoadingState (state) {
        isLoading.value = state
    }

    async function fetchVersionDetail () {
        try {
            setLoadingState(true)
            appDetail.value = await $store.dispatch('store/getComponentDetailByVersion', {
                storeId: storeId.value,
            })
        } catch (error) {
            console.log(error)
        } finally {
            setLoadingState(false)
        }
    }

    async function handleRebuild () {
        try {
            setLoadingState(true)
            await $store.dispatch('store/rebuildComponent', storeId.value)
            await loopProgress()
            buildKey.value += 1
        } catch (error) {
            console.log(error)
        } finally {
            setLoadingState(false)
        }
    }

    async function handleGoStep (step, status) {
        const processInfos = progress.value?.processInfos
        if (status === 'next') {
            await loopProgress()
            currentStep.value = processInfos[step.step]
        } else if (status === 'prev') {
            await stepBack()
            // 等待后端准备数据
            await new Promise(resolve => setTimeout(resolve, 1000))
            await loopProgress()
            const targetStep = processInfos[step.step - 2]
            currentStep.value = targetStep
            // 如果返回到 BUILD 步骤，需要重置 buildKey 强制重新创建组件
            if (targetStep?.code === STEP_CODES.BUILD) {
                buildKey.value += 1
            }
        }
    }

    async function handleJumpStep (step) {
        try {
            // 检查当前步骤是否为编辑步骤，并确认离开
            if (currentStep.value?.code === STEP_CODES.EDIT && fillInInformationEditing.value) {
                const confirmed = await leaveConfirm($bkInfo, $t)
                if (!confirmed) {
                    return
                }
            }
            currentStep.value = step
            await handleProgressJump(step)
        } catch (error) {
            console.log(error)
        }
    }

    async function handleProgressJump (step) {
        if (runningStep.value?.code !== step.code) {
            isProgressClick.value = true
            refreshProgress()
            if (componentRef.value?.refreshBuildProgress) {
                componentRef.value.refreshBuildProgress()
            }
        } else {
            isProgressClick.value = false
            await loopProgress()
        }
    }

    async function stepBack () {
        try {
            await $store.dispatch('store/stepBack', storeId.value)
            await loopProgress()
        } catch (error) {
            console.log(error)
        }
    }

    async function cancelRelease () {
        try {
            setLoadingState(true)
            await $store.dispatch('store/cancelClientRelease', storeId.value)
            refreshProgress()
            if (componentRef.value?.refreshBuildProgress) {
                componentRef.value.refreshBuildProgress()
                currentStep.value = null
            }
            $router.push({
                name: 'addReleaseVersion',
                params: {
                    storeCode: storeCode.value,
                    storeId: storeId.value,
                },
            })
        } catch (error) {
            console.log(error)
        } finally {
            setLoadingState(false)
        }
    }

    function goOverview () {
        $router.push({
            name: 'statisticData',
            params: {
                code: storeCode.value,
                type: 'devx'
            }
        })
    }

    function updateFillInInformationEditing (editing) {
        fillInInformationEditing.value = editing
    }

    defineExpose({
        updateFillInInformationEditing
    })
</script>

<style lang="scss" scoped>
.client-progress {
  height: 100%;
  flex: 1;
}

.progress-detail-container {
    width: 1200px;
    margin: 0 auto;
    height: calc(100% - 90px);
}

.progress-header-card {
    width: 1200px;
    padding: 16px 24px;
    margin-top: 24px;
    border-radius: 2px;
    background: white;
    box-shadow: 0 2px 4px 0 #1919290d;
    height: 156px;
    margin-bottom: 16px;
}

.header-content {
    display: flex;
    justify-content: space-between;
    margin-bottom: 20px;
    font-size: 14px;

    .action-btn {
        color: #3A84FF;
        cursor: pointer;

        .bk-icon {
            font-size: 16px;
            margin-right: 6px;
        }
    }
}

.progress-content-card {
    height: calc(100% - 172px);
    overflow-y: auto;
    width: 1200px;
    padding: 24px 38px;
    border-radius: 2px;
    background: white;
    box-shadow: 0 2px 4px 0 #1919290d;
}

.approve-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  
  .approve-text {
    margin-top: 20px;
  }
}
</style>
