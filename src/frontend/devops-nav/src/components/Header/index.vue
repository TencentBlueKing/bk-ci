<template>
    <div class="devops-header">
        <div class="header-left-bar">
            <router-link
                class="header-logo"
                to="/console/"
            >
                <template v-if="platformInfo.appLogo">
                    <img
                        class="logo"
                        :src="platformInfo.appLogo"
                        alt=""
                    >
                    
                    <div class="app-name">{{ appName }}</div>
                </template>
                <template v-else>
                    <span>
                        <Logo
                            :name="headerLogoName"
                            width="auto"
                            height="28"
                        />
                    </span>
                </template>
            </router-link>

            <template v-if="showProjectList">
                <bk-select
                    ref="projectDropdown"
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
                    :popover-width="250"
                >
                    <bk-option
                        v-for="item in selectProjectList"
                        :key="item.projectCode"
                        :id="item.projectCode"
                        :name="item.projectName"
                    >
                        <template>
                            <div class="option-item">
                                <div
                                    class="project-name"
                                    v-bk-tooltips="{ content: item.projectName, allowHTML: false, delay: [300, 0] }"
                                >
                                    {{ item.projectName }}
                                </div>
                                <span
                                    v-if="item.showUserManageIcon"
                                    :class="{
                                        'user-manaeg-icon': true,
                                        'is-selected': projectId === item.projectCode,
                                        'is-disabled': !item.managePermission
                                    }"
                                    v-bk-tooltips="$t('userManage')"
                                    @click.stop.prevent="goToUserManage(item)"
                                >
                                    <img
                                        v-if="item.managePermission"
                                        src="../../assets/scss/logo/user-manage.svg"
                                        alt=""
                                    >
                                    <img
                                        v-else
                                        src="../../assets/scss/logo/user-manage-disabled.svg"
                                        alt=""
                                    >
                                </span>
                            </div>
                        </template>
                    </bk-option>
                    <template slot="extension">
                        <div class="extension-wrapper">
                            <span
                                class="bk-selector-create-item"
                                @click.stop.prevent="popProjectDialog"
                            >
                                <i class="devops-icon icon-plus-circle mr5" />
                                <span class="text">{{ $t('newProject') }}</span>
                            </span>
                            <span class="extension-line" />
                            <span
                                class="bk-selector-create-item"
                                @click.stop.prevent="handleApplyProject"
                            >
                                <icon
                                    name="icon-apply"
                                    size="14"
                                    class="mr5"
                                />
                                <span class="text">{{ $t('joinProject') }}</span>
                            </span>
                        </div>
                    </template>
                </bk-select>
            </template>
            <nav-menu v-if="showNav" />
            <h3
                v-if="title"
                class="service-title"
                @click="goHome"
            >
                <logo
                    :name="serviceLogo"
                    size="20"
                />
                {{ title }}
            </h3>
        </div>
        <div class="header-right-bar">
            <bk-popover
                v-if="!isInIframe"
                theme="light navigation-message"
                placement="bottom"
                trigger="click"
                :arrow="false"
                ref="popoverRef"
                :on-hide="handleHide"
                :on-show="handleShow"
            >
                <div class="flag-box">
                    <Icon
                        :name="curLang.icon"
                        size="20"
                    />
                </div>
                <template slot="content">
                    <li
                        v-for="(item, index) in langs"
                        :key="index"
                        :class="['bkci-dropdown-item', { active: curLang.id === item.id }]"
                        @click="handleChangeLang(item)"
                    >
                        <Icon
                            class="mr5"
                            :name="item.icon"
                            size="20"
                        />
                        {{ item.name }}
                    </li>
                </template>
            </bk-popover>
            <bk-popover
                theme="light navigation-message"
                placement="bottom"
                trigger="click"
                :arrow="false"
                ref="popoverRef"
                :on-hide="handleHide"
                :on-show="handleShow"
            >
                <div class="flag-box">
                    <Icon
                        name="help-fill"
                        size="20"
                    />
                </div>
                <template slot="content">
                    <li
                        class="bkci-dropdown-item"
                        @click.stop="goToDocs"
                    >
                        {{ $t('documentation') }}
                    </li>
                    <li
                        class="bkci-dropdown-item"
                        @click.stop="toggleShowVersionLog(true)"
                    >
                        {{ $t('releaseNotes') }}
                    </li>
                    <li
                        class="bkci-dropdown-item"
                        @click.stop="goToFeedBack"
                    >
                        {{ $t('feedback') }}
                    </li>
                    <li
                        class="bkci-dropdown-item"
                        @click.stop="goToGithubSource"
                    >
                        {{ $t('openSource') }}
                    </li>
                </template>
            </bk-popover>
            <User
                class="user-info"
                v-bind="user"
            />
        </div>

        <project-dialog
            :init-show-dialog="showProjectDialog"
            :title="projectDialogTitle"
        />
        <apply-project-dialog ref="applyProjectDialog"></apply-project-dialog>
        <system-log
            :show-system-log="showSystemLog"
            :toggle-show-log="toggleShowVersionLog"
        />
    </div>
