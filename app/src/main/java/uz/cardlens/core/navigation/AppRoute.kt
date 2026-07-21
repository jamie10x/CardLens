package uz.cardlens.core.navigation

enum class Tab {
    Home,
    Contacts,
    Scan,
    FollowUps,
    Settings,
}

sealed interface AppRoute {
    data object Onboarding : AppRoute
    data object Auth : AppRoute
    data object Main : AppRoute
    data class ContactProfile(val contactId: String) : AppRoute
    data object OcrReview : AppRoute
}
