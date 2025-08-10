package com.example.voidshop.data.local

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.voidshop.data.local.dao.CartDao
import com.example.voidshop.data.local.dao.ProductDao
import com.example.voidshop.data.local.entity.CartItemEntity
import com.example.voidshop.data.local.entity.ProductEntity
import java.util.concurrent.Executors

@Database(
    entities = [
        CartItemEntity::class,
        ProductEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voidshop.db"
                )
                // En desarrollo, si cambias el schema, destruye y recrea (evita crash por migraciones)
                // Quita esta línea cuando agregues migraciones reales.
                builder.fallbackToDestructiveMigration()

                if (isDebug) {
                    builder.setQueryCallback(
                        { sql, args -> Log.d("RoomSQL", "SQL: $sql | args=$args") },
                        Executors.newSingleThreadExecutor()
                    )
                }

                builder.build().also { db ->
                    INSTANCE = db
                    if (isDebug) {
                        val dbPath = context.getDatabasePath("voidshop.db").absolutePath
                        Log.d("VoidShopDB", "DB path: $dbPath")
                    }
                }
            }
    }
}