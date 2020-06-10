
<template>
    <div
        v-clickoutside="hideFeedBackMenu"
        :class="{ &quot;devops-feedback&quot;: true, &quot;active&quot;: show }"
    >
        <icon
            class="bk-icon"
            name="feedback"
            @click="toggleFeedBackMenu(show)"
        />
        <ul
            v-if="show"
            class="feedback-menu"
        >
            <li
                v-for="item in menu"
                :key="item.label"
            >
                <a
                    :href="item.href"
                    :target="item.target"
                    @click.stop="hideFeedBackMenu"
                >{{ item.label }}</a>
            </li>
        </ul>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Watch } from 'vue-property-decorator'
    import { clickoutside } from '../../directives/index'
    import { State, Action } from 'vuex-class'

    @Component({
        directives: {
            clickoutside
        }
    })
    export default class FeedBack extends Vue {
        @State user
        @Action togglePopupShow
        show: boolean = false
        menu = [
            {
                label: '问题反馈',
                href: 'http://x.code.oa.com/bkdevops/devops/issues',
                target: '_blank'
            },
            {
                label: '联系助手',
                href: 'wxwork://message/?username=DevOps',
                target: ''
            }
        ]

        @Watch('show')
        handleShow (show, oldVal) {
            if (show !== oldVal) {
                this.togglePopupShow(show)
            }
        }

        toggleFeedBackMenu (): void {
            this.show = !this.show
        }
        hideFeedBackMenu (): void {
            this.show = false
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    @import '../../assets/scss/mixins/triangle';
    .devops-feedback {
        position: relative;
        height: 100%;
        display: flex;
        align-items: center;
        color: $fontLigtherColor;

        > .bk-icon {
            margin: 0 10px;
            font-size: 20px;
            cursor: pointer;
            margin-top: 2px;

        }

        .feedback-menu {
            position: absolute;
            background-color: white;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;
            top: 52px;
            left: 6px;
            box-shadow: 0 3px 6px rgba(51, 60, 72, 0.12);
            &:before {
                position: absolute;
                content: '';
                width: 8px;
                height: 8px;
                border: 1px solid $borderWeightColor;
                border-bottom: 0;
                border-right: 0;
                left: 9px;
                top: -5px;
                transform: rotate(45deg);
                background: white;
            }
            li {
                border-bottom: 1px solid $borderWeightColor;
                &:last-child {
                    border: 0;
                }
                a {
                    cursor: pointer;
                    line-height: 36px;
                    white-space: nowrap;
                    padding: 0 14px;
                    color: $fontWeightColor;
                    &:hover {
                        color: $primaryColor;
                    }
                }
            }
        }
    }
</style>
