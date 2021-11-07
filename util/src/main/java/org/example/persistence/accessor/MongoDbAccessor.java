package org.example.persistence.accessor;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * 对MongoTemplate进行简单的封装
 *
 * @author ZJP
 * @since 2021年11月05日 15:23:56
 **/
public class MongoDbAccessor<PK, T> implements Accessor<PK, T> {

  private static final String ID = "_id";

  /** spring的封装的mongodb操作 */
  private MongoTemplate template;

  public MongoDbAccessor(MongoTemplate template) {
    this.template = template;
  }

  @Override
  public T load(Class<T> entityClass, PK key) {
    return template.findById(key, entityClass);
  }

  @Override
  public T delete(Class<T> entityClass, PK key) {
    return template.findAndRemove(Query.query(Criteria.where(ID).is(key)), entityClass);
  }

  @Override
  public T save(T entity) {
    return template.save(entity);
  }
}
