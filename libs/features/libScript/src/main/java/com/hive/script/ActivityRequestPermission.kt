// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.hive.permissions.PermissionsCallback
import com.hive.permissions.PermissionsChecker
import com.hive.utils.GlobalApp
import com.hive.utils.utils.IntentUtils

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/18/21
 */
class ActivityRequestPermission : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsChecker?.startCheck(arrayOf(permission), object :
            PermissionsCallback {
            override fun onDenied(lackedPermissions: MutableList<String>?) {
                failureFun?.invoke()
                failureFun = null
            }

            override fun onGranted() {
                successFun?.invoke()
                successFun = null
            }
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionsChecker?.onActivityResult(requestCode, resultCode, data)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsChecker?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    override fun onDestroy() {
        super.onDestroy()
        if (failureFun != null && successFun != null) {
            if(permissionsChecker?.lacksPermission(permission) == false){
                successFun?.invoke()
            }else{
                failureFun?.invoke()
            }
        }
        failureFun = null
        successFun = null
        permissionsChecker = null

    }

    companion object {

        var successFun: (() -> Unit)? = null

        var failureFun: (() -> Unit)? = null

        var permission = ""

        private var permissionsChecker: PermissionsChecker? = null

        fun checkOrRequestPermission(
            context: Context?,
            permission: String,
            success: (() -> Unit)?,
            failure: (() -> Unit)?
        ) {
            val cxt = context ?: GlobalApp.getApp()
            this.failureFun = failure
            this.successFun = success
            this.permission = permission
            permissionsChecker = PermissionsChecker()
            if ((permissionsChecker?.getLacksPermissions(permission)?.size ?: 1) == 0) {
                this.successFun?.invoke()
                this.successFun = null
                permissionsChecker = null
                return
            }
            IntentUtils.safeStartActivity(cxt, Intent(cxt, ActivityRequestPermission::class.java))
        }

    }
}