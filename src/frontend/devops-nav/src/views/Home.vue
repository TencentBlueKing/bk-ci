<template>
    <div class="devops-home-page">
        <section>
            <accordion>
                <accordion-item :init-content-show="true">
                    <span
                        slot="header"
                        class="home-accordion-header"
                    >
                        最近访问服务
                    </span>
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
                                <Logo
                                    :name="service.key"
                                    size="16"
                                />
                                {{ service.name }}
                            </router-link>
                        </template>
                        <p
                            v-else
                            class="no-recent-service"
                        >
                            暂无最近访问服务，请查看
                            <span @click="updateShowAllService(true)">所有服务</span>
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
                        所有服务
                        <span class="service-count">共{{ serviceCount }}个服务</span>
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
                <h2>一站式研发解决方案</h2>
                <span style="left: 112px;">需求</span>
                <span style="left: 247px;">开发</span>
                <span style="left: 382px;">测试</span>
                <span style="left: 518px;">部署</span>
                <span style="left: 652px;">运营</span>
                <router-link
                    class="bkdevops-button"
                    :to="{ name: &quot;quickstart&quot; }"
                >
                    <!--<bk-button theme="primary" icon-right="angle-double-right">
                        新手接入
                    </bk-button>-->
                </router-link>
            </div>

            <div class="devops-news">
                <header>
                    <p class="title">
                        最新动态
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
                            <span v-if="index === 0">[最新]</span>
                            {{ item.name }}
                        </a>
                        <span>{{ item.create_time }}</span>
                    </p>
                </div>
            </div>
        </section>
        <aside>
            <article>
                <h2>蓝盾DevOps平台</h2>
                <p>
                    蓝鲸团队打造的一站式DevOps研发平台，从业务安全出发，贯穿产品研发、测试和运营的全生命周期；助力业务平滑过渡到敏捷研发模式，打造一站式研发运维体系，持续快速交付高质量的产品。
                    <a
                        :href="DOCS_URL_PREFIX"
                        class="more"
                        target="_blank"
                    >了解更多</a>
                </p>
            </article>
            <article>
                <h2>用蓝盾流水线加速你的交付</h2>
                <p>
                    持续交付强调更快、更可靠、低成本的自动化软件交付，蓝盾流水线（Pipeline）提供可视化、一键式部署服务，和持续集成无缝集成，支持并行部署。
                    <a
                        :href="`${DOCS_URL_PREFIX}/所有服务/流水线/什么是流水线/summary.html`"
                        target="_blank"
                        class="more"
                    >了解更多</a>
                </p>
            </article>
            <article>
                <h2>相关链接</h2>
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
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { State, Action } from 'vuex-class'
    import NavBox from '../components/NavBox/index.vue'
    import Logo from '../components/Logo/index.vue'
    import { Accordion, AccordionItem } from '../components/Accordion/index'
    
    import { urlJoin } from '../utils/util'

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
        DOCS_URL_PREFIX: string = DOCS_URL_PREFIX

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
        flex: 1;
        justify-content: center;
        width: 1280px;
        padding: 30px 0 100px 0;
        overflow: auto;

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
                    > svg {
                        margin-right: 6px;
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
                            .bk-icon {
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
                }
                .bkdevops-button {
                    position: absolute;
                    bottom: 30px;
                    left: 342px;
                    .bk-icon {
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
</style>
