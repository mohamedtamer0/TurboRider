package com.example.turborider.Utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.example.turborider.Common
import com.example.turborider.Model.TokenModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object UserUtils {
    fun updateUser(
        view: View?,
        updateData:Map<String,Any>
    ) {
        FirebaseDatabase.getInstance()
            .getReference(Common.RIDER_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateData)
            .addOnFailureListener {
                Snackbar.make(view!!,it.message!!, Snackbar.LENGTH_LONG).show()
            }.addOnSuccessListener {
                Snackbar.make(view!!,"Update Information Success", Snackbar.LENGTH_LONG).show()
            }

    }


    fun updateToken(context: Context, token: String) {
        val tokenModel = TokenModel()
        tokenModel.token = token

        FirebaseDatabase.getInstance()
            .getReference(Common.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(tokenModel)
            .addOnFailureListener {
                Toast.makeText(context,it.message, Toast.LENGTH_LONG).show()
            }
            .addOnSuccessListener {

            }
    }



}