<template>
    <div v-bkloading="{ isLoading: isLoading, zIndex: 3 }">
        <bread-crumbs
            :bread-crumbs="navList"
            type="devx"
        ></bread-crumbs>
        <bk-form
            ref="formRef"
            :model="formValue"
            :rules="rules"
            class="edit-client-form"
        >
            <div class="addVersion-content">
                <div class="basic-info">
                    <p class="section-title">{{ $t('store.基础信息') }}</p>
                    <BasicInfo :basic-info.sync="basicInfo" />
                </div>

                <div class="network-strategy">
                    <p class="section-title">{{ $t('store.网络策略') }}</p>
                    <NetPolicy :net-policy-info.sync="netPolicyInfo" />

                    <bk-form-item
                        label="Scheme"
                        property="urlScheme"
                        required
                        error-display-type="normal"
                    >
                        <bk-input
                            v-model="urlScheme"
                            :clearable="true"
                        />
                    </bk-form-item>
                </div>

                <div class="version-info">
                    <p class="section-title">{{ $t('store.版本信息') }}</p>
                    <VersionInfoWidget
                        :version-info.sync="versionInfo"
                        :has-package="hasPackage"
                        :uploading="uploading"
                        :store-code="storeCode"
                        :package-version="packageVersion"
                        @inputChange="handleInputChange"
                        @updateReleaseType="updateReleaseType"
                    />
                </div>

                <div
                    v-if="sourceType !== 'OFFICIAL_HOSTING'"
                    class="version-page"
                >
                    <p class="section-title">{{ $t('store.版本包') }}</p>
                    <bk-form-item
                        :label="$t('store.上传软件安装包')"
                        property="hasPackage"
                        error-display-type="normal"
                        required
                    >
                        <VersionPackage
                            ref="packageRef"
                            :store-code="storeCode"
                            :store-id="latestVersionDetail?.storeId || ''"
                            :version="versionInfo.version"
                            :last-version="versionInfo.lastVersion"
                            :release-type="versionInfo.releaseType"
                            @uploaded="handlePackageUpload"
                            @delete="handleDelete"
                            @progress="handleProgress"
                        />
                    </bk-form-item>
                </div>
            </div>

            <div class="submit-bar">
                <bk-button
                    theme="primary"
                    :disabled="isLoading"
                    @click="submit"
                >
                    {{ $t('store.提交') }}
                </bk-button>
                <bk-button
                    class="button"
                    :disabled="isLoading"
                    @click="handleCancel"
                >
                    {{ $t('store.取消') }}
                </bk-button>
            </div>
        </bk-form>
    </div>
</template>

