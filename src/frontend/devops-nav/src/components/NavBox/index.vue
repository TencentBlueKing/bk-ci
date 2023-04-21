<template>
    <div :class="{ 'nav-box': true, 'with-hover': withHover }">
        <div
            v-for="(service, index) in services"
            :key="index"
            class="menu-column"
            :style="{ width: `${columnWidth}px` }"
        >
            <ul class="service-item">
                <h4>
                    {{ service.title }}
                </h4>
                <li
                    v-for="child in service.children"
                    :key="child.id"
                    class="menu-item"
                    :disabled="child.status !== 'ok' && child.status !== 'new' && child.status !== 'hot'"
                >
                    <a
                        :href="addConsole(child.link_new)"
                        @click.prevent="gotoPage(child)"
                    >
                        <img class="service-url-icon" v-if="isAbsoluteUrl(child.logoUrl)" :src="child.logoUrl" />
                        <i
                            v-else-if="serviceIcon(child.logoUrl) === 'logo-bcs'"
                            class="devops-icon service-icon icon-logo-bcs"
                        >
                            <span
                                v-for="key in [1,2,3,4]"
                                :key="key"
                                :class="`path${key}`"
                            />
                        </i>
                        <icon
                            v-else
                            class="devops-icon service-icon"
                            :size="20"
                            :name="serviceIcon(child.logoUrl)"
                        />
                        <span class="service-name">{{ serviceName(child.name) }}</span>
                        <!-- <span class="service-id">{{ serviceId(child.name) }}</span> -->
                        <span
                            v-if="child.status === 'new'"
                            class="new-service-icon"
                        >new</span>
                    </a>
                    <template v-if="showCollectStar">
                        <i
                            v-if="child.collected"
                            :title="$t('collected')"
                            class="devops-icon collect-icon icon-star-shape"
                            @click.stop="toggleCollect(child, false)"
                        />
                        <i
                            v-else
                            :title="$t('toCollect')"
                            class="devops-icon collect-icon icon-star"
                            @click.stop="toggleCollect(child, true)"
                        />
                    </template>
                </li>
            </ul>
        </div>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'
    import { urlJoin, getServiceAliasByPath, isAbsoluteUrl } from '../../utils/util'
    import eventBus from '../../utils/eventBus'

    @Component
    export default class NavBox extends Vue {
        readyServices: ObjectMap = {}

        @Prop()
        currentPage

        @Prop({ required: true })
        services

        @Prop()
        toggleCollect

        @Prop()
        getDocumentTitle

        @Prop({ default: 207 })
        columnWidth: string

        @Prop({ default: true })
        withHover: boolean

        isAbsoluteUrl = isAbsoluteUrl
        
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

       addConsole (link: string): string {
           return urlJoin('/console', link)
       }

       get showCollectStar (): boolean {
           return typeof this.toggleCollect === 'function'
       }

        serviceName (name): string {
            const charPos = name.indexOf('(')
            return charPos > -1 ? name.slice(0, charPos) : name
        }

       serviceId (name): string {
           return name.replace(/^\S+?\(([\s\S]+?)\)\S*$/, '$1')
       }

       serviceIcon (logoUrl): string {
           return logoUrl ? `logo-${logoUrl}` : 'placeholder'
       }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    .nav-box {
        display: flex;
        flex-wrap: wrap;

        &.with-hover {
            .menu-item:hover {
                color: $primaryColor;
                background-color: $primaryColor;

                > a,
                .service-icon,
                .collect-icon {
                    color: white !important;
                }
                .collect-icon:hover {
                    background-color: #0368fc;
                    color: white;
                }
            }

        }
        .menu-column {
            vertical-align: top;
            margin-right: 58px;
            padding-top: 10px;
            padding-bottom: 15px;
            .service-item {
                &:first-child {
                    border-bottom: 1px solid $borderWeightColor;
                }
                &:last-child {
                    border-bottom: 0;
                }
                > h4 {
                    margin-bottom: 10px;
                    line-height: 48px;
                    border-bottom: 1px solid #c3cad9;
                    color: #0a1633;
                    font-weight: normal;
                }
                .menu-item {
                    position: relative;
                    font-size: 13px;
                    display: flex;
                    align-items: center;
                    line-height: 1.5;
                    cursor: pointer;
                    > a {
                        flex: 1;
                        color: $fontWeightColor;
                        @include ellipsis();
                        display: flex;
                        align-items: center;
                        padding: 7px 32px 7px 5px;
                    }
                    .devops-icon.service-icon {
                        font-size: 20px;
                        margin-right: 12px;
                        color: #6b798e;
                    }
                    .service-url-icon {
                        margin-right: 12px;
                        width: 20px;
                        height: 20px;
                    }
                    .service-name {
                        @include ellipsis();
                    }
                    // .service-id {
                    //     margin-left: 5px;
                    //     @include ellipsis();
                    // }
                    .collect-icon {
                        color: #abb4c3;
                        padding: 10px;
                        position: absolute;
                        right: 0;
                        &.icon-star-shape {
                            color: #f9ce1d !important;
                        }
                    }

                    &:hover {
                        color: $primaryColor;
                        > a,
                        .devops-icon.service-icon {
                            color: $primaryColor;
                        }
                    }

                    &[disabled] {
                        pointer-events: none;
                        color: $fontLigtherColor;
                        cursor: default;

                        > a,
                        .devops-icon.service-icon {
                            color: $fontLigtherColor;
                        }
                    }

                    .new-service-icon {
                        display: inline-block;
                        margin-left: 4px;
                        min-width: 26px;
                        width: 26px;
                        height: 14px;
                        background-color: #f44343;
                        color: #fff;
                        font-size: 10px;
                        text-align: center;
                        line-height: 14px;
                    }
                }
            }
        }
    }
</style>
