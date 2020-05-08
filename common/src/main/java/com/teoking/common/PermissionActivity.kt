package com.teoking.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import rebus.permissionutils.PermissionManager

open class PermissionActivity(val perms: Array<String>) : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askForPermission(this, perms)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.handleResult(this, requestCode, permissions, grantResults)
    }
}