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
            <span>{{ $t('service') }}</span>
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
                :class="{ 'showTopPrompt': showAnnounce }"
            >
                <div
                    class="nav-menu-layout-content"
                    @click.stop="hideNavMenu"
                >
                    <div class="nav-collect">
                        <h3>{{ $t('collection') }}</h3>
                        <ul
                            v-if="collectServices.length > 0"
                            class="nav-collect-menu"
                        >
                            <li
                                v-for="service in collectServices"
                                :key="service.id"
                                class="collect-item"
                            >
                                <img v-if="isAbsoluteUrl(service.logoUrl)" :src="service.logoUrl" class="service-logo" />
                                <logo
                                    v-else
                                    class="service-logo"
                                    :name="service.logoUrl"
                                    size="18"
                                />
                                <a
                                    :href="addConsole(service.link_new)"
                                    @click.prevent="gotoPage(service)"
                                >
                                    {{ serviceName(service.name) }}
                                    <!-- <span
                                        class="service-id"
                                    >{{ serviceId(service.name) }}</span> -->
                                </a>
                            </li>
                        </ul>
                        <div
                            v-else
                            class="empty-collect"
                        >
                            <img src="../../assets/images/empty.png">
                            <p>{{ $t('noCollection') }}</p>
                        </div>
                    </div>
                    <div class="nav-menu">
                        <nav-box
                            :services="services"
                            :current-page="currentPage"
                            :toggle-collect="toggleCollect"
                            :get-document-title="getDocumentTitle"
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
    import { getServiceLogoByPath, urlJoin, getServiceAliasByPath, isAbsoluteUrl } from '../../utils/util'
    import { clickoutside } from '../../directives/index'
    import Logo from '../Logo/index.vue'
    import NavBox from '../NavBox/index.vue'
    import eventBus from '../../utils/eventBus'
    import { mapDocumnetTitle } from '@/utils/constants'

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
        @Getter showAnnounce
        @State services
        @State currentPage
        @Action toggleServiceCollect
        show: boolean = false
        showNewServiveTips: boolean = false
        isAbsoluteUrl = isAbsoluteUrl

        get newServiceList (): object[] {
            const newServiceList = localStorage.getItem('newServiceList')
            return newServiceList ? JSON.parse(newServiceList) : []
        }

        get curNewServices (): object[] {
            const newServices = []
            this.services.forEach(service => {
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
            const charPos = name.indexOf('(')
            return charPos > -1 ? name.slice(0, charPos) : name
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

        getDocumentTitle (linkNew) {
            const title = linkNew.split('/')[1]
            return this.$t(mapDocumnetTitle(title)) as string
        }

        gotoPage ({ link_new: linkNew, newWindow = false, newWindowUrl = '' }) {
            const cAlias = this.currentPage && getServiceAliasByPath(this.currentPage.link_new)
            const nAlias = getServiceAliasByPath(linkNew)
            const destUrl = this.addConsole(linkNew)

            if (cAlias === nAlias && this.currentPage && this.currentPage.inject_type === 'iframe') {
                eventBus.$emit('goHome')
                return
            }
            
            (newWindow && newWindowUrl) ? window.open(newWindowUrl, '_blank') : this.$router.push(destUrl)
            document.title = this.getDocumentTitle(linkNew)
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
                        message: this.$t('outofCollectionTips'),
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
            font-size: 22px;
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
                        width: 18px;
                        height: 18px;
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
