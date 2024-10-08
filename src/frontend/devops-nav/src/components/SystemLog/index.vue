<template>
    <bk-dialog
        ext-cls="system-log-dialog"
        v-model="isShow"
        :width="1105"
        :close-icon="true"
        :show-footer="false"
        :esc-close="true"
    >
        <div
            ref="log"
            class="system-log-layout"
        >
            <div class="layout-left">
                <div class="version-warpper">
                    <div
                        v-for="(log, index) in list"
                        :key="log.version"
                        class="log-tab"
                        :class="{ active: index === activeIndex }"
                        @click="handleTabChange(index)"
                    >
                        <div class="title">
                            {{ log.version }}
                            <div
                            v-if="index === 0"
                                class="new-flag"
                            >
                                {{ $t('currentVersion') }}
                            </div>
                        </div>
                        <div class="date">{{ log.time }}</div>
                    </div>
                </div>
            </div>
            <div class="layout-right">
                <div class="content-warpper">
                    <div
                        v-html="logContent"
                        class="markdown-container"
                    />
                </div>
            </div>
        </div>
    </bk-dialog>
</template>
  
<script>
    import MarkdownIt from 'markdown-it'
    import createLocale from '../../../../locale'
    export default {
        name: 'VersionLog',
        props: {
            value: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                isShow: false,
                activeIndex: 0,
                list: [],
                latestVerSion: ''
            }
        },
        computed: {
            logContent () {
                if (this.list.length < 1) {
                    return ''
                }
                const md = new MarkdownIt()
                const defaultRender = md.renderer.rules.link_open || function (tokens, idx, options, env, self) {
                    return self.renderToken(tokens, idx, options)
                }
                md.renderer.rules.link_open = function (tokens, idx, options, env, self) {
                    const aIndex = tokens[idx].attrIndex('target')
            
                    if (aIndex < 0) {
                        tokens[idx].attrPush(['target', '_blank'])
                    } else {
                        tokens[idx].attrs[aIndex][1] = '_blank'
                    }
            
                    return defaultRender(tokens, idx, options, env, self)
                }
                return md.render(this.list[this.activeIndex].content)
            }
        },
        created () {
            this.fetchData()
        },
        methods: {
            async fetchData () {
                const { i18n } = createLocale(require.context('@locale/nav/', false, /\.json$/), true)
                const requestHandler = i18n.locale === 'en-US' ? 'fetchVersionsLogListEn' : 'fetchVersionsLogList'
                const res = await this.$store.dispatch(requestHandler)
                this.list = res || []

                this.latestVerSion = (this.list.length && this.list[0].version) || ''
                const curVerSion = localStorage.getItem('bk_latest_version')
                if (curVerSion !== this.latestVerSion && this.list.length) {
                    localStorage.setItem('bk_latest_version', this.latestVerSion)
                    this.isShow = true
                }
            },
            handleTabChange (index) {
                this.activeIndex = index
            }
        }
    }
