package com.capstone.todo.config;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class AppStoragePropertiesTest {

    @Test
    public void rootPathShouldDefaultToStorage() {
        AppStorageProperties appStorageProperties = new AppStorageProperties();

        assertEquals(appStorageProperties.getRootPath(), "storage");
    }

    @Test
    public void setRootPathShouldUpdateValue() {
        AppStorageProperties appStorageProperties = new AppStorageProperties();

        appStorageProperties.setRootPath("custom-storage");

        assertEquals(appStorageProperties.getRootPath(), "custom-storage");
    }
}
