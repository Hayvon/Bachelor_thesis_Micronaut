package com.example.Repo;

import com.example.Entity.HolidayRequest;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

//Crudrepository for Holidayrequest
@Repository
public interface HolidayRequestRepo extends CrudRepository<HolidayRequest, Long> {
}
