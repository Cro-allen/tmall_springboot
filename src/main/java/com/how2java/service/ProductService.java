package com.how2java.service;

import com.how2java.dao.ProductDao;
import com.how2java.es.ProductESDao;
import com.how2java.pojo.Category;
import com.how2java.pojo.Product;
import com.how2java.util.Page4Navigator;
import com.how2java.util.SpringContextUtil;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@CacheConfig(cacheNames="products")
public class ProductService {
    @Autowired
    ProductDao productDao;
    @Autowired
    CategoryService categoryService;
    @Autowired
    ProductImageService productImageService;
    @Autowired
    OrderItemService orderItemService;
    @Autowired
    ReviewService reviewService;
    @Autowired
    ProductESDao productESDao;

    @CacheEvict(allEntries=true)
    public void add(Product bean){
        productDao.save(bean);
        productESDao.save(bean);
    }

    @CacheEvict(allEntries=true)
    public void delete(int id){
        productDao.delete(id);
        productESDao.delete(id);
    }

    @CacheEvict(allEntries=true)
    public void update(Product bean){
        productDao.save(bean);
        productESDao.save(bean);
    }

    @Cacheable(key="'products-one-'+ #p0")
    public Product get(int id){
        return productDao.findOne(id);
    }

    public Page4Navigator<Product> list(int cid, int start, int size, int navigatePages){
        Category category = categoryService.get(cid);
        Sort sort = new Sort(Sort.Direction.DESC, "id");
        Pageable pageable = new PageRequest(start, size, sort);
        Page<Product> pageFromJPA =productDao.findByCategory(category,pageable);
        return new Page4Navigator<>(pageFromJPA,navigatePages);
    }

    public void fill(List<Category> categorys) {
        for (Category category : categorys) {
            fill(category);
        }
    }

    //
    public void fill(Category category) {
        // 在ProductService的一个方法里调用另一个缓存管理的方法，不能够直接调用，
        // 需要通过一个工具再拿一次 ProductService，然后再调用。
        ProductService productService = SpringContextUtil.getBean(ProductService.class);
        // 这个listByCategory方法本来就是 ProductService 的方法，却不能直接调用。
        // 为什么呢？因为 springboot 的缓存机制是通过切面编程aop来实现的。
        // 从fill方法里直接调用 listByCategory 方法，aop 是拦截不到的，也就不会走缓存了。
        // 所以要通过这种 绕一绕 的方式故意诱发 aop, 这样才会想我们期望的那样走redis缓存。
        List<Product> products = productService.listByCategory(category);
        productImageService.setFirstProdutImages(products);
        category.setProducts(products);
    }

    public void fillByRow(List<Category> categorys) {
        int productNumberEachRow = 8;
        for (Category category : categorys) {
            List<Product> products =  category.getProducts();
            List<List<Product>> productsByRow =  new ArrayList<>();
            for (int i = 0; i < products.size(); i+=productNumberEachRow) {
                int size = i+productNumberEachRow;
                size= size>products.size()?products.size():size;
                List<Product> productsOfEachRow =products.subList(i, size);
                productsByRow.add(productsOfEachRow);
            }
            category.setProductsByRow(productsByRow);
        }
    }

    @Cacheable(key="'products-cid-'+ #p0.id")
    public List<Product> listByCategory(Category category){
        return productDao.findByCategoryOrderById(category);
    }

    public void setSaleAndReviewNumber(Product product) {
        int saleCount = orderItemService.getSaleCount(product);
        product.setSaleCount(saleCount);

        int reviewCount = reviewService.getCount(product);
        product.setReviewCount(reviewCount);

    }

    public void setSaleAndReviewNumber(List<Product> products) {
        for (Product product : products)
            setSaleAndReviewNumber(product);
    }

    public List<Product> search(String keyword, int start, int size) {
        initDatabase2ES();
        FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery()
                .add(QueryBuilders.matchPhraseQuery("name", keyword),
                        ScoreFunctionBuilders.weightFactorFunction(100))
                .scoreMode("sum")
                .setMinScore(10);
        Sort sort  = new Sort(Sort.Direction.DESC,"id");
        Pageable pageable = new PageRequest(start, size,sort);
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
                .withQuery(functionScoreQueryBuilder).build();

        Page<Product> page = productESDao.search(searchQuery);
        return page.getContent();
    }

    private void initDatabase2ES() {
        Pageable pageable = new PageRequest(0, 5);
        Page<Product> page =productESDao.findAll(pageable);
        if(page.getContent().isEmpty()) {
            List<Product> products= productDao.findAll();
            for (Product product : products) {
                productESDao.save(product);
            }
        }
    }
}
