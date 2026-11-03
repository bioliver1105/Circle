package nz.ac.massey.circle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
// Change this import to the theme from your Greetings project
import nz.ac.massey.circle.ui.theme.CircleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CircleTheme {
                CircleApp()
            }
        }
    }
}

// Top-level screen switcher   Uses a simple state variable instead of a navigation library to keep the app small
@Composable
fun CircleApp(vm: SocialViewModel = viewModel()) {
    val me = vm.me
    if (me == null) {
        AuthScreen(vm)
    } else {
        // System back button: chat -> people list, any other tab -> feed
        BackHandler(enabled = vm.screen != Screen.Feed) {
            vm.screen = if (vm.screen is Screen.Chat) Screen.People else Screen.Feed
        }
        Scaffold(
            bottomBar = { if (vm.screen !is Screen.Chat) BottomBar(vm) }
        ) { padding ->
            Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                when (val s = vm.screen) {
                    Screen.Feed -> FeedScreen(vm)
                    Screen.NewPost -> NewPostScreen(vm)
                    Screen.People -> PeopleScreen(vm)
                    Screen.Profile -> ProfileScreen(vm, me)
                    is Screen.Chat -> ChatScreen(vm, s.other)
                }
            }
        }
    }
}

@Composable
fun BottomBar(vm: SocialViewModel) {
    NavigationBar {
        NavItem(vm, Screen.Feed, Icons.Default.Home, "Feed")
        NavItem(vm, Screen.NewPost, Icons.Default.Add, "Post")
        NavItem(vm, Screen.People, Icons.Default.Email, "Chats")
        NavItem(vm, Screen.Profile, Icons.Default.Person, "Profile")
    }
}

@Composable
private fun RowScope.NavItem(vm: SocialViewModel, target: Screen, icon: ImageVector, label: String) {
    NavigationBarItem(
        selected = vm.screen == target,
        onClick = { vm.screen = target },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) }
    )
}

// Round profile picture, or the first letter of the name if there's no photo
@Composable
fun Avatar(user: User, size: Dp) {
    val bitmap = remember(user.avatar) { ImageUtils.base64ToBitmap(user.avatar) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.name.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}