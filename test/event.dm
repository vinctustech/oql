entity event {
 *id: uuid
  what: text
  when: timestamp
  duration: float
  attendees: [attendee] (attendance)
}

entity attendee {
 *id: uuid
  name: text
  events: [event] (attendance)
}

entity attendance {
  event: event
  attendee: attendee
}
