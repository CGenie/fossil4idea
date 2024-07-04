package org.github.irengrig.fossil4idea.log

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 5:25 PM
 */
object DateUtil {
    private val ourFormats: MutableList<DateFormat> = ArrayList()

    init {
        ourFormats.add(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"))
        ourFormats.add(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'000Z'"))
        ourFormats.add(SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US))
        ourFormats.add(SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z (EE, d MMM yyyy)", Locale.getDefault()))
        ourFormats.add(SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss' 'ZZZZ' ('E', 'dd' 'MMM' 'yyyy')'"))
        ourFormats.add(SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss'Z'"))
        ourFormats.add(SimpleDateFormat("EEE' 'MMM' 'dd' 'HH:mm:ss' 'yyyy"))
        ourFormats.add(SimpleDateFormat("MM' 'dd'  'yyyy"))
        ourFormats.add(SimpleDateFormat("MM' 'dd'  'HH:mm"))
        ourFormats.add(SimpleDateFormat("MM' 'dd'  'HH:mm:ss"))
    }

    fun parseDate(date: String?): Date {
        for (format in ourFormats) {
            try {
                return format.parse(date)
            } catch (e: ParseException) {
                continue
            }
        }
        return Date(0)
    }
}