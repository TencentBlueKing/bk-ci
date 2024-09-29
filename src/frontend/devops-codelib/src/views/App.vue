<template>
    <div class="devops-codelib">
        <header class="devops-codelib-header">
            <div>
                <logo
                    size="32"
                    :name="logo"
                />
                <span>{{ title }}</span>
            </div>
            <a
                class="devops-codelib-header-copilot"
                target="_blank"
                href="https://git.woa.com/help/menu/solutions/copilot.html"
            >
                <span>{{ $t("体验工蜂 Copilot") }}</span>
                <icon
                    name="tiaozhuan"
                    :size="12"
                    class="score-icon"
                ></icon>
            </a>
        </header>
        <main>
            <router-view />
        </main>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        name: 'app',
        computed: {
            logo () {
                return this.$route.meta.logo
            },
            title () {
                return this.$t('codelib.codelib')
            }
        },
        created () {
            this.fetchCodeTypeList()
        },
        methods: {
            ...mapActions('codelib', [
                'fetchCodeTypeList'
            ])
        }
    }
</script>

<style lang="scss">
    .devops-codelib {
        flex: 1;
        display: flex;
        flex-direction: column;
        min-width: 1380px;
        color: #63656E;
        &-header {
            display: flex;
            width: 100%;
            height: 60px;
            padding: 0 30px;
            border-bottom: 1px solid #dde4eb;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.03);
            background: white;
            align-items: center;
            justify-content: space-between;
            svg {
                margin-right: 11px;
                vertical-align: middle;
            }
            span {
                letter-spacing: .5px;
                font-size: 16px;
                color: #333948;
            }
            i {
                padding-left: 10px;
                color: #c4cdd6;
                cursor: pointer;
            }
            .devops-codelib-header-copilot {
                color: #1592ff;
                cursor: pointer;
                span {
                    font-size: 12px;
                    color: #1592ff;
                    vertical-align: middle;
                }
            }
        }
        > main {
            flex: 1;
            overflow: auto;
        }
    }
</style>
