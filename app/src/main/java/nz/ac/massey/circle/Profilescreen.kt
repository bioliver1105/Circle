package nz.ac.massey.circle

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(vm: SocialViewModel, me: User) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Editable copies; reset if the saved profile changes
    var name by remember(me) { mutableStateOf(me.name) }
    var bio by remember(me) { mutableStateOf(me.bio) }
    var avatar by remember(me) { mutableStateOf(me.avatar) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch { avatar = ImageUtils.uriToBase64(context, uri, maxSize = 300) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Avatar(me.copy(name = name, avatar = avatar), 96.dp)
        TextButton(onClick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) {
            Text("Change photo")
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { vm.saveProfile(name, bio, avatar) },
            enabled = !vm.loading && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save profile")
        }
        OutlinedButton(onClick = { vm.logOut() }, modifier = Modifier.fillMaxWidth()) {
            Text("Log out")
        }
    }
}