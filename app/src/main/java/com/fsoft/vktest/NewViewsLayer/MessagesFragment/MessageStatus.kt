package com.fsoft.vktest.NewViewsLayer.MessagesFragment

import com.fsoft.vktest.AnswerInfrastructure.Message
import java.util.Objects


class MessageStatus {
    val STATUS_RECEIVED: Int = 0
    val STATUS_ANSWERED: Int = 1
    val STATUS_IGNORED: Int = 2
    val STATUS_ERROR: Int = 3
    var message: Message? = null
    var status: Int = 0

    constructor(message: Message?, status: Int) {
        this.message = message
        this.status = status
    }

    constructor(message: Message?) {
        this.message = message
    }

    fun received(): MessageStatus {
        status = STATUS_RECEIVED
        return this
    }

    fun answered(): MessageStatus {
        status = STATUS_ANSWERED
        return this
    }

    fun error(): MessageStatus {
        status = STATUS_ERROR
        return this
    }

    fun ignored(): MessageStatus {
        status = STATUS_IGNORED
        return this
    }

    val isReceived: Boolean
        get() = status == STATUS_RECEIVED

    val isAnswered: Boolean
        get() = status == STATUS_ANSWERED

    val isIgnored: Boolean
        get() = status == STATUS_IGNORED

    val isError: Boolean
        get() = status == STATUS_ERROR


    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        //чтобы можно было использовать contains (message)
        if (o!!.javaClass == Message::class.java) return o == message
        if (javaClass != o.javaClass) return false
        val that = o as MessageStatus
        return message == that.message
    }

    override fun hashCode(): Int {
        return Objects.hash(message)
    }
}