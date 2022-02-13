entity main {
 *id: int
  label: text
  infos: [info]
}

entity info {
 *id: int
  main: main
  data: json
}
