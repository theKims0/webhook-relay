package com.abdul.relay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProjectRequestDTO {
    @NotBlank(message = "Name tidak boleh kosong")
    @Size(min = 3, max = 50, message = "Name harus 3-50 karakter")
    private String name;

    @NotBlank(message = "Description tidak boleh kosong")
    private String description;

    private Boolean isActive;

    @NotBlank(message = "Public Url tidak boleh kosong")
    private String publicUrl;

    public ProjectRequestDTO(){
    }
    public ProjectRequestDTO(String name, String description, Boolean isActive, String publicUrl){
        this.description = description;
        this.name = name;
        this.isActive = isActive;
        this.publicUrl = publicUrl;
    }
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
    public String getDescription(){
        return description;
    }
    public void setDescription(String description){
        this.description = description;
    }
    public Boolean getIsActive(){
        return isActive;
    }
    public void setIsActive(Boolean isActive){
        this.isActive = isActive;
    }
    public String getPublicUrl(){
        return publicUrl;
    }
    public void setPublicUrl(String publicUrl){
        this.publicUrl = publicUrl;
    }
}
