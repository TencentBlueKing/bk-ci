<template>
    <div class="devops-header">
        <div class="header-left-bar">
            <router-link
                class="header-logo"
                to="/console/"
            >
                <Logo
                    name="devops-logo"
                    width="100"
                    height="28"
                />
            </router-link>
            <template v-if="showProjectList && !isMooc">
                <bk-select ref="projectDropdown"
                    class="bkdevops-project-selector"
                    :placeholder="$t('selectProjectPlaceholder')"
                    :value="projectId"
                    :clearable="false"
                    :searchable="true"
                    @selected="handleProjectChange"
                    @toggle="handleDropdownVisible"
                    :enable-virtual-scroll="selectProjectList && selectProjectList.length > 3000"
                    :list="selectProjectList"
                    id-key="projectCode"
                    display-key="projectName"
                >
                    <bk-option
                        v-for="item in selectProjectList"
                        :key="item.projectCode"
                        :id="item.projectCode"
                        :name="item.projectName"
                    >
                    </bk-option>
                    <template slot="extension">
                        <div
                            class="bk-selector-create-item"
                            @click.stop.prevent="popProjectDialog()"
                        >
                            <i class="devops-icon icon-plus-circle" />
                            <span class="text">{{ $t('newProject') }}</span>
                        </div>
                        <div
                            class="bk-selector-create-item"
                            @click.stop.prevent="goToPm"
                        >
                            <i class="devops-icon icon-apps" />
                            <span class="text">{{ $t('projectManage') }}</span>
                        </div>
                    </template>
                </bk-select>
            </template>
            <nav-menu v-if="showNav && !isMooc" />
            <h3
                v-if="title && !isMooc"
                class="service-title"
                @click="goHome"
            >
                <img v-if="isAbsoluteUrl(serviceLogo)" :src="serviceLogo" class="service-logo" />
                <logo
                    v-else
                    :name="serviceLogo"
                    class="service-logo"
                    size="20"
                />
                {{ title }}
            </h3>
        </div>
        <div class="header-right-bar">
            <locale-switcher v-if="!isMooc || !isInIframe"></locale-switcher>
            <qrcode v-if="!isMooc" class="feed-back-icon" />
            <span v-if="!isMooc" class="seperate-line">|</span>
            
            <i
                v-if="!isMooc"
                class="devops-icon icon-helper"
                @click.stop="goToDocs"
            />
            <User
                class="user-info"
                :disabled="isMooc"
                v-bind="user"
            />
        </div>

        <project-dialog
            v-if="!isMooc"
            :init-show-dialog="showProjectDialog"
            :title="projectDialogTitle"
        />
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Inject } from 'vue-property-decorator'
    import { State, Action, Getter } from 'vuex-class'
    import User from '../User/index.vue'
    import NavMenu from './NavMenu.vue'
    import Qrcode from './Qrcode.vue'
    import Logo from '../Logo/index.vue'
    import LocaleSwitcher from '../LocaleSwitcher/index.vue'
    import DevopsSelect from '../Select/index.vue'
    import ProjectDialog from '../ProjectDialog/index.vue'
    import eventBus from '../../utils/eventBus'
    import { urlJoin, isAbsoluteUrl } from '../../utils/util'

    @Component({
        components: {
            User,
            NavMenu,
            Qrcode,
            ProjectDialog,
            Logo,
            DevopsSelect,
            LocaleSwitcher
        }
    })
    export default class Header extends Vue {
        @State user
        @State currentPage
        @State showProjectDialog
        @State projectDialogTitle
        @State headerConfig

        @Getter enableProjectList

        @Action toggleProjectDialog
        @Action togglePopupShow

        isDropdownMenuVisible: boolean = false
        isShowTooltip: boolean = true
        isAbsoluteUrl = isAbsoluteUrl

        @Inject()
        isMooc: boolean

        get showProjectList (): boolean {
            return this.headerConfig.showProjectList
        }

        get showNav (): boolean {
            return this.headerConfig.showNav
        }

        get projectId (): string {
            return this.$route.params.projectId
        }

        get title (): string {
            const name = this.currentPage && this.currentPage.name ? this.currentPage.name : ''
            const charPos = name.indexOf('(')
            return charPos > -1 ? name.slice(0, charPos) : name
        }

        get serviceLogo (): string {
            return this.currentPage && this.currentPage.logoUrl ? this.currentPage.logoUrl : 'placeholder'
        }

        get selectProjectList (): Project[] {
            return this.enableProjectList.map(project => ({
                ...project,
                id: project.projectCode,
                name: project.projectName
            }))
        }

        get isInIframe () {
            return top !== window
        }

        $refs: {
            projectDropdown: any
        }

        created () {
            eventBus.$on('show-project-menu', () => {
                const ele = this.$refs.projectDropdown && this.$refs.projectDropdown.$el
                if (ele) {
                    const triggerEle = ele.querySelector('.bk-select-name')
                    triggerEle && triggerEle.click()
                }
            })

            eventBus.$on('hide-project-menu', () => {
                if (this.isDropdownMenuVisible) {
                    const ele = this.$refs.projectDropdown && this.$refs.projectDropdown.$el
                    if (ele) {
                        const triggerEle = ele.querySelector('.bk-select-name')
                        triggerEle && triggerEle.click()
                    }
                }
            })

            eventBus.$on('show-project-dialog', (project: Project) => {
                this.popProjectDialog(project)
            })
        }

        handleDropdownVisible (isShow: boolean): void {
            if (this.isDropdownMenuVisible !== isShow) {
                this.togglePopupShow(isShow)
            }
            this.isDropdownMenuVisible = isShow
        }

        goHome (): void {
            eventBus.$emit('goHome')
            const homeRouter = this.$route.meta.to

            if (homeRouter) {
                this.$router.push({
                    name: homeRouter,
                    params: this.$route.params
                })
            }
        }

        goHomeById (projectId: string, reload: boolean = false): void {
            const hasProjectId = this.currentPage.show_project_list
            let path = urlJoin('/console', this.currentPage.link_new)
            if (hasProjectId) {
                if (this.currentPage.project_id_type === 'path') {
                    path = urlJoin(path, projectId)
                } else {
                    path += `?projectId=${projectId}`
                }
            }

            reload
              ? location.href = path
              : this.$router.replace({
                path
            })
        }

        handleProjectChange (id: string) {
            const { projectId } = this.$route.params
            const oldProject = this.selectProjectList.find(project => project.projectCode === projectId) || {}
            const project = this.selectProjectList.find(project => project.projectCode === id) || {}
            
            window.setProjectIdCookie(id)

            if (projectId && !oldProject) { // 当前无权限时返回首页
                this.goHomeById(id)
            } else {
                this.$router.replace({
                    params: {
                        projectId: id
                    }
                })
            }
            
            if (project.routerTag !== oldProject.routerTag) {
                this.goHomeById(id, true)
            }
        }

        to (url: string): void {
            window.open(url, '_blank')
        }

        goToDocs (): void {
            this.to(`${IWIKI_DOCS_URL}/display/DevOps`)
        }

        goToPm (): void {
            this.to('/console/pm')
        }

        popProjectDialog (project: object): void {
            this.toggleProjectDialog({
                showProjectDialog: true,
                project
            })
            if (this.$refs.projectDropdown && typeof this.$refs.projectDropdown.close === 'function') {
                this.$refs.projectDropdown.close()
            }
        }

        closeTooltip (): void {
            this.isShowTooltip = false
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';

    $headerBgColor: #191929;
    .link {
        color: white;
        margin: 0 20px;
    }
    .devops-header {
        height: $headerHeight;
        display: flex;
        align-items: center;
        position: relative;
        z-index: 1002;
        min-width: 1280px;
        background-color: $headerBgColor;
        transition: all .3s ease;
        .header-left-bar {
            height: 100%;
            flex: 1;
            display: flex;
            align-items: center;
            .header-logo {
                margin-left: 15px;
                margin-right: 15px;
                width: 230px;
            }
            $dropdownBorder: #2a2a42;
            .bkdevops-project-selector {
                width: 233px;
                color: $fontColor;
                border-color: $dropdownBorder;
                background-color: $headerBgColor;
                
                height: 36px;
                line-height: 36px;

                &:hover,
                &.active,
                &.is-focus {
                    border-color: $dropdownBorder !important;
                    background-color: black;
                    color: white !important;
                    box-shadow: none;
                }
                .bk-select-angle {
                    color: white;
                    top: 7px;
                }
                .bk-tooltip-ref {
                    outline: none;
                }
                .bk-select-dropdown .bk-select-name {
                    color: $fontLighterColor;
                    height: 36px;
                    line-height: 36px;
                    font-size: 14px;
                    outline: none;
                }
            }

            .service-title {
                display: flex;
                align-items: center;
                height: 100%;
                padding: 0 18px;
                margin-left: 10px;
                color: $fontLighterColor;
                font-size: 14px;
                cursor: pointer;

                &:hover {
                    color: white;
                    background-color: black;
                }
                .service-logo {
                    width: 20px;
                    height: 20px;
                    margin-right: 5px;
                }
            }
        }

        .header-right-bar {
            justify-self: flex-end;
            height: $headerHeight;
            display: flex;
            display: flex;
            align-items: center;

            >.devops-icon:hover,
            >.feed-back-icon:hover,
            >.user-info:hover,
            >.feed-back-icon.active,
            >.user-info.active {
                color: white;
                background-color: black;
            }

            > .seperate-line {
                padding: 0 5px;
                font-size: 20px;
                // color: $fontLighterColor;
                line-height: $headerHeight;
            }

            > .devops-icon {
                padding: 0 10px;
                font-size: 20px;
                color: $fontLighterColor;
                line-height: $headerHeight;
                cursor: pointer;
            }

            > .user-info {
                margin: 0 10px;
            }
        }
    }
    .bk-selector-create-item{
        cursor: pointer;
        &:hover {
            color: $primaryColor;
            .text {
                color: $primaryColor;
            }
        }
        &:first-child {
            border-top: 0
        }
    }
</style>
