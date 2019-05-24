package steffan.springmqdemoapp.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.full.companionObject

fun getLogger(forClass: Class<*>): Logger =
        LoggerFactory.getLogger(forClass)

interface Logging
inline fun <reified T : Logging> T.logger(): Logger = getLogger(getClassForLogging(T::class.java))

inline fun <T : Any> getClassForLogging(javaClass: Class<T>): Class<*> {
    return javaClass.enclosingClass?.takeIf {
        it.kotlin.companionObject?.java == javaClass
    } ?: javaClass
}