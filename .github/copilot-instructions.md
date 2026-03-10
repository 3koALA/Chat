项目快速上手（面向 AI 编码助手）

**目标**: 帮助 AI 代理快速在本 Android (Java) 项目中变得可生产，指明架构要点、关键文件、常用网络客户端、以及实现 Model 管理页面时应遵循的可发现模式。

**项目概览**
- **类型**: Android 原生应用（Java）
- **网络层**: 使用 `retrofit2` + `okhttp`；后端统一通过 `BackendRetrofitClient`（`BaseUrl.BACK`）访问；本地 Ollama 调用使用 `OllamaApiService`。
- **模型管理要点**: 应用在 `beans/ModelInfo.java`、`beans/ModelDetails.java` 和 `ModelManager.java` 中定义了模型信息解析与运行时选择的单例管理。

**关键文件/目录（高优先级）**
- `app/src/main/java/com/example/chat/ModelManagementActivity.java` — 目标 Activity（目前为壳）。
- `app/src/main/res/layout/activity_model_management.xml` — 对应布局（目前为空，需要添加 RecyclerView、按钮等）。
- `app/src/main/java/com/example/chat/beans/ModelDetails.java` — 包含从 Ollama `/api/show` 返回解析逻辑（`fromJson`、参数解析、getTemperature/TopP/numPredict、system prompt 提取）。
- `app/src/main/java/com/example/chat/ModelManager.java` — 运行时模型选择与默认参数存储（temperature、top_p、system prompt 等）。
- `app/src/main/java/com/example/chat/retrofitclient/BackendRetrofitClient.java` — 后端统一 Retrofit 客户端（使用 `BaseUrl.BACK`）。
- `app/src/main/java/com/example/chat/services/OllamaApiService.java` — Ollama 本地 API（`api/tags`, `api/show`, `api/chat` 等）。

**实施 Model 管理页（设计提示，具体到本仓库）**
- 用户界面建议：顶部提供 `同步模型` 按钮触发 `POST /api/models/sync`；主区域使用 `RecyclerView` 列表展示 `/api/models` 返回的模型；每项显示 `modelName`、`modelFamily`、`parameterSize`、修改时间；点击项进入详情或在弹窗展示 `GET /api/models/{id}` 的详细信息。
- 验证/辅助接口：在新建/输入模型名时可使用 `GET /api/models/check/{modelName}` 进行存在性校验；支持按 `family` 筛选（`GET /api/models/family/{family}`）并提供按 name 查找（`GET /api/models/name/{modelName}`）。
- 后端调用位置：新增 `services/ModelApiService.java`，并使用现有的 `BackendRetrofitClient.getClient()` 创建实例。

**实现细节要点（示例代码提示）**
- Retrofit 接口样例方法签名：

```java
@POST("/api/models/sync")
Call<Map<String,Object>> syncModels();

@GET("/api/models")
Call<List<ModelDto>> listModels();

@GET("/api/models/{id}")
Call<ModelDto> getModel(@Path("id") long id);

@GET("/api/models/check/{modelName}")
Call<Map<String, Boolean>> checkModel(@Path("modelName") String name);
```

- 使用约定：所有后台请求使用 `BackendRetrofitClient.getClient()`，在 Activity 中以异步 `enqueue` 方式处理响应，并在主线程更新 UI（使用 `runOnUiThread` 或 `Activity` 的 callback）。
- 对于模型详情，若后端未提供完全字段，可结合 `OllamaApiService` 的 `getModelDetails`（`api/show`）来解析更丰富的 `ModelDetails`，解析逻辑位于 `beans/ModelDetails.java`。

**项目风格与约定**
- 代码使用 Java 8 风格，尽量保持与现有类命名对齐（`XxxActivity`, `XxxService`, `XxxManager`）。
- 全局 URL 使用 `BaseUrl.BACK` 与 `BackendRetrofitClient`，不要硬编码地址。Ollama 本地调用使用 `OllamaApiService`（相对路径 `api/...`）。
- 模型运行时状态保存在 `ModelManager` 中：在加载模型详情后应调用 `ModelManager.setModelDetails(details)` 更新运行时参数。

**调试与常用命令**
- 构建与运行（Windows + Android Studio / Gradle wrapper）：

```pwsh
./gradlew assembleDebug
./gradlew installDebug
```

（在 Windows PowerShell 中运行 `.\\gradlew`）

**注意事项 / 限制**
- 本仓库已包含 Ollama 本地调用的解析逻辑，但后端 `/api/models` 系列还未实现客户端接口（请添加 `ModelApiService`）。
- `ModelDetails.fromJson` 假定 Ollama 返回的字段名与当前解析一致；如果后端返回不同结构，应优先使用后端 DTO 并映射到 `ModelDetails`。

如果你希望，我可以：
- 自动添加 `services/ModelApiService.java` 样例接口；
- 在 `activity_model_management.xml` 中添加一个基础布局（`RecyclerView` + `Sync` 按钮）；
- 在 `ModelManagementActivity` 中加入示例网络调用与列表渲染代码。

请告诉我是否现在生成上述实现草案（接口 + 布局 + Activity 示例）。
