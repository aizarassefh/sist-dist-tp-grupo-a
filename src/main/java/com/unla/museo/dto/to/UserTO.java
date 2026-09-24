package com.unla.museo.dto.to;

import lombok.Data;

@Data
public class UserTO {
    private Long id;

    private String email;

    private String firstName;

    private String lastName;

    private String role;

    private String phoneNumber;

    private boolean active;



}
