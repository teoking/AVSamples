package com.teoking.common

import android.app.Activity
import androidx.fragment.app.Fragment
import rebus.permissionutils.AskAgainCallback
import rebus.permissionutils.PermissionEnum
import rebus.permissionutils.PermissionManager

fun askForPermission(activity: Activity, perms: Array<String>) {
    PermissionManager.Builder()
        .permissions(convertToPermissionEnums(perms))
        .askAgain(true)
        .askAgainCallback { response -> showDialog(response) }
        .callback { permissionsGranted, permissionsDenied, permissionsDeniedForever, permissionsAsked -> }
        .ask(activity)
}

fun askForPermission(fragment: Fragment, perms: Array<String>) {
    PermissionManager.Builder()
        .permissions(convertToPermissionEnums(perms))
        .askAgain(true)
        .askAgainCallback { response -> showDialog(response) }
        .callback { permissionsGranted, permissionsDenied, permissionsDeniedForever, permissionsAsked -> }
        .ask(fragment)
}

private fun convertToPermissionEnums(perms: Array<String>): ArrayList<PermissionEnum> {
    val list = ArrayList<PermissionEnum>(perms.size)
    perms.forEach {
        list.add(PermissionEnum.fromManifestPermission(it))
    }
    return list
}

fun showDialog(response: AskAgainCallback.UserResponse?) {
    // Do nothing
}
