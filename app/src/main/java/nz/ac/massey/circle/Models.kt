package nz.ac.massey.circle
// A user's public profile   The document ID is the user's Firebase uid
data class User(
    val uid: String = "",
    val name: String = "",
    val bio: String = "",
    val avatar: String = ""
)

// One post in the feed   likes holds the uids of everyone who liked it, so the same person can't like a post twice
data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val caption: String = "",
    val image: String = "",
    val likes: List<String> = emptyList(),
    val createdAt: Long = 0L
)

//One chat message, stored under chats/{chatId}/messages.
data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val sentAt: Long = 0L
)

// Which screen is showing   A sealed class means `when` must handle every screen, so a new one can't be forgotten  Chat carries the other person
sealed class Screen {
    object Feed : Screen()
    object NewPost : Screen()
    object People : Screen()
    object Profile : Screen()
    data class Chat(val other: User) : Screen()
}