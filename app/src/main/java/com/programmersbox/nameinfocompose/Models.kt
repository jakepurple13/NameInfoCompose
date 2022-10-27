package com.programmersbox.nameinfocompose

import com.programmersbox.nameinfocompose.ui.theme.FemaleColor
import com.programmersbox.nameinfocompose.ui.theme.MaleColor
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.util.*

class ApiService {
    private val client by lazy {
        HttpClient {
            install(Logging)
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        prettyPrint = true
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    }
                )
            }
        }
    }

    private val genderizeUrl = "https://api.genderize.io/"
    private val nationalizeUrl = "https://api.nationalize.io"
    private val agifyUrl = "https://api.agify.io"

    suspend fun getInfo(name: String, countryId: String = Locale.getDefault().country) = runCatching {
        val gender = getGenderInfo(name, countryId)
        val age = getAgeInfo(name, countryId)
        val nation = getNationalInfo(name, countryId)

        IfyInfo(
            name = age.name,
            age = age.age,
            gender = Gender(gender.gender, gender.probability * 100),
            nationality = nation.country
        )
    }

    suspend fun getGenderInfo(name: String, countryId: String = Locale.getDefault().country) =
        client.get("$genderizeUrl?name=$name&country_id=$countryId").body<GenderizeInfo>()

    suspend fun getAgeInfo(name: String, countryId: String = Locale.getDefault().country) =
        client.get("$agifyUrl?name=$name&country_id=$countryId").body<AgifyInfo>()

    suspend fun getNationalInfo(name: String, countryId: String = Locale.getDefault().country) =
        client.get("$nationalizeUrl?name=$name&country_id=$countryId").body<NationalizeInfo>()

}

@Serializable
data class Gender(val gender: String, val probability: Float) {
    val genderColor
        get() = when (gender) {
            "male" -> MaleColor
            "female" -> FemaleColor
            else -> null
        }

    val genderColorInverse
        get() = when (gender) {
            "male" -> FemaleColor
            "female" -> MaleColor
            else -> null
        }

    fun capitalGender() =
        gender.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@Serializable
data class IfyInfo(
    val name: String = "",
    val age: Int = 0,
    val gender: Gender? = Gender("", 0f),
    val nationality: List<Country> = emptyList()
)

@Serializable
data class AgifyInfo(val name: String, val age: Int, val count: Int)

@Serializable
data class GenderizeInfo(val name: String, val gender: String, val probability: Float, val count: Int)

@Serializable
data class Country(val country_id: String, val probability: Float) {
    val flagUrl get() = "https://www.countryflagsapi.com/png/$country_id"
    val countryName: String get() = Locale("", country_id).displayCountry
}

@Serializable
data class NationalizeInfo(val name: String, val country: List<Country>)