package ru.practicum.service;



import ru.practicum.dto.userDto.NewUserRequest;
import ru.practicum.dto.userDto.UserDto;

import java.util.List;

public interface UserService {

    UserDto saveUser(NewUserRequest request);

    List<UserDto> getUsers(List<Long> ids, Long from, Long size);

    void deleteUser(Integer id);
}
