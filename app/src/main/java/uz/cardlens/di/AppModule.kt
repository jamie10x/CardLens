package uz.cardlens.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import uz.cardlens.feature.contacts.presentation.ContactProfileViewModel
import uz.cardlens.feature.contacts.presentation.ContactsViewModel
import uz.cardlens.feature.followups.presentation.FollowUpsViewModel
import uz.cardlens.feature.home.presentation.HomeViewModel
import uz.cardlens.feature.scan.presentation.ScanViewModel
import uz.cardlens.feature.settings.presentation.SettingsViewModel

val viewModelModule = module {
    viewModel { HomeViewModel(get()) }
    viewModel { ContactsViewModel(get()) }
    viewModel { ScanViewModel(get(), get(), get()) }
    viewModel { FollowUpsViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { params -> ContactProfileViewModel(params.get(), get()) }
}
