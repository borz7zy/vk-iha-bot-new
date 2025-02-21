package com.fsoft.vktest.NewViewsLayer

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.fsoft.vktest.ApplicationManager
import com.fsoft.vktest.Communication.Account.Telegram.TgAccount
import com.fsoft.vktest.Communication.Account.Telegram.TgAccountCore.GetMeListener
import com.fsoft.vktest.Communication.Account.Telegram.User
import com.fsoft.vktest.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AccountsScreenCompose(applicationManager: ApplicationManager) {
    val tgAccounts = remember { mutableStateListOf<TgAccount>() }
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var tgToken by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            loadAccounts(applicationManager, tgAccounts)
            delay(5000) // Обновлять каждые 5 секунд
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (tgAccounts.isEmpty()) {
                EmptyAccountsView()
            } else {
                AccountsListView(tgAccounts, applicationManager)
            }
            Text(
                text = "Нажми на аккаунт, чтобы изменить его настройки",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            )
        }

        FloatingActionButton(
            onClick = {
                //Проверка на null для ApplicationManager
                if (ApplicationManager.getInstance() == null || ApplicationManager.getInstance().context == null) {
                    Toast.makeText(
                        context,
                        "ApplicationManager еще не инициализирован. Попробуйте позже.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Добавить аккаунт")
        }

        if (showDialog) {
            AddTgAccountDialog(
                onDismissRequest = { showDialog = false },
                onAddClick = { inputText ->
                    // Создание аккаунта здесь, используя inputText
                    tgToken = inputText

                    val tgAccount = TgAccount(applicationManager, "tg" + System.currentTimeMillis())
                    applicationManager.communicator.addAccount(tgAccount)

                    //проверить и если валидно сохранить
                    var bad: Boolean? = false
                    if (tgToken.toString().isEmpty()) {
                        Toast.makeText(context, "Токен не введён!", Toast.LENGTH_SHORT).show()
                        bad = true
                    }
                    val parts = tgToken.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    if (parts.size != 2 && bad == false) {
                        Toast.makeText(context, "Токен введён неверно.", Toast.LENGTH_SHORT).show()
                        bad = true
                    }
                    val idString = parts[0]
                    val token = parts[1]
                    var id: Long = 0
                    if (bad == false) {
                        try {
                            id = idString.toLong()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Токен введён неверно: " + e.message,
                                Toast.LENGTH_SHORT
                            ).show()
                            bad = true
                        }
                    }
                    tgAccount.id = id
                    tgAccount.token = token
                    tgAccount.getMe(object : GetMeListener {
                        override fun gotUser(user: User) {
                            Toast.makeText(context, "Вход выполнен!", Toast.LENGTH_SHORT).show()
                            tgAccount.startAccount()

                            //Обновление списка аккаунтов после добавления
                            coroutineScope.launch {
                                loadAccounts(applicationManager, tgAccounts)
                            }
                        }

                        override fun error(error: Throwable) {
                            tgAccount.id = 0
                            tgAccount.token = ""
                            Toast.makeText(
                                context,
                                "Токен не сработал: " + error.javaClass.name + " " + error.message,
                                Toast.LENGTH_SHORT
                            ).show()
                            bad = true
                        }
                    })
                    showDialog = false
                }
            )
        }
    }
}

private fun loadAccounts(
    applicationManager: ApplicationManager,
    tgAccounts: MutableList<TgAccount>
) {
    tgAccounts.clear()
    tgAccounts.addAll(applicationManager.communicator.getTgAccounts())
}

@Composable
fun EmptyAccountsView() {
    Text(
        text = "Не добавлено ни одного аккаунта. Чтобы добавить аккаунт, нажми на \"+\" снизу.",
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    )
}

@Composable
fun AccountsListView(
    tgAccounts: List<TgAccount>,
    applicationManager: ApplicationManager
) {
    LazyColumn {
        items(tgAccounts) { account ->
            AccountItem(account = account, applicationManager = applicationManager)
        }
    }
}

@Composable
fun AccountItem(account: TgAccount, applicationManager: ApplicationManager) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Аватарка
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(/*account.userPhotoUrl ?: */R.drawable.ic_ban)
                        .crossfade(true)
                        .build()
                )
                Image(
                    painter = painter,
                    contentDescription = "Account Avatar",
                    modifier = Modifier.size(50.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Название и статус
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = account.toString(), style = MaterialTheme.typography.titleMedium)
                    Text(text = "${account.state}", style = MaterialTheme.typography.bodySmall)
                }
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Account Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("Настройки") }, onClick = { /*TODO*/ })
                        DropdownMenuItem(text = { Text("Удалить аккаунт") }, onClick = {
                            applicationManager.communicator.remTgAccount(account)
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Таблица статы
            Column(modifier = Modifier.fillMaxWidth()) {
                DataRow(name = "Отправлено сообщений", value = "${account.messageProcessor.messagesSentCounter}")
                DataRow(name = "Принято сообщений", value = "${account.messageProcessor.messagesReceivedCounter}")
                DataRow(name = "Выполнено запросов", value = "${account.apiCounter}")
                DataRow(name = "Запросов с ошибкой", value = "${account.errorCounter}")
                var value: Boolean = account.isEnabled
                var text = if (value) "Включено" else "Выключено"
                DataRow(name = "Ответ инструкцией", value = text)
                value = account.getMessageProcessor().isChatsEnabled()
                text = if (value) "Включено" else "Выключено"
                DataRow(name = "Отвечать в беседах", value = text)
//                value = account.getMessageProcessor().isChatsEnabled()
//                text = if (value) "Включено" else "Выключено"
//                DataRow(name = "Трансляция статуса", value = text)
            }
        }
    }
}

@Composable
fun DataRow(name: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = name, modifier = Modifier.weight(1f))
        Text(text = value, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
fun AddTgAccountDialog(
    onDismissRequest: () -> Unit,
    onAddClick: (String) -> Unit // Callback с введенным текстом
) {
    var textInputValue by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Введи токен для Telegram бота", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Для работы с Telegram требуется создать аккаунт бота. " +
                            "Чтобы это сделать, найди в телеграме бота @BotFather и следуй инструкции. " +
                            "Ты получишь от него токен. Его и надо ввести сюда, чтобы бот заработал.",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = textInputValue,
                    onValueChange = { textInputValue = it },
                    label = { Text("000000000:AAAAAAA-BBBBBBBB-CCCCCCC") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { onDismissRequest() }) {
                        Text("Выйти")
                    }

                    Button(
                        onClick = {
                            onAddClick(textInputValue)  // Передаем текст обратно
                            onDismissRequest()
                        },
                        enabled = textInputValue.isNotBlank()
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}