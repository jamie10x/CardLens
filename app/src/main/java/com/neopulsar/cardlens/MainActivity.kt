package com.neopulsar.cardlens

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.neopulsar.cardlens.core.datastore.AppPreferences
import com.neopulsar.cardlens.core.navigation.Tab
import java.util.Locale
import com.neopulsar.cardlens.core.ui.components.CardLensBottomBar
import com.neopulsar.cardlens.feature.contacts.presentation.ContactProfileRoute
import com.neopulsar.cardlens.feature.contacts.presentation.ContactsRoute
import com.neopulsar.cardlens.feature.followups.presentation.FollowUpsRoute
import com.neopulsar.cardlens.feature.home.presentation.HomeRoute
import com.neopulsar.cardlens.feature.onboarding.presentation.OnboardingRoute
import com.neopulsar.cardlens.feature.scan.presentation.ScanRoute
import com.neopulsar.cardlens.feature.settings.presentation.SettingsRoute
import com.neopulsar.cardlens.ui.theme.CardLensTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context?) {
        try {
            val prefs = newBase?.let { AppPreferences(it) }
            val langCode = prefs?.languageSync() ?: "en"
            val locale = Locale.forLanguageTag(langCode)
            Locale.setDefault(locale)
            val config = Configuration(newBase?.resources?.configuration)
            config.setLocale(locale)
            super.attachBaseContext(newBase?.createConfigurationContext(config))
            return
        } catch (_: Exception) {}
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splashScreen.setKeepOnScreenCondition { false }
        setContent {
            CardLensTheme {
                CardLensApp()
            }
        }
    }
}

sealed class AppRoute(val route: String) {
    data object Onboarding : AppRoute("onboarding")
    data object Main : AppRoute("main")
    data class Contact(val contactId: String) : AppRoute("contact/$contactId") {
        companion object {
            const val PATTERN = "contact/{contactId}"
        }
    }
}

@Composable
private fun CardLensApp() {
    val navController = rememberNavController()
    val appPrefs: AppPreferences = koinInject()
    val scope = rememberCoroutineScope()

    var startRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val completed = try { appPrefs.isOnboardingCompleted.first() } catch (_: Exception) { false }
        startRoute = if (completed) AppRoute.Main.route else AppRoute.Onboarding.route
    }

    val currentRoute = startRoute ?: "splash"

    NavHost(navController = navController, startDestination = currentRoute) {
        composable("splash") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Text(
                        stringResource(R.string.loading),
                        modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        composable(AppRoute.Onboarding.route) {
            OnboardingRoute(
                onCompleted = {
                    scope.launch {
                        appPrefs.setOnboardingCompleted(true)
                        navController.navigate(AppRoute.Main.route) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(AppRoute.Main.route) {
            MainScaffold(
                onNavigateToContact = { id -> navController.navigate(AppRoute.Contact(id).route) },
            )
        }
        composable(AppRoute.Contact.PATTERN) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: return@composable
            ContactProfileRoute(
                contactId = contactId,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    onNavigateToContact: (String) -> Unit,
) {
    var activeTab by remember { mutableStateOf(Tab.Home) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { },
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            CardLensBottomBar(
                activeTab = activeTab,
                onTabClick = { activeTab = it },
            )
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (activeTab) {
                Tab.Home -> HomeRoute(
                    onNavigateToScan = { activeTab = Tab.Scan },
                    onNavigateToContact = onNavigateToContact,
                    onNavigateToFollowUps = { activeTab = Tab.FollowUps },
                )
                Tab.Contacts -> ContactsRoute(
                    onNavigateToContact = onNavigateToContact,
                )
                Tab.Scan -> ScanRoute(
                    onOcrComplete = { activeTab = Tab.Home },
                    onNavigateToHome = { activeTab = Tab.Home },
                )
                Tab.FollowUps -> FollowUpsRoute()
                Tab.Settings -> {
                    val context = LocalContext.current
                    SettingsRoute(
                        onToggleLanguage = { (context as? ComponentActivity)?.recreate() },
                    )
                }
            }
        }
    }
}