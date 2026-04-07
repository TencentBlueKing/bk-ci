<template>
    <div class="devops-home-page">
        <div class="devops-home-content">
            <section>
                <accordion>
                    <accordion-item :init-content-show="true">
                        <span
                            slot="header"
                            class="home-accordion-header"
                        >{{ $t('recentVisiteService') }}</span>
                        <div
                            slot="content"
                            class="recent-visit-service-list"
                        >
                            <template v-if="recentVisitService.length">
                                <router-link
                                    v-for="service in recentVisitService"
                                    :key="service.key"
                                    :to="addConsole(service.link_new)"
                                >
                                    <img
                                        v-if="isAbsoluteUrl(service.logoUrl)"
                                        :src="service.logoUrl"
                                        class="recent-logo-icon"
                                    />
                                    <Logo
                                        v-else
                                        :name="service.logoUrl"
                                        size="16"
                                    />
                                    {{ serviceName(service.name) }}
                                </router-link>
                            </template>
                            <p
                                v-else
                                class="no-recent-service"
                            >
                                {{ $t("noRecentVisiteService") }}
                                <span @click="updateShowAllService(true)">{{ $t('allService') }}</span>
                            </p>
                        </div>
                    </accordion-item>
                    <accordion-item
                        :init-content-show="isAllServiceListShow"
                        @update:contentShow="updateShowAllService"
                    >
                        <p
                            slot="header"
                            class="all-service-header"
                        >
                            {{ $t('allService') }}
                            
                            <span class="service-count">{{ $t("sumService", { serviceCount }) }}</span>
                        </p>
                        <NavBox
                            slot="content"
                            class="all-service-list"
                            column-width="190px"
                            :with-hover="false"
                            :services="services"
                        />
                    </accordion-item>
                </accordion>

                <div class="bkdevops-box">
                    <h2>{{ $t('slogan') }}</h2>
                    <span
                        v-for="(item, index) in funcArray"
                        :key="index"
                        :style="{ left: item.left }"
                    >{{ item.label }}</span>
                    <div class="bkdevops-button">
                        <a
                            :href="BKCI_DOCS.BKCI_DOC"
                            target="_blank"
                        >
                            <bk-button
                                theme="primary"
                                icon-right="angle-double-right"
                            >
                                {{ $t('accessGuide') }}
                            </bk-button>
                        </a>
                    </div>
                </div>

                <div
                    class="devops-news"
                    v-if="news.length > 0"
                >
                    <header>
                        <p class="title">
                            {{ $t("latestNews") }}
                        </p>
                    </header>

                    <div class="devops-news-content">
                        <p
                            v-for="(item, index) in news"
                            :key="item.name"
                            class="news-item"
                        >
                            <a
                                target="_blank"
                                :href="item.link"
                            >
                                <span v-if="index === 0">[{{ $t("latest") }}]</span>
                                {{ item.name }}
                            </a>
                            <span>{{ item.create_time }}</span>
                        </p>
                    </div>
                </div>
            </section>
            <aside>
                <article>
                    <h2>
                        {{ $t("bkdevopsTitle") }}
                        <bk-tag
                            v-if="BK_CI_VERSION"
                            theme="info"
                            type="stroke"
                        >
                            {{ BK_CI_VERSION.trim() }}
                        </bk-tag>
                    </h2>
                    <p>
                        {{ $t("bkdevopsDesc") }}
                        <a
                            :href="BKCI_DOCS.BKCI_DOC"
                            class="more"
                            target="_blank"
                        >{{ $t("learnMore") }}</a>
                    </p>
                </article>
                <article class="product-download">
                    <h2>
                        {{ $t("产品下载") }}
                    </h2>
                    <div
                        v-for="(section, sIndex) in downloadSections"
                        :key="sIndex"
                        class="download-section"
                    >
                        <div class="section-header">
                            <p class="section-title">{{ section.title }}</p>
                            <p class="section-desc">{{ section.desc }}</p>
                        </div>
                        <div class="platform-cards">
                            <div
                                v-for="(platform, pIndex) in section.platforms"
                                :key="pIndex"
                                class="platform-card"
                                :class="{
                                    disabled: platform.disabled,
                                    repo: section.isRepo,
                                    large: platform.isLarge
                                }"
                            >
                                <div class="card-header">
                                    <Icon
                                        :name="platform.icon"
                                        size="32"
                                    />
                                    <span
                                        v-if="platform.hasCornerBadge"
                                        class="corner-badge"
                                        :class="{ 'qr-code': platform.cornerBadgeType === 'qr-code' }"
                                    >
                                        <Icon
                                            :name="platform.cornerBadgeType === 'qr-code' ? 'qrcode' : 'download'"
                                            :size="platform.cornerBadgeType === 'qr-code' ? 14 : 12"
                                        />
                                    </span>
                                </div>
                                <div class="card-body">
                                    <span class="platform-name">{{ platform.name }}</span>
                                </div>
                                <!-- 二维码悬浮显示 -->
                                <span
                                    v-if="platform.type === 'qr'"
                                    class="qr-hover"
                                >
                                    <img
                                        :src="platform.qrSrc"
                                        alt=""
                                    >
                                    {{ $t("扫码下载") }}
                                </span>
                                <!-- 下载按钮悬浮显示 -->
                                <span
                                    v-if="platform.type === 'download'"
                                    class="download-hover"
                                    @click="handlePlatformClick(platform)"
                                >
                                    <span class="down-icon">
                                        <Icon
                                            name="download"
                                            size="18"
                                        />
                                    </span>
                                    {{ $t("点击下载") }}
                                    <span class="file-name">{{ platform.fileName }}</span>
                                </span>
                            </div>
                        </div>
                    </div>
                </article>
                <article v-if="related.length > 0">
                    <h2>{{ $t("relatedLink") }}</h2>
                    <div>
                        <a
                            v-for="item in related"
                            :key="item.name"
                            :href="item.link"
                            target="_blank"
                        >
                            {{ item.name }}
                        </a>
                    </div>
                </article>
            </aside>
        </div>
        <section class="devops-home-footer">
            <template v-if="hasSharedResUrl">
                <p
                    class="item"
                    v-html="platformInfo.i18n.footerInfoHTML"
                />
                <p class="bkci-copyright">{{ platformInfo.footerCopyrightContent }}</p>
            </template>
            <template v-else>
                <section class="devops-home-footer">
                    <div class="item">
                        <a
                            href="https://wpa1.qq.com/KziXGWJs?_type=wpa&qidian=true"
                            target="_blank"
                        >{{ $t('technicalSupport') }}</a> |
                        <a
                            href="https://bk.tencent.com/s-mart/community/"
                            target="_blank"
                        >{{ $t('communityForum') }}</a> |
                        <a
                            href="https://bk.tencent.com/index/"
                            target="_blank"
                        >{{ $t('ProductOfficialWebsite') }}</a>
                    </div>
                    <p class="bkci-copyright">Copyright © 2012-{{ getFullYear() }} Tencent BlueKing. All Rights Reserved {{ BK_CI_VERSION.trim() }}</p>
                </section>
            </template>
        </section>
        <consult-tools />
    </div>
