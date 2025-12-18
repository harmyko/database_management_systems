CREATE MATERIALIZED VIEW daily_booking_statistics AS
SELECT 
    DATE(b.booking_date) AS booking_day,
    COUNT(DISTINCT b.booking_id) AS bookings_count,
    COUNT(DISTINCT b.guest_id) AS guests_count,
    SUM(b.total_price) AS daily_revenue,
    AVG(b.total_price) AS average_booking_value
FROM BOOKING b
WHERE b.status <> 'Cancelled'
GROUP BY DATE(b.booking_date)
ORDER BY booking_day DESC;