package com.fsoft.vktest.NewViewsLayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.fsoft.vktest.ApplicationManager
import com.fsoft.vktest.Communication.Account.Telegram.TgAccount
import com.fsoft.vktest.R

@Composable
fun AccountsScreenCompose(applicationManager: ApplicationManager) {
    val tgAccounts = remember { mutableStateListOf<TgAccount>() }
    val context = LocalContext.current
    val activity = context as? MainActivity

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
                val tgAccount = TgAccount(applicationManager, "tg" + System.currentTimeMillis())
                applicationManager.communicator.addAccount(tgAccount)
                tgAccount.login {
                    loadAccounts(applicationManager, tgAccounts) // Refresh accounts after login
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Добавить аккаунт")
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