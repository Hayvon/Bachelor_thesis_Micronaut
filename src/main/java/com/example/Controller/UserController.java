package com.example.Controller;

import com.example.Entity.User;
import com.example.Repo.UserRepo;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.annotation.Error;

import java.util.List;

@Controller("/api/Users")
public class UserController {

    public static class NotFoundException extends Exception{}
    public static class PayloadException extends Exception{}
    public static class NoSearchResultException extends Exception{}

    private UserRepo userRepo;

    public UserController(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    //Shows all Users
    @Get(uri = "/", produces = {"application/json"})
    List<User> allUsers() throws NoSearchResultException {
        List<User> userList = (List<User>) userRepo.findAll();

        if (userList.isEmpty()){
            throw new NoSearchResultException();
        }

        return userList;
    }

    //Shows specific User
    @Get(uri = "/{id}", produces = {"application/json"})
    User findUser(@PathVariable("id") Long id) throws NotFoundException{
        return userRepo.findById(id).orElseThrow(() -> new NotFoundException());
    }

    //Creating user
    @Post(value = "/create", consumes ={"application/json"},produces = {"application/json"})
    String createUser(@Body() User newUser) throws PayloadException {

        if (newUser == null || newUser.getName() == null){
            System.out.println("Test");
            throw new PayloadException();
        }

        userRepo.save(newUser);
        return "User created";
    }

    //Exceptionhandling
    @Status(HttpStatus.NOT_FOUND)
    @Error(NotFoundException.class)
    public void handleNotFound(){}

    @Status(HttpStatus.BAD_REQUEST)
    @Error(PayloadException.class)
    public void handleBadPayload(){}

    @Status(HttpStatus.NO_CONTENT)
    @Error(NoSearchResultException.class)
    public void handleNoSearchResult(){}

}
