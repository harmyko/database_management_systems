-- Update booking total price
CREATE OR REPLACE FUNCTION update_total_price()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE BOOKING
    SET total_price = (
        SELECT COALESCE(SUM(nights * price), 0)
        FROM BOOKING_ITEM
        WHERE booking_id = COALESCE(NEW.booking_id, OLD.booking_id)
    )
    WHERE booking_id = COALESCE(NEW.booking_id, OLD.booking_id);

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_total_insert
AFTER INSERT ON BOOKING_ITEM
FOR EACH ROW
EXECUTE FUNCTION update_total_price();

CREATE TRIGGER trg_update_total_update
AFTER UPDATE ON BOOKING_ITEM
FOR EACH ROW
EXECUTE FUNCTION update_total_price();

CREATE TRIGGER trg_update_total_delete
AFTER DELETE ON BOOKING_ITEM
FOR EACH ROW
EXECUTE FUNCTION update_total_price();


-- Cannot book more rooms than available
CREATE OR REPLACE FUNCTION decrease_room_availability()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE ROOM
    SET availability = availability - NEW.nights
    WHERE room_id = NEW.room_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_decrease_availability
AFTER INSERT ON BOOKING_ITEM
FOR EACH ROW
EXECUTE FUNCTION decrease_room_availability();


-- When booking is cancelled, restore room availability
CREATE OR REPLACE FUNCTION restore_room_availability()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'Cancelled' AND OLD.status <> 'Cancelled' THEN
        UPDATE ROOM r
        SET availability = availability + bi.nights
        FROM BOOKING_ITEM bi
        WHERE r.room_id = bi.room_id
          AND bi.booking_id = NEW.booking_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_restore_availability
AFTER UPDATE ON BOOKING
FOR EACH ROW
WHEN (NEW.status = 'Cancelled' AND OLD.status <> 'Cancelled')
EXECUTE FUNCTION restore_room_availability();