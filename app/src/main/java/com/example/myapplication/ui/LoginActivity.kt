package com.example.myapplication.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class LoginActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton: Button =findViewById(R.id.buttonLogin)
        val registerButton: Button =findViewById(R.id.buttonRegister)

        loginButton.setOnClickListener{
            Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
        }
        registerButton.setOnClickListener{
            Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show()
        }
    }
}