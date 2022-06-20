<template>
    <footer class="footer-wrapper">
        <div class="footer-content">
            <div class="footer-services">
                <section class="footer-service-item">
                    <div class="service-icon">
                        <img
                            src="./../../assets/images/footer1.png"
                            alt="7*24"
                        >
                    </div>
                    <span class="service-name">7*24小时技术支持</span>
                </section>
                <section class="footer-service-item">
                    <div class="service-icon">
                        <img
                            src="./../../assets/images/footer2.png"
                            alt="compile speed up"
                        >
                    </div>
                    <span class="service-name">游戏编译加速服务</span>
                </section>
                <section class="footer-service-item">
                    <div class="service-icon">
                        <img
                            src="./../../assets/images/footer3.png"
                            alt="vip"
                        >
                    </div>
                    <span class="service-name">大客户旗舰服务</span>
                </section>
                <section class="footer-service-item">
                    <div class="service-icon">
                        <img
                            src="./../../assets/images/footer4.png"
                            alt="micro service"
                        >
                    </div>
                    <span class="service-name">容器咨询服务</span>
                </section>
            </div>

            <div class="footer-links">
                <section class="footer-links-wrapper">
                    <div class="footer-links-top">
                        <div class="footer-links-nav links-top-section">
                            <p class="links-top-title">
                                网站导航
                            </p>
                            <p class="links-top-row">
                                <a
                                    href="/"
                                    class="text-link"
                                >蓝盾动态</a>
                                <a
                                    href="/"
                                    class="text-link"
                                >解决方案</a>
                                <a
                                    href="/"
                                    class="text-link"
                                >文档中心</a>
                            </p>
                            <p class="links-top-row">
                                <a
                                    href="/"
                                    class="text-link"
                                >成功案例</a>
                                <a
                                    href="/"
                                    class="text-link"
                                >编译加速咨询</a>
                            </p>
                        </div>
                        <div class="footer-links-contact links-top-section">
                            <p class="links-top-title">
                                联系我们
                            </p>
                            <p class="links-top-row">
                                <a
                                    href="wxwork://message/?username=DevOps"
                                    class="text-link"
                                >企业微信：蓝盾助手</a>
                                <a
                                    href="/"
                                    class="text-link"
                                >蓝盾K吧</a>
                            </p>
                            <p class="links-top-row">
                                <a
                                    href="javascript:;"
                                    class="text-link"
                                >电话：84288438</a>
                            </p>
                        </div>
                    </div>
                    <div class="footer-links-bottom">
                        <div class="footer-links-friends">
                            <span class="plain-text">友情链接：</span>
                            <a
                                href="/"
                                class="text-link"
                            >TAPD</a>
                            <a
                                href="/"
                                class="text-link"
                            >SCAN扫描</a>
                            <a
                                href="/"
                                class="text-link"
                            >Code平台</a>
                            <a
                                href="/"
                                class="text-link"
                            >企业云盘</a>
                            <a
                                href="/"
                                class="text-link"
                            >CodeCC</a>
                            <a
                                href="/"
                                class="text-link"
                            >WeTest</a>
                            <a
                                href="/"
                                class="text-link"
                            >蓝鲸智云</a>
                            <a
                                href="/"
                                class="text-link"
                            >APK加固</a>
                        </div>
                    </div>
                </section>
                <section class="footer-qrcodes">
                    <div class="qrcode">
                        <p class="qrcode-title">
                            蓝盾APP
                        </p>
                        <img
                            src="./../../assets/images/qrcode_app.png"
                            alt="蓝盾APP"
                        >
                    </div>
                    <div class="qrcode">
                        <p class="qrcode-title">
                            蓝盾服务号
                        </p>
                        <img
                            src="./../../assets/images/qrcode_service.png"
                            alt="蓝盾服务号"
                        >
                    </div>
                </section>
            </div>

            <div class="footer-copyright">
                <div class="footer-logo">
                    <Logo
                        name="devops-logo"
                        width="160"
                    />
                </div>
                <div class="footer-copyright-detail">
                    <span class="copyright-content">Copyright &copy; 2012-2018 Tencent BlueKing. All Rights Reserved. 腾讯蓝鲸 版权所有</span>
                    <div class="language-toggle">
                        简体中文 <span><i class="devops-icon icon-angle-down" /></span>
                        <div class="language-choose">
                            <a href="javascript:;">简体中文</a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </footer>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'

    @Component
    export default class Footer extends Vue {

    }
