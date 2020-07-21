package ru.skillbranch.kotlinexemple

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName:String,
        email:String,
        password:String
    ):User{
        val user = User.makeUser(fullName, email = email, password = password)
        if(map.containsKey(user.login)) throw IllegalArgumentException("A user with this email already exists")
        map[user.login] = user
        return user
//        return User.makeUser(fullName, email = email, password = password)
//            .also { user->
//                map[user.login] = user }
    }

    fun registerUserByPhone(
        fullName:String,
        phone:String
    ):User{
        return User.makeUser(fullName, phone=phone)
            .also { user->
                if(map.containsKey(user.login)) throw IllegalArgumentException("A user with this email already exists")
                map[user.login] = user }
    }

    fun loginUser(login:String, password:String):String?{
        var finalLogin = login
        println("trying to login $login")
        map.forEach { (key, value) ->
           println("stored login $key")
        }

        val checkLoginToPhone = login.replace("[^+\\d]".toRegex(),"")
        if(checkLoginToPhone .matches("^\\+\\d{11}\$".toRegex())){
            finalLogin = checkLoginToPhone
        }
        return map[finalLogin.trim()]?.run{
            if(checkPassword(password)) {
                println("login is correct getting info")
                this.userInfo
            }
            else {
                println("login is not correct")
                return null
            }
        }
    }

    fun requestAccessCode(login: String) : Unit{

        var finalLogin = login
        val checkLoginToPhone = login.replace("[^+\\d]".toRegex(),"")
        if(checkLoginToPhone .matches("^\\+\\d{11}\$".toRegex())){
            finalLogin = checkLoginToPhone
        }

        map.forEach { (key, value) ->
            println("stored login ${value.userInfo}")
        }
        if (map.containsKey(finalLogin.trim())) {
            val user = map[finalLogin.trim()]!!.updateAccessCode()
            map[finalLogin.trim()] = user
        }
        map.forEach { (key, value) ->
            println("stored login ${value.userInfo}")
        }
    }


    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder(){
        map.clear()
    }
}