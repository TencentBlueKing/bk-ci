<template>
    <div class="basic-info-wrapper">
        <!-- 基础信息 -->
        <div class="info-card">
            <p class="card-title">{{ $t('store.基础信息') }}</p>
            <div class="card-content">
                <client-basic-info :detail="appDetail" />
            </div>
        </div>

        <!-- 网络策略 -->
        <div class="info-card">
            <p class="card-title">{{ $t('store.网络策略') }}</p>
            <div class="card-content network-policy">
                <div class="info-item">
                    <span class="label">{{ $t('store.上行带宽峰值') }}</span>
                    <span class="value">{{ appDetail?.extData?.netPolicyInfo?.maxPeakBandwidth || 0 }}</span>
                </div>
                <div class="info-item">
                    <span class="label">{{ $t('store.下行带宽峰值') }}</span>
                    <span class="value">{{ appDetail?.extData?.netPolicyInfo?.minPeakBandwidth || 0 }}</span>
                </div>
                <div
                    v-if="showSitesInfo"
                    class="info-item sites-item"
                >
                    <span class="label">{{ $t('store.需要访问的站点') }}</span>
                    <div class="value">
                        <site-info-table
                            :site-infos="appDetail?.extData?.netPolicyInfo?.needVisitedSiteInfos"
                            :editable="false"
                        />
                    </div>
                </div>
                <div class="info-item scheme-item">
                    <span class="label">Scheme</span>
                    <span class="value">{{ appDetail?.extData?.urlScheme || '--' }}</span>
                </div>
            </div>
        </div>

        <!-- 版本信息 -->
        <div class="info-card">
            <p class="card-title">{{ $t('store.版本信息') }}</p>
            <div class="card-content version-info">
                <div class="info-item">
                    <span class="label">{{ $t('store.发布者') }}</span>
                    <span class="value">{{ appDetail?.versionInfo?.publisher || '--' }}</span>
                </div>
                <div class="info-item">
                    <span class="label">{{ $t('store.发布类型') }}</span>
                    <span class="value">{{ $t(`store.${appDetail?.versionInfo?.releaseType}`) || '--' }}</span>
                </div>
                <div class="info-item">
                    <span class="label">{{ $t('store.版本') }}</span>
                    <span class="value">{{ appDetail?.versionInfo?.version || '--' }}</span>
                </div>
                <div class="info-item version-content-item">
                    <span class="label">{{ $t('store.版本日志') }}</span>
                    <div class="value">
                        <mavon-editor
                            v-if="appDetail?.versionInfo?.versionContent"
                            :editable="false"
                            default-open="preview"
                            :subfield="false"
                            :toolbars-flag="false"
                            :box-shadow="false"
                            :external-link="false"
                            preview-background="#fff"
                            :language="$i18n.locale === 'en-US' ? 'en' : $i18n.locale"
                            :value="appDetail.versionInfo.versionContent"
                        />
                        <span v-else>--</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup name="BasicInfoProgress">
    import UseInstance from '@/hook/useInstance.js'
    import SiteInfoTable from '@/components/siteInfoTable.vue'
    import ClientBasicInfo from '@/components/clientBasicInfo.vue'

    const props = defineProps({
        appDetail: {
            type: Object,
            required: true
        },
        showSitesInfo: {
            type: Boolean,
            default: false
        }
    })

    defineEmits(['rebuild', 'viewLog', 'pass', 'jumpStep'])

    const { proxy } = UseInstance()
    const { $t } = proxy
</script>

<style lang="scss" scoped>
.basic-info-wrapper {
    .info-card {
        padding: 16px 24px;
        margin-bottom: 16px;

        &:last-child {
            margin-bottom: 0;
        }

        .card-title {
            margin-bottom: 20px;
            font-size: 14px;
            color: #313238;
        }

        .card-content {
            display: flex;
            font-size: 12px;

            &.network-policy,
            &.version-info {
                margin-left: 132px;
                flex-direction: column;
            }

            .info-item {
                display: flex;
                margin-bottom: 12px;

                &:last-child {
                    margin-bottom: 0;
                }

                &.scheme-item {
                    margin-top: 20px;
                }

                &.version-content-item {
                    .label {
                        margin-bottom: 8px;
                    }

                    .value {
                        width: 100%;
                    }

                    ::v-deep .v-note-wrapper {
                        max-height: 500px;
                        border: 1px solid #c4c6cc;
                        
                        .v-note-panel {
                            box-shadow: none;
                        }
                    }
                }

                .label {
                    flex-shrink: 0;
                    width: 150px;
                    color: #63656e;
                }

                .value {
                    flex: 1;
                    color: #313238;
                    min-height: 40px;

                    ::v-deep .v-note-wrapper {
                        border: none;
                        box-shadow: none;
                        
                        .v-note-panel {
                            border: none;
                        }

                        .v-show-content {
                            padding: 0;
                            background: #fff;
                        }
                    }
                }
            }
        }
    }
}
</style>
