package org.example.persistence.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import org.example.persistence.EntityCache;
import org.example.persistence.accessor.Accessor;
import org.example.util.Identity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 手动配置环境，用Ideal测试吧
 */
@EnabledIfSystemProperty(named = "mongoDbTest", matches = "true")
public class EntityCacheTest {

  /* The mongo client */
  private static long expireTime = 1;
  private static MongoClient client;
  private static EntityCache<Long, Avatar> avatarCache;

  @BeforeAll
  static void beforeAll() {
    client = MongoClients.create(
        "mongodb://game:123456@localhost:27017/test?authSource=admin&connecttimeoutms=30000&sockettimeoutms=300000&appName=appName&maxPoolSize=20&minPoolSize=1");
    Accessor accessor = new MongoDbAccessor(new MongoTemplate(client, "test"));
    avatarCache = new EntityCache<>(expireTime, Avatar.class, accessor, ForkJoinPool.commonPool());
  }

  @AfterAll
  static void afterAll() {
    if (client != null) {
      //清空测试数据
      MongoDatabase database = client.getDatabase("test");
      database.getCollection("avatar").drop();
      client.close();
    }
  }

  @Test
  void readWriteTest() throws Exception {
    Long avatarId = 2L;
    String name = "Test";
    avatarCache.getOrCreate(avatarId, id -> {
      Avatar a = new Avatar();
      a.id(id);
      a.name(name);
      return a;
    });

    TimeUnit.MILLISECONDS.sleep(expireTime * 10);
    Assertions.assertNull(avatarCache.getIfPresent(avatarId));

    Avatar expected = new Avatar();
    expected.id(avatarId).name(name);
    Assertions.assertEquals(expected, avatarCache.get(avatarId));
  }

  @Test
  void multiReadWriteTest() throws Exception {
    String namePrefix = "Test";
    for (long i = 10; i < 20; i++) {
      avatarCache.getOrCreate(i, id -> {
        Avatar a = new Avatar();
        a.id(id);
        a.name(namePrefix + id);
        return a;
      });
    }

    TimeUnit.MILLISECONDS.sleep(expireTime * 10);
    for (long i = 10; i < 20; i++) {
      Assertions.assertNull(avatarCache.getIfPresent(i));
    }

    for (long i = 10; i < 20; i++) {
      Avatar expected = new Avatar();
      expected.id(i).name(namePrefix + i);
      Assertions.assertEquals(expected, avatarCache.get(i));
    }
  }

  @Document(collation = "avatar")
  public static class Avatar implements Identity<Long> {

    @Id
    /* 唯一ID */
    private Long id;
    /* 名字 */
    private String name;

    @Override
    public Long id() {
      return id;
    }

    public Avatar id(long id) {
      this.id = id;
      return this;
    }

    public String name() {
      return name;
    }

    public Avatar name(String name) {
      this.name = name;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Avatar avatar = (Avatar) o;
      return Objects.equals(id, avatar.id) && Objects.equals(name, avatar.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, name);
    }
  }

}
