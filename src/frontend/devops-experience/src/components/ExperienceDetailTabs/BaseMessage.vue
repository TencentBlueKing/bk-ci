<template>
    <section>
        <div class="base-content">
            <div class="base-detail-item">
                <label class="item-label">发布人：</label>
                <div class="item-content">{{ curReleaseDetail.publisher }}</div>
            </div>
            <div class="base-detail-item">
                <label class="item-label">发布时间：</label>
                <div class="item-content">{{ curReleaseDetail.publish_time }}</div>
            </div>
            <div class="base-detail-item">
                <label class="item-label">体验结束时间：</label>
                <div class="item-content">{{ curReleaseDetail.end_time }}</div>
            </div>
            <div class="base-detail-item installation-package">
                <label class="item-label">安装包：</label>
                <div class="item-content"
                    :class="{ 'installation-package-text': (curReleaseDetail.canExperience && !curReleaseDetail.expired && curReleaseDetail.online) }"
                    @click="downloadInstallation(curReleaseDetail.canExperience, curReleaseDetail.expired, curReleaseDetail.online)">{{ curReleaseDetail.installation_package }}</div>
                <div class="item-content installation-package-text" style="margin-left: 20px;" v-if="(curReleaseDetail.canExperience && !curReleaseDetail.expired && curReleaseDetail.online) && isApkOrIpa(curReleaseDetail) && isWindows && isMof"
                    @click="downloadInstallation(curReleaseDetail.canExperience, curReleaseDetail.expired, curReleaseDetail.online, 'MoF')">{{ curReleaseDetail.installation_package }}（魔方有线安装）</div>
            </div>
            <div class="base-detail-item list-item">
                <label class="item-label">体验名单：</label>
                <div class="item-content">
                    <bk-tab :active.sync="curTab">
                        <bk-tab-panel
                            v-for="(panel, index) in panels"
                            v-bind="panel"
                            :key="index"
                        >
                            <div class="bk-tab2-pane" v-if="curTab === 'experienceGroups'">
                                <div class="release-list-textarea">{{ curReleaseDetail.experienceGroups.join('; ') }}</div>
                            </div>
                            <div class="bk-tab2-pane" v-else-if="curTab === 'internal'">
                                <div class="release-list-textarea">
                                    {{ curReleaseDetail.internal_list.join(';') }}
                                </div>
                            </div>
                            <div class="bk-tab2-pane" v-else-if="curTab === 'external'">
                                <div class="release-list-textarea">{{ curReleaseDetail.external_list.join(',') }}</div>
                            </div>
                        </bk-tab-panel>
                    </bk-tab>
                </div>
            </div>
            <div class="base-detail-item version-desc-item">
                <label class="item-label">版本描述：</label>
                <div class="item-content">
                    <p class="version-desc" v-html="curReleaseDetail.desc"></p>
                </div>
            </div>
        </div>
        <div class="qrcode-box">
            <div class="qrcode-contetnt" v-if="curReleaseDetail.url"
                v-bkloading="{ isLoading: codeLoading }">
                <qrcode class="qrcode-view" :text="curReleaseDetail.url" :size="100"></qrcode>
            </div>
            <div class="qrcode-contetnt" v-else>
                <img src="../../images/nopermission-qrcode.png" class="qrcode-view">
                <div class="bg-cover">无权限体验</div>
            </div>
            <p v-if="curReleaseDetail.url">扫一扫，下载体验<span class="refresh-btn" @click="refreshUrl">刷新</span></p>
            <p v-else>如有需要，请联系<span>{{ curReleaseDetail.publisher }}</span></p>
        </div>
    </section>
</template>

<script>
    import qrcode from '@/components/devops/qrcode'
    import { mapActions } from 'vuex'
    export default {
        name: 'base-message',
        components: {
            qrcode
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
                        label: '体验组'
                    },
                    {
                        name: 'internal',
                        label: '附加内部人员'
                    },
                    {
                        name: 'external',
                        label: '附加外部人员'
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
                return type === 'APK' || type === 'IPA'
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
    .base-content {
        margin: 20px 0 0;

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
        }

        .installation-package,
        .list-item,
        .version-desc-item {
            width: 90%;
            white-space: pre-line;
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
        position: relative;
        left: 870px;
        top: -240px;
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
