package ru.skillbranch.kotlinexemple

import androidx.annotation.VisibleForTesting
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.IllegalArgumentException

class User private constructor(
    private val firstName:String,
    private val lastName:String?,
    email:String? = null,
    rawPhone:String?=null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName:String
        get() = listOfNotNull(firstName,lastName)
            .joinToString(" ")
            .capitalize()

    private val initials:String
        get() = listOfNotNull(firstName,lastName)
            .map{it.first().toUpperCase()}
            .joinToString(" ")

    private var phone:String?=null
        set(value){
            val checkValue = value?.replace("[^+\\d]".toRegex(),"")
            if(checkValue !=null && !checkValue .matches("^\\+\\d{11}\$".toRegex())){
                throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
            }
            field = checkValue

        }

    private var _login:String? = null
    var login:String
        set(value){
            _login = value?.toLowerCase()
        }
        get() = _login!!

    private val salt:String by lazy {
        ByteArray(16).also{SecureRandom().nextBytes(it)}.toString()
    }
    private lateinit var passwordHash:String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode:String?=null

    //for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password:String
    ):this(firstName,lastName,email=email, meta = mapOf("auth" to "password")){
        println("Secondary mail constructor")
        passwordHash = encrypt(password)
    }

    //for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ):this(firstName,lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")){
        println("Secondary phone constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        println("Phone passwordhash is $passwordHash")
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }


    init {
        println("First init block, primary constructor was called")

        check(!firstName.isBlank()) {"Firstname must be not blank"}
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) {"Email or phone must be not blank"}

        phone = rawPhone


        login = email?: phone!!

        userInfo = """
      firstName: $firstName
      lastName: $lastName
      login: $login
      fullName: $fullName
      initials: $initials
      email: $email
      phone: $phone
      meta: $meta
    """.trimIndent()
    }


    fun checkPassword(pass:String):Boolean {
        println("pasword: $pass, hash: ${encrypt(pass)} stored hash: $passwordHash")
        return encrypt(pass) == passwordHash
    }

    fun changePassword(oldPass:String, newPass:String){
        if(checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not match the current password")
    }


    private fun encrypt(password: String): String = salt.plus(password).md5()

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKOPQRSTUVWXYZabcdefghijkopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also {index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    fun updateAccessCode():User {
        val possible = "ABCDEFGHIJKOPQRSTUVWXYZabcdefghijkopqrstuvwxyz0123456789"

         val code = StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also {index ->
                    append(possible[index])
                }
            }
        }.toString()

        this.accessCode = code
        this.passwordHash = encrypt(code)
        return this
    }

    private fun sendAccessCodeToUser(phone:String, code:String){
        println("...sending access code: $code on $phone")

    }

    private fun String.md5():String{
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1,digest).toString(16)
        return hexString.padStart(32,'0')
    }

    companion object Factory{
        fun makeUser(
            fullName:String,
            email:String? = null,
            password: String? = null,
            phone:String? = null
        ):User{
            val (firstName, lastName) = fullName.fullNameToPair()

            return when{
                !phone.isNullOrBlank() ->User(firstName, lastName,phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() ->User(firstName,lastName,email,password)
                else -> throw IllegalArgumentException("Email or phone must be not null or blank")
            }
        }

        private fun String.fullNameToPair(): Pair<String, String?>{
            return this.split(" ")
                .filter{it.isNotBlank()}
                .run {
                    when(size){
                        1->first() to null
                        2-> first() to last()
                        else ->throw IllegalArgumentException("FullName must contain only first name and last name, current split result ${this@fullNameToPair}")
                    }
                }
        }
    }
}