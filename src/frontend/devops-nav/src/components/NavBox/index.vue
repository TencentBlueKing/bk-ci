<template>
    <div :class="{ &quot;nav-box&quot;: true, &quot;with-hover&quot;: withHover }">
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
                    :disabled="child.status !== &quot;ok&quot; && child.status !== &quot;new&quot; && child.status !== &quot;hot&quot;"
                >
                    <a
                        :href="addConsole(child.link_new)"
                        @click.prevent="gotoPage(child)"
                    >
                        <i
                            v-if="serviceIcon(child.name) === &quot;logo-bcs&quot;"
                            class="bk-icon service-icon icon-logo-bcs"
                        >
                            <span
                                v-for="key in [1,2,3,4]"
                                :key="key"
                                :class="`path${key}`"
                            />
                        </i>
                        <i
                            v-else
                            :class="{ &quot;bk-icon&quot;: true, &quot;service-icon&quot;: true, [`icon-${serviceIcon(child.name)}`]: true }"
                        />
                        {{ serviceName(child.name) }}
                        <span class="service-id">{{ serviceId(child.name) }}</span>
                        <span
                            v-if="child.status === &quot;new&quot;"
                            class="new-service-icon"
                        >new</span>
                    </a>
                    <template v-if="showCollectStar">
                        <i
                            v-if="child.collected"
                            title="已收藏"
                            class="bk-icon collect-icon icon-star-shape"
                            @click.stop="toggleCollect(child, false)"
                        />
                        <i
                            v-else
                            title="收藏"
                            class="bk-icon collect-icon icon-star"
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
    import { urlJoin, getServiceAliasByPath } from '../../utils/util'
    import eventBus from '../../utils/eventBus'

    @Component
    export default class NavBox extends Vue {
        readyServices: ObjectMap = {}
        @Prop({ required: true })
        services

        @Prop()
        toggleCollect

        @Prop({ default: 207 })
        columnWidth: string

        @Prop({ default: true })
        withHover: boolean

       iconMap: ObjectMap = {
         'Teamwork': 'tapd',
         'Cloud Drive': '',
         'Code': 'code',
         'Pipeline': 'pipeline',
         'Exp': 'experience',
         'Artifactory': 'artifactory',
         'OSIS': '',
         'CodeCC': 'codecc',
         'WeTest': 'wetest',
         'BCS': 'bcs',
         'Eagle': '',
         'SOP': '',
         'AD': '',
         'Job': 'job',
         'Env': 'environment',
         'Monitor': 'monitor',
         'LogSearch': '',
         'KingKong': '',
         'Scan': '',
         'Apk': 'apk',
         'Vs': 'vs',
         'Measure': 'measure',
         'Perm': 'perm',
         'Ticket': 'ticket',
         'Gate': 'gate',
         'Turbo': 'turbo',
         'Store': 'store'
       }

       gotoPage ({ link_new: linkNew }) {
         const cAlias = window.currentPage && getServiceAliasByPath(window.currentPage['link_new'])
         const nAlias = getServiceAliasByPath(linkNew)
         const destUrl = this.addConsole(linkNew)

         if (cAlias === nAlias && window.currentPage && window.currentPage['inject_type'] === 'iframe') {
           eventBus.$emit('goHome')
           return
         }
         this.$router.push(destUrl)
       }

       addConsole (link: string): string {
         return urlJoin('/console', link)
       }

       get showCollectStar (): boolean {
         return typeof this.toggleCollect === 'function'
       }

       serviceName (name): string {
         return name.slice(0, name.indexOf('('))
       }

       serviceId (name): string {
         return name.replace(/^\S+?\(([\s\S]+?)\)\S*$/, '$1')
       }

       serviceIcon (name): string {
         const iconName = this.iconMap[this.serviceId(name)]
         return iconName ? `logo-${iconName}` : 'placeholder'
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
                    .bk-icon.service-icon {
                        font-size: 20px;
                        margin-right: 12px;
                        color: #6b798e;
                    }
                    .service-id {
                        margin-left: 5px;
                        @include ellipsis();
                    }
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
                        .bk-icon.service-icon {
                            color: $primaryColor;
                        }
                    }

                    &[disabled] {
                        pointer-events: none;
                        color: $fontLigtherColor;
                        cursor: default;

                        > a,
                        .bk-icon.service-icon {
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
