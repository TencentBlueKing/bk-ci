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
                                    @click.native="updateDocumnetTitle(service.link_new)"
                                >
                                    <img v-if="isAbsoluteUrl(service.logoUrl)" :src="service.logoUrl" class="recent-logo-icon" />
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
                            :get-document-title="getDocumentTitle"
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
                    >
                        {{ item.label }}
                    </span>
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

                <div class="devops-news" v-if="news.length > 0">
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
                        <bk-tag v-if="BK_CI_VERSION" theme="info" type="stroke">{{ BK_CI_VERSION.trim() }}</bk-tag>
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
            <div class="item">
                <a href="https://wpa1.qq.com/KziXGWJs?_type=wpa&qidian=true" target="_blank">{{ $t('technicalSupport') }}</a> |
                <a href="https://bk.tencent.com/s-mart/community/" target="_blank">{{ $t('communityForum') }}</a> |
                <a href="https://bk.tencent.com/index/" target="_blank">{{ $t('ProductOfficialWebsite') }}</a>
            </div>
            <p class="bkci-copyright">Copyright © 2012-{{ getFullYear() }} Tencent BlueKing. All Rights Reserved {{ BK_CI_VERSION.trim() }}</p>
        </section>
    </div>
</template>

<script lang="ts">
    import { mapDocumnetTitle } from '@/utils/constants'
    import { isAbsoluteUrl, urlJoin } from '@/utils/util'
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { Action, State } from 'vuex-class'
    import { Accordion, AccordionItem } from '../components/Accordion/index'
    import Logo from '../components/Logo/index.vue'
    import NavBox from '../components/NavBox/index.vue'

    @Component({
        components: {
            NavBox,
            Accordion,
            AccordionItem,
            Logo
        }
    })
    export default class Home extends Vue {
        @State services
        @State news
        @State related
        @Action fetchLinks
        isAllServiceListShow: boolean = false
        isAbsoluteUrl = isAbsoluteUrl
        BK_CI_VERSION: string = window.BK_CI_VERSION

        get funcArray (): object[] {
            const funcArray = ['issueLabel', 'developLabel', 'testLabel', 'deployLabel', 'operationLabel']
            return funcArray.map((item, index) => ({
                label: this.$t(item),
                left: `${index * 135 + 92}px`
            }))
        }

        get recentVisitService (): object[] {
            const recentVisitService = localStorage.getItem('recentVisitService')
            const recentVisitServiceList = recentVisitService ? JSON.parse(recentVisitService) : []
            return recentVisitServiceList.map(service => {
                const serviceObj = window.serviceObject.serviceMap[service.key] || {}
                return {
                    ...service,
                    ...serviceObj
                }
            })
        }

        get serviceCount (): number {
            return this.services.reduce((sum, service) => {
                sum += service.children.length
                return sum
            }, 0)
        }

        updateShowAllService (show: boolean): void {
            this.isAllServiceListShow = show
        }

        addConsole (link: string): string {
            return urlJoin('/console/', link)
        }
        
        getDocumentTitle (linkNew) {
            const title = linkNew.split('/')[1]
            return this.$t(mapDocumnetTitle(title)) as string
        }

        updateDocumnetTitle (linkNew) {
            document.title = this.getDocumentTitle(linkNew)
        }

        serviceName (name = ''): string {
            const charPos = name.indexOf('(')
            return charPos > -1 ? name.slice(0, charPos) : name
        }

        getFullYear () {
            return (new Date()).getFullYear()
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
