<template>
    <div class="consult-tools">
        <a
            v-for="(entry, index) in consultTypeList"
            :key="index"
            :class="{ 'consult-item': true, 'default-hover': index === 0 }"
            :href="entry.href"
            :id="entry.id"
            :target="entry.target"
        >
            <icon
                class="devops-icon"
                :name="entry.icon"
                size="13"
            />
            <div
                class="custom-panel trans-fadeout"
                @click.stop.prevent=""
            >
                <div class="panel-arrow" />
                <p>{{ entry.label }}</p>
            </div>
        </a>
    </div>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { State } from 'vuex-class'

    @Component
    export default class ConsultTools extends Vue {
        @State user
        get consultTypeList () {
            return [
                {
                    icon: 'service',
                    label: this.$t('contactUs'),
                    href: 'wxwork://message?username=DevOps-helper',
                    id: 'contactUs',
                    target: ''
                },
                {
                    icon: 'feedback',
                    id: 'feedback',
                    label: this.$t('feedback'),
                    href: '//tapd.oa.com/PaaS/prong/stories/stories_list',
                    target: '_blank'
                }
            ]
        }
    // mounted () {
    //     // 参数说明
    //     // sign：公司渠道唯一标识，复制即可，无需改动
    //     // uid：用户唯一标识，如果没有则不填写，默认为空
    //     // data：用于传递用户信息，最多支持5个，参数名分别为c1,c2,c3,c4,c5；默认为空
    //     // selector：css选择器(document.querySelector, 如#btnid .chat-btn等)，用于替换默认的常驻客服入口
    //     // callback(type, data): 回调函数,type表示事件类型， data表示事件相关数据
    //     // type支持的类型：newmsg有新消息，error云智服页面发生错误， close聊天窗口关闭
    //     // @ts-ignore
    //     window.yzf && window.yzf.init({
    //         sign: '37ef9b97d1210ac42416cce84db7b56b934ef302f69e6f6494d847d943e00fa4c8c24c7fbff6003642099e2260e29aeaecbfe7',
    //         uid: this.user ? this.user.username : '',
    //         selector: '#contactUs',
    //         callback: function () {}
    //     })
    // }
    }
</script>

<style lang="scss" scoped>
@import "../../assets/scss/conf";
.consult-tools {
    position: fixed;
    right: 12px;
    top: 66%;
    .custom-panel {
        position: absolute;
        top: -6px;
        right: 30px;
        padding: 8px 10px;
        white-space: nowrap;
        background: rgba(255, 255, 255, 1);
        box-shadow: 0px 2px 4px 0px rgba(195, 205, 215, 0.4);
        border: 1px solid #c3cdd7;
        visibility: hidden;
        opacity: 0;
        cursor: default;
        p {
            color: #737987;
            font-size: 14px;
        }
        .panel-arrow {
            padding-top: 4px;
            position: absolute;
            top: 13px;
            right: -4px;
            width: 8px;
            height: 8px;
            border: 1px solid #c3cdd7;
            border-right-color: transparent;
            border-bottom-color: transparent;
            background-color: #fff;
            transform: rotate(135deg);
        }
    }
    .trans-fadeout {
        -webkit-transition: all 0.1s linear;
        -moz-transition: all 0.1s linear;
        -ms-transition: all 0.1s linear;
        -o-transition: all 0.1s linear;
        transition: all 0.1s linear;
    }
    .consult-item {
        display: inherit;
        position: relative;
        margin-bottom: 5px;
        width: 24px;
        height: 24px;
        text-align: center;
        background: rgba(51, 60, 72, 0.9);
        box-shadow: 0px 2px 4px 0px rgba(115, 121, 135, 0.3);
        border-radius: 2px;
        cursor: pointer;
        color: #fff;
        &:hover {
            background: rgba(51, 60, 72, 0.8);
            .custom-panel {
                visibility: visible;
                opacity: 1;
            }
        }
        .devops-icon {
            display: inline-block;
            margin-top: 6px;
            color: #fff;
            font-size: 13px;
        }
    }
    .default-hover:hover {
        cursor: default;
    }
}
</style>
