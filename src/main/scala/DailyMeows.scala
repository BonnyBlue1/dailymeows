import cats.implicits.*
import cats.effect.*
import org.http4s.blaze.client.BlazeClientBuilder
import skunk.*
import skunk.implicits.*
import natchez.Trace.Implicits.noop
import telegramium.bots.{ChatIntId, Message}
import telegramium.bots.client.Method
import telegramium.bots.high.*
import telegramium.bots.high.Methods.*
import telegramium.bots.high.implicits.*
import pureconfig.*
import pureconfig.generic.derivation.default.*

import java.time.{LocalTime, ZoneOffset}
import scala.concurrent.duration.*

object DailyMeows extends IOApp.Simple:

  case class Config(
    telegramToken: String,
    dbHost: String,
    dbName: String,
    dbUser: String,
    dbPassword: String
  ) derives ConfigReader
        
  val config: Config = ConfigSource
    .resources("local.conf")
    .optional
    .withFallback(ConfigSource.resources("application.conf"))
    .loadOrThrow[Config]

  val sessionResource: Resource[IO, Session[IO]] = Session.single[IO](
    host = config.dbHost,
    user = config.dbUser,
    database = config.dbName,
    password = Some(config.dbPassword),
    ssl = SSL.System
  )

  val defaultFact: Fact = Fact("mouse", "The mouse did not supply enough facts")

  def sendFact(chatId: ChatIntId): IO[List[Method[Message]]] =
    sessionResource.use { session =>
      for
        lastIndex <- session.unique(Repo.getLastSentFactIndexByChatId)(chatId.id)
        nextIndex  = lastIndex + 1
        factList  <- session.execute(Repo.getAllFacts)
        nextFact   = factList.get(nextIndex).getOrElse(defaultFact)
        result     = sendMessage(chatId, nextFact.content)
        _         <- session.execute(Repo.setLastSent)((nextIndex, chatId.id))
      yield List(result)
    }

  private def checkAndSendDaily(botApi: BotApi[IO]): IO[Unit] =
    sessionResource.use { session =>
      for
        _        <- IO.println("Getting all users due for a new fact...")
        users    <- session.execute(Repo.getUsersDueForAFact)
        _        <- IO.println(s"Got ${users.length} users due for a new fact. Sending out the facts...")
        commands <- users.flatTraverse(user => sendFact(ChatIntId(user.chatId)))
        _        <- commands.traverse(botApi.execute)
        _        <- IO.println("Finished sending out facts")
      yield ()
    }

  private def checkAndSendDailyIfWorkingHours(botApi: BotApi[IO]): IO[Unit] = for
    _           <- IO.println("Checking if this is a good time to send out facts")
    now         <- Clock[IO].realTimeInstant
    localTimeUtc = now.atOffset(ZoneOffset.UTC).toLocalTime
    _           <- checkAndSendDaily(botApi)
      .whenA(localTimeUtc.isAfter(LocalTime.of(10, 0)) && localTimeUtc.isBefore(LocalTime.of(20, 0)))
  yield ()

  val run: IO[Unit] =
    BlazeClientBuilder[IO]
      .resource
      .use { httpClient =>
        val botApi: BotApi[IO] = BotApi[IO](httpClient, s"https://api.telegram.org/bot${config.telegramToken}")
        val bot = new DailyMeowsLongPollBot(botApi)
        for
          _        <- IO.println("Starting DailyMeows")
          _        <- checkAndSendDailyIfWorkingHours(botApi).flatMap(_ => IO.sleep(1.hour)).foreverM.start
          botFiber <- bot.start().start
          _        <- IO.println("Started successfully")
          _        <- botFiber.join
        yield ()
      }