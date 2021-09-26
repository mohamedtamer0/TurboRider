package com.example.turborider

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.turborider.Utils.UserUtils
import com.example.turborider.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_home.*
import java.util.HashMap

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHomeBinding
    private lateinit var navView: NavigationView
    private lateinit var img_avatar: ImageView
    private lateinit var waitingDialog: AlertDialog
    private lateinit var storageReference: StorageReference
    private var imageUri: Uri? = null


    companion object {
        val PICK_IMAGE_REQUEST = 7272
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)


        val drawerLayout: DrawerLayout = binding.drawerLayout
         navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_home)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        init()
    }




    private fun init() {

        storageReference = FirebaseStorage.getInstance().getReference()

        waitingDialog = AlertDialog.Builder(this)
            .setMessage("Waiting....")
            .setCancelable(false).create()

        //when clicked on items Navigation
        navView.setNavigationItemSelectedListener {
            if (it.itemId == R.id.nav_exit) {
                val builder = AlertDialog.Builder(this, R.style.AlertDialog)
                builder.apply {
                    setTitle("Sign out")
                    setMessage("Do you want to sign out?")
                    setNegativeButton("CANCEL", { dialogInterface, _ -> dialogInterface.dismiss() })
                    setPositiveButton("SIGN OUT") { dialogInterface, _ ->
                        FirebaseAuth.getInstance().signOut()
                        val intent =
                            Intent(this@HomeActivity, SplashScreenActivity::class.java)
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    }.setCancelable(false)

                    val dialog = builder.create()
                    dialog.setOnShowListener {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setTextColor(resources.getColor(android.R.color.holo_red_dark))

                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(resources.getColor(R.color.colorAccent))
                    }
                    dialog.show()
                }
            }

            true
        }

        //the data for the user
        val headerView = navView.getHeaderView(0)
        val txt_name = headerView.findViewById<View>(R.id.txt_name) as TextView
        val txt_phone = headerView.findViewById<View>(R.id.txt_phone) as TextView
        img_avatar = headerView.findViewById<View>(R.id.img_avt) as ImageView


        txt_name.setText(Common.buildWelcomeMessage())
        txt_phone.setText(Common.currentRider!!.phoneNumber)


        if (Common.currentRider != null && Common.currentRider!!.avatar != null && !TextUtils.isEmpty(
                Common.currentRider!!.avatar
            )
        ) {
            Glide.with(this).load(Common.currentRider!!.avatar).into(img_avatar)


        }
        img_avatar.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE_REQUEST
            )
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && data.data != null) {
            imageUri = data.data
            img_avatar.setImageURI(imageUri)

            showDialogUpload()
        }
    }



    private fun showDialogUpload() {


        val builder = AlertDialog.Builder(this, R.style.AlertDialog)
        builder.apply {
            setTitle("Change Avatar")
            setMessage("Do you Really want to change Avatar?")
            setNegativeButton("CANCEL", { dialogInterface, _ -> dialogInterface.dismiss() })
            setPositiveButton("Change") { dialogInterface, _ ->

                if (imageUri != null) {
                    waitingDialog.show()
                    val avatarFolder =
                        storageReference.child("avatars/" + FirebaseAuth.getInstance().currentUser!!.uid)

                    avatarFolder.putFile(imageUri!!)
                        .addOnFailureListener {
                            Snackbar.make(drawer_layout, it.message!!, Snackbar.LENGTH_LONG).show()
                            waitingDialog.dismiss()
                        }.addOnCompleteListener {
                            if (it.isSuccessful) {
                                avatarFolder.downloadUrl.addOnSuccessListener {
                                    val update_data = HashMap<String, Any>()
                                    update_data.put("avatar", it.toString())

                                    UserUtils.updateUser(drawer_layout, update_data)
                                }
                            }
                            waitingDialog.dismiss()
                        }.addOnProgressListener {
                            val progress = (100.0 * it.bytesTransferred / it.totalByteCount)
                            waitingDialog.setMessage(
                                StringBuilder("Uploading: ").append(progress).append("%")
                            )
                        }
                }

            }.setCancelable(false)

            val dialog = builder.create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this@HomeActivity,android.R.color.holo_red_dark))

                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this@HomeActivity,R.color.colorAccent))
            }
            dialog.show()
        }


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_home)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}