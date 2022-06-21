<template>
    <bk-dialog
        v-model="showNotice"
        ext-cls="devops-announcement-dialog"
        :ok-text="$t('expNow')"
        :width="828"
        :close-icon="false"
        :position="{ top: '100' }"
        :title="currentNotice.noticeTitle"
        @confirm="toLink(currentNotice.redirectUrl)"
        @cancel="closeDialog"
    >
        <main class="new-service-content">
            <div class="announcement-content">
                <div
                    class="content-detail"
                    v-html="currentNotice.noticeContent"
                />
            </div>
        </main>
    </bk-dialog>
</template>
<script lang='ts'>
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { State, Action } from 'vuex-class'
    @Component
    export default class NewServiceDialog extends Vue {
        @State showNotice
        @State currentNotice
        
        @Action toggleNoticeDialog
    
        get announcementHistory () : object[] {
            const announcementHistory = localStorage.getItem('announcementHistory')
            return announcementHistory ? JSON.parse(announcementHistory) : []
        }

        toLink (url) {
            if (url) {
                window.location.href = url
            } else {
                this.toggleNoticeDialog(false)
            }
        }

        closeDialog () {
            this.toggleNoticeDialog(false)
        }
    }
</script>
<style lang="scss">
    @import '../../assets/scss/conf';
    .devops-announcement-dialog {
        .bk-dialog-body {
            margin: 0px;
            padding: 0px !important;
        }
        .bk-dialog-header {
            padding: 0px !important;
        }
        .new-service-content {
            padding: 0 20px;
            height: 547px;
            background-image: url('../../assets/images/guide-foot.png');
            background-size: 100% 100%;
            overflow-y: scroll;
        }
        .announcement-title {
            text-align: center;
            color: $primaryColor;
            h2 {
                margin-top: 20px;
                font-size: 31px;
            }
            .icon-close {
                position: absolute;
                top: 20px;
                right: 16px;
                font-size: 16px;
                color: $fontWeightColor;
                cursor: pointer;
            }
        }
        .announcement-content {
            padding: 24px 50px 10px;
            height: 360px;
            .content-detail img {
                width: 100%;
            }
            p {
                text-align: left;
            }
        }
        .announcement-footer {
            padding: 20px 0;
            text-align: center;
            .bk-button {
                padding: 0 50px;
                height: 46px;
                font-size: 20px;
            }
        }
    }
</style>
