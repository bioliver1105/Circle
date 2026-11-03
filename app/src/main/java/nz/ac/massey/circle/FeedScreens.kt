package nz.ac.massey.circle

import android.content.ActivityNotFoundException
import android.content.Intent
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(vm: SocialViewModel) {
    // Listen for posts only while this screen is visible and the app is in front
    LifecycleResumeEffect(Unit) {
        vm.startFeed()
        onPauseOrDispose { vm.stopFeed() }
    }

    if (vm.posts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No posts yet. Share the first one!")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(vm.posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    myUid = vm.me?.uid ?: "",
                    onLike = { vm.toggleLike(post) },
                    onDelete = { vm.deletePost(post) }
                )
            }
        }
    }
}

@Composable
fun PostCard(post: Post, myUid: String, onLike: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    // remember() so the image is only decoded once, not on every redraw
    val image = remember(post.image) { ImageUtils.base64ToBitmap(post.image) }
    val liked = myUid in post.likes
    val isMine = post.authorId == myUid
    var confirmDelete by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(post.authorName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = DateUtils.getRelativeTimeSpanString(post.createdAt).toString(),
                style = MaterialTheme.typography.bodySmall
            )
            if (image != null) {
                Spacer(Modifier.height(8.dp))
                Image(
                    bitmap = image,
                    contentDescription = post.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            if (post.caption.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(post.caption)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLike) {
                    Icon(
                        imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (liked) Color.Red else LocalContentColor.current
                    )
                }
                Text(if (post.likes.size == 1) "1 like" else "${post.likes.size} likes")
                Spacer(Modifier.weight(1f))

                // Share the post's text to any app through the Android share sheet
                IconButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${post.authorName} on Circle: ${post.caption}")
                    }
                    context.startActivity(Intent.createChooser(send, "Share post"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }

                // Only the author sees the delete button
                if (isMine) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }

    // Ask before deleting, since it can't be undone
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete post?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun NewPostScreen(vm: SocialViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var caption by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }
    val preview = remember(image) { ImageUtils.base64ToBitmap(image) }

    // Android photo picker: no storage permission needed
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch { image = ImageUtils.uriToBase64(context, uri) }
        }
    }

    // Opens the phone's camera app and returns a small photo
    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            scope.launch { image = ImageUtils.bitmapToBase64(bitmap) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("New post", style = MaterialTheme.typography.headlineSmall)

        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = "Selected photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Text(if (image.isEmpty()) "Choose photo" else "Change photo")
            }
            OutlinedButton(onClick = {
                try {
                    camera.launch(null)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Take photo")
            }
        }

        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text("Caption") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { vm.createPost(caption, image) },
            enabled = !vm.loading && (caption.isNotBlank() || image.isNotEmpty()),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.loading) "Posting..." else "Share")
        }
    }
}
