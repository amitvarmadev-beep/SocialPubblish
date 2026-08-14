package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          SocialPublisherApp(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

@Composable
fun SocialPublisherApp(modifier: Modifier = Modifier) {
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoTopic by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    
    var youtubeTitle by remember { mutableStateOf("") }
    var youtubeDesc by remember { mutableStateOf("") }
    
    var fbTitle by remember { mutableStateOf("") }
    var fbDesc by remember { mutableStateOf("") }
    
    var igTitle by remember { mutableStateOf("") }
    var igDesc by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SocialPublisher", style = MaterialTheme.typography.headlineMedium)
        
        Button(
            onClick = {
                videoPickerLauncher.launch(
                    PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        .build()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.VideoLibrary, contentDescription = "Pick Video")
            Spacer(Modifier.width(8.dp))
            Text(if (videoUri != null) "Video Selected" else "Select Video")
        }
        
        OutlinedTextField(
            value = videoTopic,
            onValueChange = { videoTopic = it },
            label = { Text("What is this video about?") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Button(
            onClick = {
                if (videoTopic.isBlank()) {
                    Toast.makeText(context, "Please enter a topic", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isGenerating = true
                coroutineScope.launch {
                    val result = generateSocialMediaCaptions(videoTopic)
                    isGenerating = false
                    
                    youtubeTitle = extractSection(result, "[YouTube]", "Title:")
                    youtubeDesc = extractSection(result, "[YouTube]", "Description:")
                    
                    igTitle = extractSection(result, "[Instagram]", "Title:")
                    igDesc = extractSection(result, "[Instagram]", "Description:")
                    
                    fbTitle = extractSection(result, "[Facebook]", "Title:")
                    fbDesc = extractSection(result, "[Facebook]", "Description:")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isGenerating && videoTopic.isNotBlank()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Generating with AI...")
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Generate")
                Spacer(Modifier.width(8.dp))
                Text("Generate Captions (AI)")
            }
        }

        if (youtubeTitle.isNotBlank() || youtubeDesc.isNotBlank() || true) {
            SocialPlatformCard(
                platform = "YouTube",
                title = youtubeTitle,
                description = youtubeDesc,
                onTitleChange = { youtubeTitle = it },
                onDescChange = { youtubeDesc = it },
                onPublish = {
                    shareVideo(context, videoUri, youtubeTitle, youtubeDesc, "com.google.android.youtube")
                }
            )
        }
        
        if (igTitle.isNotBlank() || igDesc.isNotBlank() || true) {
            SocialPlatformCard(
                platform = "Instagram",
                title = igTitle,
                description = igDesc,
                onTitleChange = { igTitle = it },
                onDescChange = { igDesc = it },
                onPublish = {
                    shareVideo(context, videoUri, igTitle, igDesc, "com.instagram.android")
                }
            )
        }
        
        if (fbTitle.isNotBlank() || fbDesc.isNotBlank() || true) {
            SocialPlatformCard(
                platform = "Facebook",
                title = fbTitle,
                description = fbDesc,
                onTitleChange = { fbTitle = it },
                onDescChange = { fbDesc = it },
                onPublish = {
                    shareVideo(context, videoUri, fbTitle, fbDesc, "com.facebook.katana")
                }
            )
        }
    }
}

fun extractSection(text: String, platformHeader: String, fieldHeader: String): String {
    val platformIndex = text.indexOf(platformHeader)
    if (platformIndex == -1) return ""
    
    val fieldIndex = text.indexOf(fieldHeader, platformIndex)
    if (fieldIndex == -1) return ""
    
    var endIndex = text.indexOf("\n[", fieldIndex)
    if (endIndex == -1) endIndex = text.length
    
    val nextFieldIndex = text.indexOf("\nTitle:", fieldIndex + 1)
    if (nextFieldIndex != -1 && nextFieldIndex < endIndex) endIndex = nextFieldIndex
    
    val nextFieldDescIndex = text.indexOf("\nDescription:", fieldIndex + 1)
    if (nextFieldDescIndex != -1 && nextFieldDescIndex < endIndex) endIndex = nextFieldDescIndex

    return text.substring(fieldIndex + fieldHeader.length, endIndex).trim()
}

@Composable
fun SocialPlatformCard(
    platform: String,
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onPublish: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(platform, style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = onDescChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Button(
                onClick = onPublish,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Publish to $platform")
                Spacer(Modifier.width(8.dp))
                Text("Publish to $platform")
            }
        }
    }
}

fun shareVideo(context: android.content.Context, videoUri: Uri?, title: String, description: String, targetPackage: String) {
    if (videoUri == null) {
        Toast.makeText(context, "Please select a video first", Toast.LENGTH_SHORT).show()
        return
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/*"
        putExtra(Intent.EXTRA_STREAM, videoUri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_TEXT, "$title\n\n$description")
        setPackage(targetPackage)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        intent.setPackage(null)
        try {
            context.startActivity(Intent.createChooser(intent, "Share video via..."))
        } catch (ex: Exception) {
            Toast.makeText(context, "No app available to share video", Toast.LENGTH_SHORT).show()
        }
    }
}
