<template>
    <article class="project-setting-home">
        <transition-tab :panels="tabs"
            @child-tab-change="childTabChange"
            :active-tab.sync="activeTab"
            :active-child-tab.sync="activeChildTab"
        ></transition-tab>

        <component :is="activeChildTab" class="project-setting-main" ref="member"></component>
    </article>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { Action } from 'vuex-class'
    import TransitionTab from '../components/TransitionTab'
    import basic from '../components/ProjectSetting/basic'
    import member from '../components/ProjectSetting/member'

    @Component({
        components: {
            TransitionTab,
            basic,
            member
        }
    })
    export default class ProjectSetting extends Vue {
        @Action getV3UserGroupUrl

        tabs: object[] = []
        activeTab: string = ''
        activeChildTab: string = ''

        created () {
            this.initData()
        }

        initData () {
            this.tabs = [
                {
                    label: this.$t('overview'),
                    name: 'setting',
                    children: [{ label: this.$t('basicInfo'), name: 'basic' }],
                    showChildTab: true
                },
                {
                    label: this.$t('accessManagement'),
                    name: 'manage',
                    children: [
                        { label: this.$t('memberManagement'), name: 'member' },
                        { label: this.$t('userGroupPermissions'), name: 'userGroup' }
                    ],
                    showChildTab: true
                }
            ]
            const type = this.$route.params.type
            this.tabs.forEach((tab) => {
                tab.children.forEach((child) => {
                    if (child.name === type) {
                        this.activeTab = tab.name
                        this.activeChildTab = child.name
                    }
                })
            })
        }

        childTabChange (name) {
            if (name === 'userGroup') {
                this.activeChildTab = 'member'
                const memberComponent = this.$refs.member || {}
                const roleId = memberComponent.curRole.id !== 'bkAllMember' ? memberComponent.curRole.id : null
                const params = {
                    projectId: memberComponent.projectCode,
                    roleId
                }
                this.getV3UserGroupUrl(params).then((res) => {
                    window.open(res, '_blank')
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            } else {
                this.$router.push({
                    name: 'ps',
                    params: {
                        type: name
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .project-setting-home {
        width: 100%;
        background-color: #f1f2f3;
    }
    .project-setting-main {
        width: 90%;
        height: calc(100% - 14.3vh - 32px);
        background: #fff;
        margin: 16px auto 0;
        box-shadow: 1px 2px 3px 0 rgba(0,0,0,0.05);
        padding: 3.2vh;
    }
</style>