<script setup name="AddVersion">
    import { ref, reactive, computed, onMounted, nextTick } from 'vue'
    import UseInstance from '@/hook/useInstance.js'
    import { STORE_TYPE, ReleaseTypeEnum, CANCEL_RE_RELEASE } from '@/utils/constants'
    import leaveConfirm from '@/utils/leave-confirm'
    import breadCrumbs from '@/components/bread-crumbs.vue'
    import BasicInfo from '@/components/basicInfo.vue'
    import NetPolicy from '@/components/netPolicy.vue'
    import VersionInfoWidget from '@/components/versionInfoWidget.vue'
    import VersionPackage from '@/components/versionPackage.vue'

    const { proxy } = UseInstance()
    const { $store, $router, $route, $bkMessage, $bkInfo, $t } = proxy

    const formRef = ref(null)
    const packageRef = ref(null)
    const latestVersionDetail = ref(null)
    const isLoading = ref(false)
    const storeCode = computed(() => $route.params.storeCode)
    const hasPackage = ref(false)
    const packageVersion = ref('')
    const uploading = ref(false)
    const sourceType = ref('')

    const basicInfo = ref({
        name: '',
        classifyCode: '',
        logoUrl: '',
        summary: '',
        description: '',
    })

    const netPolicyInfo = ref({
        maxPeakBandwidth: 0,
        minPeakBandwidth: 0,
        needVisitedSiteInfos: [],
    })

    const versionInfo = ref({
        publisher: '',
        releaseType: ReleaseTypeEnum.NEW,
        version: '',
        versionContent: '',
        lastVersion: '',
    })

    const urlScheme = ref('')

    const formValue = computed(() => ({
        ...basicInfo.value,
        ...versionInfo.value,
        ...netPolicyInfo.value,
        urlScheme: urlScheme.value,
        hasPackage: hasPackage.value,
    }))

    const rules = {
        name: [
            {
                required: true,
                pattern: /^(?!.*^\s)(?!.*\s$)[\u4e00-\u9fa5a-zA-Z0-9\-_.\s]{1,40}$/,
                message: $t('由汉字、英文字母、数字、连字符(-)、下划线(_)或点(.)组成，不超过40个字符'),
                trigger: 'blur',
            },
        ],
        classifyCode: [
            {
                required: true,
                message: $t('应用分类不能为空'),
                trigger: 'change',
            },
        ],
        logoUrl: [
            {
                required: true,
                message: $t('Logo必填'),
                trigger: 'blur',
            },
        ],
        summary: [
            {
                required: true,
                message: $t('应用简介不能为空'),
                trigger: 'blur',
            },
        ],
        hasPackage: [
            {
                required: true,
                validator: (value) => value,
                message: $t('请上传版本包'),
                trigger: 'change',
            },
        ],
        urlScheme: [
            {
                required: true,
                pattern: /^[a-z][a-z\d-]*$/i,
                message: $t('Scheme只能由大小写字母、数字和中划线组成，首字母必须为字母'),
                trigger: 'blur',
            },
        ],
        publisher: [
            {
                required: true,
                message: $t('发布者不能为空'),
                trigger: 'change',
            },
        ],
        versionContent: [
            {
                required: true,
                message: $t('版本日志不能为空'),
                trigger: 'change',
            },
        ],
        version: [
            {
                required: true,
                message: $t('版本号不能为空'),
                trigger: 'blur',
            },
            {
                pattern: /^[0-9][a-zA-Z0-9._-]{0,31}$/,
                message: $t(
                    '由数字、英文字母、点(.)、中划线(-)、下划线(_)组成，且以数字开头，不超过 32 个字符'
                ),
                trigger: 'blur',
            },
        ],
    }

    const versionMismatch = computed(() => {
        return (
            versionInfo.value.releaseType !== CANCEL_RE_RELEASE && versionInfo.value.lastVersion === versionInfo.value.version
        )
    })

    const navList = computed(() => {
        return [
            { name: $t('store.工作台') },
            { name: $t('store.云研发'), to: { name: 'devxWork' } },
            { name: `${$t('store.新增版本')}` }
        ]
    })

    onMounted(() => {
        fetchVersionDetail()
    })

    async function fetchVersionDetail () {
        try {
            isLoading.value = true
            const [appDetail, { showVersionList }] = await Promise.all([
                $store.dispatch('store/getComponentDetailByVersion', {
                    storeId: $route.params.storeId,
                }),
                $store.dispatch('store/showVersionInfo', storeCode.value),
            ])

            latestVersionDetail.value = appDetail
            sourceType.value = latestVersionDetail.value.extData?.sourceType

            Object.assign(basicInfo.value, {
                name: latestVersionDetail.value.name,
                logoUrl: latestVersionDetail.value.logoUrl ?? '',
                classifyCode: latestVersionDetail.value.classify?.classifyCode ?? '',
                summary: latestVersionDetail.value.summary ?? '',
                description: latestVersionDetail.value.description ?? '',
            })
            versionInfo.value = {
                ...versionInfo.value,
                versionContent: latestVersionDetail.value.versionInfo?.versionContent,
            }

            showVersionList.some((item) => {
                if (item.defaultFlag) {
                    Object.assign(versionInfo.value, {
                        releaseType: item.releaseType,
                        lastVersion: item.lastVersion,
                        ...(item.releaseType === 'CANCEL_RE_RELEASE' ? { version: item.lastVersion } : {}),
                    })
                    return true
                }
                return false
            })

            Object.assign(netPolicyInfo.value, {
                maxPeakBandwidth: latestVersionDetail.value.extData?.netPolicyInfo?.maxPeakBandwidth ?? 0,
                minPeakBandwidth: latestVersionDetail.value.extData?.netPolicyInfo?.minPeakBandwidth ?? 0,
                needVisitedSiteInfos:
        latestVersionDetail.value.extData?.netPolicyInfo?.needVisitedSiteInfos ?? [],
            })

            urlScheme.value = latestVersionDetail.value.extData?.urlScheme ?? ''
        } catch (error) {
            $bkMessage({
                theme: 'error',
                message: error.message || error,
            })
        } finally {
            isLoading.value = false
        }
    }

    async function submit () {
        if (versionMismatch.value) {
            $bkMessage({
                theme: 'error',
                message: $t('store.版本号已存在', [versionInfo.value.version]),
            })
            return
        }

        try {
            console.log(netPolicyInfo.value, '网络策略提交的数据')
            console.log(basicInfo.value, '基础信息提交的数据')
            console.log(versionInfo.value, '版本信息提交的数据')
            isLoading.value = true
            const valid = await formRef.value?.validate()
            if (valid) {
                const result = await $store.dispatch('store/addVersion', {
                    projectCode: latestVersionDetail.value.initProjectCode,
                    baseInfo: {
                        ...basicInfo.value,
                        storeCode: storeCode.value,
                        storeType: STORE_TYPE,
                        versionInfo: versionInfo.value,
                        extBaseInfo: {
                            urlScheme: urlScheme.value,
                            netPolicyInfo: netPolicyInfo.value,
                        },
                    },
                })

                if (result.storeId) {
                    // 刷新应用详情
                    const detail = await $store.dispatch('store/getComponentDetail', storeCode.value)
                    $store.commit('store/SET_APP_DETAIL', detail)

                    $router.push({
                        name: 'progressDetail',
                        params: {
                            storeCode: storeCode.value,
                            storeId: result.storeId,
                        },
                    })
                }
            }
        } catch (error) {
            $bkMessage({
                theme: 'error',
                message: error.message || error.content || error,
            })
        } finally {
            isLoading.value = false
        }
    }

    async function handleCancel () {
        try {
            const res = await leaveConfirm($bkInfo, $t)
            if (!res) return
    formRef.value?.clearError()
            $router.back()
        } catch (error) {
            console.error(error)
        }
    }

    function updateReleaseType (releaseType) {
        versionInfo.value.releaseType = releaseType
    }

    function handlePackageUpload (result) {
        hasPackage.value = true
        packageVersion.value = versionInfo.value.version
        nextTick(() => {
          formRef.value?.validate?.()
        })
    }

    function handleDelete () {
        uploading.value = false
        hasPackage.value = false
    }

    function handleProgress (result) {
        uploading.value = result
    }

    function handleInputChange () {
        hasPackage.value = false
        packageRef.value?.empty?.()
    }
