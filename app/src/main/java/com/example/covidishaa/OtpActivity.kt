package com.example.covidishaa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.covidishaa.utils.FirebaseUtils
import com.example.covidishaa.utils.FirebaseUtils.db
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_otp.*
import java.util.concurrent.TimeUnit

class OtpActivity : AppCompatActivity() {
    lateinit var auth: FirebaseAuth
    lateinit var phone: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        auth=FirebaseAuth.getInstance()

        // get storedVerificationId from the intent
        val storedVerificationId= intent.getStringExtra("storedVerificationId")
        phone = """+91${intent.getStringExtra("phone")!!}"""

        // fill otp and call the on click on button
        login.setOnClickListener {
            val otp = et_otp.text.trim().toString()
            if(otp.isNotEmpty()){
                val credential : PhoneAuthCredential = PhoneAuthProvider.getCredential(
                        storedVerificationId.toString(), otp)
                signInWithPhoneAuthCredential(credential)
            }else{
                Toast.makeText(this, "Enter OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getRandomString(length: Int) : String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        return List(length) { charset.random() }
                .joinToString("")
    }


    fun getUniqueId() : String {
        val temp = getRandomString(6)
        val docRef = db.collection("uuids").document(temp)
        var uid = ""
        docRef?.get()
                ?.addOnSuccessListener { document ->
                    if (document != null) {
                        if(document.data == null) {

                            uid = temp

                            db.collection("uuids").document(temp)
                                    .set(hashMapOf(
                                            "phone" to phone
                                    ))
                                    .addOnSuccessListener {
                                        Log.i("tag", "successfully written")

                                    }
                                    .addOnFailureListener { e -> Log.w("TAG", "Error writing document", e) }
                        } else {
                            //login
                            getUniqueId()
                        }
                    } else {
                        Log.d("TAG", "No such document")
                    }
                }
                ?.addOnFailureListener { exception ->
                    Log.d("TAG", "get failed with ", exception)

                }

        return temp
    }
    // verifies if the code matches sent by firebase
    // if success start the new activity in our case it is main Activity
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this, BottomNavActivity::class.java)

                        //doc exists
//                            if true:
//                              login ->
//                                  alreadylogged in -> cannot use phone number for multiple devices / false: login -> log out wali screen me
//                             else:
//                              signup -> new doc create

                        // get storedVerificationId from the intent

                        val docRef = db.collection("users").document(phone)


//
                        Log.i("ff", docRef.toString())
                        Log.i("ff", phone)
                        val curr = getUniqueId()
                        Log.i("tag", curr)
                        docRef?.get()
                                ?.addOnSuccessListener { document ->
                                    if (document != null) {
                                        if(document.data == null) {

                                            db.collection("users").document(phone)
                                                    .set(hashMapOf(
                                                            "name" to "",
                                                            "status" to "1",
                                                            "unique" to curr,

                                                    ))
                                                    .addOnSuccessListener {
                                                        val user = FirebaseUtils.firebaseAuth.currentUser
                                                        val profileUpdates = userProfileChangeRequest {
                                                            displayName = curr
                                                        }

                                                        user!!.updateProfile(profileUpdates)
                                                                .addOnCompleteListener { task ->
                                                                    if (task.isSuccessful) {
                                                                        val dn : String? = FirebaseUtils.firebaseAuth.currentUser!!.displayName
                                                                        Log.i("brrr", dn+"-=-=otp")
                                                                        startActivity(intent)
                                                                        finish()
                                                                    }
                                                                }

                                                    }
                                                    .addOnFailureListener { e -> Log.w("TAG", "Error writing document", e) }
                                        } else {
                                            //login
                                            startActivity(intent)
                                            finish()
                                        }
                                    } else {
                                        Log.d("TAG", "No such document")
                                    }
                                }
                                ?.addOnFailureListener { exception ->
                                    Log.d("TAG", "get failed with ", exception)

                                }




//                        startActivity(intent)
//                        finish()
                    } else {
                        // Sign in failed, display a message and update the UI
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                            Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
    }
}