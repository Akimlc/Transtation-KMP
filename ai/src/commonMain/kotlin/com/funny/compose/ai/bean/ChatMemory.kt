package com.funny.compose.ai.bean

import com.funny.compose.ai.token.TokenCounters
import java.util.Date
import java.util.LinkedList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class ChatMemory {
    /**
     * 获取实际要发送的消息，注意不包含最初的 Prompt
     * @param list List<ChatMessage>
     * @return List<ChatMessage>
     */
    abstract suspend fun getIncludedMessages(list: List<ChatMessage>) : List<ChatMessage>

    companion object {
        val Saver = { chatMemory: ChatMemory ->
            when(chatMemory) {
                is ChatMemoryFixedMsgLength -> "chat_memory#fixed_length#${chatMemory.length}"
                is ChatMemoryFixedDuration -> "chat_memory#fixed_duration#${chatMemory.duration.inWholeMilliseconds}"
                else -> ""
            }
        }

        val Restorer = lambda@ { str: String ->
            val parts = str.split("#")
            if (parts.size != 3 || parts[0] != "chat_memory") {
                return@lambda DEFAULT_CHAT_MEMORY
            }
            when(parts[1]) {
                "fixed_length" -> ChatMemoryFixedMsgLength(parts[2].toInt())
                "fixed_duration" -> ChatMemoryFixedDuration(parts[2].toLong().milliseconds)
                else -> DEFAULT_CHAT_MEMORY
            }
        }
    }
}

class ChatMemoryFixedMsgLength(val length: Int) : ChatMemory() {
    override suspend fun getIncludedMessages(list: List<ChatMessage>): List<ChatMessage> {
        val linkedList = LinkedList<ChatMessage>()
        // 反向遍历 list
        var i = list.lastIndex
        while (i >= 0) {
            val item = list[i]
            // 如果这是错误消息，且前面有我发的消息，那么就跳过这条消息和我的那一条
            if (item.error != null) {
                if (i > 0 && list[i-1].sendByMe) {
                    i -= 2;
                    continue
                }
            }
            linkedList.addFirst(item)
            if (linkedList.size >= length) {
                break
            }
            i--
        }
        return linkedList
    }
}

class ChatMemoryFixedDuration(val duration: Duration) : ChatMemory() {
    override suspend fun getIncludedMessages(list: List<ChatMessage>): List<ChatMessage> {
        val now = Date()
        val last = list.lastOrNull { it.timestamp > now.time - duration.inWholeMilliseconds }
        return if (last == null) {
            emptyList()
        } else {
            list.dropWhile { it.timestamp < last.timestamp }
        }
    }
}

class ChatMemoryMaxContextSize(var maxContextSize: Int, var systemPrompt: String): ChatMemory() {
    override suspend fun getIncludedMessages(list: List<ChatMessage>): List<ChatMessage> {
        var idx = list.size - 1
        var curLength = systemPrompt.length
        while (curLength < maxContextSize && idx >= 0) {
            curLength += list[idx].content.length
            idx--
        }
        return list.subList(idx + 1, list.size)
    }
}

class ChatMemoryMaxToken(val model: Model, var systemPrompt: String): ChatMemory() {
    private val tokenCounter = TokenCounters.findById(model.tokenCounterId)
    private val maxAllTokens = (model.maxContextTokens * 0.95).toInt()
    private val maxInputToken = maxAllTokens / 2

    override suspend fun getIncludedMessages(list: List<ChatMessage>): List<ChatMessage> {
        val remainingInputTokens = maxInputToken - tokenCounter.countMessages(listOf(
            ChatMessageReq.text(systemPrompt, "system")
        ))
        var idx = list.size - 1
        var curInputTokens = 0
        while (curInputTokens < remainingInputTokens && idx >= 0) {
            curInputTokens += tokenCounter.countMessages(listOf(list[idx].toReq()))
            idx--
        }
        return list.subList(idx + 1, list.size)
    }
}

private val DEFAULT_CHAT_MEMORY = ChatMemoryFixedMsgLength(2)