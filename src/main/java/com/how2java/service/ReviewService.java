package com.how2java.service;

import com.how2java.dao.ReviewDao;
import com.how2java.pojo.Product;
import com.how2java.pojo.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@CacheConfig(cacheNames = "reviews")
public class ReviewService {
    @Autowired
    ReviewDao reviewDao;
    @Autowired
    ProductService productService;

    @CacheEvict(allEntries = true)
    public void add(Review review) {
        reviewDao.save(review);
    }

    @Cacheable(key = "'reviews-pid-'+ #p0.id")
    public List<Review> list(Product product) {
        List<Review> result = reviewDao.findByProductOrderByIdDesc(product);
        return result;
    }

    @Cacheable(key = "'reviews-count-pid-'+ #p0.id")
    public int getCount(Product product) {
        return reviewDao.countByProduct(product);
    }
}