</script>
<style lang='scss'>
    .system-log-dialog {
        .bk-dialog-tool,
        .bk-dialog-header {
            display: none;
        }
        .bk-dialog-body {
            padding: 0;
        }
    }
    .system-log-layout {
        position: relative;
        display: flex;
        height: 600px;
        background: #fff;
        &.hide {
            overflow: hidden;
            box-shadow: 0 1px 2px 0 rgba(99, 101, 110, 1);
            transition: 0.4s cubic-bezier(0.74, 0.01, 0.2, 1);
            transform-origin: center;
        }
        .layout-left {
            position: relative;
            padding: 40px 0;
            background: #fafbfd;
            &::after {
                position: absolute;
                top: 0;
                right: 0;
                width: 1px;
                height: 100%;
                background: #dcdee5;
                content: '';
            }
        }
        .layout-right {
            flex: 1;
            padding: 45px;
        }
        .version-warpper {
            overflow-y: hidden;
            overflow-x: hidden;
            max-height: 520px;
            &:hover {
                overflow-y: auto;
            }
        }
        .content-warpper {
            overflow-y: hidden;
            max-height: 510px;
            &:hover {
                overflow-y: auto;
            }
        }
        .log-tab {
            position: relative;
            display: flex;
            height: 54px;
            padding-left: 30px;
            cursor: pointer;
            border-bottom: 1px solid #dcdee5;
            flex-direction: column;
            justify-content: center;
            &.active {
                background: #fff;
                &::before {
                    background: #3a84ff;
                }
                .title {
                    color: #313238;
                }
            }
            &:first-child {
                border-top: 1px solid #dcdee5;
            }
            &::before {
                position: absolute;
                top: -1px;
                left: 0;
                width: 4px;
                height: 100%;
                border: 1px solid transparent;
                content: '';
            }
            .title {
                display: flex;
                font-size: 16px;
                font-weight: bold;
                line-height: 22px;
                color: #63656e;
            }
            .date {
                font-size: 12px;
                line-height: 17px;
                color: #63656e;
            }
            .new-flag {
                display: flex;
                width: 58px;
                height: 20px;
                font-size: 12px;
                color: #fff;
                background: #699df4;
                border-radius: 2px;
                align-items: center;
                justify-content: center;
                margin: 0 15px;
            }
        }
        .markdown-container {
            font-size: 14px;
            color: #313238;
            a {
                color: #3c96ff;
            }
            h1,
            h2,
            h3,
            h4,
            h5 {
                height: auto;
                margin: 10px 0;
                font-weight: bold;
                color: #34383e;
            }
            h1 {
                font-size: 30px;
            }
            h2 {
                font-size: 24px;
            }
            h3 {
                font-size: 18px;
            }
            h4 {
                font-size: 16px;
            }
            h5 {
                font-size: 14px;
            }
            em {
                font-style: italic;
            }
            div,
            p,
            font,
            span,
            li {
                line-height: 1.3;
            }
            p {
                margin: 0 0 1em;
            }
            table,
            table p {
                margin: 0;
            }
            ul,
            ol {
                padding: 0;
                margin: 0 0 1em 2em;
                text-indent: 0;
            }
            ul {
                padding: 0;
                margin: 10px 0 10px 15px;
                list-style-type: none;
            }
            ol {
                padding: 0;
                margin: 10px 0 10px 25px;
            }
            ol > li {
                line-height: 1.8;
                white-space: normal;
            }
            ul > li {
                padding-left: 15px !important;
                line-height: 1.8;
                white-space: normal;
                &::before {
                    display: inline-block;
                    width: 6px;
                    height: 6px;
                    margin-right: 9px;
                    margin-left: -15px;
                    background: #000;
                    border-radius: 50%;
                    content: '';
                }
            }
            li > ul {
                margin-bottom: 10px;
            }
            li ol {
                padding-left: 20px !important;
            }
            ul ul,
            ul ol,
            ol ol,
            ol ul {
                margin-bottom: 0;
                margin-left: 20px;
            }
            ul.list-type-1 > li {
                padding-left: 0 !important;
                margin-left: 15px !important;
                list-style: circle !important;
                background: none !important;
            }
            ul.list-type-2 > li {
                padding-left: 0 !important;
                margin-left: 15px !important;
                list-style: square !important;
                background: none !important;
            }
            ol.list-type-1 > li {
                list-style: lower-greek !important;
            }
            ol.list-type-2 > li {
                list-style: upper-roman !important;
            }
            ol.list-type-3 > li {
                list-style: cjk-ideographic !important;
            }
            pre,
            code {
                width: 95%;
                padding: 0 3px 2px;
                font-family: Monaco, Menlo, Consolas, "Courier New", monospace;
                font-size: 14px;
                color: #333;
                -webkit-border-radius: 3px;
                -moz-border-radius: 3px;
                border-radius: 3px;
            }
            code {
                padding: 2px 4px;
                font-family: Consolas, monospace, tahoma, Arial;
                color: #d14;
                border: 1px solid #e1e1e8;
            }
            pre {
                display: block;
                padding: 9.5px;
                margin: 0 0 10px;
                font-family: Consolas, monospace, tahoma, Arial;
                font-size: 13px;
                word-break: break-all;
                word-wrap: break-word;
                white-space: pre-wrap;
                background-color: #f6f6f6;
                border: 1px solid #ddd;
                border: 1px solid rgba(0, 0, 0, 0.15);
                border-radius: 2px;
            }
            pre code {
                padding: 0;
                white-space: pre-wrap;
                border: 0;
            }
            blockquote {
                padding: 0 0 0 14px;
                margin: 0 0 20px;
                border-left: 5px solid #dfdfdf;
            }
            blockquote p {
                margin-bottom: 0;
                font-size: 14px;
                font-weight: 300;
                line-height: 25px;
            }
            blockquote small {
                display: block;
                line-height: 20px;
                color: #999;
            }
            blockquote small::before {
                content: '\2014 \00A0';
            }
            blockquote::before,
            blockquote::after {
                content: "";
            }
        }
    }
  </style>
