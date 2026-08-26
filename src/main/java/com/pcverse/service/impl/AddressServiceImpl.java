package com.pcverse.service.impl;

import com.pcverse.dto.request.CreateAddressRequest;
import com.pcverse.dto.request.UpdateAddressRequest;
import com.pcverse.dto.response.AddressResponse;
import com.pcverse.entity.Address;
import com.pcverse.entity.User;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.AddressMapper;
import com.pcverse.repository.AddressRepository;
import com.pcverse.repository.UserRepository;
import com.pcverse.service.AddressService;
import com.pcverse.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public List<AddressResponse> getMyAddresses(Jwt jwt) {
        User user = userService.ensureUserExistsFromToken(jwt);

        return addressRepository.findAllByUserId(user.getId())
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse createMyAddress(
            Jwt jwt,
            CreateAddressRequest request
    ) {
        User user = resolveAndLockCurrentUser(jwt);
        List<Address> currentAddresses =
                addressRepository.findAllByUserId(user.getId());

        normalizeDefaultAddress(currentAddresses, firstOrNull(currentAddresses));

        // Nếu danh sách địa chỉ rỗng thì sẽ lập tức hiểu là địa chỉ đang được tạo là Default address
        boolean makeDefault = currentAddresses.isEmpty()
                || Boolean.TRUE.equals(request.isDefault());

        if (makeDefault) {
            clearDefaultAddress(currentAddresses);
        }

        Address address = new Address();
        address.setUser(user);
        address.setRecipientName(request.recipientName());
        address.setRecipientPhone(request.recipientPhone());
        address.setProvince(request.province());
        address.setDistrict(request.district());
        address.setWard(request.ward());
        address.setStreetDetail(request.streetDetail());
        address.setIsDefault(makeDefault);

        return addressMapper.toResponse(addressRepository.saveAndFlush(address));
    }

    @Override
    @Transactional
    public AddressResponse updateMyAddress(
            Jwt jwt,
            UUID addressId,
            UpdateAddressRequest request
    ) {
        User user = resolveAndLockCurrentUser(jwt);
        Address address = findOwnedAddress(addressId, user.getId());
        List<Address> currentAddresses =
                addressRepository.findAllByUserId(user.getId());

        applyPatch(address, request);

        if (Boolean.TRUE.equals(request.isDefault())) {
            makeOnlyDefault(currentAddresses, address);
        } else {
            normalizeDefaultAddress(currentAddresses, address);
        }

        return addressMapper.toResponse(addressRepository.saveAndFlush(address));
    }

    @Override
    @Transactional
    public void deleteMyAddress(Jwt jwt, UUID addressId) {
        User user = resolveAndLockCurrentUser(jwt);
        Address address = findOwnedAddress(addressId, user.getId());
        List<Address> currentAddresses =
                addressRepository.findAllByUserId(user.getId());

        // Ghi nhận địa chỉ bị xoá có phải địa chỉ mặc định hay không
        boolean deletingDefault = Boolean.TRUE.equals(address.getIsDefault());

        currentAddresses.removeIf(current -> current.getId().equals(addressId));
        addressRepository.delete(address);

        if (!currentAddresses.isEmpty()) {

            // Danh sách còn địa chỉ nên chọn phần tử đầu tiên làm ứng viên default thay thế.
            Address replacement = firstOrNull(currentAddresses);

            // Kiểm tra đang xoá address default OR không có địa chỉ nào trong danh sách address là default
            if (deletingDefault || currentAddresses.stream().noneMatch(this::isDefault)) {
                makeOnlyDefault(currentAddresses, replacement);
            } else {
                // Giữ đúng một địa chỉ mặc định nếu dữ liệu cũ có nhiều default.
                normalizeDefaultAddress(currentAddresses, replacement);
            }
        }

        addressRepository.flush();
    }

    private User resolveAndLockCurrentUser(Jwt jwt) {
        User resolvedUser = userService.ensureUserExistsFromToken(jwt);

        return userRepository.findByIdForUpdate(resolvedUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Address findOwnedAddress(UUID addressId, UUID userId) {
        return addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));
    }

    private void applyPatch(Address address, UpdateAddressRequest request) {
        if (request.recipientName() != null) {
            address.setRecipientName(request.recipientName());
        }
        if (request.recipientPhone() != null) {
            address.setRecipientPhone(request.recipientPhone());
        }
        if (request.province() != null) {
            address.setProvince(request.province());
        }
        if (request.district() != null) {
            address.setDistrict(request.district());
        }
        if (request.ward() != null) {
            address.setWard(request.ward());
        }
        if (request.streetDetail() != null) {
            address.setStreetDetail(nullIfBlank(request.streetDetail()));
        }
    }

    private void normalizeDefaultAddress(List<Address> addresses, Address fallback) {
        if (addresses.isEmpty()) {
            return;
        }

        Address selectedDefault = addresses.stream()
                .filter(this::isDefault)
                .findFirst()
                .orElse(fallback);

        makeOnlyDefault(addresses, selectedDefault);
    }

    private void makeOnlyDefault(List<Address> addresses, Address selected) {
        for (Address address : addresses) {
            address.setIsDefault(
                    selected != null
                            && address.getId().equals(selected.getId())
            );
        }
    }

    private void clearDefaultAddress(List<Address> addresses) {
        addresses.forEach(address -> address.setIsDefault(false));
    }

    private boolean isDefault(Address address) {
        return Boolean.TRUE.equals(address.getIsDefault());
    }

    private Address firstOrNull(List<Address> addresses) {
        return addresses.isEmpty() ? null : addresses.get(0);
    }

    private String nullIfBlank(String value) {
        return value.isBlank() ? null : value;
    }
}
