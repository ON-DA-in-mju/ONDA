package com.mju.onda.driver.data.mock

/** 로그인 성공 시 화면에 넘기는 기사 프로필. */
data class MockDriver(
    val id: String,
    val password: String = "",
    val name: String,
    val organization: String,
)
