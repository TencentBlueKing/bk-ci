<template>
    <div
        v-clickoutside="hideUserInfo"
        :class="{ &quot;devops-user-info&quot;: true, &quot;active&quot;: show }"
    >
        <div
            class="user-entry"
            @click.stop="toggleUserInfo"
        >
            {{ username }}
            <i class="bk-icon icon-down-shape" />
        </div>
        <div
            v-if="show"
            class="user-info-dropmenu"
        >
            <p class="user-avatar">
                <!--<img :src="avatarUrl" alt="用户头像" />-->
                <span>{{ chineseName }}</span>
            </p>
            <slot name="menu">
                <ul>
                    <li
                        v-for="(item, index) in menu"
                        :key="index"
                    >
                        <router-link
                            v-if="item.to"
                            class="user-menu-item"
                            :to="item.to"
                            @click.native="hideUserInfo"
                        >
                            {{ item.label }}
                        </router-link>
                        <span
                            v-else-if="item.cb"
                            class="user-menu-item"
                            @click.stop="item.cb"
                        >{{ item.label }}</span>
                    </li>
                </ul>
            </slot>
        </div>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Prop, Watch } from 'vue-property-decorator'
    import { Action } from 'vuex-class'
    import { clickoutside } from '../../directives/index'

    @Component({
      directives: {
        clickoutside
      }
    })
    export default class User extends Vue {
        @Prop()
        username: string
        @Prop()
        avatarUrl: string
        @Prop()
        chineseName: string
        @Prop()
        bkpaasUserId: string
        show: boolean = false

        @Action togglePopupShow

        toggleUserInfo (show: boolean) :void {
          this.show = !this.show
        }

        hideUserInfo () : void {
          this.show = false
        }

        @Watch('show')
        handleShow (show, oldVal) {
          if (show !== oldVal) {
            this.togglePopupShow(show)
          }
        }

        get menu (): object[] {
          return [
            {
              to: '/console/pm',
              label: '项目管理'
            }
          ]
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';

    $dropmenuWidth: 212px;
    .devops-user-info {
        position: relative;
        display: flex;
        align-items: center;
        color: $fontLigtherColor;
        height: 100%;
        cursor: pointer;

        .user-entry {
            display: flex;
            height: 100%;
            padding:0 12px;
            align-items: center;
        }

        .bk-icon.icon-down-shape {
            vertical-align: -2px;
        }

        .user-info-dropmenu {
            // display: none;
            width: $dropmenuWidth;
            position: absolute;
            background: white;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;
            box-shadow: 0 3px 6px rgba(51, 60, 72, 0.12);
            top: 53px;
            right: 0;
            cursor: default;
            &:after {
                position: absolute;
                content: '';
                width: 8px;
                height: 8px;
                border: 1px solid $borderWeightColor;
                border-bottom: 0;
                border-right: 0;
                transform: rotate(45deg);
                background: white;
                top: -5px;
                right: 36px;

            }
            .user-avatar {
                display: flex;
                align-items: center;
                padding-bottom: 15px;
                border-bottom: 1px solid $borderWeightColor;
                color: $fontWeightColor;
                padding: 20px;
                > img {
                    width: 34px;
                    height: 34px;
                }
                > span {
                    padding-left: 15px;
                }
            }
            > ul {
                margin: 20px;
                > li {
                    margin: 0 0 10px 0;
                    line-height: 24px;
                    &:last-child {
                        margin-bottom: 0;
                    }
                    .user-menu-item {
                        color: $fontWeightColor;
                        &:hover {
                            color: $aHoverColor;
                        }
                    }
                }
            }
        }
    }
</style>
