package com.how2java.es;

import com.how2java.pojo.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductESDao extends ElasticsearchRepository<Product,Integer> {
}
