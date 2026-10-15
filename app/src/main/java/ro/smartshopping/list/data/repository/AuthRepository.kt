package ro.smartshopping.list.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ro.smartshopping.list.data.local.dao.UserDao
import ro.smartshopping.list.data.local.entity.UserEntity

data class AuthUser(
    val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null,
    val isGuest: Boolean = false
)

interface AuthRepository {
    val currentUserFlow: Flow<AuthUser?>
    suspend fun getCurrentUser(): AuthUser?
    suspend fun signInAsGuest(): AuthUser
    suspend fun signInWithGoogleCredential(idToken: String, email: String, displayName: String, avatarUrl: String?): AuthUser
    suspend fun signOut()
}

class AuthRepositoryImpl(
    private val userDao: UserDao
) : AuthRepository {

    private val _currentUserState = MutableStateFlow<AuthUser?>(
        AuthUser(
            id = "guest_user",
            displayName = "Utilizator Local",
            email = "offline@smartshopping.local",
            avatarUrl = null,
            isGuest = true
        )
    )
    override val currentUserFlow: Flow<AuthUser?> = _currentUserState.asStateFlow()

    override suspend fun getCurrentUser(): AuthUser? = _currentUserState.value

    override suspend fun signInAsGuest(): AuthUser {
        val guest = AuthUser("guest_user", "Utilizator Local", "offline@smartshopping.local", null, true)
        _currentUserState.value = guest
        return guest
    }

    override suspend fun signInWithGoogleCredential(
        idToken: String,
        email: String,
        displayName: String,
        avatarUrl: String?
    ): AuthUser {
        val userEntity = UserEntity(
            id = email,
            displayName = displayName,
            email = email,
            avatarUrl = avatarUrl,
            createdAt = System.currentTimeMillis()
        )
        userDao.insertUser(userEntity)
        val authUser = AuthUser(email, displayName, email, avatarUrl, false)
        _currentUserState.value = authUser
        return authUser
    }

    override suspend fun signOut() {
        userDao.clearUser()
        signInAsGuest()
    }
}
