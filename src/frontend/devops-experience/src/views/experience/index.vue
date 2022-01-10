<template>
    <div class="biz-container release-experience-wrapper experience">
        <aside-nav :nav="nav" :menu-click="handleClick">
            <router-view slot="content" class="biz-content"></router-view>
        </aside-nav>
    </div>
</template>

<script>
    import { mapState } from 'vuex'

    export default {
        computed: {
            ...mapState('experience', [
                'curReleaseDetail'
            ]),
            state () {
                return {
                    curReleaseDetail: this.curReleaseDetail
                }
            },
            nav () {
                return {
                    icon: 'experience',
                    title: '版本体验',
                    // url: 'http://docs.devops.oa.com/所有服务/版本体验/summary.html',
                    menu: [{
                               id: 'experienceList',
                               name: '体验管理',
                               icon: 'icon-release',
                               showChildren: false,
                               children: [
                                   {
                                       id: 'experienceDetail',
                                       name: '发布详情',
                                       icon: 'icon-release'
                                   },
                                   {
                                       id: 'editExperience',
                                       name: '编辑发布',
                                       icon: 'icon-release'
                                   }
                               ]
                           },
                           {
                               id: 'createExperience',
                               name: '新增体验',
                               icon: 'icon-experience',
                               showChildren: false,
                               children: []
                           },
                           {
                               id: 'setting',
                               name: '设置',
                               icon: 'icon-cog',
                               showChildren: false,
                               children: []
                           }]
                }
            }
        },
        methods: {
            handleClick (name) {
                this.$router.push({
                    name
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .release-experience-wrapper {
        min-width: 1200px;
    }
    .sub-view-port {
        height: calc(100% - 60px);
        padding: 20px;
        overflow: auto;
    }

</style>
