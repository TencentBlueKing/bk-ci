<template>
    <div class="devops-atomstore">
        <router-view></router-view>
    </div>
</template>

<script>
    export default {
        name: 'app',

        computed: {
            logo () {
                return this.$route.meta.logo
            },
            title () {
                return this.$route.meta.title
            }
        },

        created () {
            window.addEventListener('resize', this.flexible, false)
            this.flexible()
        },

        beforeDestroy () {
            window.removeEventListener('resize', this.flexible, false)
            const doc = window.document
            const docEl = doc.documentElement
            docEl.style.fontSize = '14px'
        },

        methods: {
            flexible () {
                const doc = window.document
                const docEl = doc.documentElement
                const designWidth = 1580 // 默认设计图宽度
                const maxRate = 2300 / designWidth
                const minRate = 1280 / designWidth
                const clientWidth = docEl.getBoundingClientRect().width || window.innerWidth
                const flexibleRem = Math.max(Math.min(clientWidth / designWidth, maxRate), minRate) * 100
                docEl.style.fontSize = flexibleRem + 'px'
            }
        }
    }
</script>

<style lang="scss">
    .devops-atomstore {
        flex: 1;
        display: flex;
        flex-direction: column;
        width: 100%;
        height: 100%;
        &-header {
            display: flex;
            width: 100%;
            height: 60px;
            padding: 0 30px;
            border-bottom: 1px solid #dde4eb;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.03);
            background: white;
            align-items: center;
            > svg {
                margin-right: 11px;
            }
            > span {
                letter-spacing: .5px;
                font-size: 16px;
                color: #333948;
            }
            > i {
                padding-left: 10px;
                color: #c4cdd6;
                cursor: pointer;
            }
        }
        > main {
            flex: 1;
            overflow: auto;
        }
        ::-webkit-scrollbar-thumb {
            background-color: #c4c6cc !important;
            border-radius: 3px !important;
            &:hover {
                background-color: #979ba5 !important;
            }
        }
        ::-webkit-scrollbar {
            width: 6px !important;
            height: 6px !important;
        }
    }
</style>
