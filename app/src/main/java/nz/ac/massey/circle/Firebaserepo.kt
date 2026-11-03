package nz.ac.massey.circle

/*
 * Circle: 159.336 Assignment 3 (individual), Shuairan Bi 24021445
 *
 * External libraries (Gradle dependencies, not my code):
 *   - Firebase Authentication and Cloud Firestore (com.google.firebase)
 *   - kotlinx-coroutines-play-services (for await() on Firebase tasks)
 *   - AndroidX Compose, Material 3, Lifecycle (standard Android libraries)
 * All other code in this project was written for this assignment.
 *
 * This file holds all communication with Firebase.
 * Firestore layout: users/{uid}, posts/{postId},
 * chats/{uidA_uidB}/messages/{messageId}
 */



import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object FirebaseRepo {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUid: String?
        get() = auth.currentUser?.uid

    // Accounts

    suspend fun signUp(email: String, password: String, name: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        db.collection("users").document(uid).set(User(uid = uid, name = name)).await()
    }

    suspend fun logIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    fun logOut() = auth.signOut()

    suspend fun getUser(uid: String): User? =
        db.collection("users").document(uid).get().await().toObject(User::class.java)

    suspend fun saveProfile(user: User) {
        db.collection("users").document(user.uid).set(user).await()
    }

    // Live list of all users. Caller must call remove() on the result when done.
    fun listenToUsers(onChange: (List<User>) -> Unit): ListenerRegistration =
        db.collection("users").addSnapshotListener { snap, _ ->
            onChange(snap?.toObjects(User::class.java) ?: emptyList())
        }

    //  Posts

    suspend fun createPost(post: Post) {
        db.collection("posts").add(post).await()
    }

    // Newest 50 posts, updated live
    fun listenToPosts(onChange: (List<Post>) -> Unit): ListenerRegistration =
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                val posts = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                }
                onChange(posts ?: emptyList())
            }

    // arrayUnion/arrayRemove change the list on the server, so two people
    // liking at the same time don't overwrite each other.
    fun toggleLike(post: Post, uid: String) {
        val ref = db.collection("posts").document(post.id)
        if (uid in post.likes) {
            ref.update("likes", FieldValue.arrayRemove(uid))
        } else {
            ref.update("likes", FieldValue.arrayUnion(uid))
        }
    }

    fun deletePost(postId: String) {
        db.collection("posts").document(postId).delete()
    }

    // Chat

    // Sorting the two uids means both people get the same chat id
    fun chatId(a: String, b: String): String = listOf(a, b).sorted().joinToString("_")

    suspend fun sendMessage(chatId: String, message: Message) {
        db.collection("chats").document(chatId)
            .collection("messages").add(message).await()
    }

    fun listenToMessages(chatId: String, onChange: (List<Message>) -> Unit): ListenerRegistration =
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("sentAt")
            .addSnapshotListener { snap, _ ->
                val messages = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }
                onChange(messages ?: emptyList())
            }
}