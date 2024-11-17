package com.funny.trans.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.funny.trans.login.ui.LoginNavigation
import com.funny.trans.login.ui.supportBiometric
import com.funny.translation.AppConfig
import com.funny.translation.BaseActivity
import com.funny.translation.helper.Log
import com.funny.translation.helper.biomertic.BiometricUtils
import com.funny.translation.ui.App

actual class LoginActivity : BaseActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (supportBiometric) {
            BiometricUtils.init()
        }

        setContent {
            App {
                LoginNavigation(onLoginSuccess = {
                    Log.d(TAG, "登录成功: 用户: $it")
                    if(it.isValid()) AppConfig.login(it, updateVipFeatures = true)
                    setResult(RESULT_OK, Intent())
                    finish()
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}