</template>

<script lang="ts">
    import { isAbsoluteUrl, urlJoin } from '@/utils/util'
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { Action, Getter, State } from 'vuex-class'
    import { Accordion, AccordionItem } from '../components/Accordion/index'
    import ConsultTools from '../components/ConsultTools/index.vue'
    import Logo from '../components/Logo/index.vue'
    import NavBox from '../components/NavBox/index.vue'
    import Icon from '../components/Icon/index.vue'
    import devopsAppQrcode from '../assets/images/devopsapp-qrcode.png'

    @Component({
        components: {
            NavBox,
            Accordion,
            AccordionItem,
            Logo,
            ConsultTools,
            Icon
        }
    })
    export default class Home extends Vue {
        @State services
        @State news
        @State related
        @Action fetchLinks
        @Getter platformInfo
        isAllServiceListShow: boolean = false
        isAbsoluteUrl = isAbsoluteUrl
        BK_CI_VERSION: string = window.BK_CI_VERSION
        hasSharedResUrl: boolean = !!(window.BK_SHARED_RES_URL)

        get funcArray (): any[] {
            const funcArray = ['issueLabel', 'developLabel', 'testLabel', 'deployLabel', 'operationLabel']
            return funcArray.map((item, index) => ({
                label: this.$t(item),
                left: `${index * 135 + 92}px`
            }))
        }

        get recentVisitService (): any[] {
            const recentVisitService = localStorage.getItem('recentVisitService')
            const recentVisitServiceList = recentVisitService ? JSON.parse(recentVisitService) : []
            const serviceList = recentVisitServiceList.map(service => {
                const serviceObj = window.serviceObject.serviceMap[service.key] || {}
                return {
                    ...service,
                    ...serviceObj
                }
            }).filter(item => item.status !== 'planning' && item.status !== 'developing')
            localStorage.setItem('recentVisitService', JSON.stringify(serviceList))
            return serviceList
        }

        get serviceCount (): number {
            // 减去1是因为项目管理服务是隐藏的
            const count = this.services.reduce((sum, service) => {
                sum += (service.children.length - 1)
                return sum
            }, 0)
            // 我的项目服务不展示，所以减去1
            return count - 1
        }

        get downloadSections (): any[] {
            return [
                {
                    title: this.$t('蓝盾 APP'),
                    desc: this.$t('支持将 CI 编译产物安装至移动设备，便于测试与体验。'),
                    platforms: [
                        {
                            icon: 'apple',
                            name: 'iOS',
                            type: 'qr',
                            qrSrc: devopsAppQrcode,
                            hasCornerBadge: true,
                            cornerBadgeType: 'qr-code'
                        },
                        {
                            icon: 'android-full',
                            name: 'Android',
                            type: 'qr',
                            qrSrc: devopsAppQrcode,
                            hasCornerBadge: true,
                            cornerBadgeType: 'qr-code'
                        },
                        {
                            icon: 'harmony',
                            name: 'HarmonyOS',
                            type: 'qr',
                            qrSrc: devopsAppQrcode,
                            hasCornerBadge: true,
                            cornerBadgeType: 'qr-code'
                        }
                    ]
                },
                {
                    title: this.$t('制品库客户端'),
                    desc: this.$t('桌面端制品库，支持预约下载与下载加速，支持 Windows 应用的版本体验。'),
                    isRepo: true,
                    platforms: [
                        {
                            icon: 'windows',
                            name: 'Windows',
                            type: 'download',
                            downloadUrl: 'https://bkrepo.woa.com/generic/bk-repo/public/bkdrive/BKDrive-x64.exe',
                            fileName: 'BKDrive-x64.exe',
                            hasCornerBadge: true,
                            cornerBadgeType: 'download',
                            isLarge: true
                        },
                        {
                            icon: 'apple',
                            name: 'macOS',
                            type: 'download',
                            downloadUrl: 'https://bkrepo.woa.com/generic/bk-repo/public/bkdrive/BKDrive-arm64.pkg',
                            fileName: 'BKDrive-arm64.pkg',
                            hasCornerBadge: true,
                            cornerBadgeType: 'download'
                        }
                    ]
                }
            ]
        }

        updateShowAllService (show: boolean): void {
            this.isAllServiceListShow = show
        }

        addConsole (link: string): string {
            return urlJoin('/console/', link)
        }

        serviceName (name = ''): string {
            const charPos = name.indexOf('(')
            return charPos > -1 ? name.slice(0, charPos) : name
        }

        getFullYear () {
            return (new Date()).getFullYear()
        }

        downloadRepoClient () {
            window.location.href = '/path/to/repo-client.exe'
        }

        handlePlatformClick (platform: any) {
            if (platform.disabled || !platform.downloadUrl) {
                return
            }
            window.location.href = platform.downloadUrl
        }

        created () {
            this.fetchLinks({
                type: 'news'
            })
            this.fetchLinks({
                type: 'related'
            })
        }
    }
