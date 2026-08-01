package com.pcverse.dto.response;

import lombok.Builder;

@Builder
public record AddressResponse(

        String id,

        String recipientName,

        String recipientPhone,

        String province,

        String district,

        String ward,

        String streetDetail,

        boolean isDefault
) {
}
