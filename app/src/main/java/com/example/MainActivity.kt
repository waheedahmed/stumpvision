package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.StumpVisionApp
import com.example.ui.StumpViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: StumpViewModel = viewModel()
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          // StumpVisionApp handles safe drawing paddings internally
          StumpVisionApp(viewModel = viewModel)
        }
      }
    }
  }
}

