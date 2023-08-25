import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import java.time.OffsetDateTime

object Repo:

  val getAllFacts: Query[Void, Fact] = sql"""
    select animal, content
    from facts
    """.query(text *: text)
       .to[Fact]

  val addNewUser: Command[Long] = sql"""
    insert into users (chat_id, last_sent_date)
    values ($int8, now())
    """.command

  val setLastSent: Command[Int *: Long *: EmptyTuple] = sql"""
    update users
    set last_sent_date = now(),
        last_sent_fact_index = $int4
    where chat_id = $int8
    """.command

  val getLastSentFactIndexByChatId: Query[Long, Int] = sql"""
    select last_sent_fact_index
    from users
    where chat_id = $int8
    """.query(int4)

  val getAllUsers: Query[Void, User] = sql"""
    select chat_id, last_sent_date, last_sent_fact_index
    from users
    """.query(int8 *: timestamptz *: int4)
       .to[User]

  val getUsersDueForAFact: Query[Void, User] = sql"""
    select chat_id, last_sent_date, last_sent_fact_index
    from users u
    where u.last_sent_date::date < now()::date
    """.query(int8 *: timestamptz *: int4)
       .to[User]

