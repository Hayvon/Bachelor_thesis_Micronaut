package com.example.Repo;

import com.example.Entity.User;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

//Crudrepository for User
@Repository
public interface UserRepo extends CrudRepository<User, Long> {
}
