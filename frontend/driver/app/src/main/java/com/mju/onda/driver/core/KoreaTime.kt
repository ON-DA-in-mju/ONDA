package com.mju.onda.driver.core

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object KoreaTime {
    val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val hm = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTime = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

    fun nowTime(): LocalTime = LocalTime.now(zone)

    fun nowHm(): String = nowTime().format(hm)

    fun nowDateTime(): String = LocalDateTime.now(zone).format(dateTime)
}
