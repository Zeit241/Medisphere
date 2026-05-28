package com.example.kursovaya.activity

import android.os.Bundle
import com.example.kursovaya.R
import com.example.kursovaya.fragment.ProfileFragment

class UserProfileActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .commit()
        }
    }
}
