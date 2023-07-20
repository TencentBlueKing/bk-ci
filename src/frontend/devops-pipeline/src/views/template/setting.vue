<template>
    <section class="pipeline-edit-wrapper" v-bkloading="{ isLoading }">
        <header class="create-pipeline-header">
            <div>
                <span class="pointer">
                    {{ $t('setting') }}
                </span>
            </div>
        </header>
        <div class="setting-content-wrapper">
            <setting-base
                @setState="setState"
                @cancel="exit"
            ></setting-base>
        </div>
    </section>
</template>

<script>
    import { mapActions } from 'vuex'
    import SettingBase from '@/components/pipelineSetting/settingBase/index.vue'
    import { navConfirm } from '@/utils/util'

    export default {
        components: {
            SettingBase
        },
        data () {
            return {
                isEditing: false,
                isLoading: true,
                confirmMsg: this.$t('editPage.confirmMsg'),
                confirmTitle: this.$t('editPage.confirmTitle'),
                cancelText: this.$t('cancel')
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        mounted () {
            this.addLeaveListenr()
        },
        beforeDestroy () {
            this.resetPipelineSetting()
            this.removeLeaveListenr()
        },
        beforeRouteUpdate (to, from, next) {
            this.leaveConfirm(to, from, next)
        },
        beforeRouteLeave (to, from, next) {
            this.leaveConfirm(to, from, next)
        },
        methods: {
            ...mapActions('pipleines', [
                'resetPipelineSetting'
            ]),
            setState ({ isLoading, isEditing }) {
                this.isLoading = isLoading
                this.isEditing = isEditing
            },
            leaveConfirm (to, from, next) {
                if (this.isEditing) {
                    navConfirm({ content: this.confirmMsg, type: 'warning', cancelText: this.cancelText })
                        .then(() => next())
                        .catch(() => next(false))
                } else {
                    next(true)
                }
            },
            addLeaveListenr () {
                window.addEventListener('beforeunload', this.leaveSure)
            },
            removeLeaveListenr () {
                window.removeEventListener('beforeunload', this.leaveSure)
            },
            leaveSure (e) {
                e.returnValue = this.confirmMsg
                return this.confirmMsg
            },
            exit () {
                this.$router.push({
                    name: 'pipelinesTemplate'
                })
            }
        }
    }
</script>

<style lang="scss" >
    @import './../../scss/conf';
    .create-pipeline-header {
        display: flex;
        align-items: center;
        height: 60px;
        border-bottom: 1px solid $borderWeightColor;
        padding: 0 20px 0 30px;
        > p {
            flex: 1;
            > span {
                display: inline-block;
            }
        }
    }
    .pipeline-bar {
        display: flex;
        flex: 1;
        align-items: center;
        height: 100%;
        justify-content: flex-end;
    }
    .setting-content-wrapper {
        position: absolute;
        top: 80px;
        bottom: 0;
        width: 100%;
        overflow-y: auto;
    }
    .pipeline-setting{
        position:absolute;
        width: 100%;
        padding:  35px;
        & .setting-container{
            width: 60%;
            min-width: 880px;
        }
    }
</style>
