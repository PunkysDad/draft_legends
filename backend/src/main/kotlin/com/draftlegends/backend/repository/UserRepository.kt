package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Int> {
    fun findByGoogleUid(googleUid: String): User?
    fun findByAppleUid(appleUid: String): User?
}
