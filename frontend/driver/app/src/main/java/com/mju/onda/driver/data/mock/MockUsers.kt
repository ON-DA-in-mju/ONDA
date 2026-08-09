package com.mju.onda.driver.data.mock

data class MockDriver(
    val id: String,
    val password: String,
    val name: String,
    val organization: String,
)

/** 관리자 웹 「사용자 관리」의 user01~user05와 동일한 기사 계정(목). */
object MockUsers {
    val drivers: List<MockDriver> = listOf(
        MockDriver(
            id = "user01",
            password = "1234",
            name = "박사용",
            organization = "명지 셔틀 운영팀",
        ),
        MockDriver(
            id = "user02",
            password = "1234",
            name = "최사용",
            organization = "명지 셔틀 운영팀",
        ),
        MockDriver(
            id = "user03",
            password = "1234",
            name = "정사용",
            organization = "명지 셔틀 운영팀",
        ),
        MockDriver(
            id = "user04",
            password = "1234",
            name = "한사용",
            organization = "명지 셔틀 운영팀",
        ),
        MockDriver(
            id = "user05",
            password = "1234",
            name = "임사용",
            organization = "명지 셔틀 운영팀",
        ),
    )
}
