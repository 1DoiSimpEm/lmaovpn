package com.amobear.freevpn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.amobear.freevpn.data.local.dao.ServerDao
import com.amobear.freevpn.data.local.entity.ServerEntity

@Database(
    entities = [ServerEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VpnDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao

    companion object {
        const val DATABASE_NAME = "vpn_database"
    }
}

