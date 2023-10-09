package com.how2java.service;

import com.how2java.dao.CategoryDao;
import com.how2java.pojo.Category;
import com.how2java.pojo.Product;
import com.how2java.util.Page4Navigator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@CacheConfig(cacheNames = "categories")
public class CategoryService {
    @Autowired
    CategoryDao categoryDao;

    // 无参的list方法，查询所有数据
    @Cacheable(key = "'categories-all'")
    public List<Category> list() {
        Sort sort = new Sort(Sort.Direction.DESC, "id");
        return categoryDao.findAll(sort);
    }

    // 带参的list方法，分页查询数据
    @Cacheable(key = "'categories-page-'+#p0+ '-' + #p1")
    public Page4Navigator<Category> list(int start, int size, int navigatePages) {
        Sort sort = new Sort(Sort.Direction.DESC, "id");
        Pageable pageable = new PageRequest(start, size, sort);
        Page pageFromJPA = categoryDao.findAll(pageable);

        return new Page4Navigator<>(pageFromJPA, navigatePages);
    }

    // 增加数据
    @CacheEvict(allEntries=true)
    public void add(Category bean) {
        categoryDao.save(bean);
    }

    // 删除数据
    @CacheEvict(allEntries=true)
    public void delete(int id) {
        categoryDao.delete(id);
    }

    // 提供get方法
    @Cacheable(key="'categories-one-'+ #p0")
    public Category get(int id) {
        Category c = categoryDao.findOne(id);
        return c;
    }

    // 修改数据
    @CacheEvict(allEntries=true)
    public void update(Category bean) {
        categoryDao.save(bean);
    }

    // removeCategoryFromProduct 这个方法的用处是删除Product对象上的分类
    // 为什么要删除呢？因为在对分类做序列还转换为 json 的时候,会遍历里面的 products,
    // 然后遍历出来的产品上,又会有分类,接着就开始子子孙孙无穷溃矣地遍历了。
    // 在这里去掉就没事了，只要在前端业务上没有通过产品获取分类的业务，去掉也没有关系。
    public void removeCategoryFromProduct(List<Category> cs) {
        for (Category category : cs) {
            removeCategoryFromProduct(category);
        }
    }

    public void removeCategoryFromProduct(Category category) {
        List<Product> products = category.getProducts();
        if (null != products) {
            for (Product product : products) {
                product.setCategory(null);
            }
        }

        List<List<Product>> productsByRow = category.getProductsByRow();
        if (null != productsByRow) {
            for (List<Product> ps : productsByRow) {
                for (Product p : ps) {
                    p.setCategory(null);
                }
            }
        }
    }
}
