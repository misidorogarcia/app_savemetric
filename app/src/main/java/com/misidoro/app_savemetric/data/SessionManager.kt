package com.misidoro.app_savemetric.data

object SessionManager {
    private var currentUser: User? = null

    fun saveUser(user: User) {
        currentUser = user
    }

    fun getUser(): User? = currentUser

    fun isLoggedIn(): Boolean = currentUser != null

    fun clear() {
        currentUser = null
    }

    /**
     * Mapea una LoginResponse a User, guarda en sesión y devuelve el User si es válido.
     */
    fun saveFromLoginResponse(resp: LoginResponse): User? {
        val apiUser = resp.user ?: return null
        val token = resp.token ?: return null
        val user = User(
            id = apiUser.id,
            name = apiUser.name,
            apellidos = apiUser.apellidos,
            email = apiUser.email,
            token = token,
            vip = resp.vip ?: false
        )
        saveUser(user)
        return user
    }
}

