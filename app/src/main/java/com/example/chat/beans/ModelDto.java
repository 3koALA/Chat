package com.example.chat.beans;

public class ModelDto {
    private long id;
    private String modelName;
    private String modelDigest;
    private long modelSize;
    private String parameterSize;
    private String quantizationLevel;
    private String modelFamily;
    private String modifiedAt;
    private String createdAt;
    private Boolean isDisplay;
    private String avatarUrl;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getModelDigest() { return modelDigest; }
    public void setModelDigest(String modelDigest) { this.modelDigest = modelDigest; }

    public long getModelSize() { return modelSize; }
    public void setModelSize(long modelSize) { this.modelSize = modelSize; }

    public String getParameterSize() { return parameterSize; }
    public void setParameterSize(String parameterSize) { this.parameterSize = parameterSize; }

    public String getQuantizationLevel() { return quantizationLevel; }
    public void setQuantizationLevel(String quantizationLevel) { this.quantizationLevel = quantizationLevel; }

    public String getModelFamily() { return modelFamily; }
    public void setModelFamily(String modelFamily) { this.modelFamily = modelFamily; }

    public String getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(String modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Boolean getIsDisplay() { return isDisplay; }
    public void setIsDisplay(Boolean isDisplay) { this.isDisplay = isDisplay; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
