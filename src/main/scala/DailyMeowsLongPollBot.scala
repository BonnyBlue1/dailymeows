import DailyMeows.{sendFact, sessionResource}
import cats.effect.*
import cats.implicits.*
import telegramium.bots.client.Method
import telegramium.bots.high.keyboards.{InlineKeyboardButtons, InlineKeyboardMarkups}
import telegramium.bots.{CallbackQuery, ChatId, ChatIntId, Message}
import telegramium.bots.high.{BotApi, LongPollBot}

class DailyMeowsLongPollBot(botApi: BotApi[IO]) extends LongPollBot[IO](botApi):

  def processStart(chatId: ChatIntId): IO[List[Method[Message]]] =
    IO.pure(List(sendMessage(
      chatId = chatId,
      text = "Meow! Interested in a daily cat-mouse fact? That's what DailyMeows does. \uD83D\uDC31",
      replyMarkup = Some(InlineKeyboardMarkups.singleButton(
        InlineKeyboardButtons.callbackData("Start daily messages", callbackData = "startdaily")
      ))
    )))

  def addNewUser(chatId: ChatIntId): IO[Unit] =
    sessionResource.use { session =>
      session.prepare(Repo.addNewUser).flatMap(_.execute(chatId.id)).void
    }

  def processStartDaily(chatId: ChatIntId): IO[List[Method[Message]]] = for
    _       <- addNewUser(chatId)
    result1  = sendMessage(chatId, "Daily messages started!")
    result2 <- sendFact(chatId)
  yield List(result1) ++ result2

  def processMessage(chatId: ChatIntId, msgText: String): IO[List[Method[Message]]] = msgText.toLowerCase match
    case "/start"             => processStart(chatId)
    case "meow" | "/meow"     => IO.pure(List(sendMessage(chatId, "Meow!"))) 
    case "squeak" | "/squeak" => IO.pure(List(sendMessage(chatId, "Squeak!")))
    case other                => IO.pure(List(sendMessage(chatId, "Meow meow!")))

  override def onMessage(msg: Message): IO[Unit] = msg.text match
    case Some(text) =>
      processMessage(ChatIntId(msg.chat.id), text).flatMap(commands =>
        commands.traverse_(command => botApi.execute(command).void))

    case None       =>
      IO.unit

  override def onCallbackQuery(query: CallbackQuery): IO[Unit] = query.data match
    case Some(data) =>
      query.message.traverse_(originalMessage =>
        processStartDaily(ChatIntId(originalMessage.chat.id)).flatMap(commands =>
          commands.traverse_(command => botApi.execute(command).void)))

    case None =>
      IO.unit

