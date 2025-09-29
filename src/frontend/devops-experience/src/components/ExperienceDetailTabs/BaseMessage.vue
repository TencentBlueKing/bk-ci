<template>
    <section class="main-content">
        <div class="base-content">
            <div class="base-detail-item">
                <label class="item-label">{{ $t('experience.publisher') }}</label>
                <div class="item-content">{{ curReleaseDetail.publisher }}</div>
            </div>
            <div class="base-detail-item">
                <label class="item-label">{{ $t('experience.publish_time') }}</label>
                <div class="item-content">{{ curReleaseDetail.publish_time }}</div>
            </div>
            <div class="base-detail-item">
                <label class="item-label">{{ $t('experience.experience_end_time') }}</label>
                <div class="item-content">{{ curReleaseDetail.end_time }}</div>
            </div>
            <div class="base-detail-item installation-package">
                <label class="item-label">{{ $t('experience.installation_package') }}</label>
                <ArtifactDownloadButton
                    :experience-id="experienceHashId"
                    :name="curReleaseDetail.installation_package"
                    :disable="!curReleaseDetail.canExperience || !curReleaseDetail.expired || !curReleaseDetail.online"
                >
                    {{ curReleaseDetail.installation_package }}
                </ArtifactDownloadButton>
                <div
                    class="item-content installation-package-text"
                    style="margin-left: 20px;"
                    v-if="(curReleaseDetail.canExperience && !curReleaseDetail.expired && curReleaseDetail.online) && isApkOrIpa(curReleaseDetail) && isWindows && isMof"
                    @click="downloadInstallation(curReleaseDetail.canExperience, curReleaseDetail.expired, curReleaseDetail.online, 'MoF')"
                >
                    {{ curReleaseDetail.installation_package }} {{ $t('experience.mof_wired_installation') }}
                </div>
            </div>
            <div class="base-detail-item list-item">
                <label class="item-label">{{ $t('experience.experience_list_label') }}</label>
                <div class="item-content">
                    <bk-tab :active.sync="curTab">
                        <bk-tab-panel
                            v-for="(panel, index) in panels"
                            v-bind="panel"
                            :key="index"
                        >
                            <div class="bk-tab2-pane">
                                <div class="release-list-textarea">{{ curReleaseDetail[curTab].join('; ') }}</div>
                            </div>
                        </bk-tab-panel>
                    </bk-tab>
                </div>
            </div>
            <div class="base-detail-item version-desc-item">
                <label class="item-label">{{ $t('experience.version_description') }}</label>
                <div class="item-content">
                    <p
                        class="version-desc"
                        v-html="curReleaseDetail.desc"
                    ></p>
                </div>
            </div>
        </div>
        <div
            v-if="!isWindowsExp"
            class="qrcode-box"
        >
            <div
                class="qrcode-contetnt"
                v-if="curReleaseDetail.url"
                v-bkloading="{ isLoading: codeLoading }"
            >
                <qrcode
                    class="qrcode-view"
                    :text="curReleaseDetail.url"
                    :size="100"
                ></qrcode>
            </div>
            <div
                class="qrcode-contetnt"
                v-else
            >
                <img
                    src="../../images/nopermission-qrcode.png"
                    class="qrcode-view"
                >
                <div class="bg-cover">{{ $t('experience.no_permission_experience') }}</div>
            </div>
            <p v-if="curReleaseDetail.url">
                {{ $t('experience.scan_to_download') }}<span
                    class="refresh-btn"
                    @click="refreshUrl"
                >{{ $t('experience.refresh') }}</span>
            </p>
            <p v-else>{{ $t('experience.contact_if_needed', [curReleaseDetail.publisher]) }}</p>
        </div>
    </section>
</template>

<script>
    import ArtifactDownloadButton from '@/components/ArtifactDownloadButton'
    import qrcode from '@/components/devops/qrcode'
    import { platformList } from '@/utils/util'
    import { mapActions } from 'vuex'
    
    export default {
        name: 'base-message',
        components: {
            qrcode,
            ArtifactDownloadButton
        },
        props: {
            downloadInstallation: {
                type: Function,
                required: true
            },
            curReleaseDetail: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                codeLoading: false,
                curTab: 'experienceGroups'
            }
        },
        computed: {
            isWindowsExp () {
                return this.curReleaseDetail.platform === platformList[3].id
            },
            projectId () {
                return this.$route.params.projectId
            },
            experienceHashId () {
                return this.$route.params.experienceId
            },
            panels () {
                return [
                    {
                        name: 'experienceGroups',
                        label: this.$t('experience.experience_group')
                    },
                    {
                        name: 'internal_list',
                        label: this.$t('experience.additional_internal_members')
                    },
                    {
                        name: 'external_list',
                        label: this.$t('experience.additional_external_members')
                    }
                ]
            }
        },
        methods: {
            ...mapActions('experience', [
                'requestExternalUrl'
            ]),
            isApkOrIpa (curReleaseDetail) {
                const type = curReleaseDetail.installation_package.toUpperCase().substring(curReleaseDetail.installation_package.lastIndexOf('.') + 1)
                return ['APK', 'IPA', 'HAP'].includes(type)
            },
            async refreshUrl () {
                this.codeLoading = true

                try {
                    const res = await this.requestExternalUrl({
                        projectId: this.projectId,
                        experienceHashId: this.experienceHashId
                    })

                    this.curReleaseDetail.url = res.url
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.codeLoading = false
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../scss/conf';
    .main-content{
        position: relative;
    }
    .base-content {
        margin: 20px 0 0;
        display: inline-flex;
        flex-wrap: wrap;

        .base-detail-item {
            display: flex;
            float: left;
            margin-right: 94px;
            margin-bottom: 20px;
        }

        .item-label,
        .item-content {
            font-size: 14px;
        }

        .item-label {
            margin-right: 14px;
            width: 100px;
            text-align: right;
            flex-shrink: 0;
        }

        .installation-package,
        .list-item,
        .version-desc-item {
            width: 90%;
            line-height: 20px;
        }

        .list-item {
            background: white;
        }

        .installation-package-text {
            line-height: 19px;
            color: $primaryColor;
            cursor: pointer;
        }

        .bk-tab-list,
        .version-desc {
            width: 700px;
        }

        .release-list-textarea,
        .version-desc {
            word-break: break-all;
            overflow: auto;
            border: none;
        }

        .version-desc {
            padding: 0;
            height: auto;
        }
    }

    .qrcode-box {
        position: absolute;
        right: 50px;
        top: 50px;
        text-align: center;
        display: inline-block;

        img {
            width: 120px;
            height: 120px;
        }

        p {
            margin-top: 4px;
            font-size: 14px;
        }

        .refresh-btn {
                margin-left: 6px;
                color: $primaryColor;
                cursor: pointer;
        }

        .bg-cover {
            position: absolute;
            top: 0;
            left: 50%;
            margin-left: -60px;
            padding-top: 48px;
            width: 120px;
            height: 120px;
            background-color: rgba(0, 0, 0, 0.6);
            color: #fff;
            font-size: 16px;
        }
    }
</style>
