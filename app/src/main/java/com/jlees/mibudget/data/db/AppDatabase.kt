package com.jlees.mibudget.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun dateToLong(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun longToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
}

@Database(
    entities = [
        CategoryEntity::class, PurchaseEntity::class, SubscriptionEntity::class, PaymentMethodEntity::class,
        SubscriptionPeriodEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun subscriptionPeriodDao(): SubscriptionPeriodDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "mi-budget.db")
                // mi. Budget started at version 7: there's nothing older to migrate. Add migrations from 7 on (the
                // exported schemas in app/schemas are what migration tests start from).
                .build()
    }
}
