import java.time.OffsetDateTime

case class User(
  chatId: Long,
  lastSentDate: OffsetDateTime,
  lastSentFactIndex: Int
)

case class Fact(
   animal: String,
   content: String
 )