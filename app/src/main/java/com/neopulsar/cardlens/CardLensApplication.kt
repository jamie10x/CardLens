package com.neopulsar.cardlens

import android.app.Application
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import com.neopulsar.cardlens.core.data.CardLensDatabase
import com.neopulsar.cardlens.core.data.CardLensRepository
import com.neopulsar.cardlens.core.data.RoomCardLensRepository
import com.neopulsar.cardlens.core.datastore.AppPreferences
import com.neopulsar.cardlens.core.notifications.NotificationChannels
import com.neopulsar.cardlens.core.notifications.ReminderScheduler
import com.neopulsar.cardlens.core.ocr.MlKitOcrProcessor
import com.neopulsar.cardlens.di.viewModelModule

class CardLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
        startKoin {
            androidContext(this@CardLensApplication)
            modules(cardLensModule, viewModelModule)
        }
    }
}

private val cardLensModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            CardLensDatabase::class.java,
            "cardlens.db",
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
    single { get<CardLensDatabase>().dao() }
    single { AppPreferences(androidContext()) }
    single<CardLensRepository> { RoomCardLensRepository(get()) }
    singleOf(::MlKitOcrProcessor)
    singleOf(::ReminderScheduler)
}
