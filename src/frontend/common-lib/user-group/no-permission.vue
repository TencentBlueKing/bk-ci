<template>
    <article class="group-manage">
        <div class="content-wrapper">
            <bk-exception
                class="exception-wrap-item exception-part"
                type="403"
                scene="part"
                :description="title"
                :class="{ 'exception-gray': isGray }"
            >
            </bk-exception>
        </div>
    </article>
</template>

<script>
    export default {
        props: {
            title: {
                type: String,
                default: ''
            }
        }
    }
</script>

<style lang="scss" scoped>
    .group-manage {
        flex: 1;
    }
    .content-wrapper {
        display: flex;
        align-items: center;
        flex-direction: column;
        padding-top: 10%;
        width: 100%;
        height: 100%;
        background-color: #fff;
        box-shadow: 0 2px 2px 0 rgba(0,0,0,0.15);
        text-align: center;
        font-size: 14px;
    }
    .btn {
        margin-top: 32px;
    }
</style>
