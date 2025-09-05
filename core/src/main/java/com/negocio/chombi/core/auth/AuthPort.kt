package com.negocio.chombi.core.auth

interface AuthPort {
    suspend fun currentUser(): UserProfile?
    suspend fun signIn(email: String, password: String): UserProfile
    suspend fun signOut()
}