</script>

<style lang="scss" scoped>
.edit-client-form {
  height: calc(100vh - 166px);
  overflow-y: auto;
}

.addVersion-content {
  width: 1220px;
  padding: 20px;
  margin: auto;

  .basic-info {
    .bk-form-label {
      width: 48px;
      height: 20px;
      font-size: 12px;
      color: #63656e;
      letter-spacing: 0;
      text-align: right;
      line-height: 32px;
    }

    .bk-upload-trigger--picture {
      width: 88px;
      height: 88px;
    }
  }

  .network-strategy {
    .box {
      width: 24px;
      height: 24px;
      margin: 3px 4px;
      line-height: 24px;
      text-align: center;
      transform: scaleX(-1);
      background: #f5f7fa;
      border-radius: 1px;
      cursor: pointer;
    }

    .crement {
      color: #979ba5;
    }

    .disable {
      color: #c4c6cc;
    }
  }

  .version-info {
    .bk-radio-label {
      font-size: 12px;
      color: #63656e;
      letter-spacing: 0;
      line-height: 20px;
    }
  }

  .basic-info,
  .network-strategy,
  .version-page,
  .version-info {
    width: 100%;
    padding: 16px 24px;
    border-radius: 2px;
    background-color: #fff;
    box-shadow: 0 2px 4px 0 #1919290d;
  }
}

.section-title {
  font-size: 14px;
  color: #313238;
  height: 56px;
}

.network-strategy,
.version-info,
.version-page {
  margin-top: 16px;

  &:first-child {
    margin-top: 0;
  }
}

.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 48px;
  padding: 8px 170px;
  background: #ffffff;
  z-index: 2;
  box-shadow: 0 -2px 4px 0 rgba(0, 0, 0, 0.08);

  .button {
    margin-right: 8px;
  }
}
</style>