</template>

<script lang="ts">
    import eventBus from '@/utils/eventBus'
    import { urlJoin } from '@/utils/util'
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { Action, Getter, State } from 'vuex-class'
    import ApplyProjectDialog from '../ApplyProjectDialog/index.vue'
    import LocaleSwitcher from '../LocaleSwitcher/index.vue'
    import Logo from '../Logo/index.vue'
    import ProjectDialog from '../ProjectDialog/index.vue'
    import DevopsSelect from '../Select/index.vue'
    import SystemLog from '../SystemLog/index.vue'
    import User from '../User/index.vue'
    import NavMenu from './NavMenu.vue'

    @Component({
        components: {
            User,
            NavMenu,
            ProjectDialog,
            ApplyProjectDialog,
            Logo,
            DevopsSelect,
            LocaleSwitcher,
            SystemLog
        }
    })
    export default class Header extends Vue {
        @State user
        @State currentPage
        @State showProjectDialog
        @State projectDialogTitle
        @State headerConfig

        @Getter enableProjectList
        @Getter platformInfo

        @Action toggleProjectDialog
        @Action togglePopupShow

        isDropdownMenuVisible: boolean = false
        isShowTooltip: boolean = true
        showSystemLog: boolean = false
        langs: Array<any> = [
            {
                icon: 'chinese',
                name: '中文',
                id: 'zh-CN'
            },
            {
                icon: 'english',
                name: 'English',
                id: 'en-US'
            },
            {
                icon: 'japanese',
                name: '日本語',
                id: 'ja-JP'
            }
        ]
 
        get headerLogoName (): string {
            const logoArr = ['devops-logo']
            const localeConst = this.$i18n.locale === 'zh-CN' ? '' : 'en'
            if (localeConst) {
                logoArr.push(localeConst)
            }
            return logoArr.join('-')
        }

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

        get curLang () {
            return this.langs.find(item => item.id === this.$i18n.locale) || { id: 'zh-CN', icon: 'chinese' }
        }

        get appName () {
            return this.platformInfo.i18n.name || this.$t('蓝盾')
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

        goToUserManage (payload): void {
            if (payload.managePermission) {
                this.to(`/console/manage/${payload.projectCode}/group`)
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
                : this.$router.replace({ path })
        }

        handleProjectChange (id: string) {
            const { projectId } = this.$route.params
            const oldProject = this.selectProjectList.find(project => project.projectCode === projectId)
            const project = this.selectProjectList.find(project => project.projectCode === id)
            
            if (projectId && !oldProject) { // 当前无权限时返回首页
                this.goHomeById(id)
            } else {
                this.$router.replace({
                    params: {
                        projectId: id
                    }
                })
            }
            window.setProjectIdCookie(id)

            if ((!oldProject && project.gray) || (oldProject && oldProject.gray !== project.gray)) {
                this.goHomeById(id, true)
            }
        }

        to (url: string): void {
            window.open(url, '_blank')
        }

        goToDocs (): void {
            this.to(this.BKCI_DOCS.BKCI_DOC)
        }

        goToFeedBack (): void {
            this.to(this.BKCI_DOCS.FEED_BACK_URL)
        }

        goToGithubSource (): void {
            this.to(this.BKCI_DOCS.BKAPP_NAV_OPEN_SOURCE_URL)
        }

        goToPm (): void {
            this.to('/console/pm')
        }

        popProjectDialog (project: object): void {
            this.to('/console/manage/apply')
            if (this.$refs.projectDropdown && typeof this.$refs.projectDropdown.close === 'function') {
                this.$refs.projectDropdown.close()
            }
        }

        handleApplyProject () {
            // this.$refs.applyProjectDialog.isShow = true
            this.to('/console/permission/apply')
        }

        closeTooltip (): void {
            this.isShowTooltip = false
        }

        handleChangeLang (item) {
            this.$setLocale(item.id, true).then(() => {
                location.reload()
            })
        }

        handleShow () {
            this.togglePopupShow(true)
        }

        handleHide () {
            this.togglePopupShow(false)
        }

        toggleShowVersionLog (value: boolean) {
            this.showSystemLog = value
        }
    }
</script>

<style lang="scss" scoped>
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
                display: flex;
                > span {
                    display: inline-flex;

                }
                .logo {
                    width: 30px;
                    height: 30px;
                    margin-right: 8px;
                }
                .app-name {
                    font-size: 16px;
                    line-height: 30px;
                    color: #fff;
                }
            }
            $dropdownBorder: #2a2a42;
            .bkdevops-project-selector {
                width: 233px;
                color: $fontLigtherColor;
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
                ::v-deep .bk-select-angle {
                    color: white;
                    top: 7px;
                }
                ::v-deep .bk-tooltip-ref {
                    outline: none;
                }
                ::v-deep .bk-select-dropdown .bk-select-name {
                    color: $fontLigtherColor;
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
                color: $fontLigtherColor;
                font-size: 14px;
                cursor: pointer;

                &:hover {
                    color: white;
                    background-color: black;
                }
                > svg {
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
                cursor: pointer;
            }

            > .seperate-line {
                padding: 0 5px;
                font-size: 20px;
                // color: $fontLigtherColor;
                line-height: $headerHeight;
            }

            > .devops-icon {
                padding: 0 10px;
                font-size: 20px;
                color: $fontLigtherColor;
                line-height: $headerHeight;
                cursor: pointer;
            }

            > .user-info {
                margin: 0 10px;
            }
        }
    }

    .option-item {
        width: 100%;
        display: flex;
        align-items: center;
        justify-content: space-between;
        &:hover {
            .user-manaeg-icon {
                display: block !important;
            }
            .is-selected {
                display: block !important;
            }
        }
        .project-name {
            text-overflow: ellipsis;
            overflow: hidden;
            white-space: nowrap;
        }
        .user-manaeg-icon {
            width: 20px;
            display: none;
            cursor: pointer;
            position: relative;
            top: 5px;
            flex-shrink: 0;
        }
        
        .is-selected {
            display: block;
        }
        .is-disabled {
            cursor: not-allowed;
        }
    }
    .bk-selector-create-item{
        display: flex;
        align-items: center;
        cursor: pointer;
        font-size: 12px !important;
        display: inline-block;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        i {
            font-size: 12px !important;
        }
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

    .extension-wrapper {
        display: flex;
        justify-content: center;
        align-items: center;
    }
    .extension-line {
        display: inline-block;
        width: 1px;
        height: 16px;
        margin: 0 10px;
        background: #DCDEE5;
    }
</style>
<style lang="scss">
    .navigation-message-theme {
        position: relative;
        top: 5px;
        padding: 0 !important;
    }
    .bkci-dropdown-item {
        display: flex;
        align-items: center;
        height: 32px;
        line-height: 33px;
        padding: 0 20px;
        color: #63656e;
        font-size: 12px;
        text-decoration: none;
        white-space: nowrap;
        background-color: #fff;
        cursor: pointer;
        &:hover {
            background-color: #f5f7fb;
        }
        &.disabled {
            color: #dcdee5;
            cursor: not-allowed;
        }
        &.active {
            background-color: #f5f7fb;
        }
    }
    .flag-box {
        align-items: center;
        border-radius: 50%;
        cursor: pointer;
        display: inline-flex;
        font-size: 16px;
        height: 32px;
        justify-content: center;
        position: relative;
        width: 32px;
        margin-right: 8px;
        &:hover {
            background: linear-gradient(270deg,#253047,#263247);
            color: #d3d9e4;
        }
    }
</style>
