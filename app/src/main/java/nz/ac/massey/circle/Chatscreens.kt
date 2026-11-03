package nz.ac.massey.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.material.icons.filled.Search

// List of everyone else on the app, with a search box to filter by name.
@Composable
fun PeopleScreen(vm: SocialViewModel) {
    LifecycleResumeEffect(Unit) {
        vm.startUsers()
        onPauseOrDispose { vm.stopUsers() }
    }

    var query by remember { mutableStateOf("") }

    // Everyone except me, whose name contains the search text (ignoring case)
    val others = vm.users.filter {
        it.uid != vm.me?.uid && it.name.contains(query.trim(), ignoreCase = true)
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "Chats",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search people") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        if (others.isEmpty()) {
            item {
                Text(
                    if (query.isBlank()) "No one else here yet." else "No one matches \"$query\".",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        items(others, key = { it.uid }) { user ->
            ListItem(
                headlineContent = { Text(user.name) },
                supportingContent = {
                    if (user.bio.isNotBlank()) Text(user.bio, maxLines = 1)
                },
                leadingContent = { Avatar(user, 44.dp) },
                modifier = Modifier.clickable { vm.screen = Screen.Chat(user) }
            )
        }
    }
}
@Composable
fun ChatScreen(vm: SocialViewModel, other: User) {
    // Restart the listener if the chat partner changes
    LifecycleResumeEffect(other.uid) {
        vm.startChat(other)
        onPauseOrDispose { vm.stopChat() }
    }

    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to the newest message whenever one arrives
    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.lastIndex)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            IconButton(onClick = { vm.screen = Screen.People }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Avatar(other, 36.dp)
            Spacer(Modifier.width(12.dp))
            Text(other.name, style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider()

        // Messages
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(vm.messages, key = { it.id }) { message ->
                MessageBubble(message, mine = message.senderId == vm.me?.uid)
            }
        }

        // Input row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Message") },
                maxLines = 4,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    vm.sendMessage(other, text)
                    text = ""
                },
                enabled = text.isNotBlank()
            ) {
                Text("Send")
            }
        }
    }
}

// My messages on the right in the primary colour, theirs on the left
@Composable
fun MessageBubble(message: Message, mine: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = message.text,
            color = if (mine) colors.onPrimary else colors.onSurfaceVariant,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (mine) colors.primary else colors.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}