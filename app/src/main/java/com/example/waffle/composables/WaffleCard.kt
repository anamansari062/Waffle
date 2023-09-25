package com.example.waffle.composables

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.waffle.viewmodel.WaffleViewModel
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaffleCard(
    identityUri: Uri,
    iconUri: Uri,
    identityName: String,
    modifier: Modifier = Modifier,
    intentSender: ActivityResultSender,
    waffleViewModel: WaffleViewModel = hiltViewModel()
) {
    val viewState = waffleViewModel.viewState.collectAsState().value
    var waffle by remember { mutableStateOf("") }


    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Text(text = "Waffle", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter a waffle you would like to create and tap on the button below.",
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = waffle, onValueChange = { waffle = it })
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                waffleViewModel.incrementCounter(identityUri, iconUri, identityName, intentSender, waffle)
            },
            enabled = viewState.canTransact,
        ) {
            Text(text = "Tap me!")
        }
    }
}
