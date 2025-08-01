<template>
    <section class="devops-empty-tips">
        <h2 class="title">
            {{ title }}
        </h2>
        <p class="desc">
            {{ desc }}
        </p>
        <p class="btns-row">
            <slot />
        </p>
    </section>
</template>

<script>
    export default {
        props: {
            title: String,
            desc: String
        }
    }
</script>

<style lang="scss">
    .devops-empty-tips {
        width: 913px;
        margin: 139px auto 0;
        text-align: center;
        .title {
            margin: 10px 0 20px;
            color: #313238;
            font-size: 20px;
            font-weight: normal;
        }
        .desc {
            margin-bottom: 20px;
            color: #979BA5;
            font-size: 14px;
            font-weight: 400;
        }
        .btns-row {
            font-size: 0;
            .bk-button {
                & + .bk-button,
                & + .empty-btns-item {
                    margin-left: 10px;
                }
            }
        }
    }
</style>
