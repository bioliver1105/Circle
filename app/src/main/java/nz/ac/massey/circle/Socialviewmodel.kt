package nz.ac.massey.circle

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class SocialViewModel : ViewModel() {

    // The signed-in user's profile   null = show the login screen
    var me by mutableStateOf<User?>(null)
        private set
    var screen by mutableStateOf<Screen>(Screen.Feed)
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)
        private set

    var posts by mutableStateOf<List<Post>>(emptyList())
        private set
    var users by mutableStateOf<List<User>>(emptyList())
        private set
    var messages by mutableStateOf<List<Message>>(emptyList())
        private set

    private var postsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    init {
        // Firebase remembers the login between app launches
        val uid = FirebaseRepo.currentUid
        if (uid != null) launchTask { me = FirebaseRepo.getUser(uid) }
    }

    // Runs a Firebase call in the background, with a loading flag and error message
    private fun launchTask(block: suspend () -> Unit) {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                block()
            } catch (e: Exception) {
                error = e.message ?: "Something went wrong"
            }
            loading = false
        }
    }

    // Accounts

    fun signUp(email: String, password: String, name: String) {
        if (name.isBlank()) {
            error = "Please enter your name"
            return
        }
        launchTask {
            FirebaseRepo.signUp(email.trim(), password, name.trim())
            me = FirebaseRepo.getUser(FirebaseRepo.currentUid!!)
        }
    }

    fun logIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            error = "Enter your email and password"
            return
        }
        launchTask {
            FirebaseRepo.logIn(email.trim(), password)
            me = FirebaseRepo.getUser(FirebaseRepo.currentUid!!)
        }
    }

    fun logOut() {
        stopFeed()
        stopUsers()
        stopChat()
        FirebaseRepo.logOut()
        me = null
        screen = Screen.Feed
    }

    fun saveProfile(name: String, bio: String, avatar: String) {
        val current = me ?: return
        launchTask {
            val updated = current.copy(name = name.trim(), bio = bio.trim(), avatar = avatar)
            FirebaseRepo.saveProfile(updated)
            me = updated
        }
    }

    // Feed

    fun createPost(caption: String, image: String) {
        val user = me ?: return
        launchTask {
            FirebaseRepo.createPost(
                Post(
                    authorId = user.uid,
                    authorName = user.name,
                    caption = caption.trim(),
                    image = image,
                    createdAt = System.currentTimeMillis()
                )
            )
            screen = Screen.Feed
        }
    }

    fun toggleLike(post: Post) {
        val user = me ?: return
        FirebaseRepo.toggleLike(post, user.uid)
    }

    // Only delete my own posts
    fun deletePost(post: Post) {
        val user = me ?: return
        if (post.authorId == user.uid) FirebaseRepo.deletePost(post.id)
    }

    fun startFeed() {
        if (postsListener == null) {
            postsListener = FirebaseRepo.listenToPosts { posts = it }
        }
    }

    fun stopFeed() {
        postsListener?.remove()
        postsListener = null
    }

    //  People and chat

    fun startUsers() {
        if (usersListener == null) {
            usersListener = FirebaseRepo.listenToUsers { users = it }
        }
    }

    fun stopUsers() {
        usersListener?.remove()
        usersListener = null
    }

    fun startChat(other: User) {
        val user = me ?: return
        stopChat()
        messages = emptyList()
        val id = FirebaseRepo.chatId(user.uid, other.uid)
        messagesListener = FirebaseRepo.listenToMessages(id) { messages = it }
    }

    fun stopChat() {
        messagesListener?.remove()
        messagesListener = null
    }

    fun sendMessage(other: User, text: String) {
        val user = me ?: return
        if (text.isBlank()) return
        val message = Message(
            senderId = user.uid,
            text = text.trim(),
            sentAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            try {
                FirebaseRepo.sendMessage(FirebaseRepo.chatId(user.uid, other.uid), message)
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    // Called when the ViewModel is destroyed: make sure no listener is left open
    override fun onCleared() {
        stopFeed()
        stopUsers()
        stopChat()
    }
}