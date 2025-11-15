package pt.ipp.estg.readytomove.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pt.ipp.estg.readytomove.data.local.dao.TripDao
import pt.ipp.estg.readytomove.data.local.dao.UserDao
import pt.ipp.estg.readytomove.data.model.local.TripCache
import pt.ipp.estg.readytomove.data.model.local.UserCache

@Database(entities = [UserCache::class, TripCache::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ready_to_move_cache_db"
                ).build().also { INSTANCE = it }
            }
    }
}