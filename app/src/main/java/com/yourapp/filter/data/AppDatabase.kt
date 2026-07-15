package com.yourapp.filter.data

import android.content.Context
import androidx.room.*
import com.yourapp.filter.scheduler.ScheduleRule

@Entity(tableName = "blocked_domains")
data class BlockedDomain(@PrimaryKey val domain: String)

@Entity(tableName = "blocked_apps")
data class BlockedApp(@PrimaryKey val packageName: String)

@Dao
interface BlockedDomainDao {
    @Query("SELECT EXISTS(SELECT 1 FROM blocked_domains WHERE domain = :domain)")
    suspend fun isBlocked(domain: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(domain: BlockedDomain)

    @Delete
    suspend fun delete(domain: BlockedDomain)

    @Query("SELECT * FROM blocked_domains")
    suspend fun getAll(): List<BlockedDomain>
}

@Dao
interface BlockedAppDao {
    @Query("SELECT EXISTS(SELECT 1 FROM blocked_apps WHERE packageName = :pkg)")
    suspend fun isBlocked(pkg: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: BlockedApp)

    @Delete
    suspend fun delete(app: BlockedApp)

    @Query("SELECT * FROM blocked_apps")
    suspend fun getAll(): List<BlockedApp>
}

@Dao
interface ScheduleRuleDao {
    @Query("SELECT * FROM schedule_rules WHERE targetPackageOrDomain = :target")
    suspend fun getRulesFor(target: String): List<ScheduleRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: ScheduleRule)

    @Delete
    suspend fun delete(rule: ScheduleRule)

    @Query("SELECT * FROM schedule_rules")
    suspend fun getAll(): List<ScheduleRule>
}

@Database(
    entities = [BlockedDomain::class, BlockedApp::class, ScheduleRule::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedDomainDao(): BlockedDomainDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "filter_app_db"
                ).build().also { INSTANCE = it }
            }
    }
}
