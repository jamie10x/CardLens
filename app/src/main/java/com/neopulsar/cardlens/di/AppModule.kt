package com.neopulsar.cardlens.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.neopulsar.cardlens.feature.contacts.presentation.ContactProfileViewModel
import com.neopulsar.cardlens.feature.contacts.presentation.ContactsViewModel
import com.neopulsar.cardlens.feature.followups.presentation.FollowUpsViewModel
import com.neopulsar.cardlens.feature.home.presentation.HomeViewModel
import com.neopulsar.cardlens.feature.scan.presentation.ScanViewModel
import com.neopulsar.cardlens.feature.settings.presentation.SettingsViewModel

val viewModelModule = module {
    viewModel { HomeViewModel(get()) }
    viewModel { ContactsViewModel(get()) }
    viewModel { ScanViewModel(get(), get(), get()) }
    viewModel { FollowUpsViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { params -> ContactProfileViewModel(params.get(), get()) }
}
