package app.grapheneos.camera.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun getHumanReadableDate(
    dateString: String,
    inputFormat: String = "yyyyMMdd",
    outputFormat: String = "EEE, MMM dd"
) : String {
    val date = LocalDate.parse(
        dateString,
        DateTimeFormatter.ofPattern(inputFormat)
    )

    if (date == LocalDate.now()) {
        return "Today"
    }

    val dayDiff = LocalDate.now().toEpochDay() - date.toEpochDay()

    if (dayDiff == 1L) {
        return "Yesterday"
    }

    if (dayDiff < 7) {
        return date.dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercaseChar)
    }

    return date.format(DateTimeFormatter.ofPattern(outputFormat))
}