</script>
<style lang="scss">
    @import '../assets/scss/conf';
    .devops-home-page {
        display: flex;
        flex-direction: column;
        align-items: center;
        overflow: auto;
        width: 100%;
    }
    .devops-home-content {
        display: flex;
        flex: 1;
        justify-content: center;
        width: 1280px;
        padding: 30px 0 100px 0;

        > section {
            width: 800px;
            margin-right: 40px;
            .recent-visit-service-list {
                display: flex;
                flex: 1;
                padding: 22px 0 0 30px;
                .no-recent-service {
                    width: 100%;
                    text-align: center;
                    padding: 0 31px 22px 0;
                    > span {
                        cursor: pointer;
                        color: $primaryColor;
                    }
                }
                > a {
                    @include ellipsis(190px);
                    width: 190px;
                    margin-bottom: 18px;
                    line-height: 24px;
                    font-size: 13px;
                    display: flex;
                    align-items: center;
                    cursor: pointer;

                    > svg,
                    .recent-logo-icon {
                        margin-right: 6px;
                    }

                    .recent-logo-icon {
                        width: 16px;
                        height: 16px;
                    }
                }
            }

            .home-accordion-header,
            .all-service-header {
                font-size: 14px;
            }

            .all-service-header {
                flex: 1;
                display: flex;
                .service-count {
                    justify-items: flex-end;
                    margin-left: auto;
                    margin-right: 12px;
                    font-size: 14px;
                }
            }

            .all-service-list {
                padding: 0 0 14px 30px;
                .menu-column {
                    width: 162px;
                    margin-right: 30px;
                    .service-item {
                        padding: 0;
                        > h4 {
                            .devops-icon {
                                opacity: 1;
                            }
                        }

                        .menu-item {
                            padding-left: 0;
                            padding-right: 0;
                            > a {
                                padding-right: 0;
                            }
                            &:last-child {
                                padding-bottom: 0;
                            }
                        }
                    }
                }
            }

            .bkdevops-box {
                position: relative;
                height: 280px;
                margin-bottom: 39px;
                border-radius: 2px;
                color: $fontWeightColor;
                border: 1px solid $borderWeightColor;
                overflow: hidden;
                background: url('../assets/images/index_ad.jpg');
                > h2 {
                    font-weight: normal;
                    font-size: 22px;
                    display: block;
                    width: 100%;
                    text-align: center;
                    position: absolute;
                    top: 30px;
                    left: 0;
                }

                > span {
                    position: absolute;
                    font-size: 16px;
                    bottom: 91px;
                    width: 73px;
                    @include ellipsis();
                    text-align: center;
                }
                .bkdevops-button {
                    position: absolute;
                    bottom: 30px;
                    left: 0;
                    width: 100%;
                    text-align: center;
                    .devops-icon {
                        font-size: 12px;
                    }
                }
            }

            .devops-news {
                > header {
                    display: flex;
                    padding-bottom: 9px;
                    border-bottom: 1px solid $borderWeightColor;
                    position: relative;
                    justify-content: space-between;
                    > p {
                        font-size: 16px;
                    }

                    > a {
                        font-size: 12px;
                        color: $primaryColor;
                    }
                }

                &-content {
                    .news-item {
                        display: flex;
                        justify-content: space-between;
                        line-height: 60px;
                        border-bottom: 1px solid $borderColor;
                        padding: 0 10px;
                        > a > span {
                            color: $dangerColor;
                        }
                    }
                }
            }
        }

        > aside {
            width: 360px;
            align-self: flex-start;
            article {
                margin-bottom: 35px;
                > h2 {
                    font-size: 16px;
                    color: $fontBoldColor;
                    margin-top: 0;
                    margin-bottom: 10px;
                    font-weight: normal;
                }
                > p {
                    font-size: 13px;
                    color: $fontWeightColor;
                    line-height: 24px;
                }
                a {
                    color: $primaryColor;
                }
            }

            .product-download {

                .download-section {
                    margin-bottom: 16px;
                    padding: 16px;
                    border-radius: 2px;
                    background: linear-gradient(108deg, #EAF2FF 21.9%, #E6F7FF 82.93%);

                    .section-header {
                        margin-bottom: 16px;

                        .section-title {
                            color: #313238;
                            text-align: center;
                            font-family: "Microsoft YaHei";
                            font-size: 16px;
                        }

                        .section-desc {
                            font-size: 12px;
                            color: #4d4f56;
                            line-height: 20px;
                            margin: 4px 0 16px;
                        }
                    }

                    .platform-cards {
                        display: flex;
                        gap: 8px;
                        flex-wrap: wrap;

                        .platform-card {
                            position: relative;
                            display: flex;
                            flex-direction: column;
                            align-items: center;
                            justify-content: center;
                            position: relative;
                            flex: 1;
                            height: 108px;
                            padding: 8px 16px;
                            background: #FFFFFF;
                            border-radius: 8px;
                            box-shadow: 0 0 16px 0 #1271d01a;
                            cursor: pointer;

                            &:hover:not(.disabled):not(.repo) {
                                .card-header,
                                .card-body {
                                    opacity: 0;
                                }

                                .qr-hover {
                                    opacity: 1;
                                }
                            }

                            &.disabled {
                                cursor: not-allowed;
                                opacity: 0.6;
                                background: #FAFBFD;

                                .card-header {
                                    color: #979BA5;

                                    .corner-badge {
                                        color: #DCDEE5;
                                    }
                                }

                                .card-body .platform-name {
                                    color: #4D4F56;
                                }
                            }

                            .card-header {
                                margin-bottom: 10px;
                                color: #3A84FF;

                                .corner-badge {
                                    position: absolute;
                                    top: 0;
                                    right: 0;
                                    width: 28px;
                                    height: 28px;
                                    padding: 6px;
                                    color: #4D4F56;
                                }
                                .qr-code {
                                    background: linear-gradient(45deg, #B9CDFA 50%, #DCE6FA 89.29%);
                                    clip-path: polygon(100% 0, 0 0, 100% 100%);
                                }
                            }

                            .card-body {
                                display: flex;
                                flex-direction: column;
                                align-items: center;
                                position: relative;

                                .platform-name {
                                    font-size: 12px;
                                    color: #3A84FF;
                                    margin-bottom: 4px;
                                }

                                > svg:last-child {
                                    position: absolute;
                                    top: -30px;
                                    right: -35px;
                                    color: #3A84FF;
                                    opacity: 0;
                                }

                                .coming-soon {
                                    font-size: 12px;
                                    color: #979BA5;
                                }
                            }

                            .qr-hover {
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                flex-direction: column;
                                gap: 8px;
                                position: absolute;
                                top: 50%;
                                left: 50%;
                                transform: translate(-50%, -50%);
                                color: #3A84FF;
                                font-size: 12px;
                                opacity: 0;
                                pointer-events: none;

                                img {
                                    width: 58px;
                                    height: 58px;
                                }
                            }

                            .download-hover {
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                position: absolute;
                                top: 50%;
                                left: 50%;
                                transform: translate(-50%, -50%);
                                font-size: 14px;
                                color: #3A84FF;
                                opacity: 0;
                                pointer-events: none;
                                white-space: nowrap;
                                cursor: pointer;
                            }
                        }

                        .large {
                            width: 203px;
                            flex: none;
                        }

                        .repo {
                            &.large {
                                width: 203px;
                            }

                            &:hover:not(.disabled) {
                                .card-header,
                                .card-body {
                                    opacity: 0;
                                }

                                .download-hover {
                                    display: flex;
                                    flex-direction: column;
                                    align-items: center;
                                    justify-content: center;
                                    font-size: 12px;
                                    opacity: 1;
                                    pointer-events: auto;

                                    .down-icon {
                                        width: 40px;
                                        height: 40px;
                                        border-radius: 50%;
                                        background-color: #3A84FF;
                                        color: #fff;
                                        margin-bottom: 4px;
                                        display: flex;
                                        align-items: center;
                                        justify-content: center;
                                    }

                                    .file-name {
                                        display: inline-block;
                                        color: #979BA5;
                                        word-break: break-all;
                                        text-align: center;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    .devops-home-footer {
        text-align: center;
        font-size: 12px;
        padding-bottom: 20px;
        .item {
            margin-bottom: 5px;
        }
        a {
            color: #3c96ff;
        }
    }
</style>
