// app/src/main/java/com/example/voidshop/data/local/dao/CartDao.kt
package com.example.voidshop.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.voidshop.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Query("SELECT * FROM cart_items ORDER BY id DESC")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Query("""
        SELECT * FROM cart_items
        WHERE productId = :productId
          AND ( (size IS NULL AND :size IS NULL) OR size = :size )
        LIMIT 1
    """)
    suspend fun getByKey(productId: String, size: String?): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CartItemEntity): Long

    @Update
    suspend fun update(item: CartItemEntity)

    @Delete
    suspend fun delete(item: CartItemEntity)

    @Query("DELETE FROM cart_items")
    suspend fun clear()
}
