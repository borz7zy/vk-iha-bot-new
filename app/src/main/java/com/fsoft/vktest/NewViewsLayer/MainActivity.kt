package com.fsoft.vktest.NewViewsLayer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fsoft.vktest.ApplicationManager
import com.fsoft.vktest.BotApplication
import com.fsoft.vktest.BotService
import com.fsoft.vktest.NewViewsLayer.MessagesFragment.MessagesScreen
import com.fsoft.vktest.Utils.F

class MainActivity : AppCompatActivity() {
    private companion object {
        private const val TAG = "MainActivity"
    }

    private var isServiceRunning = mutableStateOf(false)
    private lateinit var applicationManager: ApplicationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        applicationManager = ApplicationManager.getInstance() ?: ApplicationManager(applicationContext)

        // Проверка, запущен ли сервис. Если нет — запуск.
        Log.d(TAG, "Starting service...")
        val intent = Intent(applicationContext, BotService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent) // Использование startForegroundService для API 26 и выше
        } else {
            startService(intent)
        }

        // Get instance of ApplicationManager and pass the context
        Thread { //while (ApplicationManager.getInstance() == null) {
            while (BotApplication.getInstance().applicationManager == null) {
                F.sleep(10)
            }
            // Pass context to ApplicationManager
            ApplicationManager.getInstance().context = applicationContext

            while (ApplicationManager.getInstance().messageHistory == null) {
                F.sleep(10)
            }
        }.start()

        setContent {
            MainScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBotService()
    }

    private fun stopBotService() {
        if (!isServiceRunning.value) return

        Log.d(TAG, "Stopping service...")
        val intent = Intent(this, BotService::class.java)
        stopService(intent)
        isServiceRunning.value = false
    }
}


sealed class BottomNavItem(val route: String, val icon: ImageVector, val title: String) {
    object Messages : BottomNavItem("messages", Icons.Filled.Email, "Сообщения")
    object Accounts : BottomNavItem("accounts", Icons.Filled.Person, "Аккаунты")
    object Logs : BottomNavItem("logs", Icons.AutoMirrored.Filled.List, "Логи")
    object Settings : BottomNavItem("settings", Icons.Filled.Settings, "Настройки")
    object Training : BottomNavItem("training", Icons.Filled.Home, "Обучение")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavigationGraph(navController = navController)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Messages,
        BottomNavItem.Accounts,
        BottomNavItem.Logs,
        BottomNavItem.Settings,
        BottomNavItem.Training
    )

    NavigationBar {  // Changed BottomNavigation to NavigationBar
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem( // Changed BottomNavigationItem to NavigationBarItem
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = {
                    if (currentRoute == item.route) {
                        Text(text = item.title)
                    }
                },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = BottomNavItem.Messages.route) {
        composable(BottomNavItem.Messages.route) {
            MessagesScreen()
        }
        composable(BottomNavItem.Accounts.route) {
            AccountsScreenCompose(
                ApplicationManager.getInstance()
            )
        }
        composable(BottomNavItem.Logs.route) {
            LogsScreen()
        }
        composable(BottomNavItem.Settings.route) {
            SettingsScreen()
        }
        composable(BottomNavItem.Training.route) {
            TrainingScreen()
        }
    }
}


//@Composable
//fun MessagesScreen() {
//    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
//        Text(text = "Сообщения")
//    }
//}

//@Composable
//fun AccountsScreen() {
//    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
//        Text(text = "Аккаунты")
//        FloatingActionButton(
//            onClick = { /*TODO*/ },
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .padding(16.dp)
//        ) {
//            Icon(Icons.Filled.Add, contentDescription = "Добавить аккаунт")
//        }
//    }
//}

@Composable
fun LogsScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(text = "Логи")
    }
}

@Composable
fun SettingsScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(text = "Настройки")
    }
}

@Composable
fun TrainingScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(text = "Обучение")
    }
}