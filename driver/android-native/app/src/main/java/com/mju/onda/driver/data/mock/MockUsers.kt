package com.mju.onda.driver.data.mock



data class MockDriver(

    val id: String,

    val password: String,

    val name: String,

    val organization: String,

)



object MockUsers {

    val drivers: List<MockDriver> = listOf(

        MockDriver(

            id = "driver01",

            password = "1234",

            name = "김민수",

            organization = "명지 셔틀 운영팀",

        ),

        MockDriver(

            id = "driver02",

            password = "1234",

            name = "박지훈",

            organization = "명지 셔틀 운영팀",

        ),

        MockDriver(

            id = "2026001",

            password = "onda1234",

            name = "이서연",

            organization = "명지 셔틀 운영팀",

        ),

    )

}

