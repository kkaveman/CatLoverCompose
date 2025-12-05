package com.example.catlovercompose.feature.auth.signup

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

import com.example.catlovercompose.R
import com.example.catlovercompose.feature.auth.signin.SignInState
import com.example.catlovercompose.feature.auth.signup.SignUpState
import com.example.catlovercompose.navigation.NavDestinations

@Composable

fun SignUpScreen(navController: NavController){

    val viewModel : SignUpViewModel = hiltViewModel()
    val uiState = viewModel.state.collectAsState()


    var username by remember{
        mutableStateOf("")
    }

    var email by remember{
        mutableStateOf("")
    }

    var passwd by remember{
        mutableStateOf("")
    }

    var conf_passwd by remember{
        mutableStateOf("")
    }

    val context = LocalContext.current
    LaunchedEffect(key1 = uiState.value){
        when(uiState.value){
            is SignUpState.Success -> {
                navController.navigate(NavDestinations.SignIn.route)
            }
            is SignUpState.Error -> {
                Toast.makeText(context, "Sign up Failed", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }


    Scaffold(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp)
                .background(color = Color.White),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.cart), contentDescription = null,
                modifier = Modifier.size(200.dp)
                    .background(Color.White)
            )
            OutlinedTextField(value = username,
                onValueChange = {username = it},
                label = {Text(text = "username")},
                modifier = Modifier.fillMaxWidth())


            OutlinedTextField(value = email,
                onValueChange = {email = it},
                label = {Text(text = "Email")},
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(value = passwd,
                onValueChange = { passwd = it},
                label = {Text(text = "Password")},
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
            )

            OutlinedTextField(value = conf_passwd,
                onValueChange = { conf_passwd = it},
                label = {Text(text = "Confirm Password")},
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                isError = passwd.isNotEmpty() && conf_passwd.isNotEmpty() && (passwd != conf_passwd)
            )


            Spacer(modifier = Modifier.size(16.dp))
            if(uiState.value == SignUpState.Loading){
                CircularProgressIndicator()
            }else{
                Button(onClick = { viewModel.signUp(username,email,passwd)},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = username.isNotEmpty() && email.isNotEmpty() && passwd.isNotEmpty() && uiState.value == SignUpState.Nothing || uiState.value == SignUpState.Error

                ){
                    Text(text = "Sign Up")
                }



                TextButton(onClick = { navController.navigate("signin")}){
                    Text(text = "Already have an account? Sign In")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSignUpScreen(){
    SignUpScreen(navController = rememberNavController())
}