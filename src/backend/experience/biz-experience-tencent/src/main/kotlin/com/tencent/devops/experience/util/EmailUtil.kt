package com.tencent.devops.experience.util

import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.notify.pojo.EmailNotifyMessage

object EmailUtil {

    fun makeMessage(userId: String, projectName: String, name: String, version: String, url: String, receivers: Set<String>): EmailNotifyMessage {
        val message = EmailNotifyMessage()
        message.addAllReceivers(receivers)
        message.title = getShareEmailTitle(userId, name, version)
        message.body = getShareEmailBody(userId, name, version, listOf(Triple("$name-$version", projectName, url)))
        message.format = EnumEmailFormat.HTML
        return message
    }

    private fun getShareEmailTitle(userId: String, name: String, version: String): String {
        return "【蓝盾版本体验通知】${userId}邀您体验【$name-$version】"
    }

    private fun getShareEmailBody(userId: String, name: String, version: String, rowList: List<Triple<String, String, String>>): String {
        val stringBuffer = StringBuilder()
        stringBuffer.append(SHARE_EMAIL_HTML_PREFIX)
        rowList.forEach {
            stringBuffer.append(getShareEmailBodyRow(it.first, it.second, it.third))
        }
        stringBuffer.append(SHARE_EMAIL_HTML_SUFFIX)
        val template = stringBuffer.toString()
        return template.replace(BODY_TITLE_TEMPLATE, "${userId}邀您体验【$name-$version】")
                .replace(TABLE_COLUMN1_TITLE, "名称")
                .replace(TABLE_COLUMN2_TITLE, "所属项目")
                .replace(TABLE_COLUMN3_TITLE, "操作")
    }

    private fun getShareEmailBodyRow(name: String, projectName: String, url: String): String {
        return "                                                                           <tr>\n" +
                "                                                                               <td>$name</td>\n" +
                "                                                                               <td>$projectName</td>\n" +
                "                                                                               <td align=\"center\">\n" +
                "                                                                                   <a href=\"$url\">查看</a>\n" +
                "                                                                               </td>\n" +
                "                                                                           </tr>\n"
    }

    private val BODY_TITLE_TEMPLATE = "#{bodyTitle}"
    private val TABLE_COLUMN1_TITLE = "#{column1Title}"
    private val TABLE_COLUMN2_TITLE = "#{column2Title}"
    private val TABLE_COLUMN3_TITLE = "#{column3Title}"

