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
        }
    }
</script>

<style lang="scss">
    .devops-atomstore {
        flex: 1;
        display: flex;
        flex-direction: column;
        width: 100%;
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
