package com.example.catlovercompose.feature.auth.signin

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.catlovercompose.R
import com.example.catlovercompose.navigation.NavDestinations

@Composable
fun SignInScreen(navController: NavController) {
    val viewModel: SignInViewModel = hiltViewModel()
    val uiState = viewModel.state.collectAsState()

    var email by remember { mutableStateOf("") }
    var passwd by remember { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(key1 = uiState.value) {
        when (uiState.value) {
            is SignInState.Success -> {
                navController.navigate(NavDestinations.AppShell.route) {
                    popUpTo(NavDestinations.SignIn.route) { inclusive = true }
                }
            }
            is SignInState.Error -> {
                Toast.makeText(context, "Sign in Failed", Toast.LENGTH_SHORT).show()
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
                painter = painterResource(id = R.drawable.cart),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.White)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(text = "Email") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = passwd,
                onValueChange = { passwd = it },
                label = { Text(text = "Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.size(16.dp))

            if (uiState.value == SignInState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.signIn(email, passwd) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotEmpty() && passwd.isNotEmpty() &&
                            (uiState.value == SignInState.Nothing || uiState.value == SignInState.Error)
                ) {
                    Text(text = "Sign in")
                }

                TextButton(onClick = { navController.navigate(NavDestinations.SignUp.route) }) {
                    Text(text = "Don't have an account? Sign Up")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSignInScreen() {
    SignInScreen(navController = rememberNavController())
}