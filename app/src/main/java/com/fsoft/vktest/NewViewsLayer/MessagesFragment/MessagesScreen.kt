package com.fsoft.vktest.NewViewsLayer.MessagesFragment

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fsoft.vktest.AnswerInfrastructure.BotBrain
import com.fsoft.vktest.AnswerInfrastructure.Message
import com.fsoft.vktest.ApplicationManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MessagesScreen() {
    val applicationManager = remember { ApplicationManager.getInstance() }
    val messages = remember { mutableStateListOf<Message>() }
    val coroutineScope = rememberCoroutineScope()
    val handler = Handler(Looper.getMainLooper())

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        coroutineScope.launch {
            while (applicationManager.getMessageHistory() == null) {
                kotlinx.coroutines.delay(10)
            }
            messages.clear()
            messages.addAll(applicationManager.getMessageHistory().messages.mapNotNull { it.message })
            isLoading = false
        }
    }

    val messageListener = remember {
        object : BotBrain.OnMessageStatusChangedListener {
            override fun messageReceived(message: Message) {
                handler.post {
                    messages.add(message)
                }
            }

            override fun messageAnswered(message: Message) {
                handler.post {
                    messages.add(message)
                }
            }

            override fun messageError(message: Message, e: Exception) {
                handler.post {
                    messages.add(message)
                }
            }

            override fun messageIgnored(message: Message) {
                handler.post {
                    messages.add(message)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        applicationManager.getBrain().addMessageListener(messageListener)
        onDispose {
            applicationManager.getBrain().remMessageListener(messageListener)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(messages) { message ->
                    MessageItem(message)
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: Message) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timestamp = dateFormat.format(message.date)
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = "${message.author}: ${message.text}")
        Text(text = "[$timestamp]", modifier = Modifier.padding(start = 8.dp))
    }
}
