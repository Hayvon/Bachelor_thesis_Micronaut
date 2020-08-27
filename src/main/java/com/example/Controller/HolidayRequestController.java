package com.example.Controller;

import com.example.Entity.HolidayRequest;
import com.example.Entity.User;
import com.example.Repo.HolidayRequestRepo;
import com.example.Repo.UserRepo;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.annotation.Error;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;

import java.util.HashMap;
import java.util.List;

@Controller("api/HolidayRequests")
public class HolidayRequestController {

    public static class NotFoundHolidayRequestException extends Exception{}
    public static class PayloadException extends Exception{}
    public static class NoSearchResultException extends Exception{}


    private HolidayRequestRepo holidayRequestRepo;
    private RuntimeService runtimeService;
    private RepositoryService repositoryService;
    private UserRepo userRepo;
    private ProcessEngine processEngine;
    private TaskService taskService;

    public HolidayRequestController(HolidayRequestRepo holidayRequestRepo, RuntimeService runtimeService, RepositoryService repositoryService, UserRepo userRepo, ProcessEngine processEngine, TaskService taskService) {
        this.holidayRequestRepo = holidayRequestRepo;
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.userRepo = userRepo;
        this.processEngine = processEngine;
        this.taskService = taskService;
    }

    String taskid = null;
    String taskAssigne = null;
    String userid = null;
    List<Task> allTasks = null;
    HashMap<String, Object> variables = new HashMap<>();
    HolidayRequest holidayRequest = null;


    //Creates HolidayRequest, saves it to DB and starts new ProcessInstance of "Urlaubsantrag"
    @Post(uri = "/create", consumes ={"application/json"},produces = {"application/json"})
    String createNewHolidayRequest(@Body() HolidayRequest newHolidayRequest) throws PayloadException{

        if (newHolidayRequest == null || newHolidayRequest.getEndDate() == null || newHolidayRequest.getStartDate() == null || newHolidayRequest.getFullName() == null || newHolidayRequest.getVorgesetzter() == null){
            throw new PayloadException();
        }

        newHolidayRequest.setStatus("Pending. Waiting for Interaction");

        holidayRequestRepo.save(newHolidayRequest);

        runtimeService.createProcessInstanceByKey("urlaubsantrag")
                .setVariable("request_id", newHolidayRequest.getId())
                .setVariable("fullName", newHolidayRequest.getFullName())
                .setVariable("vorgesetzter", newHolidayRequest.getVorgesetzter())
                .setVariable("startDate", newHolidayRequest.getStartDate())
                .setVariable("endDate", newHolidayRequest.getEndDate())
                .setVariable("status", newHolidayRequest.getStatus())
                .executeWithVariablesInReturn();
        return "Holidayrequest created!";
    }

    //Shows all HolidayRequests
    @Get(uri = "/", produces = {"application/json"})
    List<HolidayRequest> allHolidayRequests() throws NoSearchResultException {
        List<HolidayRequest> allHolidayRequests = (List<HolidayRequest>) holidayRequestRepo.findAll();

        if (allHolidayRequests.isEmpty()){
            throw new NoSearchResultException();
        }

        return  allHolidayRequests;
    }

    //Shows specific Holidayrequest
    @Get(uri = "/{id}", produces = {"application/json"})
    HolidayRequest showSpecificHolidayRequest(@PathVariable("id") long id) throws NotFoundHolidayRequestException {
        checkExistingHolidayRequest(id);
        holidayRequest =  holidayRequestRepo.findById(id).get();

        return holidayRequest;
    }

    //Assigning specific Holidayrequest
    @Post(value = "/{id}/assign", consumes ={"application/json"},produces = {"application/json"})
    String assignTask(@Body() User newUser , @PathVariable("id") long id) throws PayloadException, NotFoundHolidayRequestException {

        checkExistingHolidayRequest(id);

        if (newUser == null || newUser.getName() == null || newUser.getUserId() == 0){
            throw new PayloadException();
        }
        userRepo.findById(newUser.getUserId()).orElseThrow(() -> new PayloadException());

        taskid = null;
        userid = Long.toString(newUser.getUserId());
        allTasks = getAllTasksForSpecificRequest(id);

        if (allTasks == null){
            return "No tasks available";
        }else {
            for (Task task: allTasks) {
                taskid = task.getId();
            }
            taskService.setAssignee(taskid, userid);
            updateStatus(id, newUser.getName(), "Assigned");
            return "Task Assigned";
        }
    }

