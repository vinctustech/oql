package com.vinctus.oql2

trait EventDB extends Test {

  val dm: String =
    """
      |entity event {
      | *id: bigint
      |  what: text
      |  when: timestamp
      |  attendees: [attendee] (attendance)
      |}
      |
      |entity attendee {
      | *id: bigint
      |  name: text
      |  events: [event] (attendance)
      |}
      |
      |entity attendance {
      |  event: event
      |  attendee: attendee
      |}
      |""".stripMargin
  val db = new OQL(dm, ds)

  db.create()
  db.execute(_.insert("""insert into event (what, "when") values 
                        |('start testing timestamps', '2021-04-21T17:42:49.943Z'),
                        |('woke up this morning', '2021-04-21T06:30:00.000Z')
                        |""".stripMargin))
  db.execute(_.insert("""insert into attendee (name) values 
                        |('me')
                        |""".stripMargin))
  db.execute(_.insert("""insert into attendance (event, attendee) values
                        |(1, 1),
                        |(2, 1)
                        |""".stripMargin))

}
