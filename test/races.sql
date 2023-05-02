-- https://www.digitalocean.com/community/tutorials/how-to-work-with-dates-and-times-in-sql

CREATE TABLE races (
    race_id INT PRIMARY KEY,
    runner_name TEXT,
    race_name TEXT,
    start_day DATE,
    start_time TIME,
    total_miles NUMERIC(3, 1),
    end_time TIMESTAMP
);
INSERT INTO races
    (race_id, runner_name, race_name, start_day, start_time, total_miles, end_time)
    VALUES
        (1, 'bolt', '1600_meters', '2022-09-18', '7:00:00', 1.0, '2022-09-18 7:06:30'),
        (2, 'bolt', '5K', '2022-10-19', '11:00:00', 3.1, '2022-10-19 11:22:31'),
        (3, 'bolt', '10K', '2022-11-20', '10:00:00', 6.2, '2022-11-20 10:38:05'),
        (4, 'bolt', 'half_marathon', '2022-12-21', '6:00:00', 13.1, '2022-12-21 07:39:04'),
        (5, 'bolt', 'full_marathon', '2023-01-22', '8:00:00', 26.2, '2023-01-22 11:23:10'),
        (6, 'felix', '1600_meters', '2022-09-18', '7:00:00', 1.0, '2022-09-18 7:07:15'),
        (7, 'felix', '5K', '2022-10-19', '11:00:00', 3.1, '2022-10-19 11:30:50'),
        (8, 'felix', '10K', '2022-11-20', '10:00:00', 6.2, '2022-11-20 11:10:17'),
        (9, 'felix', 'half_marathon', '2022-12-21', '6:00:00', 13.1, '2022-12-21 08:11:57'),
        (10, 'felix', 'full_marathon', '2023-01-22', '8:00:00', 26.2, '2023-01-22 12:02:10');
