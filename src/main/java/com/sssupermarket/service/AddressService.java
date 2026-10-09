package com.sssupermarket.service;

import com.sssupermarket.dto.AddressRequest;
import com.sssupermarket.entity.Address;
import com.sssupermarket.entity.User;
import com.sssupermarket.repository.AddressRepository;
import com.sssupermarket.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AddressService {
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    public AddressService(AddressRepository addressRepository, UserRepository userRepository) { this.addressRepository=addressRepository; this.userRepository=userRepository; }
    @Transactional(readOnly=true) public List<Address> list(Long userId) { return addressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(userId); }
    @Transactional public Address save(Long userId, AddressRequest request) {
        User user=userRepository.findById(userId).orElseThrow();
        if(request.isDefaultAddress()) list(userId).forEach(a->{a.setDefaultAddress(false); addressRepository.save(a);});
        Address a=new Address(); a.setUser(user); copy(a,request); return addressRepository.save(a);
    }
    private void copy(Address a, AddressRequest r){a.setFullName(r.getFullName().trim());a.setMobileNumber(r.getMobileNumber().trim());a.setAddressLine1(r.getAddressLine1().trim());a.setAddressLine2(r.getAddressLine2());a.setCity(r.getCity().trim());a.setState(r.getState().trim());a.setPincode(r.getPincode().trim());a.setCountry(r.getCountry().trim());a.setDefaultAddress(r.isDefaultAddress());}
}
