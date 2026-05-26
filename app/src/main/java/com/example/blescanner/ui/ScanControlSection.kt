package com.example.blescanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScanControlSection(onStartScan: () -> Unit, onStopScan: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStartScan,
            modifier = Modifier.weight(1f)
        ) {
            Text("Scan Start")
        }
        Button(
            onClick = onStopScan,
            modifier = Modifier.weight(1f)
        ) {
            Text("Scan Stop")
        }
    }
}