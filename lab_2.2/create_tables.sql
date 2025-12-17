-- Guest
CREATE TABLE GUEST (
    guest_id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
        CONSTRAINT guest_email_format
        CHECK (email LIKE '%@%.%')
);

-- Room
CREATE TABLE ROOM (
    room_id SERIAL PRIMARY KEY,
    room_number VARCHAR(10) NOT NULL,
    price_per_night DECIMAL(10, 2) NOT NULL
        CONSTRAINT room_price_positive
        CHECK(price_per_night > 0),
    availability INTEGER NOT NULL
        CONSTRAINT room_availability_valid
        CHECK(availability >= 0),
    description TEXT
);

-- Booking
CREATE TABLE BOOKING (
   booking_id SERIAL PRIMARY KEY,
   guest_id INTEGER NOT NULL,
   booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   status VARCHAR(20) DEFAULT 'New'
        CONSTRAINT booking_status_valid_value
        CHECK (status IN ('New', 'Confirmed', 'CheckedIn', 'CheckedOut', 'Cancelled')),
   total_price DECIMAL(10, 2) DEFAULT 0,
   country VARCHAR(50),
   city VARCHAR(50),
   postal_code VARCHAR(10),
   address_line VARCHAR(200),
   FOREIGN KEY (guest_id)
        REFERENCES GUEST
        ON DELETE CASCADE 
);

-- Booking_Item
CREATE TABLE BOOKING_ITEM (
    booking_id INTEGER NOT NULL,
    item_number INTEGER NOT NULL,
    room_id INTEGER NOT NULL,
    nights INTEGER NOT NULL
        CONSTRAINT booking_item_nights_positive
        CHECK (nights > 0),
    price DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (booking_id, item_number),
    FOREIGN KEY (booking_id)
        REFERENCES BOOKING(booking_id)
        ON DELETE CASCADE,
    FOREIGN KEY (room_id)
        REFERENCES ROOM(room_id)
);

-- Rates
CREATE TABLE RATES (
    guest_id INTEGER NOT NULL,
    room_id INTEGER NOT NULL,
    rating INTEGER NOT NULL
        CONSTRAINT rating_range_1_5
        CHECK (rating BETWEEN 1 AND 5),
    review TEXT,
    PRIMARY KEY (guest_id, room_id),
    FOREIGN KEY (guest_id)
        REFERENCES GUEST(guest_id)
        ON DELETE CASCADE,
    FOREIGN KEY (room_id)
        REFERENCES ROOM(room_id)
        ON DELETE CASCADE
);