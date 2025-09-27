package com.example.onlyfanshop_be.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    public List<String> getUsers() {
        return List.of("User A", "User B", "User C");
    }

    @PostMapping
    public String createUser(@RequestBody String name) {
        return "Created user: " + name;
    }
}
