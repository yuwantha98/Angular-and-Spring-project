package com.coderdot.controllers;

import com.coderdot.dtos.AuthenticationRequest;
import com.coderdot.dtos.AuthenticationResponse;
import com.coderdot.dtos.SignupRequest;
import com.coderdot.dtos.UserDto;
import com.coderdot.entities.Users;
import com.coderdot.repositories.UserRepository;
import com.coderdot.services.auth.AuthService;
import com.coderdot.services.jwt.UserService;
import com.coderdot.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {


    private final AuthenticationManager authenticationManager;

    private final UserService userService;

    private final JwtUtil jwtUtil;

    private final UserRepository userRepository;

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> createCustomer(@RequestBody SignupRequest signupRequest) {
        if (authService.hasCustomerWithEmail(signupRequest.getEmail()))
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Email already exist. Try again with another email");
        UserDto createdUserDto = authService.createCustomer(signupRequest);
        if (createdUserDto == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not created");
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUserDto);
    }

    @PostMapping("/login")
    public AuthenticationResponse createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getEmail(),
                            authenticationRequest.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new Exception("Incorrect username or password.");
        }
        final UserDetails userDetails = userService.userDetailsService().loadUserByUsername(authenticationRequest.getEmail());
        Optional<Users> optionalUser = userRepository.findFirstByEmail(userDetails.getUsername()); //check user have in the database
        final String jwt = jwtUtil.generateToken(userDetails);
        AuthenticationResponse authenticationResponse = new AuthenticationResponse();
        if (optionalUser.isPresent()) {
            authenticationResponse.setJwt(jwt);
            authenticationResponse.setUserId(optionalUser.get().getId());
            authenticationResponse.setUserRole(optionalUser.get().getUserRole());
        }
        return authenticationResponse;
    }

}
