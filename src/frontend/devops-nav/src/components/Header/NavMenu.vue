<template>
    <nav
        v-clickoutside="hideNavMenu"
        :class="{ &quot;devops-header-nav&quot;: true, &quot;active&quot;: show }"
    >
        <p
            class="nav-entry"
            :class="{ active: show }"
            @click.stop="toggleNavMenu"
        >
            <span>服务</span>
            <span
                v-if="showNewServiveTips"
                class="unread-icon"
            />
            <i class="bk-icon icon-angle-down" />
        </p>
        <transition name="fade">
            <div
                v-show="show"
                class="nav-menu-layout"
                :class="{ 'showTopPrompt': showExplorerTips === 'true' && isShowPreviewTips && !chromeExplorer }"
            >
                <div
                    class="nav-menu-layout-content"
                    @click.stop="hideNavMenu"
                >
                    <div class="nav-collect">
                        <h3>我的收藏</h3>
                        <ul
                            v-if="collectServices.length > 0"
                            class="nav-collect-menu"
                        >
                            <li
                                v-for="service in collectServices"
                                :key="service.id"
                                class="collect-item"
                            >
                                <logo
                                    class="service-logo"
                                    :name="getServiceLogoByPath(service.link_new)"
                                    size="18"
                                />
                                <a
                                    :href="addConsole(service.link_new)"
                                    @click.prevent="gotoPage(service)"
                                >
                                    {{ serviceName(service.name) }}
                                    <span
                                        class="service-id"
                                    >{{ serviceId(service.name) }}</span>
                                </a>
                            </li>
                        </ul>
                        <div
                            v-else
                            class="empty-collect"
                        >
                            <img src="../../assets/images/empty.png">
                            <p>未收藏任何服务</p>
                        </div>
                    </div>
                    <div class="nav-menu">
                        <nav-box
                            :services="services"
                            :toggle-collect="toggleCollect"
                        />
                    </div>
                </div>
            </div>
        </transition>
    </nav>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { State, Getter, Action } from 'vuex-class'
    import { getServiceLogoByPath, urlJoin, getServiceAliasByPath } from '../../utils/util'
    import { clickoutside } from '../../directives/index'
    import Logo from '../Logo/index.vue'
    import NavBox from '../NavBox/index.vue'
    import eventBus from '../../utils/eventBus'

    @Component({
      name: 'nav-menu',
      components: {
        Logo,
        NavBox
      },
      directives: {
        clickoutside
      }
    })
    export default class NavMenu extends Vue {
        @Getter('getCollectServices') collectServices
        @State services
        @State isShowPreviewTips
        @Action toggleServiceCollect
        show: boolean = false
        showNewServiveTips: boolean = false
        showExplorerTips: string = localStorage.getItem('showExplorerTips')

        get chromeExplorer (): boolean {
          const explorer = window.navigator.userAgent
          return explorer.indexOf('Chrome') >= 0 && explorer.indexOf('QQ') === -1
        }

        get newServiceList (): object[] {
          const newServiceList = localStorage.getItem('newServiceList')
          return newServiceList ? JSON.parse(newServiceList) : []
        }

        get curNewServices (): object[] {
          const newServices = []
          window.allServices.forEach(service => {
            service.children.forEach(child => {
              if (child.status === 'new') {
                newServices.push(child.name)
              }
            })
          })

          return newServices
        }

        toggleNavMenu (): void {
          if (this.showNewServiveTips) {
            this.showNewServiveTips = false

            this.curNewServices.forEach(service => {
              if (this.newServiceList.indexOf(service) === -1) {
                this.newServiceList.push(service)
              }
            })

            if (this.newServiceList.length) {
              localStorage.setItem('newServiceList', JSON.stringify(this.newServiceList))
            }
          }

          this.show = !this.show
        }

        hideNavMenu (): void {
          this.show = false
        }

        serviceName (name): string {
          return name.slice(0, name.indexOf('('))
        }

        getServiceLogoByPath (link: string): string {
          return getServiceLogoByPath(link)
        }

        addConsole (link: string): string {
          return urlJoin('/console/', link)
        }

        serviceId (name): string {
          return name.replace(/^\S+?\(([\s\S]+?)\)\S*$/, '$1')
        }

        gotoPage ({ link_new: linkNew }) {
          const cAlias = window.currentPage && getServiceAliasByPath(window.currentPage['link_new'])
          const nAlias = getServiceAliasByPath(linkNew)
          const destUrl = this.addConsole(linkNew)

          if (cAlias === nAlias && window.currentPage && window.currentPage['inject_type'] === 'iframe') {
            eventBus.$emit('goHome')
            return
          }
          this.$router.push(destUrl)
        }

        created () {
          if (this.curNewServices.length && this.curNewServices.some(service => {
            return this.newServiceList.indexOf(service) === -1
          })) {
            this.showNewServiveTips = true
          }
        }

        async toggleCollect (child: any, isCollected: boolean) {
          try {
            if (isCollected && this.collectServices.length === 8) {
              this.$bkMessage({
                message: '及时清除不常使用的链接，才能添加新的链接哦：）',
                theme: 'error'
              })
              return
            }
            child.collected = isCollected
            await this.toggleServiceCollect({
              serviceId: child.id,
              isCollected
            })
          } catch (e) {
            console.warn(e)
            child.collected = !isCollected
          }
        }
    }
</script>

<style lang="scss">
@import "../../assets/scss/conf";

.devops-header-nav {
    height: 100%;
    margin-left: 10px;
    display: flex;
    align-items: center;

    .nav-entry {
        color: $fontLigtherColor;
        padding: 0 20px;
        cursor: pointer;
        line-height: $headerHeight;
        span,
        i {
            transition: 0.2s all linear;
        }
        > i {
            font-size: 12px;
            margin-left: 5px;
            vertical-align: -2px;
            display: inline-block;
        }
        &:hover {
            color: white;
            background: black;
        }
        .unread-icon {
            display: inline-block;
            position: relative;
            top: -7px;
            left: 2px;
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background-color: $iconFailColor;
        }
    }
    &.active {
        > .nav-entry {
            color: white;
            background: black;
            > i {
                transform: rotate(180deg);
            }
        }
    }
    .nav-menu-layout {
        position: fixed;
        left: 0;
        bottom: 0;
        top: 50px;
        right: 0;
        background: white;
        min-height: 100%;
        background: rgba(0, 0, 0, 0.5);
        top: 50px;
        &-content {
            display: flex;
            height: 100%;
        }
        .nav-collect {
            width: 222px;
            background-color: $bgHoverColor;
            padding: 16px 10px 0 30px;
            border-right: 1px solid $borderWeightColor;
            color: $fontWeightColor;
            overflow: auto;
            > h3 {
                font-size: 14px;
                margin-bottom: 10px;
                line-height: 48px;
                border-bottom: 1px solid #c3cad9;
                color: #0a1633;
                font-weight: normal;
            }
            .empty-collect {
                text-align: center;
                margin-top: 160px;
                margin-left: -30px;
            }
            &-menu {
                .collect-item {
                    display: flex;
                    align-items: center;
                    padding: 7px 0 7px 5px;
                    &:hover {
                        a {
                            color: $primaryColor;
                        }
                    }
                    .service-logo {
                        margin-right: 10px;
                    }
                    .service-id {
                        margin-left: 5px;
                    }
                }
            }
        }
        .nav-menu {
            background-color: $bgHoverColor;
            flex: 1;
            padding: 6px 0 0 36px;
            overflow: auto;
        }
    }
    .showTopPrompt {
        top: 81px;
    }
}
</style>
