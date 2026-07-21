package uz.cardlens

import android.app.Application
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uz.cardlens.core.data.CardLensDatabase
import uz.cardlens.core.data.CardLensRepository
import uz.cardlens.core.data.RoomCardLensRepository
import uz.cardlens.core.data.SupabaseRemoteDataSource
import uz.cardlens.core.data.SyncingCardLensRepository
import uz.cardlens.core.datastore.AppPreferences
import uz.cardlens.core.notifications.NotificationChannels
import uz.cardlens.core.notifications.ReminderScheduler
import uz.cardlens.core.ocr.MlKitOcrProcessor
import uz.cardlens.core.supabase.AuthRepository
import uz.cardlens.core.supabase.EdgeAiClient
import uz.cardlens.core.supabase.SupabaseAuthRepository
import uz.cardlens.core.supabase.SupabaseClientFactory
import uz.cardlens.core.supabase.SupabaseClientRef
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
    single {
        SupabaseClientRef(
            SupabaseClientFactory.create(
                url = BuildConfig.SUPABASE_URL,
                anonKey = BuildConfig.SUPABASE_ANON_KEY,
            )
        )
    }
    single { AppPreferences(androidContext()) }
    single { SupabaseAuthRepository(get()) } bind AuthRepository::class
    single { SupabaseRemoteDataSource(get(), get()) }
    singleOf(::RoomCardLensRepository)
    single<CardLensRepository> { SyncingCardLensRepository(get(), get()) }
    singleOf(::MlKitOcrProcessor)
    single { EdgeAiClient(BuildConfig.SUPABASE_FUNCTION_URL) }
    singleOf(::ReminderScheduler)
}
