create type color as enum ('red', 'blue', 'black', 'white', 'gray', 'silver', 'green', 'yellow');

create table cars (
  make text,
  color color
);

insert into cars(make, color)
     values ('ferrari', 'red'),
            ('aston martin', 'blue'),
            ('bentley', 'gray'),
            ('ford', 'black');
