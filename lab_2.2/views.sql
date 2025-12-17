CREATE VIEW guest_booking_statistics AS
SELECT
    g.guest_id,
    g.first_name,
    g.last_name,
    g.email,
    COUNT(b.booking_id) AS total_bookings,
    COALESCE(SUM(b.total_price), 0) AS total_spent,
    MAX(b.booking_date) AS last_booking,
    COUNT(r.room_id) AS reviews_count
FROM
    GUEST g
LEFT JOIN BOOKING b
    ON g.guest_id = b.guest_id
LEFT JOIN RATES r
    ON g.guest_id = r.guest_id
GROUP BY
    g.guest_id,
    g.first_name,
    g.last_name,
    g.email;


CREATE VIEW room_statistics AS
SELECT 
    r.room_id,
    r.room_number,
    r.price_per_night,
    r.availability,
    COUNT(DISTINCT bi.booking_id) AS times_booked,
    COALESCE(SUM(bi.nights), 0) AS total_nights_booked,
    COALESCE(SUM(bi.nights * bi.price), 0) AS revenue,
    COUNT(rt.rating) AS ratings_count,
    COALESCE(AVG(rt.rating), 0) AS average_rating
FROM 
    ROOM r
LEFT JOIN BOOKING_ITEM bi
    ON r.room_id = bi.room_id
LEFT JOIN RATES rt
    ON r.room_id = rt.room_id
GROUP BY
    r.room_id,
    r.room_number,
    r.price_per_night,
    r.availability
ORDER BY
    total_nights_booked DESC;


CREATE VIEW top_rated_rooms AS
SELECT 
    r.room_id,
    r.room_number,
    r.price_per_night,
    COUNT(rt.rating) AS ratings_count,
    AVG(rt.rating) AS average_rating
FROM ROOM r
JOIN RATES rt
    ON r.room_id = rt.room_id
GROUP BY
    r.room_id,
    r.room_number,
    r.price_per_night
HAVING COUNT(rt.rating) >= 2
ORDER BY
    average_rating DESC,
    ratings_count DESC;