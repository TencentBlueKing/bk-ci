<template>
    <div class="docs-wrapper">
        <Header />
        <!-- 文档中心首页 -->
        <section class="docs-home">
            <div class="docs-banner">
                <div class="docs-banner-main">
                    <div class="docs-header">
                        蓝盾文档中心
                    </div>
                    <div class="docs-sub-header">
                        不管你是初次使用蓝盾还是一名高级用户，你都可找到该服务从简介到高级功能的所有实用信息。
                    </div>
                    <div class="docs-search">
                        <div class="docs-search-main">
                            <i
                                class="devops-icon icon-search"
                                :class="{ 'hidden': searchContent }"
                            />
                            <input
                                v-model="searchContent"
                                type="text"
                                class="bk-form-input"
                                @keyup.enter="goDetail(`?q=${searchContent}`)"
                            >
                        </div>
                    </div>
                </div>
            </div>

            <div
                v-bkloading="{
                    isLoading: docsHomeLoading
                }"
                class="docs-home-main"
            >
                <template
                    v-for="(col, index) of localDocsList"
                >
                    <section
                        v-if="col.children.length"
                        :key="`col${index}`"
                        class="docs-home-col"
                    >
                        <div class="col-icon">
                            <i :class="['devops-icon',`icon-${col.icon || 'profile'}`]" />
                        </div>
                        <div class="col-list-wrapper">
                            <p class="col-list-title">
                                {{ col.title }}
                            </p>
                            <ul class="col-list">
                                <template v-for="(_col, _index) of col.children">
                                    <li
                                        v-if="_col.visible || _col.visible !== false"
                                        :key="`col${index}_${_index}`"
                                        class="col-list-item"
                                        :class="{
                                            hidden: _index > 4 && !col.expanded
                                        }"
                                    >
                                        <a
                                            href="javascript:;"
                                            class="text-link"
                                            @click="goDetail(_col.link)"
                                        >
                                            {{ _col.name }}
                                        </a>
                                    </li>
                                </template>
                                <li
                                    v-if="col.children.length > 5"
                                    class="col-list-item more"
                                >
                                    <a
                                        href="javascript:;"
                                        class="text-link"
                                        @click="col.expanded = !col.expanded"
                                    >
                                        <span>{{ col.expanded ? '收起' : '更多' }}</span>
                                        <i
                                            class="devops-icon icon-angle-down"
                                            :class="{ expanded: col.expanded }"
                                        />
                                    </a>
                                </li>
                            </ul>
                        </div>
                    </section>
                </template>
            </div>
        </section>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import Header from '../components/Docs/header.vue'
    import Footer from '../components/Docs/footer.vue'
    import { Action } from 'vuex-class'

    @Component({
        components: {
            Header,
            Footer
        }
    })
    export default class Docs extends Vue {
        @Action getDocList

        searchContent: string = ''
        localDocsList: object = []
        docsHomeLoading: boolean = true

        async initList () {
            try {
                // 获取分类信息
                const docsList = await this.getDocList()
                docsList.forEach(item => {
                    item.expanded = false
                })
                this.localDocsList = docsList
            } catch (err) {
                this.$bkMessage({
                    message: err.message || err,
                    theme: 'error'
                })
            }
        }

        goDetail (link: string) {
            const path = `${IWIKI_DOCS_URL}/${link.replace(/^\//, '')}`
            window.open(path, '_self')
        }

        created () {
            this.docsHomeLoading = true
            this.initList()
            setTimeout(() => {
                this.docsHomeLoading = false
            }, 300)
        }
    }
</script>

<style lang="scss">
    @import '../assets/scss/conf';

    .docs-wrapper {
        min-width: 1280px;
        width: 100%;
        height: 100%;
        .docs-banner {
            position: relative;
            height: 480px;
            background: url('./../assets/images/docs_banner.jpg') center center no-repeat;
            background-color: #07090d;
            background-size: cover;
            &-main {
                padding-top: 182px;
                text-align: center;
                line-height: 1;
                .docs-header {
                    font-size: 42px;
                    color: #fff;
                }
                .docs-sub-header {
                    font-size: 14px;
                    color: #c3cdd7;
                    margin-bottom: 29px;
                    line-height: 75px;
                }
                .docs-search {
                    width: 720px;
                    height: 54px;
                    background: #fff;
                    border-radius: 2px;
                    margin: 0 auto;
                    i {
                        position: absolute;
                        font-size: 18px;
                        line-height: 54px;
                        padding-left: 20px;
                        color: #c3cdd7;
                    }
                    .bk-form-input {
                        height: 54px;
                        border-top-right-radius: 0;
                        border-bottom-right-radius: 0;
                        font-size: 20px;
                        padding: 0 20px;
                    }
                    .hidden {
                        display: none;
                    }
                }
            }
        }
        .docs-home {
            display: flex;
            flex-direction: column;
            min-height: 100%;
            background-color: $bgHoverColor;
            &-main {
                display: flex;
                flex-flow: row wrap;
                width: 1211px;
                margin: 0 auto;
                padding-top: 60px;
                min-height: 300px;
                flex: 1;
                section:nth-of-type(4n) {
                    margin-right: 0;
                }
            }
            &-col {
                display: flex;
                width: calc((100% - 159px)/4);
                margin-right: 53px;
                margin-bottom: 60px;
                .col-icon {
                    width: 24px;
                    margin-right: 21px;
                    font-size: 24px;
                }
                .col-list {
                    &-wrapper {
                        flex: 1;
                    }
                    &-title {
                        height: 40px;
                        line-height: 25px;
                        color: $fontBoldColor;
                        font-size: 16px;
                    }
                    &-item {
                        height: 42px;
                        line-height: 42px;
                        border-top: 1px solid $borderColor;
                        overflow: hidden;
                        &:last-child {
                            border-bottom: 1px solid $borderColor;
                        }
                        .devops-icon {
                            display: inline-block;
                            transition: transform linear .2s;
                            font-size: 12px;
                            &.expanded {
                                transform: rotate(180deg);
                            }
                        }
                        &.hidden {
                            display: none;
                        }
                    }
                    .text-link {
                        display: block;
                        color: $fontBoldColor;
                        font-size: 14px;
                        cursor: pointer;
                        &:hover {
                            color: $primaryColor;
                        }
                    }
                }
            }
        }
    }
</style>
