package uz.cardlens

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import uz.cardlens.core.datastore.AppPreferences
import java.util.Locale
import uz.cardlens.core.navigation.Tab
import uz.cardlens.core.ui.components.CardLensBottomBar
import uz.cardlens.feature.auth.presentation.AuthRoute
import uz.cardlens.feature.contacts.presentation.ContactProfileRoute
import uz.cardlens.feature.contacts.presentation.ContactsRoute
import uz.cardlens.feature.followups.presentation.FollowUpsRoute
import uz.cardlens.feature.home.presentation.HomeRoute
import uz.cardlens.feature.onboarding.presentation.OnboardingRoute
import uz.cardlens.feature.scan.presentation.ScanRoute
import uz.cardlens.feature.settings.presentation.SettingsRoute
import uz.cardlens.ui.theme.CardLensTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context?) {
        val prefs = newBase?.let { AppPreferences(it) }
        val langCode = prefs?.language ?: "en"
        val locale = Locale.forLanguageTag(langCode)
        Locale.setDefault(locale)
        val config = Configuration(newBase?.resources?.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase?.createConfigurationContext(config))
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

@Composable
private fun CardLensApp() {
    val navController = rememberNavController()
    val appPrefs: AppPreferences = koinInject()
    val authViewModel: uz.cardlens.feature.auth.presentation.AuthViewModel = koinViewModel()
    val authState by authViewModel.state.collectAsState()

    var startRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startRoute = if (!appPrefs.isOnboardingCompleted) {
            "onboarding"
        } else {
            "splash"
        }
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
                    CircularProgressIndicator()
                    Text(
                        stringResource(R.string.loading),
                        modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SplashRedirect(
                authState = authState,
                onDestination = { dest ->
                    navController.navigate(dest) {
                        popUpTo("splash") { inclusive = true }
                    }
                },
            )
        }
        composable("onboarding") {
            OnboardingRoute(
                onCompleted = {
                    appPrefs.isOnboardingCompleted = true
                    navController.navigate("splash") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
            )
        }
        composable("auth") {
            AuthRoute(
                onSignedIn = {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
            )
        }
        composable("main") {
            MainScaffold(
                onNavigateToContact = { id -> navController.navigate("contact/$id") },
                onSignedOut = {
                    navController.navigate("auth") {
                        popUpTo("main") { inclusive = true }
                    }
                },
            )
        }
        composable("contact/{contactId}") { backStackEntry ->
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
    onSignedOut: () -> Unit,
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
                        onSignedOut = onSignedOut,
                        onToggleLanguage = { (context as? ComponentActivity)?.recreate() },
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashRedirect(
    authState: uz.cardlens.feature.auth.presentation.AuthUiState,
    onDestination: (String) -> Unit,
) {
    LaunchedEffect(authState.sessionRestored, authState.isAuthConfigured, authState.email) {
        if (!authState.sessionRestored) return@LaunchedEffect
        when {
            !authState.isAuthConfigured -> onDestination("auth")
            authState.email.isNotEmpty() -> onDestination("main")
            else -> onDestination("auth")
        }
    }
}
