entity book {
 *id (pk_book_id): bigint
  title: text
  year: int
  author (author_id): author
}

entity author {
 *id (pk_author_id): bigint
  name: text
  books: [book]
}
