package com.example.catlovercompose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.catlovercompose.navigation.AppNavigation

@Composable
@Preview
fun MainApp() {
    Surface(modifier = Modifier.fillMaxSize()) {
        AppNavigation()
    }
}