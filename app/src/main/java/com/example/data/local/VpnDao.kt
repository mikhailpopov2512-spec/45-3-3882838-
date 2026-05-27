package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnDao {
    @Query("SELECT * FROM vpn_profiles ORDER BY id DESC")
    fun getAllProfiles(): Flow<List<VpnProfileEntity>>

    @Query("SELECT * FROM vpn_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Int): VpnProfileEntity?

    @Query("SELECT * FROM vpn_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): VpnProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VpnProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<VpnProfileEntity>)

    @Update
    suspend fun updateProfile(profile: VpnProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: VpnProfileEntity)

    @Query("DELETE FROM vpn_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)

    @Query("DELETE FROM vpn_profiles WHERE subUrl = :subUrl")
    suspend fun deleteProfilesBySubUrl(subUrl: String)

    @Query("UPDATE vpn_profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()

    @Transaction
    suspend fun setActiveProfile(id: Int) {
        deactivateAllProfiles()
        updateProfileActivityState(id, true)
    }

    @Query("UPDATE vpn_profiles SET isActive = :isActive WHERE id = :id")
    suspend fun updateProfileActivityState(id: Int, isActive: Boolean)

    @Query("UPDATE vpn_profiles SET ping = :ping WHERE id = :id")
    suspend fun updateProfilePing(id: Int, ping: Int)

    // Subscriptions
    @Query("SELECT * FROM subscriptions ORDER BY id DESC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: SubscriptionEntity): Long

    @Delete
    suspend fun deleteSubscription(sub: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: Int)
}
