package com.vinctus.oql2

trait EventDB extends Test {

  val dm: String =
    """
      |entity event {
      | *id: uuid
      |  what: text
      |  when: timestamp
      |  duration: bigint
      |  attendees: [attendee] (attendance)
      |}
      |
      |entity attendee {
      | *id: uuid
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
  db.execute(_.insert("""insert into "event" ("id", "what", "when", "duration") values 
                        |('797f15ab-56ba-4389-aca1-5c3c661fc9fb', 'start testing timestamps', '2021-04-21T17:42:49.943Z', 300),
                        |('8aef4c68-7977-48cb-ba38-2764881d0843', 'woke up this morning', '2021-04-21T06:30:00.000Z', null)
                        |""".stripMargin))
  db.execute(_.insert("""insert into "attendee" ("id", "name") values 
                        |('e8d982cd-dd19-4766-a627-ab33009bc259', 'me')
                        |""".stripMargin))
  db.execute(_.insert("""insert into "attendance" ("event", "attendee") values
                        |('797f15ab-56ba-4389-aca1-5c3c661fc9fb', 'e8d982cd-dd19-4766-a627-ab33009bc259'),
                        |('8aef4c68-7977-48cb-ba38-2764881d0843', 'e8d982cd-dd19-4766-a627-ab33009bc259')
                        |""".stripMargin))

}
