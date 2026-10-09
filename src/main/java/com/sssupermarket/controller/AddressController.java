package com.sssupermarket.controller;

import com.sssupermarket.dto.AddressRequest;
import com.sssupermarket.security.CurrentUser;
import com.sssupermarket.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressService addressService; private final CurrentUser currentUser;
    public AddressController(AddressService addressService, CurrentUser currentUser){this.addressService=addressService;this.currentUser=currentUser;}
    @GetMapping public ResponseEntity<?> list(){return ResponseEntity.ok(addressService.list(currentUser.getId()));}
    @PostMapping public ResponseEntity<?> save(@Valid @RequestBody AddressRequest request){return ResponseEntity.ok(addressService.save(currentUser.getId(),request));}
}
