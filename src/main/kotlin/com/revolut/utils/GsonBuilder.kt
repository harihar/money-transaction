package com.revolut.utils

import com.google.gson.*
import org.javamoney.moneta.Money
import java.lang.reflect.Type

object GsonUtil {
    val gson: Gson = GsonBuilder().apply {
        registerTypeAdapter(Money::class.java, MoneyDeserializer())
        registerTypeAdapter(Money::class.java, MoneySerializer())
        setPrettyPrinting()
    }.create()
}

class MoneySerializer : JsonSerializer<Money> {
    override fun serialize(src: Money, member: Type, context: JsonSerializationContext): JsonElement? {
        return JsonPrimitive(src.toString())
    }
}

class MoneyDeserializer : JsonDeserializer<Money> {
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Money {
        if (!json.isJsonPrimitive)
            throw UnsupportedOperationException("invalid money format")
        return Money.parse((json as JsonPrimitive).asString)
    }
}
