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

@Composable
fun AccountsScreenCompose(applicationManager: ApplicationManager) {
    val tgAccounts = remember { mutableStateListOf<TgAccount>() }
    val context = LocalContext.current
    val activity = context as? MainActivity
    var showDialog by remember { mutableStateOf(false) }
    var tgToken by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loadAccounts(applicationManager, tgAccounts)
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
                }else{
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
//                    applicationManager.communicator.addAccount(tgAccount)
//                    tgAccount.login { loadAccounts(applicationManager, tgAccounts) }

                    //проверить и если валидно сохранить
                    var bad: Boolean? = false
                    if (tgToken == null) {
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
                    if(bad == false) {
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
//                    tgAccount.login{ loadAccounts(applicationManager, tgAccounts) }
                    tgAccount.getMe(object : GetMeListener {
                        override fun gotUser(user: User) {
                            Toast.makeText(context, "Вход выполнен!", Toast.LENGTH_SHORT).show()
//                            closeLoginWindow()
                            tgAccount.startAccount()
//                            if (howToRefresh != null) howToRefresh.run()
                        }

                        override fun error(error: Throwable) {
//                            saveButton.setEnabled(true)
//                            saveButton.setText("Сохранить")
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

//private fun checkAccount(applicationManager: ApplicationManager){
//
//}

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
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? MainActivity // Cast to MainActivity

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            /*.clickable {
                activity?.openAccountTab(account)  // Call openAccountTab if activity is MainActivity
            }*/
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(/*account.userPhotoUrl ?: */R.drawable.ic_ban) // Используйте ic_ban как placeholder
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
                Text(text = account.toString(), modifier = Modifier.weight(1f))

                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Account Menu")
                }
            }

            Text(text = "Status: ${account.state}")
            Text(text = "Messages Received: ${account.messageProcessor.messagesReceivedCounter}")
            Text(text = "Messages Sent: ${account.messageProcessor.messagesSentCounter}")
            Text(text = "API Counter: ${account.apiCounter}")
            Text(text = "API Errors: ${account.errorCounter}")
            Text(text = "Reply Instruction: Выключено")
            Text(text = "Chats Enabled: Включено")
            Text(text = "Broadcast Status: Выключено")
        }
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
                Text(text = "Для работы с Telegram требуется создать аккаунт бота. " +
                        "Чтобы это сделать, найди в телеграме бота @BotFather и следуй инструкции. " +
                        "Ты получишь от него токен. Его и надо ввести сюда, чтобы бот заработал.",
                     style = MaterialTheme.typography.bodySmall)

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