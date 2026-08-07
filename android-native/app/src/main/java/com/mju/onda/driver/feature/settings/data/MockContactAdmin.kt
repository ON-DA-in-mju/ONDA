package com.mju.onda.driver.feature.settings.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Place
import androidx.compose.ui.graphics.vector.ImageVector

data class ContactInfoRow(
    val id: String,
    val label: String,
    val value: String,
    val icon: ImageVector,
    val underlined: Boolean = false,
)

data class InquiryTypeItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

object MockContactAdmin {
    const val SCREEN_TITLE = "관리자 문의"
    const val INQUIRY_SECTION_TITLE = "문의 유형"
    const val INFO_BANNER = "긴급 상황은 전화 문의를 우선 이용해 주세요."
    const val CALL_ADMIN_LABEL = "관리자 문의"
    const val EMAIL_INQUIRY_LABEL = "이메일 문의"

    const val PHONE_NUMBER = "031-123-4567"
    const val EMAIL_ADDRESS = "onad@mju.ac.kr"
    const val PHONE_URI = "tel:0311234567"
    const val EMAIL_URI = "mailto:onad@mju.ac.kr"

    const val INQUIRY_TYPE_TOAST_PREFIX = "문의 유형: "

    val contactRows: List<ContactInfoRow> = listOf(
        ContactInfoRow(
            id = "admin_phone",
            label = "운수회사 관리자",
            value = PHONE_NUMBER,
            icon = Icons.Outlined.Phone,
        ),
        ContactInfoRow(
            id = "school_dept",
            label = "학교 담당 부서",
            value = "학생지원팀",
            icon = Icons.Outlined.Apartment,
        ),
        ContactInfoRow(
            id = "tech_email",
            label = "기술 오류 문의",
            value = EMAIL_ADDRESS,
            icon = Icons.Outlined.Email,
            underlined = true,
        ),
        ContactInfoRow(
            id = "hours",
            label = "운영시간",
            value = "평일 09:00 - 18:00",
            icon = Icons.Outlined.AccessTime,
        ),
    )

    val inquiryTypes: List<InquiryTypeItem> = listOf(
        InquiryTypeItem(id = "account", label = "계정 문제", icon = Icons.Outlined.Person),
        InquiryTypeItem(id = "assignment", label = "운행 배정 문제", icon = Icons.Outlined.CalendarMonth),
        InquiryTypeItem(id = "vehicle", label = "차량 정보 문제", icon = Icons.Outlined.DirectionsBus),
        InquiryTypeItem(id = "gps", label = "GPS 문제", icon = Icons.Outlined.Place),
        InquiryTypeItem(id = "app", label = "앱 사용 문제", icon = Icons.Outlined.PhoneAndroid),
    )
}