    private val SHARE_EMAIL_HTML_PREFIX = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
            "<!-- saved from url=(0064)http://open.oa.com/static_api/v3/templates/template10/index.html -->\n" +
            "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
            "    \n" +
            "    <title></title>\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
            "<style type=\"text/css\">\n" +
            "    * {\n" +
            "        font-family: '微软雅黑';\n" +
            "        font-size: 14px;\n" +
            "    }\n" +
            "\n" +
            "    img {\n" +
            "        outline: none;\n" +
            "        border: none;\n" +
            "        text-decoration: none;\n" +
            "    }\n" +
            "\n" +
            "    a {\n" +
            "        border: none;\n" +
            "        text-decoration: none;\n" +
            "        color: #5c90d2;\n" +
            "    }\n" +
            "\n" +
            "    a:hover {\n" +
            "        color: #3169b1;\n" +
            "        text-decoration: none;\n" +
            "    }\n" +
            "\n" +
            "    .table-email thead tr th,\n" +
            "    .table-email tbody tr td {\n" +
            "        padding: 10px;\n" +
            "        border: 1px solid #ddd;\n" +
            "        border-right: none;\n" +
            "        border-bottom: none;\n" +
            "    }\n" +
            "\n" +
            "    .table-email thead tr th:last-child,\n" +
            "    .table-email tbody tr td:last-child {\n" +
            "        border: 1px solid #ddd;\n" +
            "        border-bottom: none;\n" +
            "        text-align: center;\n" +
            "    }\n" +
            "\n" +
            "    .table-email tbody tr:last-child td {\n" +
            "        border: 1px solid #ddd;\n" +
            "        border-right: none;\n" +
            "    }\n" +
            "\n" +
            "    .table-email tbody tr:last-child td:last-child {\n" +
            "        border: 1px solid #ddd;\n" +
            "    }\n" +
            "\n" +
            "    .email-info {\n" +
            "        display: inline-block;\n" +
            "        width: 80px;\n" +
            "        height: 28px;\n" +
            "        line-height: 28px;\n" +
            "        background-color: #4A9BFF;\n" +
            "        border: #2180F5 1px solid;\n" +
            "    }\n" +
            "\n" +
            "    .email-info>a {\n" +
            "        font-size: 12px;\n" +
            "        color: #fff;\n" +
            "    }\n" +
            "\n" +
            "    .email-info>a:hover {\n" +
            "        color: #fff;\n" +
            "    }\n" +
            "\n" +
            "    .course-titles {\n" +
            "        font-size: 18px;\n" +
            "    }\n" +
            "\n" +
            "    .table-lr-blue {\n" +
            "        width: 45px;\n" +
            "        background: #0b1731;\n" +
            "    }\n" +
            "</style></head>\n" +
            "\n" +
            "\n" +
            "<body>\n" +
            "    <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
            "        <tbody>\n" +
            "            <tr>\n" +
            "                <td>\n" +
            "                    <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"963\">\n" +
            "                        <tbody>\n" +
            "                            <tr>\n" +
            "                                <td colspan=\"3\">\n" +
            "                                    <img src=\"http://devops.oa.com/console/devops_bg.png\" alt=\"img\" style=\"width:963px;height:104px;\">\n" +
            "                                </td>\n" +
            "                            </tr>\n" +
            "                            <tr>\n" +
            "                                <td class=\"table-lr-blue\"></td>\n" +
            "                                <td>\n" +
            "                                    <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"873\" style=\"background:#fff;\">\n" +
            "                                        <tbody>\n" +
            "                                            <tr>\n" +
            "                                                <td style=\"width:20px;\"></td>\n" +
            "                                                <td>\n" +
            "                                                    <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"833\">\n" +
            "                                                        <tbody>\n" +
            "                                                            <tr>\n" +
            "                                                                <td>\n" +
            "                                                                    <div style=\"margin-top:10px;\">$BODY_TITLE_TEMPLATE</div>\n" +
            "                                                                </td>\n" +
            "                                                            </tr>\n" +
            "                                                            <tr>\n" +
            "                                                                <td>\n" +
            "                                                                    <div style=\"margin-top:10px;\"></div>\n" +
            "                                                                </td>\n" +
            "                                                            </tr>\n" +
            "                                                            <tr>\n" +
            "                                                                <td>\n" +
            "                                                                    <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"833\" class=\"table-email\">\n" +
            "                                                                        <thead>\n" +
            "                                                                            <tr>\n" +
            "                                                                                <th align=\"left\" style=\"width:40%;background-color:#F6F8F8;\">$TABLE_COLUMN1_TITLE</th>\n" +
            "                                                                                <th align=\"left\" style=\"width:40%;background-color:#F6F8F8;\">$TABLE_COLUMN2_TITLE</th>\n" +
            "                                                                                <th style=\"width:20%;background-color:#F6F8F8;\">$TABLE_COLUMN3_TITLE</th>\n" +
            "                                                                            </tr>\n" +
            "                                                                        </thead>\n" +
            "                                                                        <tbody>\n"

    private val SHARE_EMAIL_HTML_SUFFIX =
            "                                                                        </tbody>\n" +
                    "                                                                    </table>\n" +
                    "                                                                </td>\n" +
                    "                                                            </tr>\n" +
                    "                                                            <tr>\n" +
                    "                                                                <td>\n" +
                    "                                                                    <div style=\"margin-bottom:10px;\"></div>\n" +
                    "                                                                </td>\n" +
                    "                                                            </tr>\n" +
                    "                                                        </tbody>\n" +
                    "                                                    </table>\n" +
                    "                                                </td>\n" +
                    "                                                <td style=\"width:20px;\"></td>\n" +
                    "                                            </tr>\n" +
                    "                                        </tbody>\n" +
                    "                                    </table>\n" +
                    "                                </td>\n" +
                    "                                <td class=\"table-lr-blue\"></td>\n" +
                    "                            </tr>\n" +
                    "                            <tr>\n" +
                    "                                <td colspan=\"3\" style=\"height:40px;background:#0b1731;\"></td>\n" +
                    "                            </tr>\n" +
                    "                        </tbody>\n" +
                    "                    </table>\n" +
                    "                </td>\n" +
                    "            </tr>\n" +
                    "        </tbody>\n" +
                    "    </table>\n" +
                    "</body>\n" +
                    "\n" +
                    "</html>"
}