package com.fsoft.vktest.NewViewsLayer.MessagesFragment

import com.fsoft.vktest.AnswerInfrastructure.BotBrain.OnMessageStatusChangedListener
import com.fsoft.vktest.AnswerInfrastructure.Message
import com.fsoft.vktest.ApplicationManager


/*
* Этот класс занимается хранением истории сообщений для отображения её на экране "сообщения".
* Вызывать из ApplicationManager!
* Обращаться  из MessagesFragment через ApplicationManager
*
* */
class MessageHistory(applicationManager: ApplicationManager) {
    val messages: MutableList<MessageStatus> = mutableListOf() //сначала самые новые, 0=последнее
//    private var applicationManager: ApplicationManager? = null


    init {
//        this.applicationManager = applicationManager
        applicationManager.brain.addMessageListener(object : OnMessageStatusChangedListener {
            override fun messageReceived(message: Message) {
                messages.add(0, MessageStatus(message).received())
                //почистить очередь больше 100
                if (messages.size > MESSAGE_LIMIT) {
                    messages.subList(MESSAGE_LIMIT, messages.size).clear()
                }
            }

            override fun messageAnswered(message: Message) {
                for (messageStatus in messages) {
                    messageStatus.message?.let {
                        if (it.message_id == message.message_id) {
                            messageStatus.answered().message = message
                        }
                    }
                }
            }

            override fun messageError(message: Message, e: Exception) {
                for (messageStatus in messages) {
                    messageStatus.message?.let {
                        if (it.message_id == message.message_id) {
                            messageStatus.error().message = message
                        }
                    }
                }
            }

            override fun messageIgnored(message: Message) {
                for (messageStatus in messages) {
                    messageStatus.message?.let {
                        if (it.message_id == message.message_id) {
                            messageStatus.ignored().message = message
                        }
                    }
                }
            }
        })
    }

    companion object {
        private const val MESSAGE_LIMIT = 500
    }
}