</script>

<style lang="scss">
    .footer-wrapper {
        height: 290px;
        background-color: #090A1A;
        .footer-content {
            min-width: 1275px;
            width: 100%;
            height: 100%;
            margin: 0 auto;
            padding: 0 40px;
        }
        .footer-services {
            display: flex;
            height: 55px;
            padding: 0 5px;
            border-bottom: 1px solid #333C48;
        }
        .footer-service-item {
            display: flex;
            flex: 1.5;
            align-items: center;
            font-size: 0;
            justify-content: center;
            &:first-child {
                flex: 1;
                justify-content: flex-start;
            }
            &:last-child {
                flex: 1;
                justify-content: flex-end;
            }
        }
        .service-icon {
            display: inline-block;
            width: 30px;
            height: 30px;
            border-radius: 50%;
            img {
                width: 100%;
            }
        }
        .service-name {
            padding-left: 10px;
            font-size: 14px;
            color: #fff;
        }
        .footer-links {
            display: flex;
            margin: 28px 0 20px;
            &-top {
                display: flex;
                margin-bottom: 35px;
            }
            .links-top-row {
                margin-bottom: 20px;
                &:last-child {
                    margin-bottom: 0;
                }
            }
            .links-top-title {
                margin-bottom: 15px;
                font-size: 14px;
                color: #fff;
            }
            &-wrapper {
                flex: 1;
            }
            .text-link,
            .plain-text {
                font-size: 12px;
                color: #656A78;
                & + .text-link {
                    margin-left: 25px;
                }
            }
            &-friends {
                font-size: 0;
                .text-link {
                    & + .text-link {
                        margin-left: 50px;
                    }
                }
            }
        }
        .footer-qrcodes {
            display: flex;
            width: 208px;
            .qrcode {
                flex: 1;
                text-align: center;
                &-title {
                    margin-bottom: 20px;
                    font-size: 12px;
                    color: #656A78;
                }
            }
        }
        .links-top-section {
            width: 400px;
        }
        .footer-copyright {
            display: flex;
            align-items: center;
            &-detail {
                flex: 1;
                text-align: right;
                font-size: 0;
            }
            .copyright-content {
                font-size: 12px;
            }
        }
        .footer-logo {
            width: 190px;
        }
        .language-toggle {
            position: relative;
            display: inline-block;
            cursor: pointer;
            height: 28px;
            line-height: 25px;
            margin-left: 10px;
            padding: 0 5px;
            font-size: 12px;
            background: none;
            border: 1px solid #6f707c;
            border-radius: 3px;
            color: #6f707c;
            font-family: 'Microsoft YaHei';
            vertical-align: 2px;
        }

        .language-toggle span {
            margin-left: 5px;
        }

        .language-toggle:hover {
            color: #fff;
            border-top-right-radius: 0;
            border-top-left-radius: 0;
        }

        .language-toggle:hover .language-choose {
            bottom: 100%;
            opacity: 1;
            transform: translateY(0px);
            border-top-right-radius: 3px;
            border-top-left-radius: 3px;
        }

        .language-toggle option {
            color: #7b7d8a;
        }

        .language-toggle::-ms-expand {
            display: none;
        }

        .language-choose {
            opacity: 0;
            position: absolute;
            width: 80px;
            left: -1px;
            bottom: 100%;
            padding: 5px 0;
            overflow: hidden;
            border: 1px solid #6f707c;
            transform: translateY(20px);
            -webkit-transition: all .5s;
            -moz-transition: all .5s;
            -ms-transition: all .5s;
            transition: all .5s;
            text-align: center;
        }

        .language-choose a {
            display: block;
            color: #6f707c;
            padding: 0 5px;
        }

        .language-choose a:hover {
            color: #fff;
        }
    }
</style>
