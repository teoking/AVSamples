package com.teoking.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import rebus.permissionutils.PermissionManager

abstract class PermissionFragment(private val perms: Array<String>) : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
