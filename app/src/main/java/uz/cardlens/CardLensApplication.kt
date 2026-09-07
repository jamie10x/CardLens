package uz.cardlens

import android.app.Application
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import uz.cardlens.core.data.CardLensDatabase
import uz.cardlens.core.data.CardLensRepository
import uz.cardlens.core.data.RoomCardLensRepository
import uz.cardlens.core.datastore.AppPreferences
import uz.cardlens.core.notifications.NotificationChannels
import uz.cardlens.core.notifications.ReminderScheduler
import uz.cardlens.core.ocr.MlKitOcrProcessor
import uz.cardlens.di.viewModelModule

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
