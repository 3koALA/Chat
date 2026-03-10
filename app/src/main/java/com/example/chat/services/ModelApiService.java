package com.example.chat.services;

import com.example.chat.beans.ModelDto;
import com.example.chat.beans.ModelsConfig;
import com.example.chat.beans.ModelsConfigRequest;
import com.example.chat.beans.ModelWithEffectiveResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ModelApiService {
    @POST("api/models/sync")
    Call<Map<String, Object>> syncModels();//admin only

    @GET("api/models")
    Call<List<ModelDto>> listModels();

    // 获取用户可见的模型列表
    @GET("api/models/visible")
    Call<List<ModelDto>> listVisibleModels();

    @GET("api/models/{id}")
    Call<ModelWithEffectiveResponse> getModelById(@Path("id") long id, @retrofit2.http.Header("X-User-Id") Long xUserId);
    
    @GET("api/models/name/{modelName}")
    Call<ModelDto> getModelByName(@Path("modelName") String modelName);
    
    @GET("api/models/family/{family}")
    Call<List<ModelDto>> getModelsByFamily(@Path("family") String family);
    
    @GET("api/models/check/{modelName}")
    Call<Map<String, Boolean>> checkModelExists(@Path("modelName") String modelName);

    // Create user config for a model
    @retrofit2.http.POST("api/models/{modelId}/configs")
    Call<java.util.Map<String, Object>> createModelConfig(@Path("modelId") long modelId, @Body ModelsConfigRequest req, @retrofit2.http.Header("X-User-Id") Long xUserId);

    // Get effective config for a model for current user (optional header)
    @GET("api/models/{modelId}/configs/effective")
    Call<ModelsConfig> getEffectiveConfig(@Path("modelId") long modelId, @retrofit2.http.Header("X-User-Id") Long xUserId);

    // List all configs for a model
    @GET("api/models/{modelId}/configs")
    Call<java.util.List<ModelsConfig>> listConfigs(@Path("modelId") long modelId);

    // Get config by configId
    @GET("api/models/configs/{configId}")
    Call<ModelsConfig> getConfigById(@Path("configId") long configId);

    // Update config by configId
    @retrofit2.http.PUT("api/models/configs/{configId}")
    Call<java.util.Map<String,Object>> updateConfig(@Path("configId") long configId, @Body ModelsConfigRequest req, @retrofit2.http.Header("X-User-Id") Long xUserId);

    // Admin toggle display
    @retrofit2.http.PUT("api/models/admin/configs/{configId}/display")
    Call<java.util.Map<String,Object>> adminToggleDisplay(@Path("configId") long configId, @Body java.util.Map<String,Object> body, @retrofit2.http.Header("X-User-Id") Long xUserId);

    // Admin: toggle model display on user page
    @retrofit2.http.PUT("api/models/admin/{modelId}/display")
    Call<java.util.Map<String,Object>> adminToggleModelDisplay(@Path("modelId") long modelId, @Body java.util.Map<String,Object> body, @retrofit2.http.Header("X-User-Id") Long xUserId);

    // List configs by user (admin)
    @GET("api/users/{userId}/configs")
    Call<java.util.List<ModelsConfig>> listConfigsByUser(@Path("userId") long userId, @retrofit2.http.Header("X-User-Id") Long xUserId);

    // Delete config by id (admin)
    @retrofit2.http.DELETE("api/models/configs/{configId}")
    Call<java.util.Map<String,Object>> deleteConfigById(@Path("configId") long configId, @retrofit2.http.Header("X-User-Id") Long xUserId);

    // Delete config by modelId and configId
    @retrofit2.http.DELETE("api/models/{modelId}/configs/{configId}")
    Call<java.util.Map<String,Object>> deleteConfigByModelAndConfig(@Path("modelId") long modelId, @Path("configId") long configId, @retrofit2.http.Header("X-User-Id") Long xUserId);

    // Admin-configs endpoints (documented in API_DOC.md)
    @GET("api/admin-configs")
    Call<java.util.List<java.util.Map<String,Object>>> listAdminConfigs();

    @GET("api/admin-configs/admin/{adminId}")
    Call<java.util.List<java.util.Map<String,Object>>> listAdminConfigsByAdmin(@Path("adminId") long adminId);

    @GET("api/admin-configs/config/{configId}")
    Call<java.util.List<java.util.Map<String,Object>>> listAdminConfigsByConfig(@Path("configId") long configId);

    // 创建配置模板（管理员）
    @POST("api/admin-configs")
    Call<java.util.Map<String,Object>> createTemplate(@Body java.util.Map<String,Object> body, @retrofit2.http.Header("X-User-Id") Long xUserId);

    // 获取管理员模板列表（带配置详情）
    @GET("api/admin-configs/admin/{adminId}/with-configs")
    Call<java.util.List<java.util.Map<String,Object>>> listTemplatesWithConfigs(@Path("adminId") long adminId);

    // 获取所有公开模板（用户可见）
    @GET("api/admin-configs/public")
    Call<java.util.List<java.util.Map<String,Object>>> listPublicTemplates();

    // 更新模型信息
    @retrofit2.http.PUT("api/models/{id}")
    Call<java.util.Map<String,Object>> updateModel(@Path("id") long id, @Body java.util.Map<String,Object> body, @retrofit2.http.Header("X-User-Id") Long xUserId);
}