    //Approving specific Holidayrequest
    @Post(uri = "/{id}/approve", consumes ={"application/json"},produces = {"application/json"})
    String approveRequest(@Body() User newUser , @PathVariable("id") long id) throws PayloadException, NotFoundHolidayRequestException {

        checkExistingHolidayRequest(id);

        if (newUser == null || newUser.getName() == null || newUser.getUserId() == 0){
            throw new PayloadException();
        }

        userRepo.findById(newUser.getUserId()).orElseThrow(() -> new PayloadException());

        taskAssigne = null;
        taskid = null;
        userid = Long.toString(newUser.getUserId());
        variables.put("approved", "true");
        allTasks = getAllTasksForSpecificRequest(id);

        if (allTasks == null){
            return "No tasks available";
        }else {
            for (Task task : allTasks) {
                taskAssigne = task.getAssignee();
                System.out.println(taskAssigne);
                taskid = task.getId();
            }
            if (!(userid.equals(taskAssigne))){
                System.out.println(userid);
                System.out.println(taskAssigne);
                return "You are not assigned for this Task!";
            }else{
                taskService.complete(taskid, variables);
                updateStatus(id, newUser.getName(), "Approved");
                return "Task completed!";
            }
        }
    }

    //Rejecting specific Holidayrequest
    @Post(uri = "/{id}/reject", consumes ={"application/json"},produces = {"application/json"})
    String rejectRequest(@Body() User newUser , @PathVariable("id") long id) throws NotFoundHolidayRequestException, PayloadException {

        checkExistingHolidayRequest(id);

        if (newUser == null || newUser.getName() == null || newUser.getUserId() == 0){
            throw new PayloadException();
        }

        userRepo.findById(newUser.getUserId()).orElseThrow(() -> new PayloadException());

        taskAssigne = null;
        taskid = null;
        userid = Long.toString(newUser.getUserId());
        variables.put("approved", "false");
        allTasks = getAllTasksForSpecificRequest(id);

        if (allTasks == null){
            return "No tasks available";
        }else {
            for (Task task: allTasks) {
                taskAssigne = task.getAssignee();
                taskid = task.getId();
            }
            if (!(userid.equals(taskAssigne))){
                return "You are not assigned for this Task!";
            }else{
                taskService.complete(taskid, variables);
                updateStatus(id,newUser.getName(), "Rejected");
                return "Task completed!";
            }
        }
    }

    //Check if Holidayrequest with ID exists
    void checkExistingHolidayRequest(long id) throws NotFoundHolidayRequestException {
        holidayRequestRepo.findById(id).orElseThrow(() -> new NotFoundHolidayRequestException());
    }


    //Query all Tasks for a specific holidayrequest
    List<Task> getAllTasksForSpecificRequest(long id){
        allTasks = taskService.createTaskQuery().processVariableValueEquals("request_id",id).list();

        if (allTasks.size() == 0){
            return null;
        } else {
            return allTasks;
        }
    }

    //Updates status of holidayrequest
    void updateStatus(long id, String name, String status){
        holidayRequest = holidayRequestRepo.findById(id).get();
        holidayRequest.setStatus(status + " by " + name);
        holidayRequestRepo.update(holidayRequest);
    }

    //Exceptionhandling
    @Status(HttpStatus.NOT_FOUND)
    @Error(NotFoundHolidayRequestException.class)
    public void handleNotFound(){}

    @Status(HttpStatus.BAD_REQUEST)
    @Error(PayloadException.class)
    public void handleBadPayload(){}

    @Status(HttpStatus.NO_CONTENT)
    @Error(NoSearchResultException.class)
    public void handleNoSearchResult(){}


}

