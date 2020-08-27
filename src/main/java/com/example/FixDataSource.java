package com.example;

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.camunda.bpm.engine.ProcessEngine;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

@Singleton
public class FixDataSource {

    @Inject
    ProcessEngine processEngine;

    @EventListener
    void onStartup(ServerStartupEvent event) {
        System.out.println("============== foo");
        DataSource dataSource = processEngine.getProcessEngineConfiguration().getDataSource();
        PooledDataSource pds = (PooledDataSource) dataSource;
        pds.setPoolMaximumActiveConnections(20);
    }
}