# Judge Service - MyBatis 配置说明

## ✅ 已完成统一

评测服务已从 **注解方式** 改为 **XML映射文件方式**，与其他服务保持一致。

## 📁 文件结构

```
judge-service/
├── src/main/java/com/cloudoj/judge/mapper/
│   └── SubmissionMapper.java          # Mapper接口（只定义方法）
├── src/main/resources/
│   └── mapper/
│       └── SubmissionMapper.xml       # XML映射文件（SQL实现）
```

## 🔄 修改对比

### 修改前（注解方式）
```java
@Mapper
public interface SubmissionMapper {
    @Insert("INSERT INTO submission ...")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Submission submission);
    
    @Select("SELECT * FROM submission WHERE id = #{id}")
    Submission selectById(Long id);
}
```

### 修改后（XML方式）
**SubmissionMapper.java**:
```java
public interface SubmissionMapper {
    int insert(Submission submission);
    Submission selectById(Long id);
}
```

**SubmissionMapper.xml**:
```xml
<mapper namespace="com.cloudoj.judge.mapper.SubmissionMapper">
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO submission (...)
        VALUES (...)
    </insert>
    
    <select id="selectById" resultMap="BaseResultMap">
        SELECT * FROM submission WHERE id = #{id}
    </select>
</mapper>
```

## 🎯 与其他服务保持一致

| 服务 | Mapper方式 | XML文件位置 |
|------|-----------|------------|
| user-service | ✅ XML映射 | `resources/mapper/UserMapper.xml` |
| problem-service | ✅ XML映射 | `resources/mapper/ProblemMapper.xml` |
| **judge-service** | ✅ **XML映射** | `resources/mapper/SubmissionMapper.xml` |

## 📋 统一配置

所有服务的 `application.yml` 配置一致：

```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.cloudoj.model.entity
```

## ✨ 优势

1. **代码统一**: 所有服务使用相同的MyBatis配置方式
2. **SQL集中管理**: 所有SQL语句在XML中，便于维护和优化
3. **易于调试**: XML中的SQL可以直接复制到数据库工具执行
4. **类型安全**: resultMap明确定义字段映射关系
5. **复杂查询友好**: 复杂SQL在XML中更清晰

## 🔍 验证方式

启动评测服务，查看日志应该能看到：
```
Parsed mapper file: 'file [SubmissionMapper.xml]'
```

## 📝 开发规范

后续开发新功能时：
1. 在 Mapper 接口中定义方法签名
2. 在对应的 XML 文件中实现 SQL
3. 不要使用 `@Select`、`@Insert` 等注解
4. 保持与 user-service、problem-service 相同的风格

---

**统一完成时间**: 2025-12-05  
**MyBatis版本**: 3.0.4（原生MyBatis）
