-- Guests

INSERT INTO GUEST (first_name, last_name, email) VALUES
('John',    'Smith',      'john.smith@email.com'),
('Peter',   'Johnson',    'peter.johnson@gmail.com'),
('Anna',    'Williams',   'anna.williams@yahoo.com'),
('Michael', 'Brown',      'michael@mail.com');

-- Rooms

INSERT INTO ROOM (room_number, price_per_night, availability, description) VALUES
('101', 89.99,  10, 'Standard Single Room with City View'),
('102', 129.99, 8,  'Deluxe Double Room with Balcony'),
('201', 159.99, 5,  'Suite with King Bed and Living Area'),
('202', 199.99, 3,  'Presidential Suite with Panoramic View'),
('301', 79.99,  12, 'Economy Twin Room'),
('302', 109.99, 7,  'Standard Double Room'),
('401', 249.99, 2,  'Penthouse Suite with Jacuzzi'),
('402', 99.99,  9,  'Family Room with Two Double Beds');



-- Bookings

INSERT INTO BOOKING (guest_id, status, country, city, postal_code, address_line) VALUES
(1, 'CheckedOut', 'USA', 'New York',      '10001', 'Broadway 123'),
(2, 'CheckedIn',  'USA', 'Los Angeles',   '90001', 'Sunset Blvd 456'),
(1, 'Confirmed',  'USA', 'Chicago',       '60601', 'Michigan Ave 789'),
(3, 'CheckedOut', 'USA', 'Miami',         '33101', 'Ocean Drive 321'),
(4, 'New',        'USA', 'San Francisco', '94101', 'Market St 654');



-- Booking Items

INSERT INTO BOOKING_ITEM (booking_id, item_number, room_id, nights, price) VALUES
(1, 1, 1, 3, 89.99),
(1, 2, 5, 3, 79.99),

(2, 1, 3, 5, 159.99),
(2, 2, 6, 5, 109.99),

(3, 1, 2, 2, 129.99),
(3, 2, 8, 2, 99.99),

(4, 1, 4, 1, 199.99),
(4, 2, 7, 1, 249.99),

(5, 1, 1, 4, 89.99);


-- Ratings

-- John (guest_id = 1)
INSERT INTO RATES (guest_id, room_id, rating, review) VALUES
(1, 1, 5, 'Excellent room, very clean and comfortable.'),
(1, 5, 4, 'Good value for money, but a bit small.');

-- Peter (guest_id = 2)
INSERT INTO RATES (guest_id, room_id, rating, review) VALUES
(2, 3, 5, 'Amazing suite, loved the living area.'),
(2, 6, 3, 'Average room, could use some renovation.');

-- Anna (guest_id = 3)
INSERT INTO RATES (guest_id, room_id, rating, review) VALUES
(3, 4, 5, 'Best hotel experience ever! Presidential suite was fantastic.'),
(3, 7, 4, 'Great penthouse, but a bit pricey.');