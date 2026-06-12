package com.example.bookingservice.mapper;

import com.example.bookingservice.api.dto.BookingDTO;
import com.example.bookingservice.api.dto.BookingResponse;
import com.example.bookingservice.model.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    BookingDTO toDTO(Booking booking);

    BookingResponse toResponse(BookingDTO dto);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Booking toEntity(BookingDTO dto);

    @ObjectFactory
    default Booking createBooking(BookingDTO dto) {
        return new Booking(dto.getUserId(), dto.getResourceId());
    }
}
