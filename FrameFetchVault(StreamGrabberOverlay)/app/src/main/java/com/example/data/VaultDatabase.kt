package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class BorderState {
    IDLE,
    SCANNING,
    VIDEO_DETECTED,
    DOWNLOADING
}

data class QualityVariant(
    val id: String,
    val label: String,           // e.g., "1080p Full HD", "720p HD", "480p SD", "Source Stream"
    val resolution: String,      // e.g., "1920x1080"
    val format: String,          // e.g., "MP4", "WEBM", "HLS"
    val mimeType: String,        // e.g., "video/mp4"
    val streamUrl: String,
    val sizeBytes: Long,
    val bitrateLabel: String
)

data class DetectedVideoStream(
    val id: String,
    val title: String,
    val pageUrl: String,
    val sourceDomain: String,
    val primaryStreamUrl: String,
    val format: String,
    val mimeType: String,
    val resolutionLabel: String,
    val width: Int,
    val height: Int,
    val durationSeconds: Int,
    val contentLengthBytes: Long,
    val qualityVariants: List<QualityVariant>,
    val detectedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_videos")
data class VaultVideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val sourceDomain: String,
    val streamUrl: String,
    val localFilePath: String,
    val format: String,
    val mimeType: String,
    val resolution: String,
    val durationSeconds: Int,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String, // "DOWNLOADING", "COMPLETED", "FAILED"
    val createdAt: Long = System.currentTimeMillis()
)

data class VaultStorageStats(
    val vaultDirectoryPath: String = "",
    val vaultUsedBytes: Long = 0L,
    val vaultFileCount: Int = 0,
    val deviceFreeBytes: Long = 0L,
    val deviceTotalBytes: Long = 1L,
    val isIsolatedNoMediaActive: Boolean = true
)

@Dao
interface VaultVideoDao {
    @Query("SELECT * FROM vault_videos ORDER BY createdAt DESC")
    fun observeAllVideos(): Flow<List<VaultVideoEntity>>

    @Query("SELECT * FROM vault_videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: Long): VaultVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VaultVideoEntity): Long

    @Update
    suspend fun updateVideo(video: VaultVideoEntity)

    @Query("UPDATE vault_videos SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, status = :status WHERE id = :id")
    suspend fun updateProgress(id: Long, downloadedBytes: Long, totalBytes: Long, status: String)

    @Query("DELETE FROM vault_videos WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [VaultVideoEntity::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultVideoDao(): VaultVideoDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "framefetch_private_vault.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
