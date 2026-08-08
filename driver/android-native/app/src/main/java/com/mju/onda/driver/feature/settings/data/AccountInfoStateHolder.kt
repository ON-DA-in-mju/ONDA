package com.mju.onda.driver.feature.settings.data



import android.content.SharedPreferences

import com.mju.onda.driver.core.UserScopedPrefs

import com.mju.onda.driver.data.mock.MockUsers



/**

 * 계정 정보 Mock 런타임 상태. 계정별로 저장·복원.

 */

object AccountInfoStateHolder {

    private const val PREFS = "onda_account_info"

    private const val KEY_DRIVER_NAME = "driver_name"

    private const val KEY_DRIVER_ID = "driver_id"

    private const val KEY_ORG = "organization"

    private const val KEY_VEHICLE = "vehicle_name"

    private const val KEY_CONTACT = "contact_status"



    private var prefs: SharedPreferences? = null



    @Volatile

    private var current: AccountInfo = MockAccountInfo.info



    fun bindUser(userId: String) {

        prefs = UserScopedPrefs.get(PREFS)

        val savedId = prefs?.getString(KEY_DRIVER_ID, null)

        current = if (!savedId.isNullOrBlank()) {

            AccountInfo(

                driverName = prefs?.getString(KEY_DRIVER_NAME, null)

                    ?: MockAccountInfo.info.driverName,

                driverId = savedId,

                organization = prefs?.getString(KEY_ORG, null)

                    ?: MockAccountInfo.info.organization,

                vehicleName = prefs?.getString(KEY_VEHICLE, null)

                    ?: defaultVehicle(userId),

                contactStatus = prefs?.getString(KEY_CONTACT, null)

                    ?: MockAccountInfo.info.contactStatus,

            )

        } else {

            seedFromDriver(userId).also { persist(it) }

        }

    }



    fun unbindUser() {

        current = MockAccountInfo.info

        prefs = null

    }



    fun get(): AccountInfo = current



    fun update(info: AccountInfo) {

        current = info

        persist(info)

    }



    fun toProfile(): DriverProfile = DriverProfile(

        name = current.driverName,

        organization = current.organization,

    )



    fun clear() {

        current = MockAccountInfo.info

        prefs?.edit()?.clear()?.apply()

    }



    private fun seedFromDriver(userId: String): AccountInfo {

        val driver = MockUsers.drivers.find { it.id == userId }

        return AccountInfo(

            driverName = MockAccountInfo.formatDisplayName(driver?.name ?: "기사"),

            driverId = userId,

            organization = driver?.organization ?: MockAccountInfo.info.organization,

            vehicleName = defaultVehicle(userId),

            contactStatus = MockAccountInfo.info.contactStatus,

        )

    }



    private fun defaultVehicle(userId: String): String = when (userId) {

        "user02" -> "1호차"

        "user03" -> "3호차"

        "user04" -> "4호차"

        "user05" -> "5호차"

        else -> "2호차" // user01 및 기본

    }



    private fun persist(info: AccountInfo) {

        prefs?.edit()

            ?.putString(KEY_DRIVER_NAME, info.driverName)

            ?.putString(KEY_DRIVER_ID, info.driverId)

            ?.putString(KEY_ORG, info.organization)

            ?.putString(KEY_VEHICLE, info.vehicleName)

            ?.putString(KEY_CONTACT, info.contactStatus)

            ?.apply()

    }

}

