CREATE UNIQUE INDEX idx_guest_email
    ON GUEST(email, guest_id);

CREATE INDEX idx_booking_status
    ON BOOKING(status);

CREATE INDEX idx_booking_date
    ON BOOKING(booking_date);

CREATE INDEX idx_booking_item_booking
    ON BOOKING_ITEM(booking_id);

CREATE INDEX idx_rates_room
    ON RATES(room_id);