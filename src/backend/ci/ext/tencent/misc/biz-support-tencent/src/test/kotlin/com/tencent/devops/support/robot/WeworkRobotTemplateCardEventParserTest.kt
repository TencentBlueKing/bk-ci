package com.tencent.devops.support.robot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WeworkRobotTemplateCardEventParserTest {

    @Test
    fun `parses message-push lowercase template_card_event`() {
        val xml = """
            <xml>
              <msgtype><![CDATA[template_card_event]]></msgtype>
              <from>
                <userid><![CDATA[zhangsan]]></userid>
                <alias><![CDATA[royalhuang]]></alias>
              </from>
              <template_card_event>
                <card_type><![CDATA[button_interaction]]></card_type>
                <event_key><![CDATA[BKCI_REVIEW|agree|review_p1]]></event_key>
                <task_id><![CDATA[review_p1]]></task_id>
              </template_card_event>
            </xml>
        """.trimIndent()
        val click = WeworkRobotTemplateCardEventParser.parse(xml)!!
        assertEquals("royalhuang", click.userId)
        assertEquals("agree", click.action)
        assertEquals("review_p1", click.taskId)
    }

    @Test
    fun `parses official app EventKey`() {
        val xml = """
            <xml>
              <Event>template_card_event</Event>
              <EventKey>BKCI_REVIEW|agree|review_p1</EventKey>
              <UserId>zhangsan</UserId>
            </xml>
        """.trimIndent()
        val click = WeworkRobotTemplateCardEventParser.parse(xml)!!
        assertEquals("zhangsan", click.userId)
        assertEquals("agree", click.action)
        assertEquals("review_p1", click.taskId)
    }

    @Test
    fun `ignores unrelated callback`() {
        val xml = """
            <xml>
              <MsgType>attachment</MsgType>
              <CallbackId>approval</CallbackId>
            </xml>
        """.trimIndent()
        assertNull(WeworkRobotTemplateCardEventParser.parse(xml))
    }
}
