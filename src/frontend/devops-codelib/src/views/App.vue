<template>
    <div class="devops-codelib">
        <header class="devops-codelib-header">
            <logo size="32" :name="logo" />
            <span>{{title}}</span>
            <i class="bk-icon icon-question-circle" @click="gotoDoc()"></i>
        </header>
        <main>
            <router-view></router-view>
        </main>
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

        methods: {
            gotoDoc () {
                window.open(`${DOCS_URL_PREFIX}/所有服务/代码库/summary.html`, '_blank')
            }
        }
    }
</script>

<style lang="scss">
    .devops-codelib {
        flex: 1;
        display: flex;
        flex-direction: column;
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
    }
</style>
