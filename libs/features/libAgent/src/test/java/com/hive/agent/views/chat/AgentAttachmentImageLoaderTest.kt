// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.chat

import com.hive.plugin.agent.model.AttachmentType
import com.hive.plugin.agent.model.ChatAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentAttachmentImageLoaderTest {

    @Test
    fun `filters only displayable image attachments`() {
        val list = listOf(
            ChatAttachment(AttachmentType.IMAGE, url = null, base64 = "data:image/png;base64,aaa"),
            ChatAttachment(AttachmentType.IMAGE, url = null, base64 = null),
            ChatAttachment(AttachmentType.FILE, url = "/tmp/a.txt"),
            ChatAttachment(AttachmentType.IMAGE, url = "/tmp/shot.png"),
        )
        val images = AgentAttachmentImageLoader.imageAttachments(list)
        assertEquals(2, images.size)
        assertTrue(AgentAttachmentImageLoader.hasDisplayableSource(images[0]))
        assertTrue(AgentAttachmentImageLoader.hasDisplayableSource(images[1]))
        assertFalse(AgentAttachmentImageLoader.hasDisplayableSource(list[1]))
    }
}
